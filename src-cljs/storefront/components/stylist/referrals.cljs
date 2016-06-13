(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.components.utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.request-keys :as request-keys]
            [storefront.hooks.experiments :as experiments]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn circular-progress [{:keys [radius stroke-width fraction-filled]}]
  (let [inner-radius    (- radius stroke-width)
        diameter        (* 2 radius)
        circumference   (* 2 js/Math.PI inner-radius)
        arc-length      (* circumference (- 1 fraction-filled))
        svg-circle-size {:r inner-radius :cy radius :cx radius :stroke-width stroke-width :fill "none"}]
    [:svg {:width diameter :height diameter}
     [:g {:transform (str "rotate(-90 " radius " " radius ")")}
      [:circle.stroke-silver svg-circle-size]
      [:circle.stroke-green (merge svg-circle-size {:style {:stroke-dasharray circumference
                                                           :stroke-dashoffset arc-length}})]]]))

(def state-radius 36)
(def state-diameter (* 2 state-radius))
(def no-sales-icon
  (let [width (str (- state-diameter 2) "px")]
    (html
     ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
     [:.relative
      [:.h6.silver.center.absolute.overlay.m-auto {:style {:height "1em"}} "No Sales"]
      [:.border-dashed.border-silver.circle {:style {:width width :height width}}]])))

(def paid-icon
  (let [width (str state-diameter "px")]
    (html
     (svg/adjustable-check {:class "stroke-green" :width width :height width}))))

(defmulti state-icon (fn [state earning-amount commissioned-revenue] state))
(defmethod state-icon :referred [_ _ _] no-sales-icon)
(defmethod state-icon :paid [_ _ _] paid-icon)
(defmethod state-icon :in-progress [_ earning-amount commissioned-revenue]
  ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
  [:.relative
   [:.center.absolute.overlay.m-auto {:style {:height "50%"}}
    ;; Explicit font size because font-scaling breaks the circular progress
    [:.h2.green.light {:style {:font-size "18px"}} (f/as-money-without-cents (js/Math.floor commissioned-revenue))]
    [:.h6.gray.line-height-3 {:style {:font-size "9px"}} "of " (f/as-money-without-cents earning-amount)]]
   (circular-progress {:radius         state-radius
                       :stroke-width   5
                       :fraction-filled (/ commissioned-revenue earning-amount)})])

(defn show-referral [earning-amount {:keys [referred-stylist paid-at commissioned-revenue bonus-due]}]
  (html
   (let [{:keys [name join-date profile-picture-url]} referred-stylist
         state (cond
                 paid-at                      :paid
                 (zero? commissioned-revenue) :referred
                 :else                        :in-progress)]
     [:.flex.items-center.justify-between.border-bottom.border-left.border-right.border-white.p2
      {:key (str name join-date)}
      [:.mr1 (ui/circle-picture profile-picture-url)]
      [:.flex-auto
       [:.h2.navy name]
       [:.h6.gray.line-height-4
        [:div.silver "Joined " (f/long-date join-date)]
        (when (= state :paid)
          [:div "Credit Earned: " [:span.navy (f/as-money-without-cents bonus-due) " on " (f/short-date paid-at)]])]]
      [:.ml1.sm-mr3 (state-icon state earning-amount commissioned-revenue)]])))

(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in referrals since you joined Mayvenn."
                                    (f/as-money-without-cents lifetime-total))]
    [:.h6.dark-silver
     [:.p3.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn refer-button [refer-to-leads? sales-rep-email link-attrs]
  (let [mailto (str "mailto:" sales-rep-email "?Subject=Referral&body=name:%0D%0Aemail:%0D%0Aphone:")]
    [:a.col-12.btn.btn-primary
     (merge (if refer-to-leads?
              (utils/fake-href events/control-popup-show-refer-stylists)
              {:href mailto :target "_top"})
            link-attrs)
     "Refer"]))

(defn show-refer-ad [refer-to-leads? sales-rep-email bonus-amount earning-amount]
  (let [message (goog.string/format "Earn %s in credit when each stylist sells their first %s"
                                    (f/as-money-without-cents bonus-amount)
                                    (f/as-money-without-cents earning-amount))]
    [:div
     [:.py2.px3.to-sm-hide
      [:.center.fill-navy svg/large-mail]
      [:p.py1.h5.dark-silver.line-height-2 message]
      [:.h3.col-8.mx-auto.mb3 (refer-button refer-to-leads? sales-rep-email {})]]

     [:.p2.clearfix.sm-up-hide.border-bottom.border-white
      [:.left.mx1.fill-navy svg/large-mail]
      [:.right.ml2.m1.h3.col-4 (refer-button refer-to-leads? sales-rep-email {:class "btn-big"})]
      [:p.overflow-hidden.py1.h5.dark-silver.line-height-2 message]]]))

(def empty-referrals
  (html
   [:.center.p3.to-sm-hide
    [:.m2.img-no-chat-icon.bg-no-repeat.bg-contain.bg-center {:style {:height "4em"}}]
    [:p.h2.silver "Looks like you haven't" [:br] "referred anyone yet."]]))

(defn stylist-referrals-component [{:keys [sales-rep-email
                                           earning-amount
                                           bonus-amount
                                           lifetime-total
                                           referrals
                                           page
                                           pages
                                           fetching?
                                           refer-to-leads?]} _]
  (om/component
   (html
    (if (and (empty? (seq referrals)) fetching?)
      [:.my2.h1 ui/spinner]
      [:.mx-auto.container {:data-test "referrals-panel"}
       [:.clearfix.mb3
        [:.sm-up-col-right.sm-up-col-4
         (when bonus-amount
           (show-refer-ad refer-to-leads? sales-rep-email bonus-amount earning-amount))]

        [:.sm-up-col.sm-up-col-8
         (when (seq referrals)
           [:div
            (for [referral referrals]
              (show-referral earning-amount referral))
            (pagination/fetch-more events/control-stylist-referrals-fetch fetching? page pages)])
         (when (zero? pages) empty-referrals)]
        [:.sm-up-col-right.sm-up-col-4.clearfix
         (when (and (seq referrals) (pos? lifetime-total))
           (show-lifetime-total lifetime-total))]]]))))

(defn stylist-referrals-query [data]
  {:sales-rep-email (get-in data keypaths/stylist-sales-rep-email)
   :earning-amount  (get-in data keypaths/stylist-referral-program-earning-amount)
   :bonus-amount    (get-in data keypaths/stylist-referral-program-bonus-amount)
   :lifetime-total  (get-in data keypaths/stylist-referral-program-lifetime-total)
   :referrals       (get-in data keypaths/stylist-referral-program-referrals)
   :page            (get-in data keypaths/stylist-referral-program-page)
   :pages           (get-in data keypaths/stylist-referral-program-pages)
   :refer-to-leads? (experiments/stylist-referrals? data)
   :fetching?       (utils/requesting? data request-keys/get-stylist-referral-program)})

(def ordinal ["first" "second" "third" "fourth" "fifth"])

(defn refer-component [{:keys [bonus-amount earning-amount referrals]}
                       owner
                       {:keys [on-close]}]
  (om/component
   (html
    (ui/modal on-close
              [:.bg-light-white.rounded.p2.mt3.sans-serif
               [:.clearfix
                [:a.pointer.h2.right.rotate-45 {:href "#" :on-click on-close}
                 [:.fill-dark-silver {:alt "Close"} svg/counter-inc]]]
               [:.p1
                [:.h2.my1.center.navy.medium "Refer a stylist and earn " (f/as-money-without-cents bonus-amount)]
                [:p.light.dark-gray.line-height-3.my2
                 "Do you know a stylist that would be a great Mayvenn?"
                 " Enter their information below and when they sell " (f/as-money-without-cents earning-amount)
                 " of Mayvenn products you will earn " (f/as-money-without-cents bonus-amount) "!"]
                (for [[idx referral] (map-indexed vector referrals)]
                  [:.py2.border-top.border-light-silver
                   {:key idx}
                   [:.h2.black.my2 "Enter your "(get ordinal idx)" referral"]
                   [:.flex.col-12
                    [:.col-6 (ui/text-field "First Name"
                                            (conj keypaths/stylist-referrals idx :first-name)
                                            (:first-name referral)
                                            {:autofocus "autofocus"
                                             :type      "text"
                                             :name      (str "referral["idx"][first-name]")
                                             :data-test (str "referral-first-name-"idx)
                                             :id        (str "referral-first-name-"idx)
                                             :class     "rounded-left"
                                             :required  true})]

                    [:.col-6 (ui/text-field "Last Name"
                                            (conj keypaths/stylist-referrals idx :last-name)
                                            (:last-name referral)
                                            {:type      "text"
                                             :name      (str "referral["idx"][last-name]")
                                             :id        (str "referral-last-name-"idx)
                                             :data-test (str "referral-last-name-"idx)
                                             :class     "rounded-right border-width-left-0"})]]
                   [:.col-12 (ui/text-field "Mobile Phone (required)"
                                            (conj keypaths/stylist-referrals idx :phone)
                                            (:phone referral)
                                            {:type      "tel"
                                             :name      (str "referral["idx"][phone]")
                                             :id        (str "referral-phone-"idx)
                                             :data-test (str "referral-phone-"idx)
                                             :class     "rounded"
                                             :required  true})]
                   [:.col-12 (ui/text-field "Email"
                                            (conj keypaths/stylist-referrals idx :email)
                                            (:email referral)
                                            {:type      "email"
                                             :name      (str "referral["idx"][email]")
                                             :id        (str "referral-email-"idx)
                                             :data-test (str "referral-email-"idx)
                                             :class     "rounded"})]])
                (when (< (count referrals) 5)
                  [:.py3.border-top.border-light-silver.center
                   [:a.col-10.mx-auto.btn.btn-outline.dark-gray.border-light-silver.p2
                    (utils/fake-href events/control-stylist-referral-add-another)
                    [:.flex.items-center.justify-center.h3.line-height-1
                     [:.mr1.flex.items-center.fill-light-silver svg/counter-inc]
                     [:.medium "Add Another Referral"]]]])]]))))

(defn query-refer [data]
  {:earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)
   :bonus-amount   (get-in data keypaths/stylist-referral-program-bonus-amount)
   :referrals      (get-in data keypaths/stylist-referrals)})
