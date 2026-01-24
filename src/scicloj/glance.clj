(ns scicloj.glance
  (:require [scicloj.glance.impl :as impl]
            [scicloj.clay.v2.make :as clay]))

(defmacro ^:export plot
  "Takes data as input and generates a visual representation.
   Opens a browser window showing a plot of the data."
  [data]
  `(if clay/*making*
     (impl/single-value ~data)
     (binding [clay/*making* true]
       (with-out-str
         (clay/make! {:single-value (impl/single-value ~data)
                      :in-memory true
                      :base-target-path ".glance-temp"
                      :hide-ui-header true
                      :hide-info-line true}))
       :ok)))

(defn ^:export plotly
  "Creates a Plotly specification from data"
  [data]
  (when-let [raw (impl/plotly data)]
    (let [{:keys [kindly/f]} raw]
      (when f
        (with-meta (f raw) {:kind/plotly true})))))
