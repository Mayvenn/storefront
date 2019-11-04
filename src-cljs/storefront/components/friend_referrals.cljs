(ns storefront.components.friend-referrals
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.component :as component :refer [defcomponent]]
            
            
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component [data owner _]
  [:div#talkable-referrals])

(defn built-component [data opts]
  (component/build component nil nil))
