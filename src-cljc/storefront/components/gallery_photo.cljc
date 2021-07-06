(ns storefront.components.gallery-photo
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.accessors.experiments :as experiments]
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

(defn delete-modal [{:delete-modal/keys [close-event title subtitle id target]
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
      (merge (when target (apply utils/fake-href target))
             {:class     "container-size"
              :data-test "delete-photo-confirm"
              :disabled? (nil? target)})
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
       (ui/img {:src                           resizable-url
                :class                         "col-12"
                :square-size                   "400"
                :preserve-url-transformations? true})
       (ui/button-small-underline-red {:on-click  (utils/send-event-callback e/control-show-gallery-photo-delete-modal)
                                       :data-test "delete-photo"} "Delete")]]]))

(defn query [state]
  (let [photo-id           (numbers/parse-int (get-in state (conj keypaths/navigation-message 1 :photo-id)))
        gallery            (get-in state keypaths/user-stylist-gallery-images)
        show-delete-modal? (= :gallery-photo-delete (get-in state keypaths/popup))
        image              (some #(when (= photo-id (:id %)) %) gallery)
        min-approved-imgs? (and (= "aladdin" (get-in state keypaths/user-stylist-experience))
                                (->> gallery (filter (comp (partial = "approved") :status))
                                     count
                                     (< 3)))
        cant-delete        (not min-approved-imgs?)]
    {:photo                    image
     :back-link                {:navigation-event e/navigate-gallery-edit}
     :delete-modal/close-event e/control-popup-hide
     :delete-modal/title       (if cant-delete
                                 "Can't delete"
                                 "Are you sure?")
     :delete-modal/subtitle    (if cant-delete
                                 "You need to maintain at least 3 images in your gallery."
                                 "You are about to delete this photo permanently.")
     :delete-modal/id          (when show-delete-modal? "delete-photo-modal")
     :delete-modal/target      (when min-approved-imgs?
                                 (if (experiments/edit-gallery? state)
                                   [e/control-stylist-gallery-delete-v2 {:post-id (:post-id image)}]
                                   [e/control-delete-gallery-image {:image-url (:resizable-url image)}]))}))

(defmethod fx/perform-effects e/control-delete-gallery-image
  [_ event {:keys [image-url]} _ app-state]
  #?(:cljs (api/delete-gallery-image (get-in app-state keypaths/user-id)
                                     (get-in app-state keypaths/user-token)
                                     image-url)))

(defmethod fx/perform-effects e/api-success-stylist-gallery-delete
  [_ event args _ app-state]
  #?(:cljs
     (history/enqueue-navigate e/navigate-gallery-edit)))

(defmethod t/transition-state e/control-show-gallery-photo-delete-modal
  [_ _ _ state]
  (assoc-in state keypaths/popup :gallery-photo-delete))

(defn built-component [data opts]
  (component/build component (query data) nil))
