(ns storefront.accessors.stylists
  (:require [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn own-store? [data]
  (= (get-in data keypaths/user-store-slug)
     (get-in data keypaths/store-slug)))

(defn gallery? [data]
  (or (own-store? data)
      (seq (get-in data keypaths/store-gallery-images))))

(defn ->display-name
  ([stylist]
   (->display-name stylist {}))
  ([{:keys [store-nickname address]} {full? :full?}]
   (or store-nickname
       (str (:firstname address)
            (when full? (str " " (:lastname address)))))))

(def community-url
  (merge (utils/fake-href events/control-stylist-community)
         {:data-test "community"}))
