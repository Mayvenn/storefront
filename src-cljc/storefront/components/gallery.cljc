(ns storefront.components.gallery
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defn title [{:keys [store-nickname]}]
  [:div.p2.center
   (ui/ucare-img {:style {:height "50px"}} "4a0e6e66-c448-47e0-8341-bc92b91138ef")
   [:h1 (str store-nickname "'s Gallery")]
   [:p (str "Scroll through "
            store-nickname
            "'s best #MayvennMade looks and get inspiration for your next style!")]])

(def pending-approval
  (component/html
   [:div.container-size.bg-dark-gray.white.medium.flex.items-center.center.p2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defn images [{:keys [gallery]}]
  (into [:div.clearfix.mxn1]
        (for [{:keys [status resizable-url]} (:images gallery)]
          [:div.col.col-12.col-4-on-tb-dt.px1.pb2
           {:key resizable-url}
           [:div
            (ui/aspect-ratio 1 1
                             (if (= "approved" status)
                               [:img.col-12 {:src resizable-url}]
                               pending-approval))]])))

(defcomponent component [{:keys [store]} owner opts]
  [:div.container
   (title store)
   (images store)])

(defn query [data]
  {:store (get-in data keypaths/store)})

(defn built-component [data opts]
  (component/build component (query data) nil))

(defmethod effects/perform-effects events/navigate-store-gallery [_ event args _ app-state]
  #?(:cljs (api/get-store-gallery {:stylist-id (get-in app-state keypaths/store-stylist-id)})))

(defmethod effects/perform-effects events/api-success-store-gallery-fetch [_ event args _ app-state]
  (when (empty? (get-in app-state keypaths/store-gallery-images))
    (effects/page-not-found)))
