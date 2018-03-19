(ns storefront.components.free-install
  (:require [sablono.core :refer [html]]
            [storefront.history :as history]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(defn component [data owner _]
  (component/create
   (html
    (ui/modal {:col-class "col-11 col-5-on-tb col-4-on-dt"
               :bg-class  "bg-darken-4"}
              [:div.flex.flex-column.bg-cover.bg-top.bg-free-install
               [:div {:style {:height "220px"}}]
               [:div.p4.m3.center.bg-lighten-4
                [:p.h3.medium.mb1.col-9.col-12-on-tb-dt.mx-auto "Buy 3 bundles or more and get a"]
                [:div.h0.bold.teal.shout "Free Install"]
                [:p.h5.mb2.col-9.col-12-on-tb-dt.mx-auto "from a Mayvenn Certified Stylist in Fayetteville, NC"]
                (ui/teal-button
                 (merge (utils/fake-href events/control-free-install-shop-looks)
                        {:data-test "free-install-shop-looks"})
                 "Shop Looks Now")
                [:p.h4.my2.bold "Use code FREEINSTALL at checkout"]
                [:p.h5
                 [:a.inherit-color (merge (utils/fake-href events/control-free-install-dismiss)
                                          {:data-test "free-install-dismiss"})
                  "No thanks, I don't want a free install."]]]]))))

(defn built-component [data opts]
  (component/build component {} opts))

(defmethod effects/perform-effects events/control-free-install-shop-looks [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-shop-by-look))

(defmethod effects/perform-effects events/control-free-install [_ event args _ app-state]
  (cookie-jar/save-pending-promo-code (get-in app-state keypaths/cookie) "freeinstall")
  (when-let [value (get-in app-state keypaths/dismissed-free-install)]
    (cookie-jar/save-dismissed-free-install (get-in app-state keypaths/cookie) value)))

(defmethod transitions/transition-state events/control-free-install [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/pending-promo-code "freeinstall")
      (assoc-in keypaths/popup nil)
      (assoc-in keypaths/dismissed-free-install true)))
