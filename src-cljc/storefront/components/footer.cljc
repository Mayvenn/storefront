(ns storefront.components.footer
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.taxons :as taxons]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.date :as date]
            [storefront.keypaths :as keypaths]))

(def minimal-footer-events
  #{events/navigate-sign-in
    events/navigate-sign-up
    events/navigate-forgot-password
    events/navigate-reset-password
    events/navigate-cart
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(defn contact-us [number]
  [:div.contact-us
   [:div.footer-container
    [:ul.contact-us-menu
     [:li.cu-item
      [:a.cu-link.send-sonar-dynamic-number-link
       (when number
         {:href (str "sms://+1" number)})
       [:i.icon-chat]
       "text"]]
     [:li.cu-item
      [:a.cu-link
       {:href "mailto:help@mayvenn.com"}
       [:i.icon-email]
       "email"]]
     [:li.cu-item
      [:a.cu-link
       {:href "tel://+18885627952"}
       [:i.icon-phone]
       "phone"]]]]])

(def copy
  (component/html [:span {:dangerouslySetInnerHTML {:__html "&copy;"}}]))

(defn original-component [{:keys [minimal? sms-number]} _ _]
  (component/create
   [:footer#footer
    (if minimal?
      (contact-us sms-number)
      [:div
       [:div.footer-logo-container [:figure.footer-logo]]
       (contact-us sms-number)
       [:div.sm-icons
        [:div.footer-container
         [:ul.sm-list
          [:li.sm-icon.icon-facebook
           [:a.full-link.sm-icon {:href "https://www.facebook.com/MayvennHair"}]]
          [:li.sm-icon.icon-instagram
           [:a.full-link.sm-icon {:href "http://instagram.com/mayvennhair"}]]
          [:li.sm-icon.icon-pinterest
           [:a.full-link.sm-icon {:href "http://www.pinterest.com/mayvennhair/"}]]
          [:li.sm-icon.icon-twitter
           [:a.full-link.sm-icon {:href "https://twitter.com/MayvennHair"}]]]]]
       [:div.legal
        [:div.copyright copy " Mayvenn " (date/full-year (date/current-date))]
        [:a.terms {:target "_blank" :href "/tos.html"} "Terms of Use"]
        [:a.privacy {:target "_blank" :href "/privacy.html"} "Privacy Policy"]]])]))

(defn original-query [data]
  {:minimal? (minimal-footer-events (get-in data keypaths/navigation-event))
   :sms-number (get-in data keypaths/sms-number)})

(defn products-section [taxons]
  (for [{:keys [name slug]} taxons]
    [:a (merge {:key slug :data-test (str "footer-" slug)}
               (utils/route-to events/navigate-category {:taxon-slug slug}))
     [:div.gray.light.titleize name]]))

(defn shop-section [taxons own-store?]
  [:div.col-12.clearfix
   [:div.medium.border-bottom.border-light-silver.mb1 "Shop"]
   [:div.col.col-6
    (products-section (filter taxons/is-extension? taxons))]
   [:div.col.col-6
    (products-section (filter #(or (taxons/is-closure-or-frontal? %)
                                   (and own-store? (taxons/is-stylist-product? %))) taxons))]]) 

(defn contacts-section []
  [:div
   [:div.normal.border-bottom.border-light-silver.mb1 "Contact"]
   [:div.gray.light
    [:a.gray {:href "tel://18885627952"} "+1 (888) 562-7952"]
    " | 9am-5pm PST M-F | "
    [:a.gray {:href "mailto:help@mayvenn.com"} "help@mayvenn.com"]]

   (let [button-opts {:color      "bg-dark-white"
                      :border     "border border-black"
                      :padding    "px1 pyp1"
                      :text-color "black medium my1 py0"}]
     [:div.py1
      (ui/button [:div.flex.items-center.justify-center
                  [:div.p1 svg/phone-ringing]
                  [:div.left-align.h3 "Call Now"]] button-opts)
      (ui/button [:div.flex.items-center.justify-center
                  [:div.p1 svg/message]
                  [:div.left-align.h3 "Send Message"]] button-opts)
      (ui/button [:div.flex.items-center.justify-center
                  [:div.p1 svg/mail-envelope]
                  [:div.left-align.h3 "Send Email"]] button-opts)])])

(defn social-section []
  [:div.border-top.border-light-silver
   [:div.clearfix.border-bottom.border-light-silver.p1.flex.items-center.justify-between.py2
    [:div
     [:a {:href "https://www.facebook.com/MayvennHair"}
      [:div.center {:style {:width "22px" :height "22px"}} svg/facebook]]]
    [:div
     [:a {:href "http://instagram.com/mayvennhair"}
      [:div {:style {:width "22px" :height "22px"}} svg/instagram]]]
    [:div
     [:a {:href "https://twitter.com/MayvennHair"}
      [:div {:style {:width "22px" :height "22px"}} svg/twitter]]]
    [:div
     [:a {:href "http://www.pinterest.com/mayvennhair/"}
      [:div {:style {:width "22px" :height "22px"}} svg/pinterest]]]]])

(defn experimental-component [{:keys [taxons own-store?]}]
  (component/create
   [:div.h4.sans-serif.border-top.border-light-silver.bg-dark-white
    [:div.px3.my2.line-height-4 (shop-section taxons own-store?)]
    [:div.px3.my2.line-height-4 (contacts-section)]
    [:div.px3.line-height-4 (social-section)]
    [:div.mt3.bg-black.white.py2.px3.clearfix.h5.light
     [:div.left "© 2016 Mayvenn"]
     [:div.right
      [:a.white {:href "/privacy.html"} "Privacy Policy"]
      " and "
      [:a.white {:href "/tos.html"} "Terms of Use"]]]]))

(defn query [data]
  {:taxons (taxons/current-taxons data)
   :own-store? (own-store? data)})


(defn component [app-state]
  (if (experiments/footer-redesign? app-state)
    (component/build experimental-component (query app-state) nil)
    (component/build original-component (original-query app-state) nil)))




















