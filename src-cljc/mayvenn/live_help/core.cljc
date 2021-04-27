(ns mayvenn.live-help.core
  (:require #?(:cljs [storefront.hooks.kustomer :as kustomer])
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [mayvenn.visual.lib.call-out-box :as call-out-box]
            [storefront.component :as c]))

(defmethod fx/perform-effects e/flow|live-help|reset
  [_ _ _ _ state]
  (when (not (get-in state k/loaded-kustomer))
    #?(:cljs (kustomer/init))))

(defmethod fx/perform-effects e/flow|live-help|opened
  [_ _ _ _ state]
  (when (experiments/live-help? state)
    #?(:cljs (kustomer/open-conversation))))

(def live-help-query
  {:title/icon      [:svg/chat-bubble-diamonds {:style {:height "30px"
                                                        :fill   "black"
                                                        :width  "28px"}}]
   :title/primary   "How can we help?"
   :title/secondary "Text now to get live help with an expert about your dream look"
   :title/target    [e/flow|live-help|opened]
   :action/id       "live-help"
   :action/label    "Chat with us"
   :action/target   [e/flow|live-help|opened]})

(def banner
  (c/build call-out-box/variation-2 live-help-query))
