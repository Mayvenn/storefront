(ns storefront.components.footer
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.routes :as routes]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.date :as date]
            [storefront.platform.numbers :as numbers]
            [storefront.keypaths :as keypaths]))

(defn phone-uri [tel-num]
  (apply str "tel://+" (filter numbers/digits tel-num)))

(defn products-section [named-searches]
  (for [{:keys [name slug]} named-searches]
    [:a.block.py1.dark-gray.light.titleize (merge {:key slug}
                                             (utils/route-to events/navigate-named-search {:named-search-slug slug}))
     name]))

(defn old-shop-section [named-searches own-store?]
  [:div.col-12
   [:div.medium.border-bottom.border-gray.mb1 "Shop"]
   [:nav.clearfix {:aria-label "Shop Products"}
    [:div.col.col-6
     (products-section (filter named-searches/is-extension? named-searches))]
    [:div.col.col-6
     (products-section (filter #(or (named-searches/is-closure-or-frontal? %)
                                    (and own-store? (named-searches/is-stylist-product? %)))
                               named-searches))]]])

(defn shop-section [own-store? categories]
  (let [category-links (->> categories
                            (sort-by :footer/order)
                            (mapv (fn [{:keys [name slug id]}]
                                    {:title       name
                                     :slug        slug
                                     :nav-message [events/navigate-category {:id id :slug slug}]})))
        links          (if own-store?
                         (conj category-links {:title       "Stylist Exclusives"
                                               :slug        "stylist-products"
                                               :nav-message [events/navigate-named-search {:named-search-slug "stylist-products"}]})
                         category-links)]
    [:div.col-12
     [:div.medium.border-bottom.border-gray.mb1 "Shop"]
     [:nav.clearfix {:aria-label "Shop Products"}
      (for [link-column (partition-all 8 links)]
        [:div.col.col-6
         (for [{:keys [title nav-message slug]} link-column]
           [:a.block.py1.dark-gray.light.titleize (merge {:key slug}
                                                         (apply utils/route-to nav-message))
            title])])]]))

(defn contacts-section [{:keys [call-number sms-number contact-email]}]
  [:div
   [:div.medium.border-bottom.border-gray.mb1 "Contact"]
   [:div.dark-gray.light
    [:div.py1
     [:span.hide-on-tb-dt [:a.dark-gray {:href (phone-uri call-number)} call-number]] ;; mobile
     [:span.hide-on-mb call-number] ;; desktop
     " | 9am-5pm PST M-F"]
    [:a.block.py1.dark-gray {:href (str "mailto:" contact-email)} contact-email]]

   [:div.py1.hide-on-tb-dt
    (ui/ghost-button {:href (phone-uri call-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/phone-ringing {})
                      [:div.ml1.left-align "Call Now"]])
    (ui/ghost-button {:href (str "sms://+1" sms-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/message-bubble {})
                      [:div.ml1.left-align "Send Message"]])
    (ui/ghost-button {:href (str "mailto:" contact-email)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/mail-envelope {})
                      [:div.ml1.left-align "Send Email"]])]])

(def social-section
  (component/html
   [:div
    [:div.medium.border-bottom.border-gray
     [:div.hide-on-mb ui/nbsp]]
    [:div.border-bottom.border-gray.p1.flex.items-center.justify-around.py2
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

(defn footer-links [minimal?]
  [:div.center
   (when-not minimal?
     [:div
      [:a.inherit-color
       (utils/route-to events/navigate-content-about-us) "About"]
      " - "
      [:span
       [:a.inherit-color {:href "https://jobs.mayvenn.com"}
        "Careers"]
       " - "]
      [:a.inherit-color
       (utils/route-to events/navigate-content-help) "Contact"]
      " - "])
   [:a.inherit-color
    (assoc (utils/route-to events/navigate-content-privacy)
           :data-test "content-privacy") "Privacy"]
   " - "
   [:a.inherit-color
    ;; use traditional page load so anchors work
    {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
    "CA Privacy Rights"]
   " - "
   [:a.inherit-color (assoc (utils/route-to events/navigate-content-tos)
                    :data-test "content-tos") "Terms"]
   " - "
   [:a.inherit-color
    ;; use traditional page load so anchors work
    {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")}
    "Our Ads"]
   (when-not minimal?
     " - "
     [:span
      {:item-prop "name"
       :content "Mayvenn Hair"}
      " ©" (date/full-year (date/current-date)) " " "Mayvenn"])] )

(defn full-component [{:keys [named-searches
                              contacts
                              own-store?
                              new-taxon-launch?
                              categories]} owner opts]
  (component/create
   [:div.h5.border-top.border-gray.bg-light-gray
    [:div.container
     [:div.col-12.clearfix
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
       (if new-taxon-launch?
         (shop-section own-store? categories)
         (old-shop-section named-searches own-store?))]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2 (contacts-section contacts)]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2 social-section]]]

    [:div.mt3.bg-dark-gray.white.py1.px3.clearfix.h7
     (footer-links false)]]))

(defn minimal-component [{:keys [call-number]} owner opts]
  (component/create
   [:div.border-top.border-gray.bg-white
    [:div.container
     [:div.center.px3.my2
      [:div.my1.medium.dark-gray "Need Help?"]
      [:div.dark-gray.light.h5
       [:span.hide-on-tb-dt [:a.dark-gray {:href (phone-uri call-number)} call-number]]
       [:span.hide-on-mb call-number]
       " | 9am-5pm PST M-F"]
      [:div.my1.dark-silver.h6
       (footer-links true)]]]]))

(defn contacts-query [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   "+1 (888) 562-7952"
   :contact-email "help@mayvenn.com"})

(defn query [data]
  {:named-searches    (named-searches/current-named-searches data)
   :contacts          (contacts-query data)
   :own-store?        (own-store? data)
   :categories        (get-in data keypaths/categories)
   :new-taxon-launch? (experiments/new-taxon-launch? data)})

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component (contacts-query data) nil)
    (component/build full-component (query data) nil)))
