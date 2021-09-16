(ns storefront.components.template
  (:require [storefront.component :as c]
            #?@(:cljs [[storefront.components.popup :as popup]])
            [storefront.components.flash :as flash]
            [storefront.components.footer :as footer]
            [storefront.components.header :as header]
            [ui.promo-banner :as promo-banner]
            [mayvenn.live-help.core :as live-help]
            [storefront.keypaths :as keypaths]))

(defn wrap-standard
  "Standard set of accoutrement for DTC pages

  This is opposed to having top-level determine for each page."
  [state nav-event template]
  (c/html
   [:div.flex.flex-column.stretch
    {:style {:margin-bottom "-1px"}}
    [:div
     {:key "popup"}
     #?(:cljs (popup/built-component state nil))]
    [:div
     {:key "promo"}
     ^:inline (promo-banner/built-static-organism state nil)]
    ^:inline (header/built-component state nil)
    [:div.relative.flex.flex-column.flex-auto
     ^:inline (flash/built-component state nil)
     [:main.bg-white.flex-auto
      {:data-test (keypaths/->component-str nav-event)}
      template]
     [:footer (footer/built-component state nil)]]
    (live-help/bug-component state)]))
