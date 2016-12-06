(ns storefront.accessors.videos)

(def name->id
  {:review-compilation "66ysezzxwk"
   :mayvenn-story      "8n5r9cm09g"})

(def id->name (clojure.set/map-invert name->id))
