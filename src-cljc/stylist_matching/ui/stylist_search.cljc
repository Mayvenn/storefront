(ns stylist-matching.ui.stylist-search
  (:require #?(:cljs [om.core :as om])
            [storefront.events :as events]
            adventure.keypaths
            [storefront.component :as component]
            [storefront.platform.messages :as messages]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

#?(:clj
   (def ^:private change-state (constantly nil))
   :cljs
   (defn ^:private change-state
     [selected-location ^js/Event e]
     (when selected-location
       (messages/handle-message events/clear-selected-location))
     (->> {:keypath adventure.keypaths/adventure-stylist-match-address
           :value (.. e -target -value)}
          (messages/handle-message events/control-change-state))))

(defn stylist-search-location-search-box
  [{:stylist-search.location-search-box/keys
    [id placeholder value clear?]}]
  (component/html
   (when id
     (let [handler (partial change-state clear?)]
       [:div
        [:input.col-12.h4.rounded.border.border-white.p2
         {:value       value
          :id          id
          :data-test   id
          :autoFocus   true
          :placeholder placeholder
          :on-submit   handler
          :on-change   handler}]]))))

(defn stylist-search-button
  [{:stylist-search.button/keys [disabled? target label]}]
  (ui/teal-button (merge {:disabled       disabled?
                          :disabled-class "bg-light-gray gray"
                          :data-test      "stylist-match-address-submit"}
                         (apply utils/fake-href target))
                  label))

(defn stylist-search-title-molecule
  [{:stylist-search.title/keys [id primary secondary]}]
  (when id
    (component/html
     [:div.left-align
      [:div.h1.my2.light primary]
      [:div.h5.my2.light secondary]])))

(defn wrap-event-on-mount
  [target data component]
  #?(:cljs
     (reify
       om/IDidMount
       (did-mount [this]
         (apply messages/handle-message target))
       om/IRender
       (render [_]
         (component/html component)))))

(defn organism
  [data _ _]
  (wrap-event-on-mount
   [events/adventure-address-component-mounted
    {:address-elem    "stylist-match-address"
     :address-keypath adventure.keypaths/adventure-stylist-match-location}]
   data
   [:div.m5
    [:div.mb4
     (stylist-search-title-molecule data)]
    [:div.mb3
     (stylist-search-location-search-box data)]
    [:div
     (stylist-search-button data)]]))
