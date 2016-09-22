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
     [:div.gray.light.titleize name]]))

(defn shop-section [named-searches own-store?]
  [:div.col-12
   [:div.medium.border-bottom.border-dark-silver.mb1
    [:div.to-md-hide.f4 "Shop"]
    [:div.md-up-hide "Shop"]]
   [:nav.clearfix.f5
    {:role "navigation" :aria-label "Shop Products"}
    [:div.col.col-6
     (products-section (filter named-searches/is-extension? named-searches))]
    [:div.col.col-6
     (products-section (filter #(or (named-searches/is-closure-or-frontal? %)
                                    (and own-store? (named-searches/is-stylist-product? %)))
                               named-searches))]]])

(defn contacts-section [{:keys [call-number sms-number contact-email]}]
  [:div
   [:div.medium.border-bottom.border-dark-silver.mb1
    [:div.to-md-hide.f4 "Contact"]
    [:div.md-up-hide "Contact"]]
   [:div.gray.light.f5
    [:span.md-up-hide [:a.gray {:href (str "tel://" call-number)} call-number]] ;; mobile
    [:span.to-md-hide call-number] ;; desktop
    " | 9am-5pm PST M-F"
    [:a.block.gray {:href (str "mailto:" contact-email)} contact-email]]

   [:div.py1.md-up-hide
    (ui/ghost-button {:href (str "tel://" call-number)
                      :class "my1 bg-darken-1"}
                     [:div.flex.items-center.justify-center
                      [:div.px1 svg/phone-ringing]
                      [:div.left-align "Call Now"]])
    (ui/ghost-button {:href (str "sms://+1" sms-number)
                      :class "my1 bg-darken-1"}
                     [:div.flex.items-center.justify-center
                      [:div.px1 svg/message]
                      [:div.left-align "Send Message"]])
    (ui/ghost-button {:href (str "mailto:" contact-email)
                      :class "my1 bg-darken-1"}
                     [:div.flex.items-center.justify-center
                      [:div.px1 svg/mail-envelope]
                      [:div.left-align "Send Email"]])]])

(def social-section
  (component/html
   [:div
    [:div.medium.border-bottom.border-dark-silver
     [:div.to-md-hide ui/nbsp]]
    [:div.border-bottom.border-dark-silver.p1.flex.items-center.justify-around.py2
     [:a.block {:item-prop "sameAs"
                :href "https://www.facebook.com/MayvennHair"}
      [:div.center {:style {:width "22px" :height "22px"}} svg/facebook]]
     [:a.block {:item-prop "sameAs"
                :href "http://instagram.com/mayvennhair"}
      [:div {:style {:width "22px" :height "22px"}} svg/instagram]]
     [:a.block {:item-prop "sameAs"
                :href "https://twitter.com/MayvennHair"}
      [:div {:style {:width "22px" :height "22px"}} svg/twitter]]
     [:a.block {:item-prop "sameAs"
                :href "http://www.pinterest.com/mayvennhair/"}
      [:div {:style {:width "22px" :height "22px"}} svg/pinterest]]]]))

(defn full-component [{:keys [named-searches
                              contacts
                              own-store?]} owner opts]
  (component/create
   [:div.h5.border-top.border-dark-silver.bg-light-silver

    [:div.col-12.clearfix
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 (shop-section named-searches own-store?)]
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 (contacts-section contacts)]
     [:div.md-up-col.md-up-col-4.px3.my2.line-height-4 social-section]]

    [:div.mt3.bg-dark-gray.light-silver.py1.px3.clearfix.f6.light
     [:div.left
      {:item-prop "name"
       :content "Mayvenn Hair"}
      [:span "© 2016 "] "Mayvenn"]
     [:div.right
      [:a.light-silver
       (utils/route-to events/navigate-content-help) "Contact Us"]
      " - "
      [:a.light-silver
       (utils/route-to events/navigate-content-about-us) "About Us"]
      " - "
      [:a.light-silver
       (assoc (utils/route-to events/navigate-content-privacy)
              :data-test "content-privacy") "Privacy"]
      " - "
      [:a.light-silver (assoc (utils/route-to events/navigate-content-tos)
                       :data-test "content-tos") "Terms"]]]]))

(defn minimal-component [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-top.border-dark-silver.bg-white
    [:div.center.px3.my2.line-height-4
     [:div.medium.f4.gray "Need Help?"]
     [:div.gray.light.f5
      [:span.md-up-hide [:a.gray {:href (str "tel://" call-number)} call-number]]
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
