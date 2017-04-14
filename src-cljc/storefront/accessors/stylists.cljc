(ns storefront.accessors.stylists
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn own-store? [data]
  (= (get-in data keypaths/user-store-slug)
     (get-in data keypaths/store-slug)))

(defn shop? [data]
  (= "shop" (get-in data keypaths/store-slug)))

(defn gallery? [data]
  (or (own-store? data)
      (seq (get-in data keypaths/store-gallery-images))))

(def community-url
  (utils/fake-href events/control-stylist-community))
