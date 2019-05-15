(ns storefront.components.dtc-banner
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]))

(defn component
  [{:keys [button-behavior banner-copy button-copy]} owner opts]
  (component/create
   (let [close-button (ui/modal-close {:close-attrs (utils/fake-href events/control-dtc-banner-close)})
         banner-copy-with-spacing (into [:div.h5.light.letter-spacing-1]
                                        banner-copy)]
     [:div.bg-dark-gray.light-gray.p2
      ;; Mobile layout
      [:div.hide-on-tb-dt
       [:div.right close-button]
       [:div.center
        [:div.col-9.mx-auto.mb2 banner-copy-with-spacing]
        [:div.col-9.mx-auto (ui/teal-button button-behavior button-copy)]]]
      ;; Desktop / Tablet layout
      [:div.flex.items-center.hide-on-mb
       [:div.flex-auto
        [:div.col-8.mx-auto.center.flex.items-center
         [:div.col-7.mr2 banner-copy-with-spacing]
         [:div.col-7 (ui/teal-button button-behavior button-copy)]]]
       close-button]])))

(defn freeinstall-query [data]
  {:button-behavior (assoc (utils/fake-href events/external-redirect-freeinstall-from-menu {:utm-source "shop"})
                           :data-test "freeinstall-banner-dtc-button")
   :banner-copy     [[:div.medium "Get a Free Install!"]
                     " Mayvenn will pay for your sew-in from a local, certified stylist."]
   :button-copy     "Get a Free Install"})

(defn built-component [data opts]
  (when (and (= (get-in data keypaths/navigation-event) events/navigate-home)
             (= (get-in data keypaths/store-slug) "shop")
             (not (get-in data keypaths/dtc-banner-hidden?)))
    (component/build component (freeinstall-query data) opts)))
