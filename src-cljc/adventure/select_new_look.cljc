(ns adventure.select-new-look
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
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
            [adventure.components.header :as header]))

(defn ^:private query [data]
  (let [adventure-choices  (get-in data adventure-keypaths/adventure-choices)
        album-keyword      (get-in data keypaths/selected-album-keyword)
        album-copy         #?(:cljs (-> config/pixlee :copy album-keyword)
                              :clj nil)
        current-step (if (-> adventure-choices :flow #{"match-stylist"}) 3 2)]
    {:prompt        "Select your new look"
     :mini-prompt   ["We have an amazing selection for you"
                     [:br]
                     "to choose from."]
     :prompt-image  (:adventure/prompt-image album-copy)
     :data-test     "select-new-look-choice"
     :current-step  current-step
     :header-data   {:title         "The New You"
                     :progress      12
                     :shopping-bag? true
                     :back-link     events/navigate-adventure-how-shop-hair
                     :subtitle      (str "Step " current-step  " of 3")}
     :copy          album-copy
     :deals?        false
     :spinning?     false
     :color-details (->> (get-in data keypaths/v2-facets)
                         (filter #(= :hair/color (:facet/slug %)))
                         first
                         :facet/options
                         (maps/index-by :option/slug))
     :looks         (pixlee/images-in-album (get-in data keypaths/ugc) (get-in data keypaths/selected-album-keyword))}))

(defn ^:private component
  [{:keys [prompt mini-prompt prompt-image header-data data-test looks] :as data} _ _]
  (component/create
   [:div.bg-too-light-teal.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold.bg-light-lavender
     {:style {:height              "246px"
              :padding-top "46px"
              :background-size     "cover"
              :background-position "center"
              :background-image    (str "url('"prompt-image "')")}}
     [:div.col.col-12
      [:div.h3 prompt]
      [:div.mt1.h6.light mini-prompt]]]
    [:div {:data-test data-test}
     #?(:cljs (om/build ugc/adventure-component data {}))]
    [:div.h6.center.pb8
     [:div.dark-gray "Not ready to shop hair?"]
     [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
      "Find a stylist"]]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-select-new-look
  [_ _ {:keys [album-keyword]} _ _]
  #?(:cljs (pixlee-hook/fetch-album-by-keyword (keyword album-keyword))))

(defmethod transitions/transition-state events/navigate-adventure-select-new-look
  [_ _ {:keys [album-keyword]} app-state]
  (assoc-in app-state keypaths/selected-album-keyword (keyword album-keyword)))

