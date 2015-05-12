(ns storefront.components.forgot-password
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn forgot-password-component [data owner]
  (om/component
   (html
    [:div "hello"])))
