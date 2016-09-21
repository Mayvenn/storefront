(ns storefront.components.footer
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.accessors.named-searches :as named-searches]
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
    events/navigate-checkout-sign-in
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation})

(defn products-section [named-searches]
  (for [{:keys [name slug]} named-searches]
    [:a (merge {:key slug}
               (utils/route-to events/navigate-category {:named-search-slug slug}))
     [:div.dark-gray.light.titleize name]]))

(defn shop-section [named-searches own-store?]
  [:div.col-12
   [:div.medium.border-bottom.border-light-silver.mb1
    [:div.to-md-hide.f3 "Shop"]
    [:div.md-up-hide "Shop"]]
   [:nav.clearfix.f4
    {:role "navigation" :aria-label "Shop Products"}
    [:div.col.col-6
     (products-section (filter named-searches/is-extension? named-searches))]
    [:div.col.col-6
     (products-section (filter #(or (named-searches/is-closure-or-frontal? %)
                                    (and own-store? (named-searches/is-stylist-product? %)))
                               named-searches))]]])

(defn contacts-section [{:keys [call-number sms-number contact-email]}]
  [:div
   [:div.medium.border-bottom.border-light-silver.mb1
    [:div.to-md-hide.f3 "Contact"]
    [:div.md-up-hide "Contact"]]
   [:div.dark-gray.light.f4
    [:span.md-up-hide [:a.dark-gray {:href (str "tel://" call-number)} call-number]] ;; mobile
    [:span.to-md-hide call-number] ;; desktop
    " | 9am-5pm PST M-F"
    [:a.block.dark-gray {:href (str "mailto:" contact-email)} contact-email]]

   [:div.py1.md-up-hide
    (ui/footer-button {:href (str "tel://" call-number)}
                      [:div.flex.items-center.justify-center
                       [:div.p1 svg/phone-ringing]
                       [:div.left-align.h3 "Call Now"]])
    (ui/footer-button {:href (str "sms://+1" sms-number)}
                      [:div.flex.items-center.justify-center
                       [:div.p1 svg/message-bubble]
                       [:div.left-align.h3 "Send Message"]])
    (ui/footer-button {:href (str "mailto:" contact-email)}
                      [:div.flex.items-center.justify-center
                       [:div.p1 svg/mail-envelope]
                       [:div.left-align.h3 "Send Email"]])]])

(def social-section
  (component/html
   [:div
    [:div.medium.border-bottom.border-light-silver
     [:div.to-md-hide ui/nbsp]]
    [:div.border-bottom.border-light-silver.p1.flex.items-center.justify-around.py2
     [:a.block {:item-prop "sameAs"
                :href "https://www.facebook.com/MayvennHair"}
      [:div {:style {:width "22px" :height "22px"}} svg/mayvenn-on-facebook]]
     [:a.block {:item-prop "sameAs"
                :href "http://instagram.com/mayvennhair"}
      [:div {:style {:width "22px" :height "22px"}} svg/mayvenn-on-instagram]]
     [:a.block {:item-prop "sameAs"
                :href "https://twitter.com/MayvennHair"}
      [:div {:style {:width "22px" :height "22px"}} svg/mayvenn-on-twitter]]
     [:a.block {:item-prop "sameAs"
                :href "http://www.pinterest.com/mayvennhair/"}
      [:div {:style {:width "22px" :height "22px"}} svg/mayvenn-on-pinterest]]]]))

(defn full-component [{:keys [named-searches
                              contacts
                              own-store?]} owner opts]
  (component/create
   [:div.h4.border-top.border-light-silver.bg-dark-white

    [:div.col-12.clearfix
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 (shop-section named-searches own-store?)]
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 (contacts-section contacts)]
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 social-section]]

    [:div.mt3.bg-black.white.py1.px3.clearfix.f5.light
     [:div.left
      {:item-prop "name"
       :content "Mayvenn Hair"}
      [:span "© 2016 "] "Mayvenn"]
     [:div.right
      [:a.white
       (utils/route-to events/navigate-content-about-us) "About Us"]
      " - "
      [:a.white
       (assoc (utils/route-to events/navigate-content-privacy)
              :data-test "content-privacy") "Privacy"]
      " - "
      [:a.white (assoc (utils/route-to events/navigate-content-tos)
                       :data-test "content-tos") "Terms"]]]]))

(defn minimal-component [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-top.border-light-silver.bg-light-white
    [:div.center.px3.my2.line-height-4
     [:div.medium.f3.dark-gray "Need Help?"]
     [:div.dark-gray.light.f4
      [:span.md-up-hide [:a.dark-gray {:href (str "tel://" call-number)} call-number]]
      [:span.to-md-hide call-number]
      " | 9am-5pm PST M-F"]]]))

(defn contacts-query [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   "+1 (888) 562-7952"
   :contact-email "help@mayvenn.com"})

(defn query [data]
  {:named-searches (named-searches/current-named-searches data)
   :contacts       (contacts-query data)
   :own-store?     (own-store? data)})

(defn built-component [data opts]
  (if (minimal-footer-events (get-in data keypaths/navigation-event))
    (component/build minimal-component (contacts-query data) nil)
    (component/build full-component (query data) nil)))
