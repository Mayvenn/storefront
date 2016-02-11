(ns storefront.components.friend-referrals
  (:require [om.core :as om]
            [storefront.hooks.talkable :as talkable]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [sablono.core :refer-macros [html]]))

(defn friend-referrals-component [data owner]
  (om/component
   (html
    [:div#talkable-referrals])))
