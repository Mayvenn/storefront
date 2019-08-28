(ns checkout.ui.molecules
  (:require [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn promo-entry
  [{:keys [focused coupon-code field-errors updating? applying? error-message] :as promo-data}]
  [:form.mt2.bg-white
   {:on-submit (utils/send-event-callback events/control-cart-update-coupon)}
   (ui/input-group
    {:keypath       keypaths/cart-coupon-code
     :wrapper-class "flex-grow-5 clearfix"
     :class         "h6"
     :data-test     "promo-code"
     :focused       focused
     :label         "Promo code"
     :value         coupon-code
     :errors        (when (get field-errors ["promo-code"])
                      [{:long-message error-message
                        :path         ["promo-code"]}])
     :data-ref      "promo-code"}
    {:ui-element ui/teal-button
     :content    "Apply"
     :args       {:on-click    (utils/send-event-callback events/control-cart-update-coupon)
                  :class       "flex justify-center items-center"
                  :width-class "flex-grow-3"
                  :data-test   "cart-apply-promo"
                  :disabled?   updating?
                  :spinning?   applying?}})])

(defn cart-summary-line-molecule
  [{:cart-summary-line/keys
    [id label sublabel icon value action-id action-target action-icon class]}]
  (when id
    [:tr.h6.medium
     {:data-test (str "cart-summary-line-" id)
      :key       (str "cart-summary-line-" id)}
     [:td.pyp1.flex.items-center.align-middle
      icon
      label
      (when sublabel
        [:div.h7.dark-silver.ml1
         sublabel])
      (when action-id
        [:a.ml1.h6.gray.flex.items-center
         (merge {:data-test action-id}
                (apply utils/fake-href action-target))
         action-icon])]
     [:td.pyp1.right-align.medium
      {:class class}
      value]]))

(defn cart-summary-total-line
  [{:cart-summary-total-line/keys [id label value]}]
  (when id
    [:div.flex.medium.h5 {:data-test id}
     [:div.flex-auto label]
     [:div.right-align value]]))

(def freeinstall-informational
  [:div.flex.py2
   "✋"
   [:div.flex.flex-column.pl1
    [:div.purple.h5.medium
     "Don't miss out on "
     [:span.bold
      "free Mayvenn Install"]]
    [:div.h6
     "Save 10% & get a free install by a licensed stylist when you add a Mayvenn Install to your cart below."]
    [:div.flex.justify-left.py1
     [:div (ui/teal-button {:height-class :small
                                  :class        "bold"
                                  :on-click     (utils/send-event-callback events/control-cart-add-freeinstall-coupon)} "Add Mayvenn Install")]
     [:div.teal.h7.flex.items-center
      (ui/button {:class    "inherit-color px4 py1 medium"
                  :on-click (utils/send-event-callback events/popup-show-adventure-free-install)} "learn more")]]
    [:div.h8.dark-gray
     "*Mayvenn Install cannot be combined with other promo codes."]]])
