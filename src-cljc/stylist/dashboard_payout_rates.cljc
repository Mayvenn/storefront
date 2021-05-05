(ns stylist.dashboard-payout-rates
  (:require #?@(:cljs [[storefront.accessors.experiments :as experiments]
                       [storefront.api :as api]
                       [storefront.history :as history]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [ui.molecules :as ui-molecules]))

(def service-categories
  [{:name     "Install Services"
    :services [:sew-in-leave-out
               :sew-in-closure
               :sew-in-frontal
               :sew-in-360-frontal
               :wig-install]}
   {:name     "Add-On Services"
    :services [:addon-natural-hair-trim
               :addon-weave-take-down
               :addon-hair-deep-conditioning
               :addon-closure-customization
               :addon-frontal-customization
               :addon-360-frontal-customization]}
   {:name     "Other Services"
    :services [:silk-press
               :weave-maintenance
               :wig-maintenance
               :braid-down
               :wig-customization]}
   {:name     "Custom Wig Services"
    :services [:custom-unit-leave-out
               :custom-unit-closure
               :custom-unit-frontal
               :custom-unit-360-frontal]}
   {:name     "Reinstall Services"
    :services [:reinstall-leave-out
               :reinstall-closure
               :reinstall-frontal
               :reinstall-360-frontal]}])

(defcomponent service-rate
  [{:service-rate/keys [id primary secondary]} _ _]
  [:div.flex.justify-between
   {:key id}
   [:div primary]
   [:div secondary]])

(defcomponent service-category
  [{:service-rate-category/keys [id primary]
    :as data} _ _]
  [:div.mb2.content-2
   {:key id}
   [:span.shout primary]
   (component/elements service-rate data :service-rate-category/services)])

(defcomponent spinning-template
  [_ _ _]
  (ui/large-spinner
   {:style {:height "4em"}}))

(defcomponent template
  [data _ _]
  [:div.container
   [:div.px3.pt3.mb2 (ui-molecules/return-link (:return-link data))]
   [:div.px5.pb5.col-5-on-dt
    [:div.title-3.canela.mb3 "Payout Rates"]
    (component/elements service-category data :service-rate-categories)]])

(defn service-rate-category<
  [offered-services service-category]
  #:service-rate-category{:primary  (:name service-category)
                          :services (keep
                                     (fn [service]
                                       (when-let [offered-service (get offered-services service)]
                                         (let [{:keys [offered-service-name payout-price]} offered-service]
                                           #:service-rate{:primary   offered-service-name
                                                          :secondary (some-> payout-price
                                                                             mf/as-money)})))
                                     (:services service-category))})

(defn service-rate-categories<
  [stylist-service-menu]
  (->> service-categories
       (map (partial service-rate-category< stylist-service-menu))
       (filter (comp seq :service-rate-category/services))))

(defn ^:export page
  [state _]
  (let [offered-services   (get-in state (conj keypaths/user :offered-services))
        fetching-service-menu? (utils/requesting? state request-keys/fetch-user-stylist-offered-services)]
    (cond
      fetching-service-menu?
      (component/build spinning-template {})

      :else
      (component/build template
                       {:service-rate-categories (service-rate-categories< offered-services)
                        :return-link             {:return-link/event-message [events/navigate-v2-stylist-dashboard-orders]
                                                  :return-link/copy          "Back"
                                                  :return-link/id            "back-link"}}))))

(defmethod effects/perform-effects events/navigate-v2-stylist-dashboard-payout-rates
  [_ event args _ app-state]
  #?(:cljs
     (cond
       (not (and (experiments/payout-rates? app-state)
                 (= "aladdin" (get-in app-state keypaths/user-stylist-experience))))
       (history/enqueue-redirect events/navigate-v2-stylist-dashboard-orders)

       :else
       (let [stylist-id (get-in app-state keypaths/user-store-id)
             user-id    (get-in app-state keypaths/user-id)
             user-token (get-in app-state keypaths/user-token)]
         (api/fetch-user-stylist-offered-services (get-in app-state keypaths/api-cache)
                                                  {:user-id    user-id
                                                   :user-token user-token
                                                   :stylist-id stylist-id})))))
