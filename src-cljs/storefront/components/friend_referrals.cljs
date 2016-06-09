(ns storefront.components.friend-referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn friend-referrals-component [data owner]
  (om/component
   (html
    [:div#talkable-referrals])))
