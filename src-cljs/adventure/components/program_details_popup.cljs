(ns adventure.components.program-details-popup
  (:require [storefront.component :as component]
            [adventure.faq :as faq]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.browser.scroll :as scroll]))

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
    [:h3.title-2.proxima.py1.shout
     title]
    [:div.content-2.proxima
     body]]))

(defn you-buy-the-hair
  [video-navigate]
  (let [bullets [{:title/value "Pick Your Dream Look"
                   :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                  {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                   :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                  {:title/value "Schedule Your Appointment"
                   :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]]

    [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.p2
     [:div.mt2
      [:h1.title-1.canela
       "You buy the hair, we cover the service."]
      [:h2.title-1.proxima.shout.sub.my1
       "Here's how it works."]]

     [:div.col-10.flex.flex-column.items-center
      [:div.stroke-s-color
       (svg/straight-line {:width  "1px"
                           :height "42px"})]
      (map-indexed (partial shop-step "-mb-tb-")
                   bullets)]

     [:a.mt8
      (apply utils/route-to video-navigate)
      (svg/play-video {:width  "30px"
                       :height "30px"})
      [:div.underline.block.content-3.bold.p-color.shout
       "Watch Video"]]]))

(def mayvenn-guarantees
  [:div.col-12.center.flex.flex-column.items-center.py6
   [:div.mt5.mb3
    [:title-1.proxima.shout.pb1
     [:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "29px"}}]]
    [:h2.title-1.canela.shout
     "guarantees"]]
   [:div.col-8.flex.flex-column.items-center
    [:div.pb1.pt6
     [:div
      {:width "32px"}
      (svg/heart {:class  "fill-p-color"
                  :width  "32px"
                  :height "29px"})]
     [:h3.title-2.proxima.py1.shout
      "Top-Notch Customer Service"]
     [:p.content-2.py1.mx-auto
      "Our team is made up of hair experts ready to help you by phone, text, and email."]]

    [:div.pb1.pt6
     [:div
      {:width "30px"}
      (svg/calendar {:class  "fill-p-color"
                     :width  "30px"
                     :height "33px"})]
     [:h3.title-2.proxima.py1.shout
      "30 Day Guarantee"]
     [:p.content-2.py1.mx-auto
      "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."]]

    [:div.pb1.pt6
     [:div
      {:width "35px"}
      (svg/worry-free {:class  "fill-p-color"
                       :width  "35px"
                       :height "36px"})]
     [:h3.title-2.proxima.py1.shout
      "100% Virgin Hair"]
     [:p.content-2.py1.mx-auto
      "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."]]

    [:div.pb1.pt6
     [:div
      {:width "30px"}
      (svg/mirror {:class  "fill-p-color"
                   :width  "30px"
                   :height "34px"})]
     [:h3.title-2.proxima.py1.shout
      "Certified Stylists"]
     [:p.content-2.py1.mx-auto
      "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."]]]])

(defn ^:private vertical-squiggle
  [top]
  [:div.absolute.col-12.flex.justify-center
   {:style {:top top}}
   (svg/vertical-squiggle {:style {:height "72px"}})])

(defmethod popup/component :consolidated-cart-free-install
  [{:keys [faq-data video-navigate] :as queried-data} owner _]
  (ui/modal {:col-class   "col-12 col-10-on-tb-dt my8-on-tb-dt flex justify-center"
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
               (you-buy-the-hair video-navigate)]

              [:div.pb6.bg-white mayvenn-guarantees]

              [:div.bg-white.relative (vertical-squiggle "-50px")]

              [:div.bg-warm-gray
               (faq/component (assoc faq-data :modal? true))]]]))

(defmethod effects/perform-effects events/popup-show-consolidated-cart-free-install
  [_ event args previous-app-state app-state]
  (effects/fetch-cms-keypath app-state [:faq :free-mayvenn-services]))

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
  (let [checkout? (= events/navigate-checkout-free-install
                     (first (get-in data keypaths/navigation-message)))]
    (merge
     {:faq-data (faq/free-install-query data)}
     (cond
       checkout?
       {:video-navigate [events/navigate-checkout-free-install
                         {:query-params {:video "free-install"}}]}
       :else
       {:video-navigate [events/navigate-category {:catalog/category-id "23"
                                                   :page/slug           "mayvenn-install"
                                                   :query-params        {:video "free-install"}}]}))))
