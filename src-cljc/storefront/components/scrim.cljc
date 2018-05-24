(ns storefront.components.scrim
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.promotion-banner :as promotion-banner]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]))

(defn component [{:keys [show?]} owner opts]
  (component/create
   (when show?
     [:div.overlay.absolute.hide-on-mb.bg-darken-3.z3
      {:on-click #(messages/handle-message events/control-menu-collapse-all)}])))

(defn query [data]
  {:show? (and (experiments/new-flyout? data)
               (or (get-in data keypaths/shop-menu-expanded)
                   (get-in data keypaths/store-info-expanded)
                   (get-in data keypaths/account-menu-expanded)))})

(defn built-component [data opts]
  (component/build component (query data) opts))
