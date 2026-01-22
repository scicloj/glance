(ns examples
  (:require [scicloj.glance :as g]))

;; ## Basic Examples

;; Single scalar value

(g/plot 1)

;; Single vector → automatically creates histogram (quantitative)

(g/plot [1 2 3 4 5])

;; Two columns: x and y (quantitative × quantitative) → scatter plot

(g/plot {:x [1 2 3 4 5]
         :y [3 4 5 2 1]})

;; ## Single Column Visualizations

;; Demonstrates geometry selection for different data types

;; Quantitative single column → histogram shows distribution

(g/plot {:values [10 12 11 15 14 13 16 12 14 15 11 13 12]})

;; Categorical single column → bar chart shows frequencies

(g/plot {:colors ["red" "blue" "red" "green" "blue" "red" "green" "green"]})

;; ## Two Column Visualizations

;; Demonstrates pair-wise geometry selection rules

;; Quantitative × Quantitative → scatter plot (reveals correlation)

(g/plot {:height [160 165 170 175 180 155]
         :weight [60 65 75 80 85 55]})

;; Categorical × Categorical → heatmap (shows contingency)

(g/plot {:size ["small" "large" "small" "medium" "large" "small" "medium" "large"]
         :color ["red" "blue" "red" "green" "blue" "red" "green" "green"]})

;; Categorical × Quantitative → bar chart (distribution by category)

(g/plot {:category ["A" "B" "C" "A" "B" "C" "A" "B"]
         :value [10 15 12 14 16 13 11 18]})

;; ## Multi-Column Datasets

;; Demonstrates grid layout with individual column summaries

;; Three columns: shows grid with 3 cards (one per column)
;; Each card has histogram/bar + statistics

(g/plot {:name ["Alice" "Bob" "Charlie" "Diana" "Eve"]
         :age [25 30 28 35 27]
         :score [95 87 92 88 91]})

;; More complex dataset with mixed types
;; Grid layout with separate visualizations and stats for each column

(g/plot {:id [1 2 3 4 5 6 7 8]
         :department ["Sales" "IT" "Sales" "HR" "IT" "Sales" "HR" "IT"]
         :salary [50000 75000 55000 60000 80000 52000 58000 78000]
         :years-employed [2 5 1 3 6 1 4 4]})

;; ## Hiccup

;; Render custom markup alongside charts (lightweight UI composition)

(g/plot [:div
         [:h2 "Team leaderboard"]
         [:ul
          [:li "Ada — 42"]
          [:li "Grace — 38"]
          [:li "Lin — 35"]]
         [:button "Refresh scores"]])

(g/plot [:div
         [:h3 "Mini dashboard"]
         [:p "Quick glance: revenue trend and churn mix"]
         [:table
          [:tbody
           [:tr
            [:td (g/plot {:month ["Jan" "Feb" "Mar" "Apr" "May" "Jun"]
                          :revenue [12 15 14 18 22 25]})]
            [:td (g/plot {:status ["kept" "kept" "churned" "kept" "kept" "churned" "kept" "kept" "churned" "kept" "kept" "kept"]})]]]]])

;; ## Edge Cases & Tips

;; Very few data points: geometry selection still works

(g/plot {:x [1 2] :y [2 4]})

;; Single categorical value repeated: still creates bar chart

(g/plot {:group ["A" "A" "A" "A"]})

;; Mixed with nil values: automatically filtered

#_(g/plot {:values [1 2 nil 4 5 nil 7]})
