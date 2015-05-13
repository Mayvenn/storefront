(ns storefront.components.manage-account
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.utils :as utils]
            [storefront.events :as events]
            [storefront.state :as state]))

(defn manage-account-component [data owner]
  (om/component
   (html
    [:h1 "Manage"])))
