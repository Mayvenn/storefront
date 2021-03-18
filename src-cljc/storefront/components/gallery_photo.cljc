(ns storefront.components.gallery-photo
  (:require #?@(:cljs [[storefront.accessors.experiments :as experiments]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]
            [storefront.transitions :as t]
            ui.molecules))

(defn delete-modal [{:delete-modal/keys [close-event title subtitle id]
                     :keys              [photo]}]
  [:div.bg-white.p3
   {:data-test id}
   [:div.right-align [:a (merge (utils/fake-href close-event)
                                {:data-test "delete-photo-close"})
                      (svg/x-sharp {:height "20px"
                                    :width  "20px"})]]
   [:div.p3.flex.flex-column
    [:div.py6
     (svg/pink-bang {:height "36px"
                     :width  "36px"})]
    [:div.mb2.pt6.canela.title-1 title]
    [:div.my2.pb10 subtitle]
    [:div.pt10.mb2.flex
     (ui/button-medium-underline-black (merge (utils/fake-href close-event)
                                              {:class     "container-size btn-medium"
                                               :data-test "delete-photo-cancel"}) "Cancel")
     [:div.mx2]
     (ui/button-medium-red
      (merge (utils/fake-href e/control-delete-gallery-image {:image-url (:resizable-url photo)})
             {:class     "container-size"
              :data-test "delete-photo-confirm"})
      "delete")]]])

(defcomponent component [{:keys [photo back-link] :as data} owner opts]
  (let [{:keys [id status resizable-url]} photo]
    [:div.max-580.mx-auto.center
     [:div.p2
      (ui.molecules/return-link
       {:return-link/id            "back to gallery"
        :return-link/copy          "Back to Gallery"
        :return-link/event-message [(:navigation-event back-link)]})]
     (when (:delete-modal/id data)
       (ui/modal {:close-attrs (utils/fake-href e/control-popup-hide)}
                 (delete-modal data)))
     [:div.container.bg-warm-gray.p3
      [:div.bg-white.p3.container
       (ui/img {:src resizable-url :class "col-12" :square-size "400"})
       (ui/button-small-underline-red {:on-click  (utils/send-event-callback e/control-show-gallery-photo-delete-modal)
                                       :data-test "delete-photo"} "Delete")]]]))

(defn query [state]
  (let [photo-id           (numbers/parse-int (get-in state (conj keypaths/navigation-message 1 :photo-id)))
        gallery            (get-in state keypaths/user-stylist-gallery-images)
        show-delete-modal? (= :gallery-photo-delete (get-in state keypaths/popup))]
    {:photo                    (some #(when (= photo-id (:id %)) %) gallery)
     :back-link                {:navigation-event e/navigate-gallery-edit}
     :delete-modal/close-event e/control-popup-hide
     :delete-modal/title       "Are you sure?"
     :delete-modal/subtitle    "You are about to delete this photo permanently."
     :delete-modal/id          (when show-delete-modal? "delete-photo-modal")}))

(defmethod fx/perform-effects e/control-delete-gallery-image
  [_ event {:keys [image-url]} _ app-state]
  #?(:cljs (api/delete-gallery-image (get-in app-state keypaths/user-id)
                                     (get-in app-state keypaths/user-token)
                                     image-url)))

(defmethod fx/perform-effects e/api-success-stylist-gallery-delete
  [_ event args _ app-state]
  #?(:cljs
     (when (experiments/edit-gallery? app-state)
       (history/enqueue-navigate e/navigate-gallery-edit))))

(defmethod t/transition-state e/control-show-gallery-photo-delete-modal
  [_ _ _ state]
  (assoc-in state keypaths/popup :gallery-photo-delete))

(defn built-component [data opts]
  (component/build component (query data) nil))
