(ns adventure.look-detail
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.accessors.contentful :as contentful]
                       [storefront.api :as api]
                       [storefront.components.shop-by-look-details :as shop-look-details]
                       [storefront.components.ugc :as ugc]
                       [storefront.config :as config]])
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure-keypaths]
            [spice.core :as spice]
            [adventure.components.header :as header]
            [adventure.progress :as progress]
            [storefront.accessors.experiments :as experiments]))

(defn ^:private query [data]
  (let [stylist-selected? (get-in data adventure-keypaths/adventure-servicing-stylist)
        current-step      (if stylist-selected? 3 2)
        album-keyword     (get-in data keypaths/selected-album-keyword)]
    {:look-detail-data  #?(:cljs (shop-look-details/adventure-query data)
                           :clj nil)
     :header-data       {:title                   "The New You"
                         :subtitle                (str "Step " current-step " of 3")
                         :height                  "65px"
                         :progress                progress/look-detail
                         :shopping-bag?           true
                         :back-navigation-message [events/navigate-adventure-select-new-look
                                                   {:album-keyword album-keyword}]}
     :stylist-selected? stylist-selected?}))

(defn ^:private component
  [{:keys [header-data look-detail-data stylist-selected?]} _ _]
  (component/create
   [:div.bg-white.center.flex-auto.self-stretch
    [:div.white
     (when header-data
       (header/built-component header-data nil))]
    [:div.left-align
     [:div.flex.items-center.bold.bg-light-lavender {:style {:height "75px"}}]
     [:div.black
      #?(:cljs (om/build shop-look-details/adventure-component look-detail-data nil))]

     (when-not stylist-selected?
       [:div.h6.center.pb8
        [:div.dark-gray "Not ready to shop hair?"]
        [:a.teal (utils/route-to events/navigate-adventure-find-your-stylist)
         "Find a stylist"]])]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-look-detail
  [dispatch event _ prev-app-state app-state]
  #?(:cljs (when-let [shared-cart-id (contentful/shared-cart-id (contentful/selected-look app-state))]
             (api/fetch-shared-cart shared-cart-id))))

(defmethod transitions/transition-state events/navigate-adventure-look-detail [_ _ {:keys [album-keyword look-id]} app-state]
  (-> app-state
      (assoc-in keypaths/selected-album-keyword (keyword album-keyword))
      (assoc-in keypaths/selected-look-id (keyword look-id))))
