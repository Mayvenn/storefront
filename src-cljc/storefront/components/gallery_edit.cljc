(ns storefront.components.gallery-edit
  (:require #?@(:cljs [[storefront.accessors.auth :as auth]
                       [storefront.api :as api]])
            [storefront.accessors.auth :as auth]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]))

(defn manage-section [gallery-images editing? adding-photo?]
  [:div.p2.center.bg-warm-gray
   [:h1.mt6.mb3.title-1.canela "Your Gallery"]
   [:p.mb2.content-2.proxima "Show off your best work to your clients by uploading images of your #MayvennMade hairstyles."]
   (ui/narrow-container
    [:div
     [:div.p1.shout.col-9.mx-auto
      (ui/button-medium-primary
       (merge (utils/route-to events/navigate-gallery-image-picker)
              {:data-test "add-to-gallery-link"
               :spinning? adding-photo?})
                                "Upload Photos")]
     (when (seq gallery-images)
       [:div.p1.mb4
        (if editing?
          (ui/button-small-underline-primary (utils/fake-href events/control-cancel-editing-gallery) "Finish editing")
          (ui/button-small-underline-primary (utils/fake-href events/control-edit-gallery) "Edit Gallery"))])])])

(def pending-approval
  (component/html
   [:div.container-size.bg-gray.white.medium.flex.items-center.center.p2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defn images [editing? gallery-images]
  (into [:div.clearfix.mxn1]
        (for [{:keys [status resizable-url]} gallery-images]
          [:div.col.col-12.col-4-on-tb-dt.px1
           {:key resizable-url}
           [:div
            (when editing?
              [:div.bg-black.white.p2.flex.h6.medium
               [:span.flex-auto.right-align.mr2 "Delete this post"]
               (ui/modal-close {:close-attrs (merge
                                              (utils/fake-href events/control-delete-gallery-image {:image-url resizable-url})
                                              {:class "line-height-1"})})])
            (ui/aspect-ratio 1 1
                             (if (= "approved" status)
                               [:img.col-12 {:src resizable-url}]
                               pending-approval))]])))

(defcomponent component [{:keys [editing? adding-photo? gallery]} owner opts]
  [:div.container
   (manage-section gallery editing? adding-photo?)
   (images editing? gallery)])

(defn query [data]
  {:editing?      (get-in data keypaths/editing-gallery?)
   :gallery       (get-in data keypaths/user-stylist-gallery-images)
   :adding-photo? (utils/requesting? data request-keys/append-gallery)})

(defn built-component [data opts]
  (component/build component (query data) nil))

(defmethod effects/perform-effects events/control-delete-gallery-image [_ event args _ app-state]
  #?(:cljs (let [{:keys [image-url]} args]
             (api/delete-gallery-image (get-in app-state keypaths/user-id)
                                       (get-in app-state keypaths/user-token)
                                       image-url))))

(defmethod effects/perform-effects events/navigate-gallery-edit [_ event args _ app-state]
  #?(:cljs (if (auth/stylist? (auth/signed-in app-state))
             (api/get-stylist-gallery {:user-id    (get-in app-state keypaths/user-id)
                                       :user-token (get-in app-state keypaths/user-token)})
             (effects/redirect events/navigate-store-gallery))))

(defmethod transitions/transition-state events/api-success-stylist-gallery
  [_ event {:keys [images]} app-state]
  (assoc-in app-state keypaths/user-stylist-gallery-images images))

(defmethod effects/perform-effects events/poll-gallery [_ event args _ app-state]
  #?(:cljs (when (auth/stylist? (auth/signed-in app-state))
             (api/get-stylist-gallery {:user-id    (get-in app-state keypaths/user-id)
                                       :user-token (get-in app-state keypaths/user-token)}))))

(defmethod effects/perform-effects events/api-success-stylist-gallery [_ event args _ app-state]
  (let [signed-in-as-stylist? (auth/stylist? (auth/signed-in app-state))
        on-edit-page?         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery-edit])]
    (when (and signed-in-as-stylist?
               on-edit-page?
               (some (comp #{"pending"} :status) (get-in app-state keypaths/user-stylist-gallery-images)))
      (messages/handle-later events/poll-gallery {} 5000))))
