(ns storefront.components.gallery-edit
  (:require #?@(:cljs [MuuriReact
                       react
                       [storefront.api :as api]
                       [storefront.loader :as loader]])
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

(def timer-completion-timestamp (atom nil))
(def timer-delay 500)

#?(:cljs (defn maybe-complete-timer [post-id]
           (when (and @timer-completion-timestamp
                      (< (+ (.getTime @timer-completion-timestamp) timer-delay)
                         (.getTime (js/Date.))))
             (messages/handle-message events/stylist-gallery-reorder-mode-entered)
             (messages/handle-message events/control-stylist-gallery-drag-begun {:post-id post-id})
             (reset! timer-completion-timestamp nil))))

#?(:cljs (defn tap-press [e]
           (let [expiration (js/Date.)]
             ;; WARNING: date function calls mutate
             (.setSeconds expiration (.getSeconds expiration))
             (reset! timer-completion-timestamp expiration)
             (js/setTimeout (partial maybe-complete-timer (-> e
                                                              .-target
                                                              (.closest ".board-item")
                                                              .-dataset
                                                              .-postId
                                                              js/parseInt)) timer-delay))))


#?(:cljs (defn tap-release [e]
           (reset! timer-completion-timestamp nil)))

#?(:cljs (defn reorder-release [e]
           (messages/handle-later events/stylist-gallery-reorder-mode-exited)))

#?(:cljs (defn reorder-mode-handlers []
           {:on-click     (fn [e]
                            (.preventDefault e)
                            (.stopPropagation e))
            :style        {:touch-action "none"
                           :filter       "brightness(0.5)"}
            :on-mouse-up  reorder-release
            :on-touch-end reorder-release}))

#?(:cljs (defn view-mode-handlers [photo-id]
           (merge (utils/route-to events/navigate-gallery-photo {:photo-id photo-id})
                  {:style           {:touch-action "pan-y"}
                   :on-mouse-down   tap-press
                   :on-mouse-up     tap-release
                   :on-mouse-leave  tap-release
                   :on-touch-start  tap-press
                   :on-touch-end    tap-release
                   :on-context-menu (fn [e]
                                      (.preventDefault e)
                                      (.stopPropagation e))})))

(defcomponent child-node
  [{react-key :key
    :keys     [currently-dragging-post-id post images reorder-mode]} _ _]

  (component/html
   #?(:clj [:div]
      :cljs
      [:div {:key react-key}
       (let [{:keys [image-ordering]} post
             {:keys [status id resizable-url post-id]}
             (->> images
                  (filter #(= (:id %) (first image-ordering)))
                  first)]
         (ui/aspect-ratio 1 1
                          [:div.container-size.board-item
                           (update (merge {:data-post-id post-id}
                                          (if reorder-mode
                                            (reorder-mode-handlers)
                                            (view-mode-handlers id)))
                                   :style (fn [style] (merge style
                                                             {:padding "1px"}
                                                             (when (= currently-dragging-post-id post-id)
                                                               {:filter "none"}))))
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
    [:a.block.pp1.board-item
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


;; TODO: There is an api call to change sort order on drag completed (talk to diva. maybe need to debounce)
;; Changing opacity using filter css property?

(defcomponent reorderable-component-2
  [{:keys [posts images reorder-mode currently-dragging-post-id] :as data} _ _]
  ;; TODO:: Flash shows incorrectly on page
  (component/html
   #?(:clj [:div]
      :cljs [:div
             (prn "reorder mode" reorder-mode)
             (apply react/createElement
                    MuuriReact/MuuriComponent
                    #js {:dragEnabled        true
                         :itemClass          "col-4"
                         :itemDraggingClass  "embiggen"
                         :dragStartPredicate (fn [_ e]
                                               (and
                                                (>= (.-deltaTime e) timer-delay)
                                                reorder-mode))
                         :dragSortPredicate  (fn [item _e]
                                              (some-> (.defaultSortPredicate MuuriReact/ItemDrag item)
                                                      (#(when (not= 0 (.-index %)) %))))
                         :propsToData        (fn [item]
                                               (let [props (get-in (js->clj item) ["props"])]
                                                 {:post         (:post props)
                                                  :reorder-mode (:reorder-mode props)}))
                         :onDragEnd (fn [item _event]
                                      (->> item
                                           .getGrid
                                           .getItems
                                           (keep #(some-> % .getData :post :id))
                                           (assoc {} :posts-ordering)
                                           (messages/handle-message events/control-stylist-gallery-reordered-v2)))}
                    (cons
                     (component/build add-photo-square-2 {:key "add-photo"})
                     (for [post (sort-by (comp first :image-ordering) posts)]
                       (component/build child-node {:key                        (:id post)
                                                    :reorder-mode               reorder-mode
                                                    :currently-dragging-post-id currently-dragging-post-id
                                                    :post                       post
                                                    :images                     images}))))])))

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

;; TODO: init long touch and reorder states
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

(defmethod effects/perform-effects events/api-success-stylist-gallery [_ event args _ app-state]
  (let [signed-in-as-stylist? (auth/stylist? (auth/signed-in app-state))
        on-edit-page?         (routes/exact-page? (get-in app-state keypaths/navigation-message) [events/navigate-gallery-edit])]
    (when (and signed-in-as-stylist?
               on-edit-page?
               (some (comp #{"pending"} :status) (get-in app-state keypaths/user-stylist-gallery-images)))
      (messages/handle-later events/poll-gallery {} 5000))))
