(ns stylist-profile.core
  "Stylist profile"
  (:require #?@(:cljs
                [[adventure.keypaths]
                 [api.orders]
                 [catalog.services]
                 [clojure.set :refer [union]]
                 [storefront.accessors.orders :as orders]
                 [storefront.api :as api]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.hooks.seo :as seo]
                 [storefront.keypaths]
                 [storefront.platform.messages :as messages]
                 [stylist-directory.stylists :as stylists]])
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            stylist-directory.keypaths
            [spice.core :as spice]))

;; TODO(corey) figure out how to model this sort of behavior

(def stylist-profile [:models :stylist-profile])
(def swap-popup (conj stylist-profile :swap-popup))
(def sku-intended-for-swap (conj swap-popup :sku-intended-for-swap))
(def selected-stylist-intended-for-swap (conj swap-popup :selected-stylist-intended-for-swap))
(def service-swap-confirmation-commands (conj swap-popup :service-swap-confirmation-commands))

(defmethod effects/perform-effects events/control-fetch-stylist-reviews
  [dispatch event args prev-app-state app-state]
  #?(:cljs
     (api/fetch-stylist-reviews (get-in app-state storefront.keypaths/api-cache)
                                {:stylist-id (get-in app-state adventure.keypaths/stylist-profile-id)
                                 :page       (-> (get-in app-state stylist-directory.keypaths/paginated-reviews)
                                                 :current-page
                                                 (or 0)
                                                 inc)})))

(defmethod effects/perform-effects events/share-stylist
  [_ _ {:keys [url text title stylist-id]} _]
  #?(:cljs
     (.. (js/navigator.share (clj->js {:title title
                                       :text  text
                                       :url   url}))
         (then  (fn []
                  (messages/handle-message events/api-success-shared-stylist
                                           {:stylist-id stylist-id})))
         (catch (fn [err]
                  (messages/handle-message events/api-failure-shared-stylist
                                           {:stylist-id stylist-id
                                            :error      (.toString err)}))))))

(defmethod effects/perform-effects events/navigate-adventure-stylist-profile
  [dispatch event {:keys [stylist-id store-slug]} prev-app-state app-state]
  #?(:cljs
     (do
       (api/get-products (get-in app-state storefront.keypaths/api-cache)
                         (merge-with union catalog.services/discountable catalog.services/a-la-carte)
                         (partial messages/handle-message events/api-success-v3-products-for-stylist-filters))
       (google-maps/insert)
       (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache) stylist-id)
       (api/fetch-stylist-reviews (get-in app-state storefront.keypaths/api-cache) {:stylist-id stylist-id
                                                                                    :page       1})
       (seo/set-tags app-state))))

(defmethod transitions/transition-state events/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} app-state]
  (-> app-state
      (assoc-in adventure.keypaths/stylist-profile-id (spice.core/parse-double stylist-id))
      (assoc-in stylist-directory.keypaths/paginated-reviews nil)))

(defmethod trackings/perform-track events/navigate-adventure-stylist-profile
  [_ event {:keys [stylist-id]} app-state]
  #?(:cljs
     (facebook-analytics/track-event "ViewContent"
                                     {:content_type "stylist"
                                      :content_ids [(spice.core/parse-int stylist-id)]})))

(defmethod effects/perform-effects events/control-stylist-profile-add-service-to-bag
  [dispatch event {:keys [sku quantity] :as args} _ app-state]
  #?(:cljs
     (let [cart-contains-free-mayvenn-service? (orders/discountable-services-on-order? (get-in app-state storefront.keypaths/order))
           sku-is-free-mayvenn-service?        (-> sku :promo.mayvenn-install/discountable first)
           service-swap?                       (and cart-contains-free-mayvenn-service? sku-is-free-mayvenn-service?)
           stylist-to-be-swapped               (get-in app-state adventure.keypaths/adventure-servicing-stylist)
           stylist-id                          (get-in app-state adventure.keypaths/stylist-profile-id)
           intended-stylist                    (stylists/by-id app-state stylist-id)
           stylist-swap?                       (not= stylist-to-be-swapped intended-stylist)
           add-sku-to-bag-command              [events/stylist-profile-add-service-to-bag
                                                {:sku           sku
                                                 :stay-on-page? true
                                                 :service-swap? service-swap?
                                                 :quantity      quantity}]
           add-selected-stylist                [events/flow|stylist-matching|matched
                                                {:stylist      intended-stylist
                                                 :result-index 0}]]
       (if service-swap?
         (messages/handle-message events/stylist-profile-swap-popup-show
                                  {:sku-intended              sku
                                   :selected-stylist-intended (when stylist-swap? intended-stylist)
                                   :confirmation-commands     (cond-> [add-sku-to-bag-command]
                                                                stylist-swap?
                                                                (conj add-selected-stylist))})
         (apply messages/handle-message add-sku-to-bag-command)))))

(defmethod effects/perform-effects events/stylist-profile-add-service-to-bag
  [dispatch event {:keys [sku quantity stay-on-page? service-swap? stylist] :as args} _ app-state]
  #?(:cljs
     (let [nav-event          (get-in app-state storefront.keypaths/navigation-event)]
       (api/add-sku-to-bag
        (get-in app-state storefront.keypaths/session-id)
        {:sku                sku
         :quantity           quantity
         :stylist-id         (get-in app-state storefront.keypaths/store-stylist-id)
         :token              (get-in app-state storefront.keypaths/order-token)
         :number             (get-in app-state storefront.keypaths/order-number)
         :user-id            (get-in app-state storefront.keypaths/user-id)
         :user-token         (get-in app-state storefront.keypaths/user-token)
         :heat-feature-flags (get-in app-state storefront.keypaths/features)}
        #(do
           (messages/handle-message events/api-success-add-sku-to-bag
                                    {:order         %
                                     :quantity      quantity
                                     :sku           sku}))))))
