(ns stylist-matching.core
  (:require [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]))

(defmethod effects/perform-effects events/flow--stylist-matching--began
  [_ current-nav-event args _ _]
  (messages/handle-message events/navigate-adventure-find-your-stylist))
