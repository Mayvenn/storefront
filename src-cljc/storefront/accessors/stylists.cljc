(ns storefront.accessors.stylists
  "TODO move these to stylist-directory ns if they are directory related"
  (:require #?@(:cljs [[spice.core :as spice]
                       [storefront.browser.cookie-jar :as cookie-jar]])
            [storefront.keypaths :as keypaths]))

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
            (some-> app-state
                    (get-in keypaths/cookie)
                    cookie-jar/retrieve-affiliate-stylist-id
                    :affiliate-stylist-id
                    spice/parse-int))
     :clj nil))
