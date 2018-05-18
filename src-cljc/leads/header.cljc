(ns leads.header
  (:require [storefront.component :as component]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [leads.flows :as flows]
            [storefront.components.slideout-nav :as slideout-nav]
            [leads.keypaths :as keypaths]))

(defn component [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center.justify-center.flex-column.py2
    [:div.col-12 (ui/clickable-logo {:data-test "leads-header-logo"
                                     :height "40px"})]
    [:p.h6.dark-gray "Have questions? Call us: "
     (ui/link :link/phone :a.inherit-color {} call-number)]]))

(defn query [data]
  {:call-number (flows/call-number data)})

(defn built-component [data opts]
  (component/build component (query data) nil))
