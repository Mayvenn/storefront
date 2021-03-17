(ns storefront.components.gallery-photo
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]
            ui.molecules))

(defcomponent component [{:keys [photo back-link]} owner opts]
  (let [{:keys [id status resizable-url]} photo]
    [:div
     [:div.p2
      (ui.molecules/return-link
       {:return-link/id            "back to gallery"
        :return-link/copy          "Back to Gallery"
        :return-link/event-message [(:navigation-event back-link)]})]
     [:div.container.bg-warm-gray.p3
      [:div.bg-white.p3.container.center
       (ui/img {:src resizable-url :class "col-12" :square-size "400"})
       (ui/button-small-underline-red {} "Delete")]]]))

(defn query [state]
  (let [photo-id (numbers/parse-int (get-in state (conj keypaths/navigation-message 1 :photo-id)))
        gallery  (get-in state keypaths/user-stylist-gallery-images)]
    {:photo (some #(when (= photo-id (:id %)) %) gallery)
     :back-link {:navigation-event events/navigate-gallery-edit}}))

(defn built-component [data opts]
  (component/build component (query data) nil))
