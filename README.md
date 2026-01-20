# scicloj.glance

> Take a quick glance at your data.

`scicloj.glance` is a Clojure library for **getting a plot on the screen with minimal ceremony**.
It is designed as a friendly on‑ramp for people who just want to *see their data* without first learning the details of Noj.

Glance is part of the Noj collection of libraries, whose name comes from the North Star (Cynosure): something that helps you orient yourself.
Glance follows the same idea, it helps you quickly orient yourself in your data.

## Rationale

Many users reach visualization with a simple goal:

> “I have some data. I want a chart.”

While the Clojure data‑science stack is powerful, getting started can feel heavy if all you want is a quick plot.
`scicloj.glance` deliberately optimizes for:

* **Low activation energy** – one obvious function to call; you shouldn't need to decide much
* **Plain Clojure data** – vectors, maps, sequences
* **No required knowledge** of Clay, Plotly, or Tablecloth

The visualization opens automatically in your browser. The goal is immediacy.

## Installation

Add Glance to your project dependencies:

**deps.edn:**
```clojure
org.scicloj/glance {:mvn/version "0.1.0"}
```

**Alternatively**, you can depend on [Noj](https://scicloj.github.io/noj/), which includes Glance along with the broader data-science stack:

```clojure
org.scicloj/noj {:mvn/version "2.0.0"}
```

## Usage

```clojure
(require '[scicloj.glance :as g])

(g/show [1 4 9 16])

(g/show {:x [1 2 3 4]
         :y [10 12 9 14]})
```

Glance tries to **do the obvious thing** based on the shape of your data:

* A single vector → values vs index
* A map of vectors → inferred x/y or multiple series
* A sequence of maps → columns inferred automatically

## Relationship to Clay, Tableplot and Noj

Internally, Glance builds on **Clay and Tableplot** for rendering,
but does not require you to invoke Clay yourself.

If and when you want more control, you can graduate naturally to Clay, Tableplot, and the rest of Noj.

Glance is usable as a standalone library, and is included in Noj.

## Status

`scicloj.glance` is intentionally small and opinionated.
Its surface area is expected to grow slowly, if at all.

Feedback, ideas, and naming discussions are welcome — especially from people new to the ecosystem.

## License

EPL-2.0
