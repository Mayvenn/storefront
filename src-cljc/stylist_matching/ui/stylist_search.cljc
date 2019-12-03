(ns stylist-matching.ui.stylist-search
  (:require [storefront.events :as events]
            adventure.keypaths
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.platform.messages :as messages]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private change-state
  [selected-location #?(:cljs ^js/Event e :clj e)]
  (when selected-location
    (messages/handle-message events/clear-selected-location))
  (->> {:keypath adventure.keypaths/adventure-stylist-match-address
        :value (.. e -target -value)}
       (messages/handle-message events/control-change-state)))

(defn stylist-search-location-search-box
  [{:stylist-search.location-search-box/keys
    [id placeholder value clear?]}]
  (when id
    (component/html
     (let [handler (partial change-state clear?)]
       [:div
        [:input.col-12.h4.rounded.border.border-white.p3
         {:style       {:margin-right 10}
          :value       value
          :id          id
          :data-test   id
          :autoFocus   true
          :placeholder placeholder
          :on-submit   handler
          :on-change   handler}]]))))

(defn stylist-search-button
  [{:stylist-search.button/keys [id disabled? target label]}]
  (when id
    (component/html
     (ui/p-color-button (merge {:disabled?      disabled?
                                :disabled-class "bg-cool-gray gray"
                                :data-test      "stylist-match-address-submit"}
                               (apply utils/fake-href target))
                        label))))

(defn stylist-search-title-molecule
  [{:stylist-search.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.left-align
      [:div.h1.my2.light primary]
      [:div.h5.my2.light secondary]])))

(defdynamic-component organism
  (did-mount [_]
             (messages/handle-message events/adventure-address-component-mounted
                                      {:address-elem    "stylist-match-address"
                                       :address-keypath adventure.keypaths/adventure-stylist-match-location}))
  (render [this]
          (let [data (component/get-props this)]
            (component/html
             [:div.m5
              [:div.mb4
               (stylist-search-title-molecule data)]
              [:div.mb3
               (stylist-search-location-search-box data)]
              [:div
               (stylist-search-button data)]]))))
