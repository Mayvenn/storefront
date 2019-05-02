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
  [{:keys [store-nickname address] :as stylist}]
  (when stylist
    (if (= store-nickname (:firstname address))
      (str (:firstname address) " " (:lastname address))
      store-nickname)))

(def community-url
  (merge (utils/fake-href events/control-stylist-community)
         {:data-test "community"}))
