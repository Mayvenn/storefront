(ns storefront.components.footer
  #?@(:cljs [(:require-macros [storefront.component-macros :as component])])
  (:require [storefront.platform.component-utils :as utils]
            #?@(:clj [[storefront.component :as component]])
            [storefront.events :as events]
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

(defn footer-component [{:keys [minimal? sms-number]} _ _]
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

(defn footer-query [data]
  {:minimal? (minimal-footer-events (get-in data keypaths/navigation-event))
   :sms-number (get-in data keypaths/sms-number)})
