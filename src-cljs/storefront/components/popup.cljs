(ns storefront.components.popup
  (:require [storefront.component :as component]
            [storefront.components.email-capture :as email-capture]
            [storefront.components.share-your-cart :as share-your-cart]
            [storefront.components.v2-homepage-popup :as v2-homepage-popup]
            [adventure.components.program-details-popup :as adventure-program-details]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(def popup-type->popups
  {:adventure-free-install {:query     adventure-program-details/query
                            :component adventure-program-details/component}
   :v2-homepage            {:query     v2-homepage-popup/query
                            :component v2-homepage-popup/component}
   :email-capture          {:query     email-capture/query
                            :component email-capture/component}
   :share-cart             {:query     share-your-cart/query
                            :component share-your-cart/component}})

(defn query [data]
  (let [popup-type (get-in data keypaths/popup)
        query      (or (some-> popup-type popup-type->popups :query)
                       (constantly nil))]
    {:popup-type popup-type
     :popup-data (query data)}))

(defn built-component [{:keys [popup-type popup-data]} _]
  (let [opts {:opts {:close-attrs (utils/fake-href events/control-popup-hide)}}]
    (some-> popup-type popup-type->popups :component (component/build popup-data opts))))
