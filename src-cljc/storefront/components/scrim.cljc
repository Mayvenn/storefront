(ns storefront.components.scrim
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]))

(defn component [{:keys [show? banner-showing?]} owner opts]
  (component/create
   (when show?
     [:div.overlay.absolute.hide-on-mb.bg-darken-3.z3
      {:on-click #(messages/handle-message events/control-menu-collapse-all)}])))

(defn query [data]
  {:show? (and (experiments/new-flyout? data)
               (or (get-in data keypaths/shop-menu-expanded)
                   (get-in data keypaths/store-info-expanded)
                   (get-in data keypaths/account-menu-expanded)))
   :banner-showing? (promotion-banner/should-display? data)})

(defn built-component [data opts]
  (component/build component (query data) opts))
