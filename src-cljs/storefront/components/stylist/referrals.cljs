(ns storefront.components.stylist.referrals
  (:require [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.components.money-formatters :as mf]
            [storefront.components.formatters :as f]
            [storefront.components.flash :as flash]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.stylist.pagination :as pagination]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [goog.string]))

(defn circular-progress [{:keys [radius stroke-width fraction-filled]}]
  (let [inner-radius    (- radius stroke-width)
        diameter        (* 2 radius)
        circumference   (* 2 js/Math.PI inner-radius)
        arc-length      (* circumference (- 1 fraction-filled))
        svg-circle-size {:r inner-radius :cy radius :cx radius :stroke-width stroke-width :fill "none"}]
    [:svg {:width diameter :height diameter}
     [:g {:transform (str "rotate(-90 " radius " " radius ")")}
      [:circle.stroke-dark-gray svg-circle-size]
      [:circle.stroke-teal (merge svg-circle-size {:style {:stroke-dasharray  circumference
                                                           :stroke-dashoffset arc-length}})]]]))

(def state-radius 36)
(def state-diameter (* 2 state-radius))
(def no-sales-icon
  (let [width (str (- state-diameter 2) "px")]
    (html
     ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
     [:div.relative
      [:div.h6.line-height-1.dark-gray.center.absolute.overlay.m-auto {:style {:height "1em"}} "No Sales"]
      [:div.border-dashed.border-dark-gray.circle {:style {:width width :height width}}]])))

(def paid-icon
  (html
   (svg/circled-check {:class "stroke-teal"
                       :style {:width state-diameter :height state-diameter}})))

(defmulti state-icon (fn [state earning-amount commissioned-revenue] state))
(defmethod state-icon :referred [_ _ _] no-sales-icon)
(defmethod state-icon :paid [_ _ _] paid-icon)
(defmethod state-icon :in-progress [_ earning-amount commissioned-revenue]
  ;; Absolute centering: https://www.smashingmagazine.com/2013/08/absolute-horizontal-vertical-centering-css/
  [:div.relative
   [:div.center.absolute.overlay.m-auto {:style {:height "50%"}}
    [:div.h5.line-height-1.teal.medium.mbp4 (mf/as-money-without-cents (js/Math.floor commissioned-revenue))]
    [:div.h7.line-height-1.dark-gray "of " (mf/as-money-without-cents earning-amount)]]
   (circular-progress {:radius         state-radius
                       :stroke-width   5
                       :fraction-filled (/ commissioned-revenue earning-amount)})])

(defn show-referral [earning-amount {:keys [referred-stylist paid-at commissioned-revenue bonus-due]}]
  (html
   (let [{:keys [name join-date portrait]} referred-stylist
         state (cond
                 paid-at                      :paid
                 (zero? commissioned-revenue) :referred
                 :else                        :in-progress)]
     [:div.flex.items-center.justify-between.border-bottom.border-left.border-right.border-light-gray.p2
      {:key (str name join-date)}
      [:div.mr1 (ui/circle-picture (ui/square-image portrait 96))]
      [:div.flex-auto
       [:div.h3.navy name]
       [:div.h6.dark-gray
        [:div.dark-gray "Joined " (f/long-date join-date)]
        (when (= state :paid)
          [:div "Credit Earned: " [:span.navy (mf/as-money-without-cents bonus-due) " on " (f/short-date paid-at)]])]]
      [:div.ml1.mr3-on-tb-dt (state-icon state earning-amount commissioned-revenue)]])))

(defn show-lifetime-total [lifetime-total]
  (let [message (goog.string/format "You have earned %s in referrals since you joined Mayvenn."
                                    (mf/as-money-without-cents lifetime-total))]
    [:div.h6.dark-gray
     [:div.p3.hide-on-mb
      [:div.mb1.center svg/micro-dollar-sign]
      [:div message]]
     [:div.my3.hide-on-tb-dt
      [:div.center message]]]))

(defn refer-button [link-attrs]
  [:a.col-12.btn.btn-primary
   (merge (utils/fake-href events/control-popup-show-refer-stylists)
          link-attrs)
   "Refer"])

(defn show-refer-ad [bonus-amount earning-amount]
  (let [message (goog.string/format "Earn %s in credit when each stylist sells their first %s"
                                    (mf/as-money-without-cents bonus-amount)
                                    (mf/as-money-without-cents earning-amount))]
    [:div
     [:div.py2.px3.hide-on-mb
      [:div.center.fill-navy svg/large-mail]
      [:p.py1.h5.black message]
      [:div.h4.col-8.mx-auto.mb3 (refer-button {:data-test "refer-button-desktop"})]]

     [:div.p2.clearfix.hide-on-tb-dt.border-bottom.border-light-gray
      [:div.left.mx1.fill-navy svg/large-mail]
      [:div.right.ml2.m1.h4.col-4 (refer-button {:class "btn-big"
                                                 :data-test "refer-button-mobile"})]
      [:p.overflow-hidden.py1.h5.black message]]]))

(def empty-referrals
  (html
   [:div.center.p3.hide-on-mb
    [:div.m2.img-no-chat-icon.bg-no-repeat.bg-contain.bg-center {:style {:height "4em"}}]
    [:p.h3.dark-gray "Looks like you haven't" [:br] "referred anyone yet."]]))

(def show-program-terms
  [:div.col-right-on-tb-dt.col-4-on-tb-dt.clearfix
   [:div.border-top.border-gray.mx-auto.my2 {:style {:width "100px"}}]
   [:div.center.my2.h6
    [:a.dark-gray (utils/route-to events/navigate-content-program-terms) "Mayvenn Program Terms"]]])

(defn component [{:keys [earning-amount
                         bonus-amount
                         lifetime-total
                         referrals
                         page
                         pages
                         fetching?]} _]
  (om/component
   (html
    (if (and (empty? (seq referrals)) fetching?)
      [:div.my2.h2 ui/spinner]
      [:div.clearfix.mb3
       {:data-test "referrals-panel"}
       [:div.col-right-on-tb-dt.col-4-on-tb-dt
        (when bonus-amount
          (show-refer-ad bonus-amount earning-amount))]

       [:div.col-on-tb-dt.col-8-on-tb-dt
        (when (seq referrals)
          [:div
           (for [referral referrals]
             (show-referral earning-amount referral))
           (pagination/fetch-more events/control-stylist-referrals-fetch fetching? page pages)])
        (when (zero? pages) empty-referrals)]
       [:div.col-right-on-tb-dt.col-4-on-tb-dt.clearfix
        (when (and (seq referrals) (pos? lifetime-total))
          (show-lifetime-total lifetime-total))]
       show-program-terms]))))

(defn query [data]
  {:earning-amount (get-in data keypaths/stylist-referral-program-earning-amount)
   :bonus-amount   (get-in data keypaths/stylist-referral-program-bonus-amount)
   :lifetime-total (get-in data keypaths/stylist-referral-program-lifetime-total)
   :referrals      (get-in data keypaths/stylist-referral-program-referrals)
   :page           (get-in data keypaths/stylist-referral-program-page)
   :pages          (get-in data keypaths/stylist-referral-program-pages)
   :fetching?      (utils/requesting? data request-keys/get-stylist-referral-program)})

(def ordinal ["first" "second" "third" "fourth" "fifth"])

(defn refer-component [{:keys [focused
                               bonus-amount
                               earning-amount
                               referrals
                               flash-failure
                               add-referral-spinning?]
                        {:keys [field-errors] :as errors} :errors}
                       owner
                       {:keys [close-attrs]}]
  (om/component
   (html
    (ui/modal {:close-attrs close-attrs}
              [:div.bg-white.rounded.p4
               (ui/modal-close {:close-attrs close-attrs})
               [:form.p1 {:on-submit (utils/send-event-callback events/control-stylist-referral-submit)}
                (when (or (seq errors) flash-failure)
                  [:div.mb2
                   (flash/error-box
                    {:data-test "form-errors"}
                    [:div.px2
                     (when (seq errors) (:error-message errors))
                     " "
                     (when flash-failure flash-failure)])])
                [:div.h3.my1.center.navy.medium "Refer a stylist and earn " (mf/as-money-without-cents bonus-amount)]
                [:p.light.dark-gray.my2
                 "Do you know a stylist who would be a great Mayvenn?"
                 " Enter their information below and when they sell " (mf/as-money-without-cents earning-amount)
                 " of Mayvenn products you will earn " (mf/as-money-without-cents bonus-amount) "!"]
                (for [[idx referral] (map-indexed vector referrals)]
                  [:div.py2.border-top.border-gray
                   {:key idx :data-test "referral-entry"}
                   [:div.h3.dark-gray.my2 "Enter your "(get ordinal idx)" referral"
                    (when (pos? idx) [:a.mr1.flex.items-center.right
                                      (merge (utils/fake-href events/control-stylist-referral-remove {:index idx})
                                             {:data-test (str "remove-referral-button-" idx)})
                                      svg/counter-dec])]
                   [:div.col-12 (ui/text-field {:class      "rounded"
                                                :data-test  (str "referral-fullname-" idx)
                                                :errors     (get field-errors ["referrals" idx "fullname"])
                                                :id         (str "referral-fullname-" idx)
                                                :keypath    (conj keypaths/stylist-referrals idx :fullname)
                                                :focused    focused
                                                :label      "Name"
                                                :name       (str "referrals[" idx "][fullname]")
                                                :required   true
                                                :type       "text"
                                                :value      (:fullname referral)})]
                   [:div.col-12 (ui/text-field {:class     "rounded"
                                                :data-test (str "referral-phone-" idx)
                                                :errors    (get field-errors ["referrals" idx "phone"])
                                                :id        (str "referral-phone-" idx)
                                                :keypath   (conj keypaths/stylist-referrals idx :phone)
                                                :focused   focused
                                                :label     "Mobile Phone (required)"
                                                :name      (str "referrals[" idx "][phone]")
                                                :required  true
                                                :type      "tel"
                                                :value     (:phone referral)})]
                   [:div.col-12 (ui/text-field {:class     "rounded"
                                                :data-test (str "referral-email-" idx)
                                                :errors    (get field-errors ["referrals" idx "email"])
                                                :id        (str "referral-email-" idx)
                                                :keypath   (conj keypaths/stylist-referrals idx :email)
                                                :focused   focused
                                                :label     "Email"
                                                :name      (str "referrals[" idx "][email]")
                                                :type      "email"
                                                :value     (:email referral)})]])
                (when (< (count referrals) 5)
                  [:div.py3.border-top.border-gray
                   [:div.col-12.col-6-on-tb-dt.mx-auto
                    (ui/ghost-button
                     (merge (utils/fake-href events/control-stylist-referral-add-another)
                            {:data-test "another-referral-button"})
                     [:div.flex.items-center.justify-center.h5
                      [:div.mr1.flex.items-center (svg/counter-inc {:class "stroke-white fill-teal"})]
                      [:div "Add Another Referral"]])]])
                [:div.col-12.col-6-on-tb-dt.mx-auto
                 (ui/submit-button "Send" {:data-test "submit-referral"
                                           :spinning? add-referral-spinning?})]]]))))

(defn query-refer [data]
  {:earning-amount         (get-in data keypaths/stylist-referral-program-earning-amount)
   :bonus-amount           (get-in data keypaths/stylist-referral-program-bonus-amount)
   :errors                 (get-in data keypaths/errors)
   :flash-failure          (get-in data keypaths/flash-now-failure-message)
   :referrals              (get-in data keypaths/stylist-referrals)
   :focused                (get-in data keypaths/ui-focus)
   :add-referral-spinning? (utils/requesting? data request-keys/send-referrals)})

(defn built-refer-component [data opts]
  (om/build refer-component (query-refer data) opts))

(defn thanks-component [_ owner {:keys [close-attrs]}]
  (om/component
   (html
    (ui/modal {:close-attrs close-attrs
               :bg-class "bg-darken-4"}
              [:div.flex.flex-column.items-center.justify-center.pt4
               [:div.m1 (svg/circled-check {:class "stroke-light-gray"
                                            :style {:height "70px" :width "70px"}})]
               [:div.h3.light-gray.center {:data-test "referral-thanks"} "Thank you for your referral!"]]))))

(defn built-thanks-component [_ _]
  (om/build thanks-component nil nil))
