(ns storefront.components.footer
  (:require [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [catalog.categories :as categories]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]))

(defn phone-uri [tel-num]
  (apply str "tel://+" (numbers/digits-only tel-num)))

(defn ^:private category->link [{:keys        [copy/title page/slug] :as category
                                 product-id   :direct-to-details/id
                                 product-slug :direct-to-details/slug
                                 sku-id       :direct-to-details/sku-id}]
  (let [nav-message (if product-id
                      [events/navigate-product-details (merge
                                                        {:catalog/product-id product-id
                                                         :page/slug          product-slug}
                                                        (when sku-id
                                                          {:query-params {:SKU sku-id}}))]
                      [events/navigate-category category])
        slug        (or product-slug slug)]
    {:title         title
     :slug          slug
     :new-category? (categories/new-category? slug)
     :nav-message   nav-message}))

(defn shop-section [{:keys [partition-count categories]}]
  (let [links (mapv category->link categories)]
    [:div.col-12
     [:div.medium.border-bottom.border-gray.mb1 "Shop"]
     [:nav.clearfix {:aria-label "Shop Products"}
      (for [link-column (partition-all partition-count links)]
        [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :slug))}
         (for [{:keys [title new-category? nav-message slug]} link-column]
           [:a.block.py1.dark-gray.light.titleize
            (merge {:key (str "footer-link-" slug)}
                   (apply utils/route-to nav-message))
            (when new-category?
              [:span.teal "NEW "])
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
                      (svg/phone-ringing {:class "stroke-teal"})
                      [:div.ml1.left-align "Call Now"]])
    (ui/ghost-button {:href (str "sms:" sms-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/message-bubble {:class "stroke-teal"})
                      [:div.ml1.left-align "Send Message"]])
    (ui/ghost-button {:href (str "mailto:" contact-email)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      (svg/mail-envelope {:class "stroke-teal"})
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

(defn full-component
  [{:keys [contacts categories]} owner opts]
  (component/create
   [:div.h5.border-top.border-gray.bg-light-gray
    [:div.container
     [:div.col-12.clearfix
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
       (shop-section {:partition-count 10 :categories categories})]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
       (contacts-section contacts)]
      [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
       social-section]]]

    [:div.mt3.bg-dark-gray.white.py1.px3.clearfix.h8
     [:div
      {:style {:margin-bottom "90px"}}
      (component/build footer-links/component {:minimal? false} nil)]]]))

(defn contacts-query
  [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   "+1 (888) 562-7952"
   :contact-email "help@mayvenn.com"})

(defn query
  [data]
  {:contacts   (contacts-query data)
   :categories (->> (get-in data keypaths/categories)
                    (filter :footer/order)
                    (filter (partial auth/permitted-category? data))
                    (sort-by :footer/order))})

(defn built-component
  [data opts]
  (if (nav/show-minimal-footer? (get-in data keypaths/navigation-event))
    (footer-minimal/built-component data nil)
    (component/build full-component (query data) nil)))
