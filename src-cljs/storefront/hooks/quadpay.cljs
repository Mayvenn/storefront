(ns storefront.hooks.quadpay
  (:require [storefront.browser.tags :as tags]
            [storefront.browser.events :as browser.events]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]))

(def uri "https://cdn.quadpay.com/v1/quadpay.js?tagname=quadpay-widget")

(defn- get-widget-id []
  (when (.-quadpay js/window)
    (first (js/quadpay.widget.getWidgetIds))))

(defn insert []
  (when-not (pos? (.-length (.querySelectorAll js/document ".quadpay-tag")))
    (let [tag (tags/src-tag uri "quadpay-tag")
          cb #(handle-message events/inserted-quadpay)]
      (tags/insert-tag-with-callback tag cb))))

(defn show-modal
  "Requires component to be on the page"
  []
  (when-let [wid (get-widget-id)]
    ;; quadpay.widget.displayModal isn't defensive enough to validate the element is there
    ;; so we have to implement that behavior ourselves
    (when-let [container (some-> (js/document.querySelector (str "#qp-modal-" wid))
                                 (.-shadowRoot)
                                 (.getElementById "qp-modal__overlay"))]
      (try
        (js/quadpay.widget.displayModal wid)
        (catch js/Error err nil)))
    true))

(defn hide-modal
  "Requires component to be on the page"
  []
  (when-let [wid (get-widget-id)]
    ;; quadpay.widget.hideModal isn't defensive enough to validate the element is there
    ;; so we have to implement that behavior ourselves
    (when-let [container (some-> (js/document.querySelector (str "#qp-modal-" wid))
                                 (.-shadowRoot)
                                 (.getElementById "qp-modal__overlay"))]
      (try
        (js/quadpay.widget.hideModal wid)
        (catch js/Error err nil)))
    true))

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
     (let [qp-logo            ^:inline (svg/quadpay-logo)
           expanded-directive ({:no-total      [:span "Split payment into 4 interest-free" [:br] "installments with " qp-logo]
                                :just-select   [:span "Just select " qp-logo " at check out."]
                                :continue-with [:span "Continue with " qp-logo " below."]}
                               directive)]
       [:div.border.border-quadpay.my2.p2.center
        (when order-total
          [:span.mb2
           "4 interest-free payments of $" [:span {:data-test "zip-payment-amount"}
                                            (calc-installment-amount order-total)]])
        [:div.block
         expanded-directive
         [:a.quadpay.mx1 {:href      "#"
                          :data-test "zip-learn-more"
                          :on-click  (fn [e]
                                       (.preventDefault e)
                                       (show-modal))}
          "Learn more."]]
        [:div.hide (component/build widget-component {:full-amount order-total} nil)]]))])
