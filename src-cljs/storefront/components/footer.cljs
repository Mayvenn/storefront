(ns storefront.components.footer
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]))

(defn footer-component [data owner]
  (om/component
   (html
    [:footer#footer
     [:div.footer-logo-container [:figure.footer-logo]]
     [:div.contact-us
      [:div.footer-container
       [:ul.contact-us-menu
        [:li.cu-item
         [:a.cu-link.send-sonar-dynamic-number-link
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
          "phone"]]]]]
     [:div.sm-icons
      [:div.footer-container
       [:ul.sm-list
        [:li.sm-icon.icon-facebook
         [:a.full-link {:href "https://www.facebook.com/MayvennHair"}]]
        [:li.sm-icon.icon-instagram
         [:a.full-link {:href "http://instagram.com/mayvennhair"}]]
        [:li.sm-icon.icon-pinterest
         [:a.full-link {:href "http://www.pinterest.com/mayvennhair/"}]]
        [:li.sm-icon.icon-twitter
         [:a.full-link {:href "https://twitter.com/MayvennHair"}]]]]]
     [:div.legal
      [:div.copyright {:dangerouslySetInnerHTML {:__html (str "&copy; Mayvenn " (.getFullYear (js/Date.)))}}]
      [:a.terms {:href "#FIXME"} "Terms of Use"]
      [:a.privacy {:href "#FIXME"} "Privacy Policy"]]])))
