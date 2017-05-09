(ns storefront.components.checkout-credit-card
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.accessors.credit-cards :as cc]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.hooks.stripe :as stripe]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))


(defn saving-card? [data]
  (or (utils/requesting? data request-keys/stripe-create-token)
      (utils/requesting? data request-keys/update-cart-payments)))

(defn inner-piece
  [{{:keys [focused
            guest?
            name
            save-credit-card?
            saved-cards
            fetching-saved-cards?]} :credit-card
    :keys [field-errors]}
   owner opts]
  (reify
    om/IDidMount
    (did-mount [_]
      (messages/handle-message events/stripe-component-mounted
                               {:card-element (stripe/card-element "#card-element")}))
    om/IWillUnmount
    (will-unmount [_]
      (messages/handle-message events/stripe-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        (ui/text-field {:errors    (get field-errors ["cardholder-name"])
                        :data-test "payment-form-name"
                        :keypath   keypaths/checkout-credit-card-name
                        :focused   focused
                        :label     "Cardholder's Name"
                        :name      "name"
                        :required  true
                        :value     name})
        [:div#card-element.border-gray.border.rounded.p2]
       (when (and (not guest?) (empty? saved-cards))
          [:div.mb2
           [:label.dark-gray
            [:input.mr1 (merge (utils/toggle-checkbox keypaths/checkout-credit-card-save save-credit-card?)
                               {:type      "checkbox"
                                :data-test "payment-form-save-credit-card"})]
            "Save my card for easier checkouts."]])]))))

(defn component
  [{{:keys [focused
            selected-saved-card-id
            saved-cards
            fetching-saved-cards?] :as credit-card} :credit-card
    :as data}
   owner opts]
  (om/component
   (html
    [:div
     [:div.h3.my2 "Payment Information"]
     (if fetching-saved-cards?
       [:div.img-large-spinner.bg-center.bg-contain.bg-no-repeat.col-12
        {:style     {:height "4rem"}
         :data-test "spinner"}]
       [:div
        (when (seq saved-cards)
          (let [card-options (conj (mapv (juxt cc/display-credit-card :id) saved-cards)
                                   ["Add a new payment card" "add-new-card"])]
            (ui/select-field {:data-test "payment-form-selected-saved-card"
                              :id        "selected-saved-card"
                              :keypath   keypaths/checkout-credit-card-selected-id
                              :focused   focused
                              :label     "Payment Card"
                              :options   card-options
                              :required  true
                              :value     selected-saved-card-id})))

        (when (or (empty? saved-cards)
                  (= selected-saved-card-id "add-new-card"))
          (om/build inner-piece data opts))])])))

(defn query [data]
  (let [saved-cards (get-in data keypaths/checkout-credit-card-existing-cards)]
    {:credit-card {:guest?                 (get-in data keypaths/checkout-as-guest)
                   :name                   (get-in data keypaths/checkout-credit-card-name)
                   :save-credit-card?      (get-in data keypaths/checkout-credit-card-save)
                   :selected-saved-card-id (get-in data keypaths/checkout-credit-card-selected-id)
                   :saved-cards            saved-cards
                   :fetching-saved-cards?  (and (utils/requesting? data request-keys/get-saved-cards)
                                                (empty? saved-cards))
                   :focused                (get-in data keypaths/ui-focus)}}))

(defn built-component [data opts]
  (om/build component (query data) opts))
