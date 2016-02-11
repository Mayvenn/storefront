(ns storefront.components.friend-referrals
  (:require [om.core :as om]
            [storefront.hooks.talkable :as talkable]
            [storefront.keypaths :as keypaths]
            [sablono.core :refer-macros [html]]))

(defn friend-referrals-wrapped-component [user owner]
  (reify
    om/IDidMount
    (did-mount [_] (talkable/show-referrals user))
    om/IRender
    (render [_]
      (html
       [:div#talkable-referrals]))))

(defn friend-referrals-component [data owner]
  (om/component
   (html
    (when (get-in data keypaths/loaded-talkable)
      (let [user (get-in data keypaths/user)]
        (om/build friend-referrals-wrapped-component user))))))
