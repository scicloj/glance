# scicloj.glance

> Glance just plots your data.

`scicloj.glance` is a Clojure library for **getting a plot on the screen with minimal ceremony**.
It’s a friendly on‑ramp for people who just want to *see their data*.

## Rationale

Many users reach visualization with a simple goal:

> “I have some data. I want a chart.”

Glance deliberately optimizes for:

* **One function** – call `g/plot`
* **Plain Clojure data** – vectors, maps, sequences
* **Automatic chart inference** – picks a sensible visualization
* **No plotting API to learn** – opens in your browser immediately

## Installation

Add Glance to your project dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/org.scicloj/glance.svg)](https://clojars.org/org.scicloj/glance)

## Usage

```clojure
(require '[scicloj.glance :as g])

(g/plot [1 4 9 16])

(g/plot {:x [1 2 3 4]
         :y [10 12 9 14]})

(g/plot "data.csv")
```

Glance tries to **do the obvious thing** based on the shape of your data:

* A single vector → values vs index
* A map of vectors → inferred x/y or multiple series
* A sequence of maps → columns inferred automatically
* A CSV file (filename or URL) → automatically loaded and visualized

## Documentation and Examples

See [Glance Documentation](https://scicloj.github.io/glance/)
or work through the notebook [notebooks/index.clj](notebooks/index.clj) for examples and guidance.

## Relationship to Clay, Kindly, Tablecloth, and Noj

Internally, Glance uses **Clay** to serve content in the browser.
It plays well with **Kindly** (for kinds/annotations) and **Tablecloth** (for data prep).
You do not need to use any of these directly to get value from Glance.
They are there when you want more control.
In the future Glance might be useful as part of Noj, the collection of Clojure data science libraries.

## Status

`scicloj.glance` is intentionally narrow.
Feedback, ideas, and naming discussions are welcome.

The best place to discuss Glance is [#noj-dev on Zulip](https://clojurians.zulipchat.com/#narrow/channel/321125-noj-dev).

## License

Copyright © 2025 Scicloj

EPL-2.0
