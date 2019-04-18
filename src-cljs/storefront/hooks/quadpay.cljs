(ns storefront.hooks.quadpay
  (:require [storefront.browser.tags :as tags]
            [storefront.browser.events :as browser.events]
            [storefront.component :as component]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]
            [sablono.core :refer [html]]
            [om.core :as om]))

(def uri "https://widgets.quadpay.com/mayvenn/quadpay-widget-2.2.1.js")

(defn insert []
  (when-not (pos? (.-length (.querySelectorAll js/document ".quadpay-tag")))
    (let [tag (tags/src-tag uri "quadpay-tag")
          cb #(handle-message events/inserted-quadpay)]
      (tags/insert-tag-with-callback tag cb))))

(defn show-modal
  "Requires component to be on the page"
  []
  (when-let [quadpay-widget (js/document.querySelector "quadpay-widget")]
    (.displayModal quadpay-widget)))

(defn hide-modal
  "Requires component to be on the page"
  []
  (when-let [quadpay-widget (js/document.querySelector "quadpay-widget")]
    (.hideModal quadpay-widget)))

(defn calc-installment-amount [full-amount]
  (.toFixed (/ full-amount 4) 2))

(defn ^:private widget-component
  [{:keys [full-amount]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (browser.events/invoke-late-ready-state-listeners))
    om/IRender
    (render [_]
      (html
       [:quadpay-widget {:amount full-amount}]))))

(defn component [{:keys [show? order-total directive]} owner opts]
  (component/create
   [:div
    (when show?
      [:div.border.border-blue.rounded.my2.py2.h6.dark-gray.center
       "4 interest free payments of $" [:span {:data-test "quadpay-payment-amount"}
                                        (calc-installment-amount order-total)]
       [:div.flex.justify-center.items-center
        directive
        [:a.blue.mx1 {:href      "#"
                      :data-test "quadpay-learn-more"
                      :on-click  (fn [e]
                                   (.preventDefault e)
                                   (show-modal))}
         "Learn more."]]
       [:div.hide (component/build widget-component {:full-amount order-total} nil)]])]))
