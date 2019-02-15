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
            [adventure.albums :as albums]
            [spice.maps :as maps]
            [adventure.components.header :as header]))

(defn ^:private query
  [data]
  (let [adventure-choices (get-in data adventure-keypaths/adventure-choices)
        album-keyword     (get-in data keypaths/selected-album-keyword)
        stylist-selected? (some-> adventure-choices :flow #{"match-stylist"})
        current-step      (if stylist-selected? 3 2)]
    (maps/deep-merge
     (albums/by-keyword album-keyword)
     {:data-test         "select-new-look-choice"
      :current-step      current-step
      :header-data       {:subtitle (str "Step " current-step  " of 3")}
      :spinning?         false ;; TODO(jeff,corey): TO DO IT
      :color-details     (->> (get-in data keypaths/v2-facets)
                              (filter #(= :hair/color (:facet/slug %)))
                              first
                              :facet/options
                              (maps/index-by :option/slug))
      :looks             (pixlee/images-in-album (get-in data keypaths/ugc) album-keyword)
      :stylist-selected? stylist-selected?})))

;; TODO(jeff,corey): Move this to a separate template
(defn ^:private component
  [{:as   data
    :keys [prompt mini-prompt prompt-image header-data data-test looks stylist-selected?]} _ _]
  (component/create
   [:div.bg-too-light-teal.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold.bg-light-lavender
     {:style {:height              "246px"
              :padding-top         "46px"
              :background-size     "cover"
              :background-position "center"
              :background-image    (str "url('"prompt-image "')")}}
     [:div.col.col-12
      [:div.h3 prompt]
      [:div.mt1.h6.light mini-prompt]]]
    [:div {:data-test data-test}
     #?(:cljs (om/build ugc/adventure-component data {}))]
    (when-not stylist-selected?
      [:div.h6.center.pb8
           [:div.dark-gray "Not ready to shop hair?"]
           [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
            "Find a stylist"]])]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-select-new-look
  [_ _ args _ _]
  #?(:cljs
     (let [album-keyword (keyword (:album-keyword args))]
       (if (seq (albums/by-keyword album-keyword))
         (pixlee-hook/fetch-album-by-keyword (keyword album-keyword))
         (effects/redirect events/navigate-adventure-how-shop-hair)))))

(defmethod transitions/transition-state events/navigate-adventure-select-new-look
  [_ _ {:keys [album-keyword]} app-state]
  (assoc-in app-state keypaths/selected-album-keyword (keyword album-keyword)))

