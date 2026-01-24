;; ## Introducing Glance
;;
;; Glance just plots your data.
;;
;; Philosophy:
;;
;; - One function: `g/plot`
;; - Opens a browser window showing your data
;; - Automatically picks an appropriate chart type
;; - No plotting API to learn

;; ## Quick Start: `g/plot`

(ns index
  (:require [scicloj.glance :as g]))

;; Quantitative × Quantitative → scatter plot

(g/plot {:study-hours [2 3 5 4 6 8 7 9]
         :test-score [65 70 80 75 85 90 88 95]})

;; ## Plot infered from data

;; Many numbers → histogram

(g/plot {:temperatures [16.2 17.5 18.1 19.3 20.8 22.4 24.1 25.7 26.3 27.8 28.5 29.2 30.1]})

;; A few unique values → bar chart with frequencies

(g/plot {:dice-rolls [1 3 2 6 4 3 2 1 5 3 4 6 2 3 1]})

;; Time series → line chart

(g/plot {:date ["2024-01-01" "2024-01-02" "2024-01-03"
                "2024-01-04" "2024-01-05" "2024-01-06" "2024-01-07"]
         :visitors [120 132 128 150 170 165 180]})

;; Pass data as columns or rows

(g/plot [{:date "2024-01-01" :visitors 120}
         {:date "2024-01-02" :visitors 132}
         {:date "2024-01-03" :visitors 128}
         {:date "2024-01-04" :visitors 150}
         {:date "2024-01-05" :visitors 170}
         {:date "2024-01-06" :visitors 165}
         {:date "2024-01-07" :visitors 180}])

;; Categorical data → bar chart of frequencies

(g/plot {:fruits ["apple" "banana" "apple" "kiwi" "banana" "apple" "kiwi" "kiwi"]})

;; Load a local CSV file

(g/plot "datasets/sample.csv")

;; A single number

(g/plot 1)

;; A vector

(g/plot [1 2 3 4 5 1 2 3 1 2 1])

;; Categorical × Categorical → heatmap shows contingency table

(g/plot {:activity ["running" "running" "running" "running" "running" "running" "cycling" "cycling" "cycling" "cycling" "cycling" "cycling"]
         :weather ["sunny" "sunny" "sunny" "sunny" "sunny" "rainy" "sunny" "sunny" "rainy" "rainy" "rainy" "rainy"]})

;; Categorical × Quantitative → grouped bar chart

(g/plot {:region ["North" "South" "East" "North" "South" "East" "West" "North"]
         :sales [45 62 38 51 58 42 48 55]})

;; Glance picks an appropriate chart for the data.

;; ## Multi-Column Datasets

;; With 3+ columns, Glance creates a summary view with statistics
;; and individual visualizations for each column

(g/plot {:employee-id [101 102 103 104 105 106 107 108]
         :department ["Engineering" "Sales" "Engineering" "Marketing"
                      "Sales" "Engineering" "Marketing" "Sales"]
         :salary [95000 72000 88000 65000 78000 102000 61000 75000]
         :years-experience [5 3 7 2 4 9 1 3]})

;; ## Composing Kinds and Hiccup

;; You can use [Kindly](https://scicloj.github.io/kindly-noted/kindly)
;; annotations to request other visualizations:

(g/plot ^:kind/table {:product ["Widget" "Gadget" "Doohickey"]
                      :price [19.99 29.99 14.99]
                      :stock [45 23 67]})

;; See [Clay Examples](https://scicloj.github.io/clay/clay_book.examples.html)
;; for examples of other interesting kinds.

;; Beyond simple plots, you can create dashboards using Hiccup markup.
;; Nest multiple plots in custom HTML layouts:

(g/plot [:div {:style {:border "1px solid" :padding "20px"}}
         [:h3 "Sales Dashboard"]
         [:p "Monthly revenue trends and customer retention"]
         [:div
          (g/plot {:month ["Jan" "Feb" "Mar" "Apr" "May" "Jun"]
                   :revenue [12000 15000 14000 18000 22000 25000]})
          (g/plot {:status ["active" "active" "churned" "active" "active" "churned"
                            "active" "active" "churned" "active" "active" "active"]})]])

;; You can even embed JavaScript for interactive visualizations:

(g/plot [:div
         [:h3 "Lissajous Curve"]
         [:p "An animated parametric curve combining two sine waves with different frequencies"]
         [:canvas#lissajous {:width 400 :height 400
                             :style {:border "1px solid"
                                     :background "#f9fafb"}}]
         [:script "
           const canvas = document.getElementById('lissajous');
           const ctx = canvas.getContext('2d');
           const w = canvas.width;
           const h = canvas.height;

           function drawLissajous(a, b, delta) {
             ctx.clearRect(0, 0, w, h);
             ctx.strokeStyle = '#2563eb';
             ctx.lineWidth = 2.5;
             ctx.beginPath();

             for (let t = 0; t < 2 * Math.PI; t += 0.01) {
               const x = w/2 + 120 * Math.sin(a * t + delta);
               const y = h/2 + 120 * Math.sin(b * t);
               if (t === 0) ctx.moveTo(x, y);
               else ctx.lineTo(x, y);
             }
             ctx.stroke();
           }

           let delta = 0;
           setInterval(() => {
             drawLissajous(3, 2, delta);
             delta += 0.05;
           }, 50);
         "]])

;; ## Preparing Data with Tablecloth

;; Tablecloth can be used to query and transform data before plotting.
;; This is useful for filtering, selecting columns, or aggregating data.

;; Require Tablecloth:

(require '[tablecloth.api :as tc])

(def athlete-data
  (tc/dataset {:name ["Ada" "Ben" "Chloe" "Dana" "Eli"]
               :sport ["track" "track" "swim" "swim" "track"]
               :score [12.4 11.9 55.2 54.7 12.1]
               :country ["US" "CA" "US" "UK" "DE"]}))

(-> athlete-data
    (tc/select-rows #(< (:score %) 13))
    (tc/select-columns [:name :score])
    (g/plot))

(-> athlete-data
    (tc/group-by :sport)
    (tc/mean :score)
    g/plot)

;; See the [Tablecloth Documentation](https://scicloj.github.io/tablecloth/)

;; ## Plotly Specs `g/plotly`
;;
;; While `g/plot` is great for instant visualizations, sometimes you need
;; more control. The `g/plotly` function returns the inferred Plotly
;; specification as a Kindly-annotated map, which you can inspect or modify.

;; Use `g/plotly` instead of `g/plot` to get the specification:

^:kind/pprint
(g/plotly {:x [1 2 3 4 5]
           :y [3 4 5 2 1]})

;; You can modify the spec to customize the visualization:

(-> (g/plotly {:x [1 2 3 4 5]
               :y [3 4 5 2 1]})
    (assoc-in [:layout :title :text] "Custom Title")
    (assoc-in [:data 0 :marker :color] "green")
    (assoc-in [:data 0 :marker :size] 12)
    (g/plot))

;; Or if you prefer to build a spec from scratch:

(g/plot ^:kind/plotly
        {:data [{:x ["Jan" "Feb" "Mar" "Apr" "May"]
                 :y [12000 15000 14000 18000 22000]
                 :name "Revenue"
                 :type "bar"}
                {:x ["Jan" "Feb" "Mar" "Apr" "May"]
                 :y [10000 13000 12000 15000 18000]
                 :name "Costs"
                 :type "scatter"
                 :mode "lines+markers"}]
         :layout {:title {:text "Revenue vs Costs"}}})

;; For more information see the
;; [Plotly Reference](https://plotly.com/javascript/basic-charts/)

;; ## Why Glance
;;
;; Glance is for data explorers who want to visualize
;; data without needing to learn plotting APIs.
;;
;; - **Approachable**: No special tooling required; just normal Clojure calls.
;; - **Inference-first**: Automatically picks a sensible visualization from your data.
;; - **No plotting API**: You don't need to learn Plotly configs to get value.
;; - **Powerful when needed**: Compose dashboards with Hiccup; use kinds with Kindly.
;; - **Flexible**: Use `g/plot` for instant results, or `g/plotly` to inspect/modify specs.
;; - **Ecosystem-ready**: Plays well with Clay and Tablecloth for richer workflows.
;;
;; Just plot it with `(g/plot your-data)`.
