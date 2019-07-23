(ns popup.organisms
  (:require [clojure.spec.alpha :as s]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn query
  [app-state]
  {:close/event                              events/control-email-captured-dismiss
   :capture/email-capture-quadpay-new?       (experiments/email-capture-quadpay-new? app-state) ;; ns?
   :pre-title/copy                           (ui/ucare-img {:width "24px"} ;; Mayvenn logo
                                                           "6620eab6-ca9b-4400-b8e5-d6ec00654dd3")
   :monstrous-title/copy                     ["Buy Now," "Pay Later."]
   :subtitle/copy                            [[:div.h2.mtb1 "with"]
                                              [:div.ml2.mtp6 {:style {:width  "124px"
                                                                      :height "23px"}}
                                               svg/quadpay-logo]]
   :description/copy                         [:div.mt1
                                              "Buy hair with "
                                              [:span.purple "0%"]
                                              " interest over "
                                              [:div [:span.purple "4"]
                                               " installments."]]
   :form-submission-pair/errors              (get-in app-state keypaths/field-errors)
   :form-submission-pair/focused             (get-in app-state keypaths/ui-focus)
   :form-submission-pair/email               (get-in app-state keypaths/captured-email)
   :form-submission-pair/submission-callback events/control-email-captured-submit})

(defn monstrous-title-molecule
  [{:monstrous-title/keys [copy]}]
  [:div.h0.line-height-1.pt3
   (for [line (cond-> copy string? vec)]
     [:div line])])

(defn subtitle-molecule
  [{:subtitle/keys [copy]}]
  [:div.flex.my4
   (for [span (cond-> copy string? vec)]
     span)])

(defn form-submission-pair
  [{:form-submission-pair/keys [errors focused email callback]}]
  [:div.px3.my4
   [:form.col-12.flex.flex-column.items-center
    {:on-submit (utils/send-event-callback callback)}
    [:div.col-12.mx-auto
     (ui/text-field {:errors    (get errors ["email"])
                     :keypath   keypaths/captured-email
                     :focused   focused
                     :label     "Your E-Mail Address"
                     :name      "email"
                     :required  true
                     :type      "email"
                     :value     email
                     :class     "dark-gray h6 bg-light-silver"
                     :data-test "email-input"})
     (ui/submit-button "Shop Now"
                       {:color-kw     :color/teal
                        :height-class "py3"
                        :class        "h3 bold mt1"
                        :data-test    "email-input-submit"})]]])

(defn organism
  [{:as               query
    pre-title-copy    :pre-title/copy
    modal-close-event :modal-close/event
    description-copy  :description/copy} _ _]
  (component/create
   (ui/modal
    {:close-attrs (utils/fake-href modal-close-event)
     :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"}
    [:div.flex.flex-column.bg-cover.bg-top.bg-white.p2.rounded.col-12
     {:style {:max-width "345px"}}
     [:div.flex.justify-end.mt2.mr1
      (svg/simple-x (merge (utils/fake-href modal-close-event)
                           {:class     "white"
                            :data-test "dismiss-email-capture"
                            :style     {:width  "18px"
                                        :height "18px"
                                        :color  "gray"}}))]
     [:div.bold.px3
      pre-title-copy
      (monstrous-title-molecule query)
      (subtitle-molecule query)
      description-copy]
     [:div {:style {:height "30px"}}]
     (form-submission-pair query)])))
