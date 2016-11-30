(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
            [clojure.set :as set]))

(defn determine-features [data]
  data
  ;; NOTE: Ryan wants kinky-straight disabled under further review
  #_(let [stylist-id    (get-in data keypaths/store-stylist-id)
        store-slug    (get-in data keypaths/store-slug)
        bucket-offset (mod stylist-id 3)]
    (assoc-in data
              keypaths/features
              (cond
                (= store-slug "shop")  #{"kinky-straight-2-control"}
                (= store-slug "store") #{"kinky-straight-2-control"}
                (= bucket-offset 0)    #{"kinky-straight-2"}
                (= bucket-offset 1)    #{"kinky-straight-2-curly-move"}
                (= bucket-offset 2)    #{"kinky-straight-2-control"}))))

(defn display-feature? [data feature]
  ((set (get-in data keypaths/features)) feature))

(defn kinky-straight? [data]
  (display-feature? data "kinky-straight-2"))

(defn swap-curly-loose-wave? [data]
  (display-feature? data "kinky-straight-2-curly-move"))

(defn apple-pay? [data]
  (display-feature? data "apple-pay"))

(defn hero-shop-look? [data]
  (display-feature? data "hero-shop-look"))
