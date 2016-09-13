(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
            [clojure.set :as set]))

(defn determine-features [data]
  (let [stylist-id    (get-in data keypaths/store-stylist-id)
        store-slug    (get-in data keypaths/store-slug)
        bucket-offset (mod stylist-id 3)]
    (assoc-in data
              keypaths/features
              (cond
                (= store-slug "shop")  #{}
                (= store-slug "store") #{}
                (= bucket-offset 0)    #{"kinky-straight"}
                (= bucket-offset 1)    #{}
                (= bucket-offset 2)    #{}))))

(defn display-feature? [data feature]
  ((set (get-in data keypaths/features)) feature))

(defn pixlee-product? [data]
  ;; TODO: when this experiment is over, delete the related code from the events/optimizely effect.
  (display-feature? data "pixlee-product"))

(defn new-homepage? [data]
  (display-feature? data "new-homepage"))

(defn essence? [data]
  (display-feature? data "essence"))

(defn kinky-straight? [data]
  (display-feature? data "kinky-straight"))
