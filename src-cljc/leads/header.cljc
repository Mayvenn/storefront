(ns leads.header
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.components.slideout-nav :as slideout-nav]
            [leads.keypaths :as keypaths]))

(defn ^:private component [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center.justify-center.flex-column.py2
    [:div.col-12 (ui/clickable-logo {:data-test "leads-header-logo"
                                     :height "40px"})]
    [:p.h6.dark-gray "Have questions? Call us: "
     (ui/link :link/phone :a.inherit-color {} call-number)]]))

(defn query [data]
  {:call-number (if (= "a1" (get-in data keypaths/lead-flow-id))
                  config/mayvenn-leads-a1-call-number
                  config/mayvenn-leads-call-number)})

(defn built-component [data opts]
  (component/build component (query data) nil))
