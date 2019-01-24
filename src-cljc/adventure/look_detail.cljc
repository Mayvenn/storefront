(ns adventure.look-detail
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.components.shop-by-look-details :as shop-look-details]
                       [storefront.config :as config]])
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.platform.messages :as messages]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure-keypaths]
            [spice.maps :as maps]
            [spice.core :as spice]
            [adventure.components.header :as header]))

(defn ^:private query [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})
        album-keyword     (get-in data keypaths/selected-album-keyword)]
    {:look-detail-data #?(:cljs (shop-look-details/adventure-query data)
                          :clj nil)
     :data-test        "look-detail"
     :header-data      {:title         "The New You"
                        :height        "65px"
                        :current-step  3
                        :shopping-bag? true
                        :back-link     {:event events/navigate-adventure-select-new-look
                                        :args {:album-keyword album-keyword}}
                        :subtitle      (str "Step " (if-not hair-flow? 2 3) " of 3")}
     :copy             #?(:cljs (-> config/pixlee :copy album-keyword) :clj nil)
     :deals?           false
     :spinning?        false
     :color-details    (->> (get-in data keypaths/v2-facets)
                            (filter #(= :hair/color (:facet/slug %)))
                            first
                            :facet/options
                            (maps/index-by :option/slug))
     :looks            (pixlee/images-in-album (get-in data keypaths/ugc) album-keyword)}))

(defn ^:private component
  [{:keys [header-data data-test looks copy deals? spinning? color-details look-detail-data] :as data} _ _]
  (component/create
   [:div.bg-white.center.flex-auto.self-stretch
    [:div.white
     (when header-data
       (header/built-component header-data nil))]
    [:div.left-align
     [:div.flex.items-center.bold.bg-light-lavender {:style {:height "75px"}}]
     [:div.black
      #?(:cljs (om/build shop-look-details/adventure-component look-detail-data nil))]

     [:div.h6.center.pb8
      [:div.dark-gray "Not ready to shop hair?"]
      [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
       "Find a stylist"]]]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-look-detail [dispatch event event-args prev-app-state app-state]
  #?(:cljs (pixlee-hook/fetch-image :adventure (:look-id event-args))))

(defmethod transitions/transition-state events/navigate-adventure-look-detail [_ _ {:keys [album-keyword look-id]} app-state]
  (-> app-state
      (assoc-in keypaths/selected-album-keyword (keyword album-keyword))
      (assoc-in keypaths/selected-look-id (spice/parse-int look-id))))
