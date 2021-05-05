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
            [storefront.debounce :as debounce]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [spice.maps :as maps]
            [storefront.transitions :as transitions]))

(def drag-delay 150)
(def gallery-poll-rate 5000)
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
     {:data-post-id    post-id
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
         [:div
          (ui/aspect-ratio 1 1
                           [:div.container-size
                            (maps/deep-merge
                             (base-container-attrs post-id)
                             (if reorder-mode?
                               reorder-mode-attrs
                               (view-mode-attrs id))
                             (when (= currently-dragging-post-id post-id)
                               currently-dragging-post-attrs))
                            [:div.drag-handle.absolute.z4.top-0.left-0
                             {:class "mp2"
                              :style {:touch-action "none"}}
                             (ui/img {:width 25
                                      :height 25
                                      :style {:opacity 0.33}
                                      :src      "/images/icons/2d-drag-handle.png"})]
                            (if (= "approved" status)
                              (ui/img {:class    "container-size"
                                       :style    {:object-position "50% 25%"
                                                  :object-fit      "cover"
                                                  :touch-action "pan-y"}
                                       :src      resizable-url
                                       :max-size 749})
                              pending-approval)])])])))

(defn add-post-square []
  #?(:cljs
     (let [set-draggable (MuuriReact/useDraggable)]
       (set-draggable false)))
  (component/html
   [:div
    [:a.block.pp1
     (merge (utils/route-to events/navigate-gallery-image-picker)
            {:key "add-post"
             :data-test "add-post-to-gallery-link"
             :style     {:padding "1px"}})
     [:div.bg-pale-purple.white
      (ui/aspect-ratio 1 1
                       [:div.flex.flex-column.justify-evenly.container-size
                        [:div.drag-handle.hidden ui/nbsp]  ; Every Muuri Item needs a drag-handle (if using drag handles)
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
                              (fn [{a-post :post} {b-post :post}]
                                (let [a-post-id  (:id a-post)
                                      b-post-id  (:id b-post)
                                      first-post (some #{a-post-id b-post-id} post-ordering)]
                                  (cond
                                    (or (nil? a-post-id)
                                        (nil? b-post-id))    2
                                    (= a-post-id first-post) -1
                                    :else                    1))))))


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
     #js {:dragEnabled  true
          :itemClass    "col-4"
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
          (let [{:keys [posts-with-cover post-ordering reorder-mode? currently-dragging-post-id]} (component/get-props this)
                gallery-ref                                                                       (component/use-ref this "gallery")]
            (component/html
             #?(:clj [:div]
                :cljs [:div
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
  [{:as data :keys [posts fetching-posts? :appending-post?]} _ _]
  [:div
   (if (or (and (empty? posts)
                fetching-posts?)
           appending-post?)
       (ui/large-spinner {:style {:height "6em"}})
       (component/build reorderable-component data))])


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

(defn query [state]
  {:gallery (get-in state keypaths/user-stylist-gallery-images)})

(defn query-v2 [state]
  (let [images          (->> (get-in state keypaths/user-stylist-gallery-images)
                             (spice.maps/index-by :id))
        indexed-posts   (->> (get-in state keypaths/user-stylist-gallery-posts)
                             (map (fn [post] (->> post :image-ordering first (get images) (assoc post :cover-image))))
                             (spice.maps/index-by :id))
        post-ordering   (get-in state keypaths/user-stylist-gallery-initial-posts-ordering)
        sorted-posts    (map indexed-posts post-ordering)
        fetching-posts? (utils/requesting? state request-keys/get-stylist-gallery)]
    {:posts-with-cover           sorted-posts
     :post-ordering              post-ordering
     :fetching-posts?            fetching-posts?
     :appending-post?            (utils/requesting? state request-keys/append-gallery)
     :reorder-mode?              (get-in state keypaths/stylist-gallery-reorder-mode)
     :currently-dragging-post-id (get-in state keypaths/stylist-gallery-currently-dragging-post)}))

(defn ^:export built-component [data opts]
  (if (experiments/edit-gallery? data)
    (component/build reorderable-wrapper (query-v2 data))
    (component/build static-component (query data) nil)))

(defmethod effects/perform-effects events/navigate-gallery-edit [_ event args _ app-state]
  #?(:cljs
     (let [api-params {:user-id    (get-in app-state keypaths/user-id)
                       :user-token (get-in app-state keypaths/user-token)}]
       (cond
         (-> app-state auth/signed-in auth/stylist? not) (effects/redirect events/navigate-store-gallery)
         (experiments/edit-gallery? app-state)           (api/get-v2-stylist-gallery api-params)
         :else                                           (api/get-stylist-gallery api-params)))))

(defmethod transitions/transition-state events/navigate-gallery-edit [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/user-stylist-gallery-initial-posts-ordering [])
      (assoc-in keypaths/editing-gallery? false)))

(defmethod transitions/transition-state events/api-success-stylist-gallery-append
  [_ event {:keys [images posts]} app-state]
  (assoc-in app-state keypaths/user-stylist-gallery-initial-posts-ordering []))

(defmethod transitions/transition-state events/api-success-stylist-gallery
  [_ event {:keys [images posts]} app-state]
  (-> app-state
      (assoc-in keypaths/user-stylist-gallery-images images)
      (assoc-in keypaths/user-stylist-gallery-posts posts)

      ;; MuuriComponent expects the passed in children to be in the same order as the initial load
      ;; Without this, react replaces the children, but MuuriComponent does not update the Muuri Grid,
      ;; breaking the grid.
      (update-in keypaths/user-stylist-gallery-initial-posts-ordering (fn [ordering]
                                                                        (if (seq ordering)
                                                                          ordering
                                                                          (mapv :id posts))))))

(defmethod effects/perform-effects events/api-success-stylist-gallery [_ event args _ app-state]
  (let [signed-in-as-stylist? (auth/stylist? (auth/signed-in app-state))
        on-edit-page?         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery-edit])]
    (when (and signed-in-as-stylist?
               on-edit-page?
               (some (comp #{"pending"} :status) (get-in app-state keypaths/user-stylist-gallery-images)))
      (messages/handle-message events/debounced-event-initialized {:timeout gallery-poll-rate
                                                                   :message [events/poll-gallery {}]}))))

(defmethod effects/perform-effects events/poll-gallery [_ event args _ app-state]
  #?(:cljs (when (auth/stylist? (auth/signed-in app-state))
             ((if (experiments/edit-gallery? app-state)
                api/get-v2-stylist-gallery
                api/get-stylist-gallery)
              {:user-id    (get-in app-state keypaths/user-id)
               :user-token (get-in app-state keypaths/user-token)}))))
