(ns storefront.components.gallery-v202105
  "This gallery is for stylist view/edit of their own gallery."
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.gallery-edit :as gallery-edit]
            [storefront.components.gallery-edit-v202105 :as gallery-edit-v202105]
            [storefront.components.gallery-appointments-v202105 :as appointments]
            [storefront.components.tabs-v202105 :as tabs]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as keypaths]
            [spice.maps :as maps]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(def gallery-poll-rate 5000)

(def ^:private tabs
  [{:id                 :past-appointments
    :title              "past appointments"
    :navigate           events/navigate-gallery-appointments
    :not-selected-class "border-right"}
   {:id                 :my-gallery
    :title              "my gallery"
    :navigate           events/navigate-gallery-edit
    :not-selected-class "border-left"}])

(defn query [data]
  (let [selected-tab (condp = (get-in data keypaths/navigation-event)
                       events/navigate-gallery-appointments :past-appointments
                       events/navigate-gallery-edit         :my-gallery
                       nil)]
    (cond-> {:stylist-gallery/past-appts?       (experiments/past-appointments? data)
             :stylist-gallery-tabs/tabs         tabs
             :stylist-gallery-tabs/selected-tab selected-tab}

      (= :past-appointments selected-tab)
      (merge {:stylist-gallery-appointments/id           (when (= :past-appointments selected-tab) "stylist-gallery-appointments")
              :stylist-gallery-appointments/target       [events/navigate-gallery-edit]
              :stylist-gallery-appointments/no-appts     (not= "aladdin" (get-in data keypaths/user-stylist-experience))
              :stylist-gallery-appointments/appointments [{:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "18"
                                                           :title-secondary "Jan"
                                                           :detail          "Mika"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "09"
                                                           :title-secondary "Dec"
                                                           :detail          "Tamara"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "09"
                                                           :title-secondary "Dec"
                                                           :detail          "Marie"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "09"
                                                           :title-secondary "Jan"
                                                           :detail          "Mika"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "23"
                                                           :title-secondary "Nov"
                                                           :detail          "Mika"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "16"
                                                           :title-secondary "Nov"
                                                           :detail          "Mika"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "09"
                                                           :title-secondary "Nov"
                                                           :detail          "Mika"}
                                                          {:id              "based-on-appointment-entity-id"
                                                           :target          events/navigate-gallery-appointments
                                                           :title-primary   "25"
                                                           :title-secondary "Oct"
                                                           :detail          "Mika"}]})

      (= :my-gallery selected-tab)
      (merge (let [images          (->> (get-in data keypaths/user-stylist-gallery-images)
                                        (maps/index-by :id))
                   post-ordering   (get-in data keypaths/user-stylist-gallery-initial-posts-ordering)
                   sorted-posts    (->> (get-in data keypaths/user-stylist-gallery-posts)
                                        (map (fn [post] (->> post :image-ordering first (get images) (assoc post :cover-image))))
                                        (maps/index-by :id)
                                        (#(map % post-ordering)))
                   fetching-posts? (utils/requesting? data request-keys/get-stylist-gallery)]
               {:stylist-gallery-my-gallery/id                         "stylist-gallery-my-gallery"
                :stylist-gallery-my-gallery/posts-with-cover           sorted-posts
                :stylist-gallery-my-gallery/post-ordering              post-ordering
                :stylist-gallery-my-gallery/fetching-posts?            fetching-posts?
                :stylist-gallery-my-gallery/appending-post?            (utils/requesting? data request-keys/append-gallery)
                :stylist-gallery-my-gallery/reorder-mode?              (get-in data keypaths/stylist-gallery-reorder-mode)
                :stylist-gallery-my-gallery/currently-dragging-post-id (get-in data keypaths/stylist-gallery-currently-dragging-post)})))))

(defcomponent component
  [{:stylist-gallery/keys [past-appts?] :as data} owner opts]
  [:div
   [:div.bg-cool-gray.center.mx-auto.pt8.hide-on-mb-tb
    [:h1.px2.py10.canela.title-1
     "My Gallery"]]
   (when past-appts?
     [:div
      (tabs/component data)])
   [:div
    (component/build appointments/template data opts)
    (component/build gallery-edit-v202105/reorderable-wrapper data)]])

(defn old-query [data]
  {:gallery (get-in data keypaths/user-stylist-gallery-images)})

(defn ^:export built-component [data opts]
  (if (experiments/edit-gallery? data)
    (component/build component (query data) opts)
    (component/build gallery-edit/static-component (old-query data) nil)))

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
