(ns storefront.components.install-phone-capture
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn component [data owner {:keys [close-attrs]}]
  (om/component
   (html
    (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt"
               :bg-class    "bg-darken-1"
               :close-attrs close-attrs}
              [:div.fixed.z4.bottom-0.left-0.right-0
               [:div.border-top.border-dark-gray.border-width-2
                [:div.border-top.border-dark-gray.border-width-2
                 [:a.h2.bold.white.bg-purple.px3.py2.flex.items-center
                  (utils/fake-href events/control-popup-hide)
                  [:div.flex-auto.center "Get $25 – Share Your Opinion"]
                  [:div.stroke-white.ml1
                   (svg/dropdown-arrow {:height "16" :width "16"})]]]
                [:div.bg-light-gray.px3.py4
                 [:div.h6.mb3.mx1.line-height-3
                  "Enter your phone number to apply. Selected participants will be "
                  "interviewed by phone and sent $25."]
                 (ui/input-group
                  {:keypath       nil ;;voucher-keypaths/eight-digit-code
                   :wrapper-class "col-8 flex bg-white pl3 items-center circled-item"
                   :class         ""
                   :data-test     ""
                   :focused       true
                   :placeholder   "(xxx) xxx - xxxx"
                   ;; :value      code
                   ;; :errors     (get field-errors ["voucher-code"])
                   :data-ref      "voucher-code"}
                  {:ui-element ui/teal-button
                   :content    "Get Survey"
                   :args       {:on-click     (utils/send-event-callback events/control-cart-update-coupon)
                                :class        "flex justify-center medium items-center circled-item"
                                :size-class   "col-4"
                                :height-class "py2"
                                :data-test    ""
                                ;; :disabled?  updating?
                                ;; :spinning?  applying?
                                }})
                 [:a.h6.dark-gray.mx2
                  {:href "#"}
                  "No thanks."]]]]))))

(defn query
  [data]
  {})
