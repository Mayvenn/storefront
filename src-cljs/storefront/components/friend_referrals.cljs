(ns storefront.components.friend-referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn component [data owner]
  (om/component
   (html
    [:div#talkable-referrals])))

(defn built-component [data opts]
  (om/build component nil nil))
