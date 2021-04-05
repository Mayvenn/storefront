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

;; TODO: Fix gallery not showing on hard load. "Works" once a change has been saved
;; NOTE: Don't forget the experiment

(def add-reorderable-photo-square
  [:a.block.col-4.pp1.bg-pale-purple.white.board-item
   (merge (utils/route-to events/navigate-gallery-image-picker)
          {:data-test "add-to-gallery-link"})
   (ui/aspect-ratio 1 1
                    [:div.flex.flex-column.justify-evenly.container-size
                     [:div ui/nbsp]
                     [:div.center.bold
                      {:style {:font-size "60px"}}
                      "+"]
                     [:div.center.shout.title-3.proxima "Add Photo"]])])

(defdynamic-component reorderable-component
  (constructor [this props]
               (component/create-ref! this "gallery")
               {})
  (did-mount [this]
             #?(:cljs (-> (component/get-ref this "gallery")
                          (js/Muuri.
                           #js
                           {:items              ".board-item"
                            :dragEnabled        true
                            ;; TODO: Set sort to the current state :gallery_posts_ordering
                            :dragSort           true
                            :dragAutoScroll     drag-auto-scroll
                            :dragStartPredicate (fn [item event]
                                                  (when (and (-> item
                                                                 .getGrid
                                                                 .getItems
                                                                 (.indexOf item)
                                                                 (not= 0))
                                                             (-> event
                                                                 .-deltaTime
                                                                 (> 50)))
                                                    true))
                            :dragSortPredicate  (fn [item]
                                                  (some-> js/Muuri
                                                          .-ItemDrag
                                                          (.defaultSortPredicate item)
                                                          (#(when (not= 0 (.-index %)) %))))})
                          (.on "dragEnd" (fn [item _event]
                                           (->> item
                                               .getGrid
                                               .getItems
                                               (keep #(some-> % .getElement .-dataset .-photoId js/parseInt))
                                               ((fn [photo-order] {:photo-order photo-order}))
                                               (messages/handle-message events/control-stylist-gallery-reordered)))))))
  (render [this]
          (let [posts  (:posts (component/get-props this))
                images (:images (component/get-props this))]
     (component/html (into [:div
                            {:ref (component/use-ref this "gallery")}
                            add-reorderable-photo-square]
                           (for [{:keys [image-ordering]} posts
                                 :let [{:keys [status resizable-url id]} (->> images
                                                                              (filter #(= (:id %) (first image-ordering)))
                                                                              first)]]
                             [:div.col-4.board-item.absolute
                              (merge (update (utils/route-to events/navigate-gallery-photo {:photo-id id})
                                             :on-click (fn [routing-fn] (fn [e]
                                                                          (when (-> e .-target (.closest ".muuri-item-releasing") not)
                                                                            (routing-fn e)))))
                                     {:key           resizable-url
                                      :data-photo-id id})
                              (ui/aspect-ratio 1 1
                                               (if (= "approved" status)
                                                 (ui/img {:class    "container-size"
                                                          :style    {:object-position "50% 25%"
                                                                     :object-fit      "cover"}
                                                          :src      resizable-url
                                                          :max-size 749})
                                                 pending-approval))]))))))

(defmethod transitions/transition-state events/control-stylist-gallery-reordered
  [_ _ {:keys [photo-order]} app-state]
  #?(:cljs (update-in app-state keypaths/user-stylist-gallery-images
                      #(sort-by (fn [image]
                                  (.indexOf photo-order (:id image))) %))))

(defn query [state]
  {:images (get-in state keypaths/user-stylist-gallery-images)
   :posts (get-in state keypaths/user-stylist-gallery-posts)})

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
  [_ event {:keys [images posts]} app-state]
  (-> app-state
      (assoc-in keypaths/user-stylist-gallery-images images)
      (assoc-in keypaths/user-stylist-gallery-posts posts)))

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
