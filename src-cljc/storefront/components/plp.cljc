(ns storefront.components.plp
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [ui.molecules :as ui.M]))

(defn component [{:keys [] :as query-data} owner opts]
  (component/create
   [:div.container
    (ui.M/hero query-data)
    [:div.center.mx2
     [:div.purple.h7.medium.mbn1.mt3
      "NEW!"]
     [:div.h1 "Mayvenn Install"]
     [:div.h5.dark-gray.light.my2.mx5
      "Save 10% on your hair & get a free Mayvenn Install by a licensed stylist when you purchase 3 or more items. "
      [:a.teal.h6.medium
       {:on-click (utils/send-event-callback events/popup-show-adventure-free-install)}
       "learn more"]]]]))

(defn query [data]
  {:mob-uuid "41adade2-0987-4f8f-9bed-99d9586fead3"
   :file-name "plp-hero-image"
   :alt "New Mayvenn Install"
   :opts (utils/scroll-href "mayvenn-free-install-video")})

(defn built-component [data opts]
  (component/build component (query data) nil))
