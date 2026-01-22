(ns scicloj.glance.impl
  (:require [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.dataset :as ds]
            [tablecloth.column.api :as tcc]
            [scicloj.kindly-advice.v1.api :as advice]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def kolls
  #{:kind/map :kind/vector :kind/set :kind/seq})

(defn hiccup? [x]
  (and (vector? x)
       (keyword? (first x))))

(defn column-general-type
  "Returns the general type category of a column: :quantitative, :temporal, :categorical, or :identity."
  [col]
  (cond
    (tcc/typeof? col :numerical) :quantitative
    (tcc/typeof? col :datetime) :temporal
    (tcc/typeof? col :logical) :categorical
    (tcc/typeof? col :textual) :categorical
    :else :identity))

(defn all-integers?
  "Check if all values in a column are integers."
  [col]
  (let [values (remove nil? col)]
    (every? #(= % (long %)) values)))

(defn distinct-count
  "Count distinct non-nil values in a column."
  [col]
  (->> col (remove nil?) distinct count))


(defn axis-title
  "Axis title prefers keyword/symbol names, else str."
  [v]
  (cond
    (keyword? v) (name v)
    (symbol? v) (name v)
    :else (str v)))

(defn heatmap-crosstab
  "Build a Plotly heatmap spec for two categorical columns.
  Returns a plotly map tagged with :kind/plotly metadata."
  [col-a col-b col-a-name col-b-name]
  (let [pairs (->> (map vector col-a col-b)
                   (remove (fn [[a b]] (or (nil? a) (nil? b)))))
        counts (frequencies pairs)
        xs (->> pairs (map first) distinct sort vec)
        ys (->> pairs (map second) distinct sort vec)
        z  (mapv (fn [y]
                   (mapv (fn [x] (get counts [x y] 0)) xs))
                 ys)
        plot {:data [{:type "heatmap"
                      :x xs
                      :y ys
                      :z z
                      :colorscale "Viridis"
                      :hoverongaps false}]
              :layout {:xaxis {:title (axis-title col-a-name)}
                       :yaxis {:title (axis-title col-b-name)}}}]
    (with-meta plot {:kind/plotly true})))

(defn select-geometry-single
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

(defn select-geometry-pair
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

(defn column-summary-stats
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

(defn plot-summary
  "Row-based layout with each column getting a full-width row."
  [ds]
  (let [col-names (ds/column-names ds)]
    ^:kind/hiccup
    [:div {:style {:max-width "800px"
                   :margin "0 auto"}}
     (doall (for [col-name col-names]
              (let [col-data (ds col-name)
                    col-type (column-general-type col-data)
                    geom (select-geometry-single col-data)]
                [:div {:style {:border "1px solid #ddd"
                               :margin-bottom "20px"
                               :padding "15px"
                               :border-radius "5px"}}
                 [:div {:style {:display "flex"
                                :align-items "center"
                                :gap "20px"}}
                  [:div {:style {:flex "1 1 auto"}}
                   [:h4 {:style {:margin "0"}} (name col-name)]
                   [:div {:style {:margin-top "5px"
                                  :font-size "12px"
                                  :color "#666"}}
                    (column-summary-stats col-data col-type)]]
                  [:div {:style {:flex "0 0 auto"
                                 :text-align "right"}}
                   (try
                     (case geom
                       :histogram
                       (let [values (remove nil? col-data)
                             ints? (all-integers? col-data)
                             uniq (distinct-count col-data)]
                         (if (and ints? (<= uniq 30))
                           (let [freqs (->> values frequencies (sort-by key))
                                 categories (mapv first freqs)
                                 counts (mapv second freqs)
                                 agg-ds (ds/->dataset {:category categories :count counts})]
                             (-> agg-ds (plotly/layer-bar {:=x :category :=y :count})))
                           (let [histogram-opts (if ints?
                                                  (let [min-val (apply min values)
                                                        max-val (apply max values)
                                                        nbins (min (inc (- max-val min-val)) 50)]
                                                    {:=histogram-nbins (max 1 nbins)})
                                                  {})]
                             (-> ds (plotly/layer-histogram (assoc histogram-opts :=x col-name))))))
                       :bar (-> ds (plotly/layer-bar {:=x col-name}))
                       :identity (-> ds (plotly/layer-point {:=x col-name}))
                       (-> ds (plotly/layer-point {:=x col-name})))
                     (catch Exception _
                       [:span {:style {:color "#999"}} "Unable to plot"]))]]])))]))

(defn plot*
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
            :histogram
            (let [values (remove nil? col-data)
                  ints? (all-integers? col-data)
                  uniq (distinct-count col-data)]
              (if (and ints? (<= uniq 30))
                ;; Small-cardinality integers: use bar frequencies for clean integer buckets
                (let [freqs (->> values frequencies (sort-by key))
                      categories (mapv first freqs)
                      counts (mapv second freqs)
                      agg-ds (ds/->dataset {:category categories :count counts})]
                  (-> agg-ds (plotly/layer-bar {:=x :category :=y :count})))
                ;; Otherwise, histogram with integer-aware nbins when possible
                (let [histogram-opts (if ints?
                                       (let [min-val (apply min values)
                                             max-val (apply max values)
                                             ;; one bin per integer step, capped
                                             nbins (min (inc (- max-val min-val)) 50)]
                                         {:=histogram-nbins (max 1 nbins)})
                                       {})]
                  (-> ds (plotly/layer-histogram (assoc histogram-opts :=x col))))))
            :bar
            ;; For categorical, aggregate frequencies
            (let [freqs (->> col-data
                           (remove nil?)
                           (frequencies)
                           (sort-by key))
                  categories (mapv first freqs)
                  counts (mapv second freqs)
                  agg-ds (ds/->dataset {:category categories :count counts})]
              (-> agg-ds (plotly/layer-bar {:=x :category :=y :count})))
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
            :heatmap (heatmap-crosstab col-data-a col-data-b col-a col-b)
            (-> ds (plotly/layer-point {:=x col-a :=y col-b})))
          (catch Exception _
            ds)))

      ;; Many columns: show grid of distributions with stats
      :else
      (try
        (plot-summary ds)
        (catch Exception _
          ds)))))

(defn url-string? [s]
  (and (string? s)
       (try
         (io/as-url s)
         true
         (catch Exception _ false))))

(defn single-value [x]
  (let [{:keys [kind]} (advice/advise {:value x})]
    (or
     (and kind (not (kolls kind)) x)
     (and (hiccup? x) (vary-meta x assoc :kind/hiccup true))
     (and (vector? x) (not-every? map? x) (some-> (try (ds/->dataset {:x x}) (catch Exception _)) (plot*)))
     (and (url-string? x) (some-> (try (ds/->dataset (io/input-stream x) {:file-type :csv}) (catch Exception _)) (plot*)))
     (some-> (try (ds/->dataset x) (catch Exception _)) (plot*))
     x)))
