(ns storefront.components.gallery
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.stylists :as stylists]
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]))

(defn title [{:keys [store_nickname]}]
  [:div.p2.center
   [:h2 [:img {:style {:height "50px"}
                  :src (assets/path "/images/icons/profile.png")}]]
   [:h1 (str store_nickname "'s Gallery")]
   [:div (str "Scroll through "
              store_nickname
              "'s best #MayvennMade looks and get inspiration for your next style!")]])

(defn manage-section [editing?]
  [:div.p2.center.dark-gray.bg-light-gray
   [:h1 "Manage your gallery"]
   [:div.p1 "Here you can upload images, edit posts and manage your gallery settings."]
   [:div.p1 (ui/teal-button {} "Choose an image to upload")]
   ;;TODO change button depending upon state
   [:div.p1 (ui/ghost-button {} "Edit your gallery")]])

(defn images [{:keys [gallery]}]
  (into [:div.p1]
        (for [{:keys [url]} (:images gallery)]
          [:div {:key url} [:img {:src url}]])))

(defn component [{:keys [store editing? own-store?] :as data} owner opts]
  (component/create
   (ui/narrow-container
    [:div
     (title store)
     (when own-store?
       (manage-section editing?))
     (images store)])))

(defn query [data]
  (assoc-in {:store (get-in data keypaths/store)
             :editing? false
             :own-store? (stylists/own-store? data)}
            [:store :gallery]
            {:images []}))

(defn built-component [data opts]
  (component/build component (merge {:_data data} (query data)) nil))
