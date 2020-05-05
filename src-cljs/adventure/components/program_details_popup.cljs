(ns adventure.components.program-details-popup
  (:require [storefront.component :as component :refer [defcomponent]]
            [adventure.faq :as faq]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.browser.scroll :as scroll]))

(def get-a-free-install
  (let [step (fn [{:keys [icon-uuid icon-width title description]}]
               [:div.col-12.mt2.center
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                [:div.h5.p-color.medium title]
                [:p.h6.col-10.col-9-on-dt.mx-auto description]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h2 "Get a FREE Install"]
      [:div.h6 "In three easy steps"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (step {:icon-uuid   "e90526f9-546f-4a6d-a05a-3bea94aedc21"
             :icon-width  "28"
             :title       "Buy Any 3 Bundles or More"
             :description "Including closures and frontals! Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."})
      (step {:icon-uuid   "cddd38e0-f598-4aca-90fc-4350dd4469fb"
             :icon-width  "35"
             :title       "Get Your Voucher"
             :description "We’ll send you a free install voucher via SMS and email after your order ships."})
      (step {:icon-uuid   "7712537c-3805-4d92-90b5-a899748a21c5"
             :icon-width  "35"
             :title       "Show Your Stylist The Voucher"
             :description "Redeem the voucher when you go in for your appointment with your Certified Mayvenn Stylist."})]]))

(def why-mayvenn-is-right-for-you
  (let [entry (fn [{:keys [icon-uuid icon-width title description]}]
                [:div.col-12.my2.flex.flex-column.items-center.items-end
                 [:div.flex.justify-center.items-end.mb1
                  {:style {:height "35px"}}
                  (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                 [:div.h6.p-color.medium.mbnp4 title]

                 [:p.h6.col-11.center description]])]
    [:div.col-12.bg-pale-purple.mt3.py8.px4
     [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

      [:div.my2.flex.flex-column.items-center.col-12
       [:h2.titleize "Why mayvenn is right for you"]
       [:div.h6.black.titleize "It's not just about hair"]]

      (entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
              :icon-width  "29"
              :title       "Top Notch Customer Service"
              :description "Our team is made up of hair experts ready to help you by phone, text, and email."})
      (entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
              :icon-width  "52"
              :title       "30 Day Guarantee"
              :description "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."})
      (entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
              :icon-width  "35"
              :title       "100% Virgin Hair"
              :description "Our hair is gently steam processed and can last up to a year. Available in 8 textures and 8 shades."})
      (entry {:icon-uuid   "3f622e92-6d95-49e2-a0c1-51a535b22975"
              :icon-width  "35"
              :title       "Free Install"
              :description "Get your hair installed absolutely FREE!"})]]))

(defmethod popup/component :adventure-free-install
  [{:keys [footer-data faq-data]} owner _]
  (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs (utils/fake-href events/control-adventure-free-install-dismiss)
             :bg-class  "bg-darken-4"}
            [:div.bg-white
             {:style {:max-width "400px"}}
             [:div.col-12.clearfix.pt1.pk2
              [:div.right.pt2.pr2.pointer
               (svg/simple-x
                (merge (utils/fake-href events/control-adventure-free-install-dismiss)
                       {:data-test    "adventure-popup-dismiss"
                        :height       "27px"
                        :width        "27px"
                        :class        "black"}))]
              [:div.flex.justify-center.pb2
               [:div.col-6
                (ui/clickable-logo {:class "col-12 mx4"
                                    :style {:height "40px"}})]]]
             [:div.flex.flex-column
              [:div.center
               [:div  ;; Body
                [:h1.h3.bold.white.bg-p-color.p3
                 "Get a FREE install when you"
                 [:br]
                 "buy 3 bundles or more"]]]
              [:div.mt10.mb6 get-a-free-install]

              why-mayvenn-is-right-for-you

              [:div.mt10
               (faq/component (assoc faq-data :modal? true))]

              [:div.hide-on-tb-dt.pt3 ;; Footer
               (component/build footer-modal/component footer-data nil)]]]))

(defmethod popup/query :adventure-free-install
  [data]
  {:faq-data    (faq/free-install-query data)
   :footer-data (footer-modal/query data)})

(defmethod transitions/transition-state events/control-adventure-free-install-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-adventure-free-install-dismiss [_ event args previous-app-state app-state]
  (scroll/enable-body-scrolling))

(defmethod transitions/transition-state events/popup-show-adventure-free-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :adventure-free-install))

(defn ^:private shop-step
  [key-prefix
   idx
   {title :title/value
    body  :body/value}]
  (component/html
   [:div.p1
    {:key (str key-prefix idx)}
    [:div.title-2.canela.py1
     (str "0" (inc idx))]
    [:div.title-2.proxima.py1.shout
     title]
    [:div.content-2.proxima
     body]]))

(def you-buy-the-hair
  (let [bullets [{:title/value "Pick Your Dream Look"
                   :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                  {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                   :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                  {:title/value "Schedule Your Appointment"
                   :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]]

    [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.p2
     [:div.mt2
      [:h2.title-1.canela
       "You buy the hair, we cover the service."]
      [:div.title-1.proxima.shout.sub
       "Here's how it works."]]

     [:div.col-10.flex.flex-column.items-center
      [:div.stroke-s-color
       (svg/straight-line {:width  "1px"
                           :height "42px"})]
      (map-indexed (partial shop-step (str #_layer-id "-mb-tb-"))
                   bullets)]

     [:a.mt8
      (apply utils/route-to [events/navigate-home {:query-params {:video "free-install"}}])
      (svg/play-video {:width  "30px"
                       :height "30px"})
      [:div.underline.block.content-3.bold.p-color.shout
       "Watch Video"]]]))

(def mayvenn-guarantees
  [:div.col-12.center.flex.flex-column.items-center.py6
   [:div.mt5.mb3
    [:h2.title-1.proxima.shout.pb1
     [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "29px"}}]]
    [:div.title-1.canela.shout
     "guarantees"]]
   [:div.col-8.flex.flex-column.items-center
    [:div.pb1.pt6
     [:div
      {:width "32px"}
      (svg/heart {:class  "fill-p-color"
                  :width  "32px"
                  :height "29px"})]
     [:div.title-2.proxima.py1.shout
      "Top-Notch Customer Service"]
     [:p.content-2.py1.mx-auto
      "Our team is made up of hair experts ready to help you by phone, text, and email."]]

    [:div.pb1.pt6
     [:div
      {:width "30px"}
      (svg/calendar {:class  "fill-p-color"
                     :width  "30px"
                     :height "33px"})]
     [:div.title-2.proxima.py1.shout
      "30 Day Guarantee"]
     [:p.content-2.py1.mx-auto
      "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."]]

    [:div.pb1.pt6
     [:div
      {:width "35px"}
      (svg/worry-free {:class  "fill-p-color"
                       :width  "35px"
                       :height "36px"})]
     [:div.title-2.proxima.py1.shout
      "100% Virgin Hair"]
     [:p.content-2.py1.mx-auto
      "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."]]

    [:div.pb1.pt6
     [:div
      {:width "30px"}
      (svg/mirror {:class  "fill-p-color"
                   :width  "30px"
                   :height "34px"})]
     [:div.title-2.proxima.py1.shout
      "Certified Stylists"]
     [:p.content-2.py1.mx-auto
      "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."]]]])

(defn ^:private vertical-squiggle
  [top]
  [:div.absolute.col-12.flex.justify-center
   {:style {:top top}}
   (svg/vertical-squiggle {:style {:height "72px"}})])

(defmethod popup/component :consolidated-cart-free-install
  [{:keys [faq-data] :as queried-data} owner _]
  (ui/modal {:col-class   "col-12 col-10-on-tb col-10-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs (utils/fake-href events/control-consolidated-cart-free-install-dismiss)
             :bg-class    "bg-darken-4"}
            [:div.bg-cool-gray
             {:style {:max-width "400px"}}
             [:div.col-12.clearfix.pt1.pk2
              [:div.right.pt2.pr2.pointer
               (svg/x-sharp
                (merge (utils/fake-href events/control-consolidated-cart-free-install-dismiss)
                       {:data-test "consolidated-cart-free-install-popup-dismiss"
                        :height    "20px"
                        :width     "20px"}))]]

             [:div.flex.flex-column
              [:div.my10.mb10
               you-buy-the-hair]

              [:div.pb6.bg-white mayvenn-guarantees]

              [:div.bg-white.relative (vertical-squiggle "-50px")]

              [:div.bg-warm-gray
               (faq/component (assoc faq-data :modal? true))]]]))

(defmethod transitions/transition-state events/popup-show-consolidated-cart-free-install
  [_ event args app-state]
  (assoc-in app-state keypaths/popup :consolidated-cart-free-install))

(defmethod transitions/transition-state events/control-consolidated-cart-free-install-dismiss
  [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-consolidated-cart-free-install-dismiss
  [_ event args previous-app-state app-state]
  (scroll/enable-body-scrolling))

(defmethod popup/query :consolidated-cart-free-install
  [data]
  {:faq-data (faq/free-install-query data)})
