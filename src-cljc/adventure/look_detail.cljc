(ns adventure.look-detail
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.config :as config]])
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.platform.messages :as messages]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure-keypaths]
            [spice.maps :as maps]
            [adventure.components.header :as header]))

(defn ^:private query [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        hair-flow?        (-> adventure-choices :flow #{"match-stylist"})]
    {:data-test     "look-detail"
     :header-data   {:title         "The New You"
                     :current-step  3
                     :shopping-bag? true
                     :back-link     events/navigate-adventure-select-new-look
                     :subtitle      (str "Step " (if-not hair-flow? 2 3) " of 3")}
     :copy          #?(:cljs (-> config/pixlee :copy :adventure) :clj nil)
     :deals?        false
     :spinning?     false
     :color-details (->> (get-in data keypaths/v2-facets)
                         (filter #(= :hair/color (:facet/slug %)))
                         first
                         :facet/options
                         (maps/index-by :option/slug))
     :looks         (pixlee/images-in-album (get-in data keypaths/ugc) :adventure)}))

(defn ^:private component
  [{:keys [header-data data-test looks] :as data} _ _]
  (component/create
   [:div.bg-too-light-teal.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold.bg-lavender
     {:style {:height "75px"}}]

    [:div.h6.center.pb8
     [:div.dark-gray "Not ready to shop hair?"]
     [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
      "Find a stylist"]]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-select-new-look [_ _ _ _ _]
  #?(:cljs (pixlee-hook/fetch-album-by-keyword :adventure)))
