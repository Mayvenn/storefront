(ns storefront.hooks.quadpay
  (:require [storefront.browser.tags :as tags]
            [storefront.browser.events :as browser.events]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]
            [sablono.core :refer [html]]
            [om.core :as om]))

(defn show-modal
  "Requires component to be on the page"
  []
  (when-let [quadpay-widget (js/document.querySelector "quadpay-widget")]
    (when (.-displayModal quadpay-widget)
      (.displayModal quadpay-widget))))

(defn hide-modal
  "Requires component to be on the page"
  []
  (when-let [quadpay-widget (js/document.querySelector "quadpay-widget")]
    (when (.-hideModal quadpay-widget)
      (.hideModal quadpay-widget))))

(defn calc-installment-amount [full-amount]
  (.toFixed (/ full-amount 4) 2))

(defn widget-component
  [{:keys [full-amount]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (browser.events/invoke-late-ready-state-listeners))
    om/IRender
    (render [_]
      (html
       [:quadpay-widget {:amount full-amount}]))))

(defn component [{:quadpay/keys [order-total directive]} owner opts]
  (component/create
   [:div.bg-white
    (let [qp-logo            ^:inline (svg/quadpay-logo {:class "mbnp3"
                                                         :style {:width "70px" :height "14px"}})
          expanded-directive ({:no-total      [:span "Split payment into 4 interest free" [:br] "installments with " qp-logo]
                               :just-select   [:span [:br] "Just select " qp-logo " at check out."]
                               :continue-with [:span [:br] "Continue with " qp-logo " below."]}
                              directive)]
      [:div.border.border-blue.rounded.my2.p2.h6.dark-gray.center.medium
       (when order-total
         [:span
          "4 interest free payments of $" [:span {:data-test "quadpay-payment-amount"}
                                           (calc-installment-amount order-total)]])
       expanded-directive
       [:a.blue.mx1 {:href      "#"
                     :data-test "quadpay-learn-more"
                     :on-click  (fn [e]
                                  (.preventDefault e)
                                  (show-modal))}
        "Learn more."]
       [:div.hide (component/build widget-component {:full-amount order-total} nil)]])]))
