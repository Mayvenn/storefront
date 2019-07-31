(ns storefront.accessors.stylists
  "TODO move these to stylist-directory ns if they are directory related"
  (:require #?@(:cljs [[spice.core :as spice]
                       [storefront.browser.cookie-jar :as cookie-jar]])
            [adventure.keypaths]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adventure.keypaths]))

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


(defn retrieve-parsed-affiliate-id [app-state]
  #?(:cljs (and
            (contains? #{"shop" "freeinstall"} (get-in app-state keypaths/store-slug))
            (get-in app-state adventure.keypaths/adventure-affiliate-stylist-id))
     :clj nil))
