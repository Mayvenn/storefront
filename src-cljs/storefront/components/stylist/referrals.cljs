(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]))

(defn circular-progress [{:keys [radius stroke-width percent-filled]}]
  (let [inner-radius    (- radius stroke-width)
        diameter        (* 2 radius)
        circumference   (* 2 js/Math.PI inner-radius)
        arc-length      (* circumference (- 1 percent-filled))
        svg-circle-size {:r inner-radius :cy radius :cx radius :stroke-width stroke-width :fill "none"}]
    [:svg {:width diameter :height diameter :xmlns "http://www.w3.org/2000/svg"
           :style {:transform         "rotate(-90deg)"
                   :-webkit-transform "rotate(-90deg)"}}
     [:circle.stroke-silver svg-circle-size]
     [:circle.stroke-teal (merge svg-circle-size {:style {:stroke-dasharray circumference :stroke-dashoffset arc-length}})]]))

(defn stylist-referral-component [earning-amount {:keys [stylist-name paid-at commissioned-revenue bonus-due]}]
  (html
   (let [state (cond
                 paid-at                      :paid
                 (zero? commissioned-revenue) :referred
                 :else                        :in-progress)]
     [:.flex.items-center.justify-between.border.border-right.border-white.p2.sm-pr4
      [:.circle.bg-silver.mr1 {:style {:width "4em" :height "4em"}}]
      [:.flex-auto.overflow-hidden
       [:.h2 stylist-name]
       [:.h6.gray.line-height-4
        [:div "Joined " "FIXME"]
        (when (= state :paid)
          [:div "Credit Earned: " [:span.black (f/as-money-without-cents bonus-due) " on " (f/locale-date paid-at)]])]]
      (let [revenue     (min earning-amount commissioned-revenue) ;; BUG: backend should never show commissioned-revenue past earning-amount, though it does
            radius      36
            diameter    (* 2 radius)
            circle-size {:style {:width (str diameter "px") :height (str diameter "px")}}]
        [:.ml1.relative
         (case state
           :referred    [:div
                         [:.h6.gray.muted.center.absolute.left-0.right-0 {:style {:top "50%" :margin-top "-0.3rem"}} "No Sales"]
                         [:.border-dashed.border-gray.circle circle-size]]
           :paid        [:.img-earned-icon circle-size]
           :in-progress [:div
                         [:div.absolute.left-0.right-0.center {:style {:top "50%" :margin-top "-15px"}}
                          [:.teal.h2 {:style {:font-size "18px"}} (f/as-money-without-cents (js/Math.floor revenue))]
                          [:.h6.gray.line-height-4 {:style {:font-size "9px"}} "of " (f/as-money-without-cents earning-amount)]]
                         (circular-progress {:radius         radius
                                             :stroke-width   5
                                             :percent-filled (/ revenue earning-amount)})])])])))

(defn show-lifetime-total [lifetime-total]
  (let [message (str "You have earned " (f/as-money-without-cents lifetime-total) " in referrals since you joined Mayvenn.")]
    [:.h6.muted
     [:.p3.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn refer-ad [bonus-amount earning-amount sales-rep-email]
  (let [message (str "Earn " (f/as-money-without-cents bonus-amount) " in credit when each stylist sells their first " (f/as-money-without-cents earning-amount))]
    [:.border-bottom.border-white
     [:.py2.px3.to-sm-hide.border-bottom.border-white
      [:.img-mail-icon.bg-no-repeat.bg-center {:style {:height "4em"}}]
      [:p.py1.h5.muted.overflow-hidden.line-height-3 message]
      [:.h3.col-8.mx-auto.mb3 [:a.col-12.btn.btn-primary.border-teal {:href sales-rep-email :target "_top"} "Refer"]]]

     [:div.p2.clearfix.sm-up-hide
      [:.left.mx1.img-mail-icon.bg-no-repeat {:style {:height "4em" :width "4em"}}]
      [:.right.ml2.m1.h2.col-4 [:a.col-12.btn.btn-primary.btn-big.border-teal {:href sales-rep-email :target "_top"} "Refer"]]
      [:p.py1.h5.muted.overflow-hidden.line-height-3 message]] ]))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    (let [sales-rep-email (str "mailto:"
                               (get-in data keypaths/stylist-sales-rep-email)
                               "?Subject=Referral&body=name:%0D%0Aemail:%0D%0Aphone:")
          bonus-amount (get-in data keypaths/stylist-referral-program-bonus-amount)
          earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)
          referrals (get-in data keypaths/stylist-referral-program-referrals)]
      [:.mx-auto.container.border.border-white {:data-test "referrals-panel"}
       [:.clearfix
        [:.sm-col-right.sm-col-4
         (refer-ad bonus-amount earning-amount sales-rep-email)]

        [:.sm-col.sm-col-8
         (for [referral referrals]
           (stylist-referral-component earning-amount referral))]
        [:.sm-col-right.sm-col-4 {:style {:clear "right"}}
         (when-let [lifetime-total (get-in data keypaths/stylist-referral-program-lifetime-total)]
           (show-lifetime-total lifetime-total))]]]))))
