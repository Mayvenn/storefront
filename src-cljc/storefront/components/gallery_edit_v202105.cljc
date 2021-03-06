(ns storefront.components.gallery-edit-v202105
  (:require #?@(:cljs [MuuriReact
                       react
                       [storefront.api :as api]])
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [spice.maps :as maps]
            [storefront.transitions :as transitions]))

(def drag-delay 150)
(def drag-start-predicate-rate 50)
(def reorder-api-call-debounce-period 500)

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

#?(:cljs (defn do-nothing-handler [e]
           (.preventDefault e)
           (.stopPropagation e)))

#?(:cljs (def reorder-mode-attrs
           {:on-click do-nothing-handler
            :style    {:filter "brightness(0.5)"}}))

#?(:cljs
   (def currently-dragging-post-attrs
     {:style {:filter "none"}}))

#?(:cljs (defn view-mode-attrs [photo-id]
           (when photo-id
             (utils/route-to events/navigate-gallery-photo {:photo-id photo-id}))))

#?(:cljs
   (defn base-container-attrs [post-id]
     {:data-test       (str "post-id-" post-id)
      :on-context-menu do-nothing-handler
      :style           {:padding "1px"}}))

(defcomponent post-thumbnail
  [{react-key :key
    :keys     [currently-dragging-post-id post reorder-mode?]} _ _]
  (component/html
   #?(:clj [:div]
      :cljs
      [:div {:key react-key}
       (let [{:keys [status id resizable-url post-id]} (:cover-image post)]
         [:div.p1-on-dt
          (ui/aspect-ratio 1 1
                           [:div.container-size
                            (maps/deep-merge
                             (base-container-attrs post-id)
                             (if reorder-mode?
                               reorder-mode-attrs
                               (view-mode-attrs id))
                             (when (= currently-dragging-post-id post-id)
                               currently-dragging-post-attrs))
                            (if (= "approved" status)
                              (ui/img {:class                         "container-size"
                                       :style                         {:object-position "50% 25%"
                                                                       :object-fit      "cover"
                                                                       :touch-action    "pan-y"}
                                       :preserve-url-transformations? true
                                       :src                           resizable-url
                                       :max-size                      749})
                              pending-approval)])])])))

(defn add-post-square []
  #?(:cljs
     (let [set-draggable (MuuriReact/useDraggable)]
       (set-draggable false)))
  (component/html
   [:div.p1-on-dt
    [:a.block.pp1
     (merge (utils/route-to events/navigate-gallery-image-picker)
            {:key "add-post"
             :data-test "add-post-to-gallery-link"
             :style     {:padding "1px"}})
     [:div.bg-pale-purple.white
      (ui/aspect-ratio 1 1
                       [:div.flex.flex-column.justify-evenly.container-size
                        [:div ui/nbsp]
                        [:div.center.bold {:style {:font-size "60px"}} "+"]
                        [:div.center.shout.title-3.proxima "Add Post"]])]]]))

(def muuri-event-listener
  (memoize
   (fn
     [event]
     (fn [item muuri-event & other-args]
       (messages/handle-message event
                                {:item item
                                 :muuri-event muuri-event})))))

(def muuri-drag-start-predicate (muuri-event-listener events/control-stylist-gallery-posts-drag-predicate-initialized))
(def muuri-on-drag-start (muuri-event-listener events/control-stylist-gallery-posts-drag-began))
(def muuri-on-drag-end (muuri-event-listener events/control-stylist-gallery-posts-drag-ended))

(def muuri-sort-fn (memoize (fn [post-ordering]
                              (fn [{a-post :post :as a} {b-post :post :as b}]
                                (let [a-post-id  (:id a-post)
                                      b-post-id  (:id b-post)
                                      first-post (some #{a-post-id b-post-id} post-ordering)]
                                  (cond
                                    (nil? a-post-id)         -1
                                    (nil? b-post-id)         1
                                    (= a-post-id first-post) -1
                                    (= b-post-id first-post) 1))))))


(defn muuri-drag-sort-predicate [item _muuri-event]
  #?(:cljs
     (some-> (.defaultSortPredicate MuuriReact/ItemDrag item)
             (#(when (not= 0 (.-index %)) %)))))

(defn muuri-props-to-data [item]
  #?(:cljs
     (let [props (get-in (js->clj item) ["props"])]
       {:post          (:post props)
        :reorder-mode? (:reorder-mode? props)})))

#?(:cljs
   (defn muuri-config [gallery-ref reorder-mode? post-ordering]
     #js {:dragEnabled  false
          :itemClass    "col-4-col-3-on-dt"
          ;; ↓ These prevent long presses on the drag handler from
          ;; ↓ selecting or scrolling. It is very important.
          :dragCssProps #js {:touchAction  "none !important"
                             :userSelect   "none !important"
                             :touchCallout "none !important"}

          ;; By default muuri sets scale(1) or scale(0.5) onto items style tag
          ;; depending upon their visibility or hidden...ness.
          ;; Because it is on a style tag, it has higher specificity
          ;; than a class. This clears that so we can scale by adding a class
          ;; when dragging.
          :visibleStyles #js {}
          :hiddenStyles  #js {}
          :dragHandle    ".drag-handle"

          :dragAutoScroll #js {:targets #js [#js {:element  js/window
                                                  :priority 0}
                                             #js {:element  gallery-ref
                                                  :priority 1
                                                  :axis     MuuriReact/AutoScroller.AXIS_X}]}

          ;; ↓ Function which defines how sort works in the Muuri Grid
          :sort               (muuri-sort-fn post-ordering)
          ;; ↓ Function to determine whether or not a new ordering (post-drag) should be allowed
          :dragSortPredicate  muuri-drag-sort-predicate
          ;; ↓ Function to convert the children of the grid's (ie, post component) props into a specific shape.
          :propsToData        muuri-props-to-data
          ;; ↓ Function triggered by touching the drag handle
          :dragStartPredicate muuri-drag-start-predicate
          ;; ↓ Function triggered once a drag event starts.
          :onDragStart        muuri-on-drag-start
          ;; ↓ Function triggered once a drag event ends.
          :onDragEnd          muuri-on-drag-end}))

(defdynamic-component reorderable-component
  (constructor [this props]
               (component/create-ref! this "gallery")
               nil)
  ;; TODO:: Flash shows incorrectly on page (perhaps caused by lack of spinner?)
  (render [this]
          (let [{:stylist-gallery-my-gallery/keys [posts-with-cover post-ordering
                                                   reorder-mode? currently-dragging-post-id]} (component/get-props this)
                gallery-ref                                                                   (component/use-ref this "gallery")]
            (component/html
             #?(:clj [:div]
                :cljs [:div.max-1080.mx-auto
                       {:ref gallery-ref}
                       (apply react/createElement
                              MuuriReact/MuuriComponent
                              (muuri-config gallery-ref reorder-mode? post-ordering)
                              (cons
                               (component/build add-post-square {:key "add-photo"})
                               (for [post posts-with-cover]
                                 (component/build post-thumbnail {:key                        (:id post)
                                                                  :reorder-mode?              reorder-mode?
                                                                  :currently-dragging-post-id currently-dragging-post-id
                                                                  :post                       post}))))])))))

(defcomponent reorderable-wrapper
  [{:as data :stylist-gallery-my-gallery/keys [id posts-with-cover fetching-posts? appending-post?]} _ _]
  (if id
    [:div.py8-on-dt
     {:key id}
     (if (or (and (empty? posts-with-cover)
                  fetching-posts?)
             appending-post?)
       (ui/large-spinner {:style {:height "6em"}})
       (component/build reorderable-component data))]
    [:div]))

(defmethod transitions/transition-state events/control-stylist-gallery-posts-drag-began
  [_ _ {:keys [item]} app-state]
  (let [post-id (-> item .getData :post :id)]
    (-> app-state
        (assoc-in keypaths/user-stylist-gallery-new-posts-ordering [])
        (assoc-in keypaths/stylist-gallery-reorder-mode true)
        (assoc-in keypaths/stylist-gallery-currently-dragging-post post-id))))

(defmethod transitions/transition-state events/control-stylist-gallery-posts-drag-ended
  [_ _ {:keys [item]} app-state]
  (-> app-state
      (assoc-in keypaths/user-stylist-gallery-new-posts-ordering (->> item
                                                                 .getGrid
                                                                 .getItems
                                                                 (keep #(some-> % .getData :post :id))))
      (assoc-in keypaths/stylist-gallery-reorder-mode false)
      (update-in keypaths/stylist-gallery dissoc :currently-dragging-post)))

(defmethod effects/perform-effects events/control-stylist-gallery-posts-drag-ended
  [_ _ args _ app-state]
  #?(:cljs
     (let [posts-ordering (get-in app-state keypaths/user-stylist-gallery-new-posts-ordering)
           event-args     {:user-id        (get-in app-state keypaths/user-id)
                           :user-token     (get-in app-state keypaths/user-token)
                           :posts-ordering posts-ordering}]
       (messages/handle-message
        events/debounced-event-initialized
        {:timeout reorder-api-call-debounce-period
         :message [events/stylist-gallery-posts-reordered event-args]}))))

(defmethod effects/perform-effects events/stylist-gallery-posts-reordered
  [_ _ {:keys [user-id user-token posts-ordering]} _ app-state]
  #?(:cljs
     (api/reorder-store-gallery {:user-id user-id
                                 :user-token user-token
                                 :posts-ordering posts-ordering})))

(defmethod effects/perform-effects events/control-stylist-gallery-posts-drag-predicate-initialized
  [_ _ args _ app-state]
  #?(:cljs
     (messages/handle-message events/debounced-event-initialized
                              {:timeout drag-start-predicate-rate
                               :message [events/stylist-gallery-posts-drag-predicate-loop args]})))

(defmethod effects/perform-effects events/control-stylist-gallery-delete-v2
  [_ _ {:keys [post-id]} _ app-state]
  #?(:cljs (api/delete-v2-gallery-post {:user-id    (get-in app-state keypaths/user-id)
                                        :user-token (get-in app-state keypaths/user-token)
                                        :post-id    post-id})))

(defmethod effects/perform-effects events/stylist-gallery-posts-drag-predicate-loop
  [_ _ {:keys [item muuri-event startTime]} _ app-state]
  #?(:cljs
     (let [eventType  (.-type muuri-event)
           drag       (.-_drag item)
           dragger    (.-_dragger drag)
           now        (.getTime (js/Date.))
           startTime' (or startTime now)]
       (cond
         (> (- now startTime') drag-delay)   (._forceResolveStartPredicate drag muuri-event)

         :else                               (messages/handle-message
                                              events/debounced-event-initialized
                                              {:timeout drag-start-predicate-rate
                                               :message [events/stylist-gallery-posts-drag-predicate-loop
                                                         {:item        item
                                                          :muuri-event muuri-event
                                                          :delay       drag-delay
                                                          :startTime   startTime'}]})))))
