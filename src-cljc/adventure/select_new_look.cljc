(ns adventure.select-new-look
  (:require #?(:cljs [storefront.components.ugc :as ugc])
            [storefront.accessors.contentful :as contentful]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.albums :as albums]
            [spice.maps :as maps]
            [adventure.components.header :as header]))

(defn ^:private query
  [data]
  (let [album-keyword     (get-in data keypaths/selected-album-keyword)
        stylist-selected? (get-in data adventure-keypaths/adventure-servicing-stylist)
        current-step      (if stylist-selected? 3 2)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))
        looks             (-> data (get-in keypaths/cms-ugc-collection) album-keyword :looks)
        navigation-event  (get-in data keypaths/navigation-event)]
    (maps/deep-merge
     (albums/by-keyword album-keyword)
     {:data-test         "select-new-look-choice"
      :current-step      current-step
      :header-data       {:subtitle (str "Step " current-step  " of 3")}
      :spinning?         (empty? looks)
      :looks             (mapv (partial contentful/look->social-card navigation-event album-keyword color-details) looks)})))

;; TODO(jeff,corey): Move this to a separate template
(defn ^:private component
  [{:as   data
    :keys [prompt mini-prompt prompt-image header-data data-test spinning?
           looks]} _ _]
  (component/create
   [:div.bg-light-gray.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold.bg-light-lavender
     {:style {:height              "246px"
              :padding-top         "46px"
              :background-size     "cover"
              :background-position "center"
              :background-image    (str "url('" prompt-image "')")}}
     [:div.col.col-12
      [:div.h3 prompt]
      [:div.mt1.h6.light mini-prompt]]]
    (if spinning?
      [:div.flex.items-center.justify-center.h0.mt3
       ui/spinner]
      [:div {:data-test data-test}
       #?(:cljs
          [:div.flex.flex-wrap.pb4.mt2.justify-center.justify-start-on-tb-dt
           (for [look looks]
             (component/build ugc/adventure-social-image-card-component look {}))])])]))

(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-select-new-look
  [_ _ {album-keyword :album-keyword} _ app-state]
  #?(:cljs
     (when-not (albums/by-keyword (keyword album-keyword))
       (effects/redirect events/navigate-adventure-how-shop-hair))))

(defmethod transitions/transition-state events/navigate-adventure-select-new-look
  [_ _ {:keys [album-keyword]} app-state]
  (assoc-in app-state keypaths/selected-album-keyword (keyword album-keyword)))

