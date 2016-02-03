(ns storefront.components.footer
  (:require [storefront.components.utils :as utils]
            [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.hooks.experiments :as experiments]
            [goog.string.format]
            [storefront.keypaths :as keypaths]))

(def minimal-footer-events
  #{events/navigate-sign-in
    events/navigate-sign-up
    events/navigate-forgot-password
    events/navigate-reset-password
    events/navigate-cart
    events/navigate-checkout-address
    events/navigate-checkout-delivery
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(def checkout-footer-events
  #{events/navigate-cart
    events/navigate-checkout-address
    events/navigate-checkout-delivery
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(def checkout-footer-variations
  #{"homemade-seal" "norton-seal" "mcafee-seal"})

(defn footer-component [data owner]
  (let [minimal-footer?  (minimal-footer-events (get-in data keypaths/navigation-event))
        checkout-footer? (checkout-footer-events (get-in data keypaths/navigation-event))
        badge-variation (some #(if (experiments/display-variation data %1) %1) checkout-footer-variations)]
    (om/component
     (html
      [:footer#footer {:class (when badge-variation "badges")}
       (list
        (when-not minimal-footer?
          [:div.footer-logo-container [:figure.footer-logo]])
        (if (and badge-variation checkout-footer?)
          [:.checkout
           [:hr]
           [:.help-phone "Need Help? " [:a {:href "tel://8885627952"} "(888)-562-7952"]]
           [:.badges
            [:.guarantee]
            [:div {:class (goog.string.format "security %s" badge-variation)}]]]
          [:div.contact-us
           [:div.footer-container
            [:ul.contact-us-menu
             [:li.cu-item
              [:a.cu-link.send-sonar-dynamic-number-link
               (when-let [number (get-in data keypaths/sms-number)]
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
        (when-not minimal-footer?
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
              [:a.full-link {:href "https://twitter.com/MayvennHair"}]]]]])
        (when-not minimal-footer?
          [:div.legal
           [:div.copyright {:dangerouslySetInnerHTML {:__html (str "&copy; Mayvenn " (.getFullYear (js/Date.)))}}]
           [:a.terms {:target "_blank" :href "/tos.html"} "Terms of Use"]
           [:a.privacy {:target "_blank" :href "/privacy.html"} "Privacy Policy"]]))]))))
