(ns storefront.components.footer
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.accessors.nav :as nav]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.routes :as routes]
            [storefront.accessors.stylists :refer [own-store?]]
            [storefront.platform.date :as date]
            [storefront.platform.numbers :as numbers]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.auth :as auth]
            [catalog.menu :as menu]
            [storefront.accessors.experiments :as experiments]))

(defn phone-uri [tel-num]
  (apply str "tel://+" (numbers/digits-only tel-num)))

(defn ^:private category->link [{:keys        [copy/title page/slug] :as category
                                 product-id   :direct-to-details/id
                                 product-slug :direct-to-details/slug
                                 sku-id       :direct-to-details/sku-id}]
  (let [nav-message (if product-id
                      [events/navigate-product-details {:catalog/product-id product-id
                                                        :page/slug          product-slug
                                                        :query-params       {:catalog/sku-id sku-id}}]
                      [events/navigate-category category])
        slug        (or product-slug slug)]
    {:title       title
     :slug        slug
     :nav-message nav-message}))

(defn shop-section [own-store? categories]
  (let [links (mapv category->link categories)]
    [:div.col-12
     [:div.medium.border-bottom.border-gray.mb1 "Shop"]
     [:nav.clearfix {:aria-label "Shop Products"}
      (for [link-column (partition-all 10 links)]
        [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :slug))}
         (for [{:keys [title nav-message slug]} link-column]
           [:a.block.py1.dark-gray.light.titleize
            (merge {:key slug}
                   (apply utils/route-to nav-message))
            title])])]]))

(defn contacts-section [{:keys [call-number sms-number contact-email]}]
  [:div
   [:div.medium.border-bottom.border-gray.mb1 "Contact"]
   [:div.dark-gray.light
    [:div.py1
     [:span.hide-on-tb-dt (ui/link :link/phone :a.dark-gray {} call-number)] ;; mobile
     [:span.hide-on-mb call-number] ;; desktop
     " | 8am-5pm PST M-F"]
    (ui/link :link/email :a.block.py1.dark-gray {} contact-email)]

   [:div.py1.hide-on-tb-dt
    (ui/ghost-button {:href (phone-uri call-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/phone-ringing {})
                      [:div.ml1.left-align "Call Now"]])
    (ui/ghost-button {:href (str "sms:" sms-number)
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
  (into [:div.center]
        (concat
         (when-not minimal?
           [[:a.inherit-color
             (utils/route-to events/navigate-content-about-us) "About"]
            " - "
            [:span
             [:a.inherit-color {:href "https://jobs.mayvenn.com"}
              "Careers"]
             " - "]
            [:a.inherit-color
             (utils/route-to events/navigate-content-help) "Contact"]
            " - "])
         [[:a.inherit-color
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
             " ©" (date/full-year (date/current-date)) " " "Mayvenn"])])) )

(defn full-component [{:keys [contacts
                              own-store?
                              categories]} owner opts]
  (component/create
   [:div.h5.border-top.border-gray.bg-light-gray
    [:div.container
     [:div.col-12.clearfix
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
       (shop-section own-store? categories)]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2 (contacts-section contacts)]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2 social-section]]]

    [:div.mt3.bg-dark-gray.white.py1.px3.clearfix.h7
     (footer-links false)]]))

(defn minimal-component [data owner opts]
  (component/create
   [:div.border-top.border-gray.bg-white
    [:div.container
     [:div.center.px3.my2
      [:div.my1.medium.dark-gray "Need Help?"]
      [:div.dark-gray.light.h5
       [:span.hide-on-tb-dt (ui/link :link/phone :a.dark-gray {} config/mayvenn-leads-call-number)]
       [:span.hide-on-mb config/mayvenn-leads-call-number]
       " | 8am-5pm PST M-F"]
      [:div.my1.dark-silver.h6
       (footer-links true)]]]]))

(defn contacts-query [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   "+1 (888) 562-7952"
   :contact-email "help@mayvenn.com"})

(defn query [data]
  (let [human-hair? (experiments/human-hair? data)]
    {:contacts   (contacts-query data)
     :own-store? (own-store? data)
     :categories (->> (get-in data keypaths/categories)
                      (filter :footer/order)
                      (filter (partial auth/permitted-category? data))
                      (menu/maybe-hide-experimental-categories human-hair?)
                      (sort-by :footer/order))}))

(defn built-component [data opts]
  (if (nav/minimal-events (get-in data keypaths/navigation-event))
    (component/build minimal-component (contacts-query data) nil)
    (component/build full-component (query data) nil)))
