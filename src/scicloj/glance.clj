(ns scicloj.glance
  (:require [scicloj.clay.v2.make :as clay]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.dataset :as ds]))

(defn ^:export plot
  "Takes data as input and generates a visual representation.
   Opens a browser window showing a plot of the data."
  [data]
  (let [value (plotly/layer-point (ds/->dataset data))]
    (if clay/*making*
      value
      (clay/make! {:in-memory true
                   :base-target-path ".glance-temp"
                   :single-value value}))))
