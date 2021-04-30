(ns mayvenn.live-help.core
  (:require #?@(:cljs [[storefront.hooks.kustomer :as kustomer]
                       [storefront.hooks.stringer :as stringer]])
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [mayvenn.visual.lib.call-out-box :as call-out-box]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.trackings :as trackings]
            [storefront.accessors.sites :as sites]))

(defn kustomer-started? [app-state]
  (boolean
   (and
    (= :shop (sites/determine-site app-state))
    (get-in app-state k/started-kustomer))))

#?(:cljs
   (defmethod fx/perform-effects e/flow|live-help|reset
     [_ _ _ _ state]
     (when-not (get-in state k/loaded-kustomer)
       (kustomer/init))))

#?(:cljs
   (defmethod fx/perform-effects e/flow|live-help|opened
     [_ _ _ _ state]
     (when (kustomer-started? state)
       (kustomer/open-conversation))))

(defmethod trackings/perform-track e/flow|live-help|opened
  [_ _ {:keys [location]} _]
  #?(:cljs (stringer/track-event "chat_link_pressed" {:location-on-page location})))

(defn banner-query [location]
  (let [target [e/flow|live-help|opened {:location location}]]
    {:title/icon      [:svg/chat-bubble-diamonds {:class "mb2"
                                                  :style {:height "30px"
                                                          :fill   "black"
                                                          :width  "28px"}}]
     :title/primary   "How can we help?"
     :title/secondary "Text now to get live help with an expert about your dream look"
     :title/target    target
     :action/id       "live-help"
     :action/label    "Chat with us"
     :action/target   target}))

(c/defcomponent banner
  [{:live-help/keys [location]} _ _]
  (c/build call-out-box/variation-1 (banner-query location)))

(c/defcomponent button-component
  [{:live-help-button/keys [cta-label cta-target id icon label-and-border-color]} _ _]
  (when id
    [:a.flex.items-center
     (assoc (apply utils/fake-href cta-target)
            :data-test id)
     (svg/symbolic->html icon)
     [:div.button-font-3.shout.border-bottom.border-width-2
      {:style {:border-color label-and-border-color
               :color        label-and-border-color}}
      cta-label]]))
