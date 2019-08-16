(ns storefront.components.gallery-edit
  (:require #?@(:cljs [[storefront.accessors.auth :as auth]
                       [storefront.api :as api]])
            [storefront.accessors.auth :as auth]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]))


(def title
  [:div.p2.center
   [:h2 [:img {:style {:height "50px"}
                  :src (assets/path "/images/icons/gallery-profile.png")}]]
   [:h1 "Your Gallery"]
   [:p "Show off your best work to your clients by uploading images of your #MayvennMade hairstyles."]])

(defn manage-section [gallery-images editing? adding-photo?]
  [:div.p2.center.dark-gray.bg-light-gray
   [:h1 "Manage your gallery"]
   [:div.p1 "Here you can upload images, edit posts and manage your gallery settings."]
   (ui/narrow-container
    [:div
     [:div.p1 (ui/teal-button (merge (utils/route-to events/navigate-gallery-image-picker)
                                     {:data-test "add-to-gallery-link"
                                      :spinning?  adding-photo?})
                              "Choose an image to upload")]
     (when (seq gallery-images)
       [:div.p1
        (if editing?
          (ui/dark-gray-button (utils/fake-href events/control-cancel-editing-gallery) "Finish editing")
          (ui/ghost-button (utils/fake-href events/control-edit-gallery) "Edit your gallery"))])])])

(def pending-approval
  (component/html
   [:div.container-size.bg-dark-gray.white.medium.flex.items-center.center.p2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defn images [editing? gallery-images]
  (into [:div.clearfix.mxn1]
        (for [{:keys [status resizable-url]} gallery-images]
          [:div.col.col-12.col-4-on-tb-dt.px1.pb2
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

(defn component [{:keys [editing? adding-photo? gallery]} owner opts]
  (component/create
   [:div.container
    title
    (manage-section gallery editing? adding-photo?)
    (images editing? gallery)]))

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
