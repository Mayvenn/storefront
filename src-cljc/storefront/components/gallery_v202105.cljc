(ns storefront.components.gallery-v202105
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.gallery-edit :as gallery-edit]
            [storefront.components.gallery-edit-v202105 :as gallery-edit-v202105]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]))

(def gallery-poll-rate 5000)

(defn query [data]
  {:gallery (get-in data keypaths/user-stylist-gallery-images)})

(defn built-component [data opts]
  (if (experiments/edit-gallery? data)
    (component/build gallery-edit-v202105/reorderable-wrapper (gallery-edit-v202105/query data))
    (component/build gallery-edit/static-component (query data) nil)))

(defmethod effects/perform-effects events/navigate-store-gallery [_ event args _ app-state]
  #?(:cljs (api/get-store-gallery {:stylist-id (get-in app-state keypaths/store-stylist-id)})))

(defmethod effects/perform-effects events/api-success-store-gallery-fetch [_ event args _ app-state]
  (when (empty? (get-in app-state keypaths/store-gallery-images))
    (effects/page-not-found)))

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
