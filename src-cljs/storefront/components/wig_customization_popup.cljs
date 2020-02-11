(ns storefront.components.wig-customization-popup
  (:require [storefront.component :as component :refer [defcomponent]]
            [adventure.faq :as faq]
            [storefront.components.accordion :as accordion]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.browser.scroll :as scroll]
            [adventure.organisms.call-out-center :as call-out-center]))

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
  (let [bullets [{:title/value "Select Your Wig"
                  :body/value  "Decide which wig you want and buy from Mayvenn. Shop Lace Front & 360 Lace Wigs."}
                  {:title/value ["Choose A Mayvenn" ui/hyphen "Certified Stylist"]
                   :body/value  "Browse our network of professional stylists in your area and make an appointment."}
                  {:title/value "Drop Off Your Wig"
                   :body/value  "Leave the wig with your stylist and talk about what you want. Your stylist will bleach the knots, tint the lace, cut the lace, customize your hairline and make sure it fits perfectly."}
                 {:title/value "Schedule Your Pickup"
                  :body/value  "Make an appointment to pick up your wig with your stylist in a week."}
                 {:title/value "Go Get Your Wig"
                  :body/value  "You pick up your wig. Let us pick up the tab. Let us over the cost of your customization—we insist."}]]

    [:div.col-12.bg-cool-gray.center.flex.flex-column.items-center.p2
     [:div.mt2
      [:h2.title-1.canela
       "We’re taking the work out of wigs with free customization."]
      [:div.title-1.proxima.shout.sub
       "Here's how it works."]
      [:div.conent-2.proxima.py1.mx-auto.col-10.pb4
       "Purchase any of our virgin lace front wigs or virgin 360 lace wigs and we’ll find you a Mayvenn Certified Stylist to customize it for free."]]

     [:div.col-10.flex.flex-column.items-center
      [:div.stroke-s-color
       (svg/straight-line {:width  "1px"
                           :height "42px"})]
      (map-indexed (partial shop-step (str #_layer-id "-mb-tb-"))
                   bullets)]

     (ui/button-large-primary {:class "col-10"} "Buy Wigs")]))

(def mayvenn-guarantees
  [:div.col-12.center.flex.flex-column.items-center.pb6
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

(defn ^:private whats-included
  [{:whats-included/keys [header-value bullets]}]
  [:div.pt8
   [:div.col-10.col-6-on-dt.mx-auto.border.border-framed.flex.justify-center.mb8
    [:div.col-12.flex.flex-column.items-center.m5.py4
     {:style {:width "max-content"}}
     (when header-value
       [:div.proxima.title-2.shout.py1
        header-value])
     [:ul.col-12.list-purple-diamond
      {:style {:padding-left "15px"}}
      (for [[i b] (map-indexed vector bullets)]
          [:li.py1 {:key (str i)} b])]]]])

(defn ^:private faq-molecule [{:faq/keys [expanded-index sections background-color]}]
  [:div.px6.mx-auto.col-10-on-dt.py6
   {:class background-color}
   [:div.canela.title-1.center.my7 "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         (map
                        (fn [{:keys [title paragraphs]}]
                          {:title [:content-1 title]
                           :paragraphs paragraphs})
                        sections)}
    {:opts {:section-click-event events/faq-section-selected}})])

(defmethod popup/component :wigs-customization
  [{:keys [call-out-data] :as queried-data} owner _]
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
              [:div.mb10
               you-buy-the-hair]


              [:div.pb6.bg-white
               (whats-included call-out-data)
               mayvenn-guarantees]

              [:div.bg-white.relative (vertical-squiggle "-50px")]

              [:div.bg-warm-gray
               (faq-molecule queried-data)]

              (component/build call-out-center/organism call-out-data nil)]]))

(defmethod transitions/transition-state events/popup-show-wigs-customization
  [_ event args app-state]
  (assoc-in app-state keypaths/popup :wigs-customization))

(defmethod transitions/transition-state events/control-wigs-customization-dismiss
  [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-wigs-customization-dismiss
  [_ event args previous-app-state app-state]
  (scroll/enable-body-scrolling))

(defmethod popup/query :wigs-customization
  [data]
  {:faq-data           (faq/free-install-query data)
   :call-out-data      {:call-out-center/bg-class    "bg-white"
                        :call-out-center/title       "Want to book with your own stylist?"
                        :call-out-center/subtitle    "Recommend them to become Mayvenn Certified"
                        :whats-included/header-value "What's Included?"
                        :whats-included/bullets      ["Bleaching the knots" "Tinting the lace" "Cutting the lace" "Customize your hairline"]
                        :cta/id                      "recommend-stylist"
                        :cta/target                  [events/external-redirect-typeform-recommend-stylist]
                        :cta/label                   "Submit Your Stylist"
                        :element/type                :call-out
                        :react/key                   :recommend-stylist}
   :faq/sections       [{:title      "How does Free Customization work?",
                         :paragraphs ["It’s simple: when you buy any lace front or 360 frontal wig from Mayvenn, we’ll pay for the wig customization. You’ll pick the Mayvenn Certified Stylist you want to work with and they’ll bleach, pluck, and cut until it’s personalized for you."]}
                        {:title      "What kind of hair is it? Can I apply heat to it?"
                         :paragraphs ["All of our products use 100% virgin human hair, which can be heat-styled. To protect and care for your unit, we recommend using a heat protectant spray or serum with tools."]}
                        {:title      "How do I drop the wig off to the stylist?"
                         :paragraphs ["After you complete your wig purchase, Mayvenn Concierge will reach out and connect you to a Certified Stylist for your Wig Customization appointment."]}
                        {:title      "How long does it take to customize the wig?"
                         :paragraphs ["After you drop it off, the stylist has one week to complete the wig customization."]}
                        {:title      "What does the Wig Customization include?"
                         :paragraphs ["The stylist will be plucking the hairline, bleaching the knots, tinting and cutting the lace."]}
                        {:title      "What doesn’t the Wig Customization include?"
                         :paragraphs ["Add-on services and installation are not included in the free wig customization, and will require an extra fee. Non-covered add-on services can include: braiding, sewing, coloring, styling."]}
                        {:title      "What if I want to get my wig customized by another stylist? Can I still get a Mayvenn Wig Customization?"
                         :paragraphs ["You must get your wig customized by a Certified Stylist in order to receive this complimentary service."]}]
   :faq/expanded-index (get-in data keypaths/faq-expanded-section)})

