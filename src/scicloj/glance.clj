(ns scicloj.glance
  (:require [scicloj.clay.v2.api :as clay]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.dataset :as ds]))

(defn ^:export show
  [data]
  (clay/make! {:base-target-path "target"
               :single-value (plotly/layer-point (ds/->dataset data))}))

(show {:x [1 2 3]})
