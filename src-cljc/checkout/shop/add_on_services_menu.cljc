(ns checkout.shop.add-on-services-menu
  (:require
   #?@(:cljs [[storefront.api :as api]
              [storefront.components.payment-request-button :as payment-request-button]
              [storefront.components.popup :as popup]
              [storefront.confetti :as confetti]
              [storefront.history :as history]
              [storefront.hooks.quadpay :as quadpay]
              [storefront.platform.messages :as messages]])
   [spice.maps :as maps]
   spice.selector
   [storefront.accessors.mayvenn-install :as mayvenn-install]
   [storefront.component :as component :refer [defcomponent defdynamic-component]]
   [storefront.components.header :as components.header]
   [storefront.components.money-formatters :as mf]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]))


(def add-on-sku->addon-specialty
  {"SRV-DPCU-000" :specialty-addon-hair-deep-conditioning,
   "SRV-3CU-000"  :specialty-addon-360-frontal-customization,
   "SRV-TKDU-000" :specialty-addon-weave-take-down,
   "SRV-CCU-000"  :specialty-addon-closure-customization,
   "SRV-FCU-000"  :specialty-addon-frontal-customization,
   "SRV-TRMU-000" :specialty-addon-natural-hair-trim})

(defn stylist-can-perform-addon-service?
  [stylist addon-service-sku]
  (let [service-key (get add-on-sku->addon-specialty addon-service-sku)]
    (-> stylist
        :service-menu
        service-key
        boolean)))

(defn addon-service-sku->addon-service-menu-entry
  [{:keys    [catalog/sku-id sku/price addon-unavailable-reason]
    sku-name :sku/name}]
  {:addon-service-entry/id                 (str "add-on-service-" sku-id)
   :addon-service-entry/decoration-classes (when addon-unavailable-reason "bg-refresh-gray dark-gray")
   :addon-service-entry/primary            sku-name
   :addon-service-entry/secondary          "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
   :addon-service-entry/tertiary           (mf/as-money price)
   :addon-service-entry/warning            addon-unavailable-reason})

(defn query [data]
  (let [hair-family-facet                   (->> (get-in data keypaths/v2-facets)
                                                 (maps/index-by :facet/slug)
                                                 :hair/family
                                                 :facet/options
                                                 (maps/index-by :option/slug))
        {servicing-stylist :mayvenn-install/stylist
         :as               mayvenn-install} (mayvenn-install/mayvenn-install data)
        sorted-addon-services               (->> (get-in data keypaths/v2-skus)
                                                 vals
                                                 (spice.selector/match-all {} {:catalog/department "service" :service/type "addon"})
                                                 (map (fn [{addon-service-hair-family :hair/family
                                                            addon-sku-id              :catalog/sku-id
                                                            :as                       addon-service}]
                                                        (assoc addon-service
                                                               :addon-unavailable-reason
                                                               (or
                                                                (when (not (stylist-can-perform-addon-service? servicing-stylist addon-sku-id))
                                                                  "Not available with your stylist")
                                                                (when (not (contains?
                                                                            (set (map mayvenn-install/hair-family->service-type addon-service-hair-family))
                                                                            (:mayvenn-install/service-type mayvenn-install)))
                                                                  (storefront.platform.strings/format
                                                                   "Only Available with %s Install" (->> addon-service-hair-family
                                                                                                         first
                                                                                                         (get hair-family-facet)
                                                                                                         :sku/name)))))))
                                                 (sort-by (comp boolean :addon-unavailable-reason))
                                                 (partition-by :addon-unavailable-reason)
                                                 (map (partial sort-by :add-on-service/ui-order)),
                                                 flatten
                                                 (map addon-service-sku->addon-service-menu-entry))]
    {:add-on-services/spinner  (get-in data request-keys/get-skus)
     :add-on-services/services sorted-addon-services}))

(defn add-on-services-popup-template [{:add-on-services/keys [spinner services]}]
  (if spinner
    [:div.py3.h2 ui/spinner]
    [:div.bg-white
     (components.header/mobile-nav-header {:class "border-bottom border-gray" } nil
                                          (component/html [:div.center.proxima.content-1 "Add-on Services"])
                                          (component/html [:div (ui/button-medium-underline-secondary (utils/fake-href events/control-add-ons-popup-done-button) "DONE")]))
     (mapv
      (fn [{:addon-service-entry/keys [id decoration-classes primary secondary tertiary warning]}]
        [:div.p4.flex
         {:key       id
          :data-test id
          :class     decoration-classes}
         [:div.mt1 (ui/check-box {})]
         [:div.flex-grow-1.mr2
          [:div.proxima.content-2 primary]
          [:div.proxima.content-3 secondary]
          [:div.proxima.content-3.red warning]]
         [:div tertiary]])
           services)]))

(defmethod transitions/transition-state events/control-browse-add-ons-button [_ event args app-state]
  (assoc-in app-state keypaths/add-ons-popup-displayed? true))

(defmethod transitions/transition-state events/control-add-ons-popup-done-button [_ event args app-state]
  (assoc-in app-state keypaths/add-ons-popup-displayed? false))
