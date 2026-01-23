(ns examples
  (:require [scicloj.glance :as g]))

;; # Glance: Data Visualization at a Glance
;;
;; Glance is a minimalist data visualization library that automatically
;; chooses appropriate visualizations based on your data structure and types.
;;
;; Philosophy:
;; - One function: `g/plot`
;; - Automatic geometry selection (scatter, histogram, bar, heatmap)
;; - Sensible defaults, no configuration required
;; - Built on Tableplot/Plotly for quality output
;;
;; Just pass your data and get an appropriate visualization instantly.

;; ## Getting Started

;; The simplest case: a single number

(g/plot 1)

;; A vector of numbers → histogram to show distribution

(g/plot [1 2 3 4 5])

;; Two vectors in a map → scatter plot to reveal relationships

(g/plot {:x [1 2 3 4 5]
         :y [3 4 5 2 1]})

;; ## Automatic Geometry Selection

;; Glance looks at your data types and structure to pick the right visualization.
;; Here's how it works:

;; ### Single Column: Distribution Visualizations

;; Integers with few unique values → bar chart with frequencies

(g/plot {:dice-rolls [1 3 2 6 4 3 2 1 5 3 4 6 2 3 1]})

;; Many numbers → histogram with intelligent binning

(g/plot {:temperatures [22.1 23.5 21.8 24.2 23.1 22.9 24.5 23.8 22.4 23.2 24.1 22.7 23.6]})

;; Categorical data → bar chart showing frequency counts

(g/plot {:fruits ["apple" "banana" "apple" "kiwi" "banana" "apple" "kiwi" "kiwi"]})

;; ### Two Columns: Relationship Visualizations

;; The visualization changes based on what types you're comparing:

;; Quantitative × Quantitative → scatter plot reveals correlation

(g/plot {:study-hours [2 3 5 4 6 8 7 9]
         :test-score [65 70 80 75 85 90 88 95]})

;; Categorical × Categorical → heatmap shows contingency table

(g/plot {:shirt-size ["S" "M" "L" "S" "M" "L" "XL" "M"]
         :color-preference ["blue" "red" "blue" "green" "red" "blue" "green" "blue"]})

;; Categorical × Quantitative → grouped bar chart

(g/plot {:region ["North" "South" "East" "North" "South" "East" "West" "North"]
         :sales [45 62 38 51 58 42 48 55]})

;; ## Multi-Column Datasets

;; With 3+ columns, Glance creates a summary view with statistics
;; and individual visualizations for each column

(g/plot {:player ["Alice" "Bob" "Carol" "Dave" "Eve"]
         :level [12 18 15 22 19]
         :score [1250 2100 1680 2890 2340]})

;; Works great for exploratory data analysis on larger tables

(g/plot {:employee-id [101 102 103 104 105 106 107 108]
         :department ["Engineering" "Sales" "Engineering" "Marketing"
                      "Sales" "Engineering" "Marketing" "Sales"]
         :salary [95000 72000 88000 65000 78000 102000 61000 75000]
         :years-experience [5 3 7 2 4 9 1 3]})

;; ## Loading External Data

;; Load CSV files directly from URLs or local paths

(g/plot "https://drive.google.com/uc?id=13a2WyLoGxQKXbN_AIjrOogIlQKNe9uPm&export=download")

;; Glance automatically detects the file type and loads the data

;; ## Advanced Layouts with Hiccup

;; Beyond simple plots, you can create dashboards using Hiccup markup.
;; Nest multiple plots in custom HTML layouts:

(g/plot [:div
         [:h3 "Sales Dashboard"]
         [:p "Monthly revenue trends and customer retention"]
         [:table
          [:tbody
           [:tr
            [:td (g/plot {:month ["Jan" "Feb" "Mar" "Apr" "May" "Jun"]
                          :revenue [12000 15000 14000 18000 22000 25000]})]
            [:td (g/plot {:status ["active" "active" "churned" "active" "active" "churned"
                                   "active" "active" "churned" "active" "active" "active"]})]]]]])

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

;; ## Edge Cases & Data Cleaning

;; Glance handles common data quality issues automatically:

;; Nil values are filtered out before visualization

(g/plot {:sensor-readings [23 25 nil 24 26 nil 27]})

;; Single repeated value still produces a meaningful chart

(g/plot {:status ["active" "active" "active" "active"]})

;; ## Integration with Kindly & Clay

;; Glance works seamlessly with the Kindly ecosystem.
;; You can use kind annotations for alternative renderings:

(g/plot ^:kind/table {:product ["Widget" "Gadget" "Doohickey"]
                      :price [19.99 29.99 14.99]
                      :stock [45 23 67]})

;; For more advanced notebook features, see the Clay documentation:
;; https://scicloj.github.io/clay/

;; ---
;;
;; ## Summary
;;
;; Glance provides instant data visualization with zero configuration:
;;
;; - **One function**: `g/plot` handles everything
;; - **Smart defaults**: Automatic geometry selection based on data types
;; - **Flexible**: Works with scalars, vectors, maps, CSVs, and Hiccup
;; - **Composable**: Nest plots in dashboards, integrate with Clay
;;
;; Perfect for exploratory data analysis and quick insights.
