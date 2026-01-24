(ns scicloj.glance.impl
  (:require [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.dataset :as ds]
            [tablecloth.column.api :as tcc]
            [scicloj.kindly-advice.v1.api :as advice]
            [clojure.java.io :as io]
            [tablecloth.api :as tc]))

(def kolls
  #{:kind/map :kind/vector :kind/set :kind/seq :kind/dataset})

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
  (every? #(= % (long %)) (remove nil? col)))

(defn distinct-count
  "Count distinct non-nil values in a column."
  [col]
  (->> col (remove nil?) distinct count))

(defn axis-title
  "Axis title prefers keyword/symbol names, else str."
  [v]
  (cond (keyword? v) (name v)
        (symbol? v) (name v)
        :else (str v)))

(defn frequencies-dataset
  "Convert values to frequency counts dataset for bar plotting."
  [values]
  (let [freqs (->> values frequencies (sort-by key))
        categories (mapv first freqs)
        counts (mapv second freqs)]
    (ds/->dataset {:category categories :count counts})))

(defn render-histogram-or-bar
  "Render histogram for continuous or bar for discrete/categorical data."
  [col-name col-data]
  (let [values (remove nil? col-data)
        ints? (all-integers? col-data)
        uniq (distinct-count col-data)]
    (if (and ints? (<= uniq 30))
      (-> (frequencies-dataset values)
          (plotly/layer-bar {:=x :category :=y :count}))
      (let [histogram-opts (if ints?
                             (let [min-val (apply min values)
                                   max-val (apply max values)
                                   nbins (min (inc (- max-val min-val)) 50)]
                               {:=histogram-nbins (max 1 nbins)})
                             {})
            clean-ds (ds/->dataset {col-name values})]
        (-> clean-ds
            (plotly/layer-histogram (assoc histogram-opts :=x col-name)))))))

(defn render-categorical-bar
  "Render bar chart for categorical data."
  [col-data]
  (-> (remove nil? col-data)
      (frequencies-dataset)
      (plotly/layer-bar {:=x :category :=y :count})))

(defn heatmap-crosstab
  "Build a Plotly heatmap spec for two categorical columns."
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
  "Select visualization geometry for a single column based on its type."
  [col]
  (let [general-type (column-general-type col)]
    (case general-type
      :identity :identity
      :quantitative :histogram
      :temporal :histogram
      :categorical :bar
      :bar)))

(defn select-geometry-pair
  "Select visualization geometry for a pair of columns based on their types."
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
                       :histogram (render-histogram-or-bar col-name col-data)
                       :bar (render-categorical-bar col-data)
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
      (= 1 col-count)
      (let [col (first col-names)
            col-data (ds col)
            geom (select-geometry-single col-data)]
        (case geom
          :histogram (render-histogram-or-bar col col-data)
          :bar (render-categorical-bar col-data)
          :identity (-> ds (plotly/layer-point {:=x col}))
          (-> ds (plotly/layer-point {:=x col}))))

      (= 2 col-count)
      (let [[col-a col-b] col-names
            col-data-a (ds col-a)
            col-data-b (ds col-b)
            type-a (column-general-type col-data-a)
            type-b (column-general-type col-data-b)
            geom (select-geometry-pair col-data-a col-data-b)
            [x-col y-col] (cond
                            (and (= :temporal type-a) (not= :temporal type-b)) [col-a col-b]
                            (and (= :temporal type-b) (not= :temporal type-a)) [col-b col-a]
                            :else [col-a col-b])]
        (case geom
          :point (-> ds (plotly/layer-point {:=x col-a :=y col-b}))
          :line (-> ds (plotly/layer-line {:=x x-col :=y y-col}))
          :bar (-> ds (plotly/layer-bar {:=x col-a :=y col-b}))
          :heatmap (heatmap-crosstab col-data-a col-data-b col-a col-b)
          (-> ds (plotly/layer-point {:=x col-a :=y col-b}))))

      :else
      (plot-summary ds))))

(defn dataset* [x]
  (ds/->dataset x {:parser-fn {:date :local-date}}))

(defn try-dataset [x]
  (or
   (and (tc/dataset? x) x)
   (and (vector? x) (not-every? map? x) (some-> (try (dataset* {:x x}) (catch Exception _))))
   (some-> (try (dataset* x) (catch Exception _)))))

(defn plotly [x]
  (some-> (try-dataset x) (plot*)))

(defn single-value [x]
  (let [{:keys [kind]} (advice/advise {:value x})]
    (or
     (and kind (not (kolls kind)) x)
     (and (hiccup? x) (vary-meta x assoc :kind/hiccup true))
     (plotly x)
     x)))
