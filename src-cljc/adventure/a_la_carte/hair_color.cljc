(ns adventure.a-la-carte.hair-color
  (:require #?@(:cljs [[storefront.platform.messages :as messages]])
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]
            [adventure.handlers :as handlers]
            [spice.selector :as selector]))

(defn enriched-buttons [facet-options]
  (for [option facet-options]
    {:text             [:div.mynp6.flex.items-center.px1
                        {:style {:height "48px"}}
                        [:div.flex.flex-column.items-center
                         [:img.border.border-gray
                          {:src    (:option/rectangle-swatch option)
                           :height "40"
                           :width  "60"}]]
                        [:div.flex-grow-1
                         [:div.px1 (:adventure/name option)]]]
     :data-test-suffix (:option/slug option)
     :value            {:color (:option/slug option)}
     :target-message   [events/navigate-adventure-a-la-carte-product-list]}))

(defn ^:private query [data]
  (let [;; TODO(cwr,jjh) these ought to be underneath the catalog api
        facets (get-in data keypaths/v2-facets)
        skus   (vals (get-in data keypaths/v2-skus))

        ;; TODO(cwr,jjh) these ought to be stored in app-state ready-to-go
        {:keys [texture]} (get-in data adventure-keypaths/adventure-choices)
        selections        {:hair/family         handlers/default-adventure-hair-family
                           :hair/texture        #{texture}
                           :inventory/in-stock? #{true}}

        selected-skus     (selector/match-all {} selections skus)
        color-choices     (facets/available-options facets :hair/color selected-skus)
        stylist-selected? (get-in data adventure-keypaths/adventure-servicing-stylist)
        current-step      (if stylist-selected? 3 2)]
    {:prompt       "Which color are you looking for?"
     :prompt-image "//ucarecdn.com/47cd8de1-9bd0-4057-a050-c07749791d1a/-/format/auto/"
     :data-test    "hair-color"
     :current-step current-step
     :spinning?    (utils/requesting-from-endpoint? data request-keys/search-v2-products)
     :header-data  {:title                   "The New You"
                    :progress                progress/hair-texture
                    :back-navigation-message [events/navigate-adventure-how-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (enriched-buttons color-choices)}))

(defn ^:export built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))

(defmethod transitions/transition-state events/navigate-adventure-a-la-carte-hair-color [_ _ _ app-state]
  (update-in app-state adventure-keypaths/adventure-choices dissoc :color))

(defmethod effects/perform-effects events/navigate-adventure-a-la-carte-hair-color
  [_ _ args _ app-state]
  #?(:cljs (messages/handle-message events/adventure-fetch-matched-products {:criteria [:hair/texture :hair/family]})))
