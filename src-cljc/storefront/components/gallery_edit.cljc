(ns storefront.components.gallery-edit
  (:require #?@(:cljs [[storefront.api :as api]
                       Muuri])
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defdynamic-component defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]))

(def add-photo-square
  [:a.block.col-4.pp1.bg-pale-purple.white
   (merge (utils/route-to events/navigate-gallery-image-picker)
          {:data-test "add-to-gallery-link"})
   (ui/aspect-ratio 1 1
                    [:div.flex.flex-column.justify-evenly.container-size
                     [:div ui/nbsp]
                     [:div.center.bold
                      {:style {:font-size "60px"}}
                      "+"]
                     [:div.center.shout.title-3.proxima "Add Photo"]])])

(def pending-approval
  (component/html
   [:div.container-size.bg-gray.flex.items-center.center.p2.proxima.content-2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defcomponent static-component [{:keys [gallery]} owner opts]
  [:div.container
   (into [:div.clearfix.mxn1.flex.flex-wrap
          add-photo-square]
         (for [{:keys [status resizable-url id]} gallery]
           [:a.col-4.pp1.inherit-color
            (merge (utils/route-to events/navigate-gallery-photo {:photo-id id})
                   {:key resizable-url})
            (ui/aspect-ratio 1 1
                             (if (= "approved" status)
                               (ui/img {:class    "container-size"
                                        :style    {:object-position "50% 25%"
                                                   :object-fit      "cover"}
                                        :src      resizable-url
                                        :max-size 749})
                               pending-approval))]))])

;; TODO: Check if this is necessary
(defn drag-auto-scroll
  []
  #?(:cljs #js {:targets (fn [item]
               [#js {:element js/window
                 :priority 0}
                #js {:element (.-parentNode (.getElement (.getGrid item)))
                 :priority 0}])}
     :clj nil))

;; TODO: Remove onClick event when dragging is released
;; TODO: Fix gallery not showing on hard load. "Works" once a change has been saved
;; NOTE: Don't forget the experiment

(defdynamic-component reorderable-component
  (constructor
   [this props]
   (component/create-ref! this "gallery"))
  (did-mount
   [this]
   #?(:cljs (some-> (js/Muuri.
                     (component/get-ref this "gallery")
                     #js
                     {:items          ".board-item"
                      :dragEnabled    true
                      ;; TODO: Set sort to the current state :gallery_posts_ordering
                      :dragSort       true
                      :dragAutoScroll drag-auto-scroll})
                    ;; Usefull to see what's going on in the object
                    spice.core/spy)
      :clj identity))

  (render
   [this]
   (let [gallery (:gallery (component/get-props this))]
     ;; TODO: Remove spacing between posts. Turning overflow:hidden off reveals
     ;; the rest of the photos. Could add stretch but it extends the vertical
     ;; height past the bottom
     (component/html [:div.container
                      (into [:div.clearfix.mxn1.flex.flex-wrap
                             {:ref (component/use-ref this "gallery")}
                             add-photo-square]
                            (for [{:keys [status resizable-url id]} gallery]
                              [:div.col-4.pp1.inherit-color.board-item
                               (merge (utils/route-to events/navigate-gallery-photo {:photo-id id})
                                      {:key resizable-url})
                               (ui/aspect-ratio 1 1
                                                (if (= "approved" status)
                                                  (ui/img {:class    "container-size"
                                                           :style    {:object-position "50% 25%"
                                                                      :object-fit      "cover"}
                                                           :src      resizable-url
                                                           :max-size 749})
                                                  pending-approval))]))]))))

(defn query [state]
  {:gallery (get-in state keypaths/user-stylist-gallery-images)})

(defn ^:export built-component [data opts]
  (if (experiments/edit-gallery? data)
    (component/build reorderable-component (query data) nil)
    (component/build static-component (query data) nil)))

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
