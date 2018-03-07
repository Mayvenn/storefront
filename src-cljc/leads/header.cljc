(ns leads.header
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.slideout-nav :as slideout-nav]))

(defn ^:private component [data owner opts]
  (component/create
   [:div.border-bottom.border-gray.flex.items-center.justify-center.flex-column.py2
    [:div.col-12 (ui/clickable-logo {:event events/navigate-home
                                     :data-test "header-logo"
                                     :height "40px"})]
    [:p.h6.dark-gray "Have questions? Call us:"
     [:a {:href "tel://+18664247201"}] "1-866-424-7201"]]))

(defn query [data]
  {})

(defn built-component [data opts]
  (component/build component (query data) nil))
