(ns adventure.a-la-carte.hair-color
  (:require #?@(:cljs [[storefront.platform.messages :refer [handle-message]]])
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]))

(defn enriched-buttons [facet-options]
  (for [option facet-options
        :let [subtitle (if (= (:option/slug option) "black")
                         "Virgin"
                         "Dyed Virgin")]]
    {:text             [:div.mynp6.flex.items-center.px1
                        {:style {:height "48px"}}
                        [:div.flex.flex-column.items-center
                         [:img.border.border-gray
                          {:src    (:option/rectangle-swatch option)
                           :height "40"
                           :width  "60"}]
                         #_[:div.light.h7 subtitle]]
                        [:div.flex-grow-1
                         [:div.px1 (:adventure/name option)]
                         #_[:div.light.h6 subtitle]]]
     :data-test-suffix (:option/slug option)
     :value            {:color (:option/slug option)}
     :target-message   [events/navigate-adventure-shopbybundles-product-list]}))

(defn ^:private query [data]
  (let [color-facet-options (facets/available-adventure-facet-options
                             :hair/color
                             (get-in data keypaths/v2-facets)
                             (get-in data adventure-keypaths/adventure-matching-skus))
        adventure-choices     (get-in data adventure-keypaths/adventure-choices)
        stylist-selected?     (some-> adventure-choices :flow #{"match-stylist"})
        current-step          (if stylist-selected? 3 2)]
    {:prompt       "Choose the color of your hair."
     :prompt-image "//ucarecdn.com/47cd8de1-9bd0-4057-a050-c07749791d1a/-/format/auto/bg.png"
     :data-test    "hair-color"
     :current-step current-step
     :footer       (when-not stylist-selected?
                     [:div.h6.center.pb8
                      [:div.dark-gray "Not ready to shop hair?"]
                      [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
                       "Find a stylist"]])
     :header-data  {:title                   "The New You"
                    :progress                progress/hair-texture
                    :back-navigation-message [events/navigate-adventure-how-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (enriched-buttons color-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))

(defmethod transitions/transition-state events/navigate-adventure-shopbybundles-hair-color [_ _ _ app-state]
  (update-in app-state adventure-keypaths/adventure-choices dissoc :color))

(defmethod effects/perform-effects events/navigate-adventure-shopbybundles-hair-color
  [_ _ args _ app-state]
  #?(:cljs (handle-message events/adventure-fetch-matched-skus {:criteria [:hair/texture :hair/family]})))
