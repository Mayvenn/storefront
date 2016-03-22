(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.formatters :as f]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]))

(defn circular-progress [{:keys [radius stroke-width fraction-filled]}]
  (let [inner-radius    (- radius stroke-width)
        diameter        (* 2 radius)
        circumference   (* 2 js/Math.PI inner-radius)
        arc-length      (* circumference (- 1 fraction-filled))
        svg-circle-size {:r inner-radius :cy radius :cx radius :stroke-width stroke-width :fill "none"}]
    [:svg.rotate-270 {:width diameter :height diameter}
     [:circle.stroke-silver svg-circle-size]
     [:circle.stroke-teal (merge svg-circle-size {:style {:stroke-dasharray circumference
                                                          :stroke-dashoffset arc-length}})]]))

(defn profile-picture-circle [profile-picture-url]
  (let [width "4em"]
    [:.circle.bg-silver.overflow-hidden {:style {:width width :height width}}
     [:img {:src profile-picture-url :style {:width width}}]]))

(def state-radius 36)
(def state-diameter (* 2 state-radius))
(def no-sales-icon
  (let [width (str (- state-diameter 2) "px")]
    (html
     ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
     [:.relative
      [:.h6.gray.muted.center.absolute.overlay.m-auto {:style {:height "1em"}} "No Sales"]
      [:.border-dashed.border-gray.circle {:style {:width width :height width}}]])))
(def paid-icon
  (let [width (str state-diameter "px")]
    (html
     (svg/adjustable-check {:class "stroke-teal" :width width :height width}))))

(defmulti state-icon (fn [state earning-amount commissioned-revenue] state))
(defmethod state-icon :referred [_ _ _] no-sales-icon)
(defmethod state-icon :paid [_ _ _] paid-icon)
(defmethod state-icon :in-progress [_ earning-amount commissioned-revenue]
  ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
  [:.relative
   [:.center.absolute.overlay.m-auto {:style {:height "38%"}}
    ;; Explicit font size because font-scaling breaks the circular progress
    [:.h2.teal {:style {:font-size "18px"}} (f/as-money-without-cents (js/Math.floor commissioned-revenue))]
    [:.h6.gray.line-height-4 {:style {:font-size "9px"}} "of " (f/as-money-without-cents earning-amount)]]
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
     [:.flex.items-center.justify-between.border-right.border-white.p2
      [:.mr1 (profile-picture-circle profile-picture-url)]
      [:.flex-auto
       [:.h2 name]
       [:.h6.gray.line-height-4
        [:div "Joined " (f/long-date join-date)]
        (when (= state :paid)
          [:div "Credit Earned: " [:span.black (f/as-money-without-cents bonus-due) " on " (f/locale-date paid-at)]])]]
      [:.ml1.sm-mr3 (state-icon state earning-amount commissioned-revenue)]])))

(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in referrals since you joined Mayvenn."
                                    (f/as-money-without-cents lifetime-total))]
    [:.h6.muted
     [:.p3.to-sm-hide
      [:.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:.my3.flex.justify-center.items-center.sm-up-hide
      [:.mr1 svg/micro-dollar-sign]
      [:.center message]]]))

(defn show-refer-ad [sales-rep-email bonus-amount earning-amount]
  (let [mailto (str "mailto:" sales-rep-email "?Subject=Referral&body=name:%0D%0Aemail:%0D%0Aphone:")
        message (goog.string/format "Earn %s in credit when each stylist sells their first %s"
                                    (f/as-money-without-cents bonus-amount)
                                    (f/as-money-without-cents earning-amount))]
    [:.border-bottom.border-white
     [:.py2.px3.to-sm-hide.border-bottom.border-white
      [:.img-mail-icon.bg-no-repeat.bg-center {:style {:height "4em"}}]
      [:p.py1.h5.muted.line-height-3 message]
      [:.h3.col-8.mx-auto.mb3 [:a.col-12.btn.btn-primary.border-teal {:href mailto :target "_top"} "Refer"]]]

     [:.p2.clearfix.sm-up-hide
      [:.left.mx1.img-mail-icon.bg-no-repeat {:style {:height "4em" :width "4em"}}]
      [:.right.ml2.m1.h2.col-4 [:a.col-12.btn.btn-primary.btn-big.border-teal {:href mailto :target "_top"} "Refer"]]
      [:p.overflow-hidden.py1.h5.muted.line-height-3 message]] ]))

(defn stylist-referrals-component [data owner]
  (om/component
   (html
    [:.mx-auto.container.border.border-white {:data-test "referrals-panel"}
     [:.clearfix
      [:.sm-col-right.sm-col-4
       (show-refer-ad
        (get-in data keypaths/stylist-sales-rep-email)
        (get-in data keypaths/stylist-referral-program-bonus-amount)
        (get-in data keypaths/stylist-referral-program-earning-amount))]

      [:.sm-col.sm-col-8
       (let [earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)]
         (for [referral (get-in data keypaths/stylist-referral-program-referrals)]
           (show-referral earning-amount referral)))]
      [:.sm-col-right.sm-col-4.clearfix
       (when-let [lifetime-total (get-in data keypaths/stylist-referral-program-lifetime-total)]
         (show-lifetime-total lifetime-total))]]])))
