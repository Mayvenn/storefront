(ns storefront.components.marquee
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn instagram-url [instagram-account]
  (str "https://instagram.com/" instagram-account))

(defn styleseat-url [styleseat-account]
  (str "https://www.styleseat.com/v/" styleseat-account))

(defn stylist-portrait [portrait]
  (ui/circle-picture {:class "mx-auto"
                      :width (str ui/header-image-size "px")
                      :alt   ""}
                     (ui/square-image portrait ui/header-image-size)))

(def add-portrait-cta
  (component/html
   [:a (merge (utils/route-to events/navigate-stylist-account-profile) {:aria-label "Add Profile Image"})
    [:img {:width (str ui/header-image-size "px")
           :src   "//ucarecdn.com/81bd063f-56ba-4e9c-9aef-19a1207fd422/-/format/auto/stylist-bug-no-pic-fallback"}]]))

(defn portrait-status [stylist-on-own-store? portrait]
  (let [status (:status portrait)]
    (cond
      (or (= "approved" status)
          (and (= "pending" status)
               stylist-on-own-store?))
      ::show-what-we-have

      stylist-on-own-store?
      ::ask-for-portrait

      :else
      ::show-nothing)))

(defn actions
  [{:keys [gallery? instagram-account styleseat-account]} gallery-link instagram-link styleseat-link]
  (cond-> []
    gallery?          (conj gallery-link)
    instagram-account (conj (instagram-link instagram-account))
    styleseat-account (conj (styleseat-link styleseat-account))))

(defn query [data]
  (-> (get-in data keypaths/store)
      (assoc :gallery? (stylists/gallery? data))
      (assoc :expanded? (get-in data keypaths/store-info-expanded))))
