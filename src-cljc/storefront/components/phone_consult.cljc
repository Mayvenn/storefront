(ns storefront.components.phone-consult
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c :refer [defcomponent]]
            [catalog.cms-dynamic-content :as cms-dynamic-content]))

(defcomponent component
  [{:keys [message-rich-text released] :as data} owner _]
  (when released
    [:div.m1.border.p4.center.black
     [:a.black
      (merge {:href (str "tel:+")})
      (map cms-dynamic-content/build-hiccup-tag (:content message-rich-text))]])
  )
