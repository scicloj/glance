(ns scicloj.glance
  (:require [scicloj.clay.v2.make :as clay]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.dataset :as ds]
            [tablecloth.column.api :as tcc]
            [scicloj.kindly-render.note.to-hiccup]
            [scicloj.kindly-advice.v1.api :as advice]))

(def ^:private kolls
  #{:kind/map :kind/vector :kind/set :kind/seq})

(defn- hiccup? [x]
  (and (vector? x)
       (keyword? (first x))))

(defn- column-general-type
  "Returns the general type category of a column: :quantitative, :temporal, :categorical, or :identity."
  [col]
  (cond
    (tcc/typeof? col :numerical) :quantitative
    (tcc/typeof? col :datetime) :temporal
    (tcc/typeof? col :logical) :categorical
    (tcc/typeof? col :textual) :categorical
    :else :identity))

(defn- select-geometry-single
  "Select visualization geometry for a single column based on its type.
   Rules:
   - Identity → :identity
   - Quantitative → :histogram
   - Temporal → :histogram
   - Categorical → :bar
   - Default → :bar"
  [col]
  (let [general-type (column-general-type col)]
    (case general-type
      :identity :identity
      :quantitative :histogram
      :temporal :histogram
      :categorical :bar
      :bar)))

(defn- select-geometry-pair
  "Select visualization geometry for a pair of columns based on their types.
   Rules:
   - Quantitative × Quantitative → :point (scatter plot reveals correlation)
   - Temporal × Quantitative → :line (line chart shows time series)
   - Quantitative × Temporal → :line
   - Categorical × Categorical → :heatmap (shows contingency)
   - Categorical × Anything else → :bar (show distribution by category)
   - Default → :bar"
  [col-a col-b]
  (let [type-a (column-general-type col-a)
        type-b (column-general-type col-b)]
    (cond
      ;; Quantitative × Quantitative → scatter plot reveals correlation
      (and (= :quantitative type-a) (= :quantitative type-b)) :point
      ;; Temporal × Quantitative → line chart shows time series
      (and (= :temporal type-a) (= :quantitative type-b)) :line
      (and (= :quantitative type-a) (= :temporal type-b)) :line
      ;; Categorical × Categorical → heatmap shows contingency
      (and (= :categorical type-a) (= :categorical type-b)) :heatmap
      ;; Categorical × Anything else → bar chart (show distribution by category)
      (or (= :categorical type-a) (= :categorical type-b)) :bar
      ;; Fallback
      :else :bar)))

(defn- column-summary-stats
  "Generate summary statistics for a column based on its type."
  [col col-type]
  (let [values (remove nil? col)
        n (count values)]
    (case col-type
      :quantitative
      (let [sorted (sort values)
            mean (/ (reduce + values) n)
            min-val (first sorted)
            max-val (last sorted)]
        (str "n=" n ", min=" (format "%.2f" (double min-val))
             ", mean=" (format "%.2f" (double mean))
             ", max=" (format "%.2f" (double max-val))))
      :temporal
      (str "n=" n)
      :categorical
      (let [unique (count (set values))]
        (str "n=" n ", unique=" unique))
      :identity
      (str "n=" n " (unique)")
      (str "n=" n))))

(defn- plot-summary-grid
  "Create a grid layout showing distribution and stats for each column."
  [ds]
  (let [col-names (ds/column-names ds)
        cards (doall
               (for [col-name col-names]
                 (let [col-data (ds col-name)
                       col-type (column-general-type col-data)
                       geom (select-geometry-single col-data)]
                   [:div {:style {:border "1px solid #ccc"
                                  :border-radius "4px"
                                  :padding "12px"
                                  :background "#f9f9f9"}}
                    [:h4 {:style {:margin "0 0 8px 0"
                                  :font-size "14px"
                                  :font-weight "600"}}
                     (name col-name)]
                    [:div {:style {:margin-bottom "8px"
                                   :height "120px"
                                   :display "flex"
                                   :align-items "center"
                                   :justify-content "center"}}
                     (try
                       (case geom
                         :histogram (-> ds (plotly/layer-histogram {:=x col-name}))
                         :bar (-> ds (plotly/layer-bar {:=x col-name}))
                         :identity (-> ds (plotly/layer-point {:=x col-name}))
                         (-> ds (plotly/layer-point {:=x col-name})))
                       (catch Exception _
                         [:span {:style {:color "#999"}} "Unable to plot"]))]
                    [:div {:style {:font-size "12px"
                                   :color "#666"
                                   :line-height "1.4"}}
                     (column-summary-stats col-data col-type)]])))]

    (into
     ^:kind/hiccup
     [:div {:style {:display "grid"
                    :grid-template-columns "repeat(auto-fit, minmax(200px, 1fr))"
                    :gap "16px"
                    :padding "16px"}}]
     cards)))

(defn- plot*
  "Generate a plot using the appropriate geometry based on column types.
   Intelligently selects visualization based on data structure and types."
  [ds]
  (let [col-names (ds/column-names ds)
        col-count (count col-names)]
    (cond
      ;; Single column: use single-column geometry selection
      (= 1 col-count)
      (let [col (first col-names)
            col-data (ds col)
            geom (select-geometry-single col-data)]
        (try
          (case geom
            :histogram (-> ds (plotly/layer-histogram {:=x col}))
            :bar (-> ds (plotly/layer-bar {:=x col}))
            :identity (-> ds (plotly/layer-point {:=x col}))
            (-> ds (plotly/layer-point {:=x col})))
          (catch Exception _
            ds)))

      ;; Two columns: use pair geometry selection
      (= 2 col-count)
      (let [[col-a col-b] col-names
            col-data-a (ds col-a)
            col-data-b (ds col-b)
            geom (select-geometry-pair col-data-a col-data-b)]
        (try
          (case geom
            :point (-> ds (plotly/layer-point {:=x col-a :=y col-b}))
            :line (-> ds (plotly/layer-line {:=x col-a :=y col-b}))
            :bar (-> ds (plotly/layer-bar {:=x col-a :=y col-b}))
            :heatmap (-> ds (plotly/layer-heatmap {:=x col-a :=y col-b}))
            (-> ds (plotly/layer-point {:=x col-a :=y col-b})))
          (catch Exception _
            ds)))

      ;; Many columns: show grid of distributions with stats
      :else
      (try
        (plot-summary-grid ds)
        (catch Exception _
          ds)))))

(defn- single-value [x]
  (let [{:keys [kind]} (advice/advise {:value x})]
    (or
     (and kind (not (kolls kind)) x)
     (and (hiccup? x) (vary-meta x assoc :kind/hiccup true))
     (and (vector? x) (not-every? map? x) (some-> (try (ds/->dataset {:x x}) (catch Exception _)) (plot*)))
     (some-> (try (ds/->dataset x) (catch Exception _)) (plot*))
     x)))

(defn ^:export plot
  "Takes data as input and generates a visual representation.
   Opens a browser window showing a plot of the data."
  [data]
  (if clay/*making*
    (single-value data)
    (clay/make! {:single-value (single-value data)
                 :in-memory true
                 :base-target-path ".glance-temp"
                 :hide-ui-header true
                 :hide-info-line true})))
