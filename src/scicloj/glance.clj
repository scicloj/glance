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
       (clay/make! {:single-value (impl/single-value ~data)
                    :in-memory true
                    :base-target-path ".glance-temp"
                    :hide-ui-header true
                    :hide-info-line true}))))
