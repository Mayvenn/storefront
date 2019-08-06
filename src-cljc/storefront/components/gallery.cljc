(ns storefront.components.gallery
  (:require [storefront.accessors.stylists :as stylists]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn title [own-store? {:keys [store-nickname]}]
  [:div.p2.center
   [:h2 [:img {:style {:height "50px"}
                  :src (assets/path "/images/icons/gallery-profile.png")}]]
   [:h1 (str store-nickname "'s Gallery")]
   [:p (if own-store?
         "Show off your best work to your clients by uploading images of your #MayvennMade hairstyles."
         (str "Scroll through "
              store-nickname
              "'s best #MayvennMade looks and get inspiration for your next style!"))]])

(defn manage-section [{:keys [gallery]} editing? adding-photo?]
  [:div.p2.center.dark-gray.bg-light-gray
   [:h1 "Manage your gallery"]
   [:div.p1 "Here you can upload images, edit posts and manage your gallery settings."]
   (ui/narrow-container
    [:div
     [:div.p1 (ui/teal-button (merge (utils/route-to events/navigate-gallery-image-picker)
                                     {:data-test "add-to-gallery-link"
                                      :spinning?  adding-photo?})
                              "Choose an image to upload")]
     (when (seq (:images gallery))
       ;;TODO change button depending upon state
       [:div.p1 (if editing?
                  (ui/dark-gray-button (utils/fake-href events/control-cancel-editing-gallery) "Finish editing")
                  (ui/ghost-button (utils/fake-href events/control-edit-gallery) "Edit your gallery"))])])])

(def pending-approval
  (component/html
   [:div.container-size.bg-dark-gray.white.medium.flex.items-center.center.p2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defn images [editing? {:keys [gallery]}]
  (into [:div.clearfix.mxn1]
        (for [{:keys [status resizable-url]} (:images gallery)]
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

(defn component [{:keys [store editing? own-store? adding-photo?]} owner opts]
  (component/create
   [:div.container
    (title own-store? store)
    (when own-store?
      (manage-section store editing? adding-photo?))
    (images editing? store)]))

(defn query [data]
  {:store         (get-in data keypaths/store)
   :editing?      (get-in data keypaths/editing-gallery?)
   :own-store?    (stylists/own-store? data)
   :adding-photo? (utils/requesting? data request-keys/append-gallery)})

(defn built-component [data opts]
  (component/build component (query data) nil))
