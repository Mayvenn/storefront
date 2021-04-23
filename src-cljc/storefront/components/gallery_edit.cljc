(ns storefront.components.gallery-edit
  (:require #?@(:cljs [MuuriReact
                       react
                       [storefront.api :as api]
                       [storefront.loader :as loader]])
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [spice.maps :as maps]
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

#?(:cljs (defn do-nothing-handler [e]
           (.preventDefault e)
           (.stopPropagation e)))


#?(:cljs (def reorder-mode-attrs
           {:on-click     do-nothing-handler
            :style        {:touchAction "none"
                           :filter       "brightness(0.5)"}
            :on-context-menu do-nothing-handler}))

#?(:cljs
   (def currently-dragging-post-attrs
     {:style {:filter "none"}}))

#?(:cljs (defn view-mode-attrs [photo-id]
           (merge (utils/route-to events/navigate-gallery-photo {:photo-id photo-id})
                  {:style           {:touchAction "pan-y"}
                   :on-context-menu do-nothing-handler})))

#?(:cljs
   (defn base-container-attrs [post-id]
     {:data-post-id post-id
      :style {:padding "1px"}}))


(defcomponent child-node
  [{react-key :key
    :keys     [currently-dragging-post-id post images reorder-mode]} _ _]
  (component/html
   #?(:clj [:div]
      :cljs
      [:div {:key react-key}
       (let [{:keys [image-ordering]} post
             {:keys [status id resizable-url post-id]}
             ;; TODO: refactor this out of the child node and into the top level post data structure.
             ;; The image is the "COVER photo" for the post
             (->> images
                  (filter #(= (:id %) (first image-ordering)))
                  first)]
         (ui/aspect-ratio 1 1
                          [:div.container-size
                           (maps/deep-merge
                            (base-container-attrs post-id)
                            (if reorder-mode
                              reorder-mode-attrs
                              (view-mode-attrs post-id))
                            (when (= currently-dragging-post-id post-id)
                              currently-dragging-post-attrs))
                           (if (= "approved" status)
                             (ui/img {:class    "container-size"
                                      :style    {:object-position "50% 25%"
                                                 :object-fit      "cover"}
                                      :src      resizable-url
                                      :max-size 749})
                             pending-approval)]))])))

(defn add-photo-square-2 []
  #?(:cljs
     (let [set-draggable (MuuriReact/useDraggable)]
       (set-draggable false)))
  (component/html
   [:div
    [:a.block.pp1
     (merge (utils/route-to events/navigate-gallery-image-picker)
            {:key "add-photo"
             :data-test "add-to-gallery-link"
             :style     {:padding "1px"}})
     [:div.bg-pale-purple.white
      (ui/aspect-ratio 1 1
                       [:div.flex.flex-column.justify-evenly.container-size
                        [:div ui/nbsp]
                        [:div.center.bold {:style {:font-size "60px"}} "+"]
                        [:div.center.shout.title-3.proxima "Add Photo"]])]]]))



#?(:cljs
   (defn muuri-config [gallery-ref reorder-mode]
     (let [touch-action (if reorder-mode
                          "none"
                          "pan-y")]
       #js {:dragEnabled true
            :itemClass   "col-4"
            :dragCssProps #js {:touchAction touch-action}

            ;; By default muuri sets scale(1) or scale(0.5) onto items style tag
            ;; depending upon their visibility or hidden...ness.
            ;; Because it is on a style tag, it has higher specificity
            ;; than a class. This clears that so we can scale by adding a class
            ;; when dragging.
            :visibleStyles #js {}
            :hiddenStyles  #js {}

            :dragAutoScroll #js {:targets #js [#js {:element  js/window
                                                    :priority 0}
                                               #js {:element  gallery-ref
                                                    :priority 1
                                                    :axis     MuuriReact/AutoScroller.AXIS_X}]}

            :dragStartPredicate (fn [item event options]
                                  (messages/handle-message events/debounced-event-initialized
                                                           {:timeout 50
                                                            :message [events/control-stylist-gallery-drag-predicate-start-loop
                                                                      {:item item
                                                                       :event event
                                                                       :delay 500}]}))
            :dragSortPredicate  (fn [item _e]
                                  (some-> (.defaultSortPredicate MuuriReact/ItemDrag item)
                                          (#(when (not= 0 (.-index %)) %))))
            :propsToData        (fn [item]
                                  (let [props (get-in (js->clj item) ["props"])]
                                    {:post         (:post props)
                                     :reorder-mode (:reorder-mode props)}))
            :onDragStart        (fn [item _event]
                                  (messages/handle-message events/control-stylist-gallery-drag-begun
                                                           {:post-id (-> item .getData :post :id )})
                                  (messages/handle-message events/stylist-gallery-reorder-mode-entered
                                                           {:item item}))
            :onDragEnd          (fn [item _event]
                                  (->> item
                                       .getGrid
                                       .getItems
                                       (keep #(some-> % .getData :post :id))
                                       (assoc {} :posts-ordering)
                                       (messages/handle-message events/control-stylist-gallery-reordered-v2))
                                  (messages/handle-message events/stylist-gallery-reorder-mode-exited
                                                           {:item item}))})))

;; TODO: There is an api call to change sort order on drag completed (talk to diva. maybe need to debounce)
;; Ordering is not being retained
;; Move first element not draggable logic into Muuri config

(defdynamic-component reorderable-component-2
  (constructor [this props]
               (component/create-ref! this "gallery")
               nil)
  ;; TODO:: Flash shows incorrectly on page
  (render [this]
          (let [{:keys [posts images reorder-mode currently-dragging-post-id]} (component/get-props this)
                gallery-ref (component/use-ref this "gallery")]
            (component/html
             #?(:clj [:div]
                :cljs [:div

                       {:ref gallery-ref}
                       (apply react/createElement
                              MuuriReact/MuuriComponent
                              (muuri-config gallery-ref reorder-mode)
                              (cons
                               (component/build add-photo-square-2 {:key "add-photo"})
                               (for [post (sort-by (comp first :image-ordering) posts)]
                                 (component/build child-node {:key                        (:id post)
                                                              :reorder-mode               reorder-mode
                                                              :currently-dragging-post-id currently-dragging-post-id
                                                              :post                       post
                                                              :images                     images}))))])))))

(defmethod transitions/transition-state events/control-stylist-gallery-drag-begun
  [_ _ {:keys [post-id]} app-state]
  (assoc-in app-state keypaths/stylist-gallery-currently-dragging-post post-id))

(defmethod transitions/transition-state events/stylist-gallery-reorder-mode-entered
  [_ _ args app-state]
  (assoc-in app-state keypaths/stylist-gallery-reorder-mode true))

(defmethod transitions/transition-state events/stylist-gallery-reorder-mode-exited
  [_ _ args app-state]
  (-> app-state
      (assoc-in keypaths/stylist-gallery-reorder-mode false)
      (update-in keypaths/stylist-gallery dissoc :currently-dragging-post)))

(defcomponent reorderable-wrapper
  [data _ _]
  [:div
   ;; TODO: real loader. Make sure page works when experiment is turned on while on the page
   (if true #_(seq (:posts data))
     (component/build reorderable-component-2 data)
     (ui/large-spinner {:style {:height "6em"}}))])

(defmethod transitions/transition-state events/control-stylist-gallery-reordered-v2
  [_ _ {:keys [posts-ordering]} app-state]
  #?(:cljs (update-in app-state keypaths/user-stylist-gallery-images
                      #(sort-by (fn [image]
                                  (.indexOf posts-ordering (:id image))) %))))


#?(:cljs
   (defn set-dragger-touch [dragger value]
     (.setCssProps dragger #js {:touchAction value})))

(defmethod effects/perform-effects events/control-stylist-gallery-drag-predicate-start-loop
  [_ _ {:keys [item event delay startTime]} app-state]
  #?(:cljs
     (let [eventType (.-type event)
           drag (.-_drag item)
           dragger (.-_dragger drag)
           now (.getTime (js/Date.))
           startTime' (or startTime now)]
      (cond
         (not= eventType "start") (set-dragger-touch dragger "pan-y")

         (> (- now startTime) delay) (do
                                       (set-dragger-touch dragger "none")
                                       (._forceResolveStartPredicate drag event))

         :otherwise (messages/handle-message events/debounced-event-initialized
                                  {:timeout 50
                                   :message [events/control-stylist-gallery-drag-predicate-start-loop
                                             {:item item
                                              :event event
                                              :delay delay
                                              :startTime startTime'}]})))))

(defmethod effects/perform-effects events/control-stylist-gallery-reordered-v2
  [_ _ {:keys [posts-ordering]} app-state]
  #?(:cljs (api/reorder-store-gallery {:user-id        (get-in app-state keypaths/user-id)
                                       :user-token     (get-in app-state keypaths/user-token)
                                       :posts-ordering posts-ordering})))

(defmethod effects/perform-effects events/control-stylist-gallery-delete-v2
  [_ _ {:keys [post-id]} app-state]
  #?(:cljs (api/delete-v2-gallery-post {:user-id    (get-in app-state keypaths/user-id)
                                        :user-token (get-in app-state keypaths/user-token)
                                        :post-id    post-id})))

(defn query [state]
  {:gallery (get-in state keypaths/user-stylist-gallery-images)})

(defn query-v2 [state]
  {:images                     (get-in state keypaths/user-stylist-gallery-images)
   :posts                      (get-in state keypaths/user-stylist-gallery-posts)
   :reorder-mode               (get-in state keypaths/stylist-gallery-reorder-mode)
   :currently-dragging-post-id (get-in state keypaths/stylist-gallery-currently-dragging-post)})

(defn ^:export built-component [data opts]
  (if (experiments/edit-gallery? data)
    (component/build reorderable-wrapper (query-v2 data))
    (component/build static-component (query data) nil)))

(defmethod effects/perform-effects events/navigate-gallery-edit [_ event args _ app-state]
  #?(:cljs (if (auth/stylist? (auth/signed-in app-state))
             ((if (experiments/edit-gallery? app-state)
                api/get-v2-stylist-gallery
                api/get-stylist-gallery)
              {:user-id    (get-in app-state keypaths/user-id)
               :user-token (get-in app-state keypaths/user-token)})
             (effects/redirect events/navigate-store-gallery))))

(defmethod transitions/transition-state events/api-success-stylist-gallery
  [_ event {:keys [images posts]} app-state]
  (-> app-state
      (assoc-in keypaths/user-stylist-gallery-images images)
      (assoc-in keypaths/user-stylist-gallery-posts posts)))

(defmethod effects/perform-effects events/poll-gallery [_ event args _ app-state]
  #?(:cljs (when (auth/stylist? (auth/signed-in app-state))
             ((if (experiments/edit-gallery? app-state)
                api/get-v2-stylist-gallery
                api/get-stylist-gallery)
              {:user-id    (get-in app-state keypaths/user-id)
               :user-token (get-in app-state keypaths/user-token)}))))

 ;; TODO(ellie, 2021-04-21): Move these somewhere better?
(defmethod effects/perform-effects events/debounced-event-enqueued
  [_ _ {:keys [debounced-event]} prev-app-state app-state]
  #?(:cljs
     (when-let [timer (get-in prev-app-state (conj keypaths/debounce-timers debounced-event))]
       (js/clearTimeout timer))))

(defmethod transitions/transition-state events/debounced-event-enqueued
  [_ _ {:keys [timer debounced-event]} app-state]
  (assoc-in app-state (conj keypaths/debounce-timers debounced-event) timer))

(defmethod effects/perform-effects events/debounced-event-initialized
  [_ _ {[debounced-event debounced-event-args] :message
        timeout                                :timeout} prev-app-state app-state]
  (when-let [debounce-timer (messages/handle-later debounced-event debounced-event-args timeout)]
    (messages/handle-message events/debounced-event-enqueued {:timer           debounce-timer
                                                              :debounced-event debounced-event})))


(defmethod effects/perform-effects events/api-success-stylist-gallery [_ event args _ app-state]
  (let [signed-in-as-stylist? (auth/stylist? (auth/signed-in app-state))
        on-edit-page?         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery-edit])]
    (when (and signed-in-as-stylist?
               on-edit-page?
               (some (comp #{"pending"} :status) (get-in app-state keypaths/user-stylist-gallery-images)))
      (messages/handle-message events/debounced-event-initialized {:timeout 5000
                                                                   :message [events/poll-gallery {}]}))))
