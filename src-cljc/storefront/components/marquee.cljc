(ns storefront.components.marquee
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.stylists :as stylists]
            [clojure.set :as set]))

(defn instagram-url [instagram-account]
  (str "http://instagram.com/" instagram-account))

(defn styleseat-url [styleseat-account]
  (str "https://www.styleseat.com/v/" styleseat-account))

(defn stylist-portrait [portrait]
  (ui/circle-picture {:class "mx-auto"
                      :width (str ui/header-image-size "px")}
                     (ui/square-image portrait ui/header-image-size)))

(def add-portrait-cta
  (component/html
   [:a (utils/route-to events/navigate-stylist-account-profile)
    [:img {:width (str ui/header-image-size "px")
           :src   (assets/path "/images/icons/stylist-bug-no-pic-fallback.png")}]]))

(defn portrait-status [stylist? portrait]
  (let [status (:status portrait)]
    (cond
      (or (= "approved" status)
          (and (= "pending" status)
               stylist?))
      ::show-what-we-have

      stylist?
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
