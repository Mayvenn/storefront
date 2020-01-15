(ns storefront.hooks.quadpay
  (:require [storefront.browser.tags :as tags]
            [storefront.browser.events :as browser.events]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]))

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

(defdynamic-component widget-component
  (did-mount [_] (browser.events/invoke-late-ready-state-listeners))
  (render [this] (let [{:keys [full-amount]} (component/get-props this)]
                   (component/html
                    [:quadpay-widget {:amount full-amount}]))))

(defcomponent component [{:quadpay/keys [show? order-total directive]} owner opts]
  [:div.bg-white.proxima.content-3
   (when show?
     (let [qp-logo            ^:inline (svg/quadpay-logo {:class "mbnp3"
                                                          :style {:width "70px" :height "14px"}})
           expanded-directive ({:no-total      [:span "Split payment into 4 interest-free" [:br] "installments with " qp-logo]
                                :just-select   [:span "Just select " qp-logo " at check out."]
                                :continue-with [:span "Continue with " qp-logo " below."]}
                               directive)]
       [:div.border.border-blue.my2.p2.center
        (when order-total
          [:span.mb2
           "4 interest-free payments of $" [:span {:data-test "quadpay-payment-amount"}
                                            (calc-installment-amount order-total)]])
        [:div.block
         expanded-directive
         [:a.blue.mx1 {:href      "#"
                       :data-test "quadpay-learn-more"
                       :on-click  (fn [e]
                                    (.preventDefault e)
                                    (show-modal))}
          "Learn more."]]
        [:div.hide (component/build widget-component {:full-amount order-total} nil)]]))])
