(ns storefront.components.footer
  (:require [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
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
  (component/html
   (let [links (mapv category->link categories)]
     [:div.col-12
      [:div.medium.border-bottom.border-gray.mb1 "Shop"]
      [:nav.black.clearfix {:aria-label "Shop Products"}
       (for [link-column (partition-all partition-count links)]
         [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :slug))}
          (for [{:keys [title new-category? nav-message slug]} link-column]
            [:a.inherit-color.block.py1.light.titleize
             (merge {:key (str "footer-link-" slug)}
                    (apply utils/route-to nav-message))
             (when new-category?
               [:span.p-color "NEW "])
             title])])]])))

(defcomponent contacts-section [{:keys [call-number sms-number contact-email]} _ _]
  [:div
   [:div.medium.border-bottom.border-gray.mb1 "Contact"]
   [:div.light
    [:div.py1
     [:span.hide-on-tb-dt (ui/link :link/phone :a.inherit-color {} call-number)] ;; mobile
     [:span.hide-on-mb call-number] ;; desktop
     " | 8am-5pm PST M-F"]
    (ui/link :link/email :a.block.py1.inherit-color {} contact-email)]

   [:div.py1.hide-on-tb-dt
    (ui/ghost-button {:href (phone-uri call-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      ^:inline (svg/phone-ringing {:class "stroke-p-color"})
                      [:div.ml1.left-align "Call Now"]])
    (ui/ghost-button {:href (str "sms:" sms-number)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      ^:inline (svg/message-bubble {:class "stroke-p-color"})
                      [:div.ml1.left-align "Send Message"]])
    (ui/ghost-button {:href (str "mailto:" contact-email)
                      :class "my1"}
                     [:div.flex.items-center.justify-center
                      ^:inline (svg/mail-envelope {:class "stroke-p-color"})
                      [:div.ml1.left-align "Send Email"]])]])

(defcomponent social-section [_ _ _]
  [:div
   [:div.medium.border-bottom.border-gray
    [:div.hide-on-mb ui/nbsp]]
   [:div.border-bottom.border-gray.p1.flex.items-center.justify-around.py2
    [:a.block {:href "https://www.facebook.com/MayvennHair"}
     [:div {:style {:width "22px" :height "22px"}}
      ^:inline (svg/mayvenn-on-facebook)]]
    [:a.block {:href "http://instagram.com/mayvennhair"}
     [:div {:style {:width "22px" :height "22px"}}
      ^:inline (svg/mayvenn-on-instagram)]]
    [:a.block {:href "https://twitter.com/MayvennHair"}
     [:div {:style {:width "22px" :height "22px"}}
      ^:inline (svg/mayvenn-on-twitter)]]
    [:a.block {:href "http://www.pinterest.com/mayvennhair/"}
     [:div {:style {:width "22px" :height "22px"}}
      ^:inline (svg/mayvenn-on-pinterest)]]]])

(defcomponent full-component
  [{:keys [contacts categories essence-copy]} owner opts]
  [:div.h5.border-top.border-gray.bg-cool-gray
   [:div.container
    [:div.col-12.clearfix
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (shop-section {:partition-count 10 :categories categories})]
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (component/build contacts-section contacts)]
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (component/build social-section)]
     (when essence-copy
       [:div.col-on-tb-dt.col-4-on-tb-dt.px4.pt3.pb2.h7.center.line-height-4.underline
        essence-copy])]]

   [:div.mt3.bg-black.white.py1.px3.clearfix.h8
    [:div
     {:style {:margin-bottom "90px"}}
     (component/build footer-links/component {:minimal? false} nil)]]])

(defn contacts-query
  [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   "+1 (888) 562-7952"
   :contact-email "help@mayvenn.com"})

(defn query
  [data]
  (let [shop? (= (get-in data keypaths/store-slug) "shop")]
    {:contacts     (contacts-query data)
     :categories   (if shop?
                     (->> (get-in data keypaths/categories)
                          (into []
                                (comp (filter :dtc-footer/order)
                                      (filter (partial auth/permitted-category? data))))
                          (sort-by :dtc-footer/order))
                     (->> (get-in data keypaths/categories)
                          (into []
                                (comp
                                 (filter :footer/order)
                                 (filter (partial auth/permitted-category? data))))
                          (sort-by :footer/order)))
     :essence-copy (str "Included is a one year subscription to ESSENCE Magazine - a $10 value! "
                        "Offer and refund details will be included with your confirmation.")}))

(defn dtc-link [{:keys [title new-category? nav-message slug]}]
  (component/html
   [:a.inherit-color.block.py1.light.titleize
    (merge {:key (str "footer-link-" slug)}
           ;; be super specific so we can utilize the routing fast path
           (utils/route-to (first nav-message)
                           (select-keys (second nav-message)
                                        [:catalog/category-id
                                         :page/slug
                                         :catalog/product-id
                                         :named-search-slug
                                         :legacy/product-slug])))
    (when new-category?
      [:span.p-color "NEW "])
    (str title)]))

(defcomponent dtc-shop-section [{:keys [categories partition-count]} _ _]
  (let [links                          (mapv category->link categories)
        [column-1-links rest-of-links] (split-at partition-count links)]
    [:div.col-12
     [:div.medium.border-bottom.border-gray.mb1 "Shop"]
     [:nav.black.clearfix {:aria-label "Shop Products"}
      [:div.col.col-6
       [:a.inherit-color.block.py1.light.titleize
        (assoc (utils/route-to events/navigate-adventure-match-stylist)
               :data-test "freeinstall-footer-link")
        [:span.p-color "NEW "]
        "Mayvenn Install"]
       (map dtc-link column-1-links)]
      (for [link-column (partition-all partition-count rest-of-links)]
        [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :slug))}
         (map dtc-link link-column)])]]))

(defcomponent dtc-full-component
  [{:keys [contacts categories essence-copy]} owner opts]
  [:div.h5.border-top.border-gray.bg-cool-gray
   [:div.container
    [:div.col-12.clearfix
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (component/build dtc-shop-section {:categories      categories
                                                  :partition-count 5})]
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (component/build contacts-section contacts)]
     [:div.col-on-tb-dt.col-4-on-tb-dt.px3.my2
      ^:inline (component/build social-section)]
     (when essence-copy
       [:div.col-on-tb-dt.col-4-on-tb-dt.px4.pt3.pb2.h7.center.line-height-4.underline
        essence-copy])]]

   [:div.mt3.bg-black.white.py1.px3.clearfix.h8
    [:div
     {:style {:margin-bottom "90px"}}
     (component/build footer-links/component {:minimal? false} nil)]]])

(defn built-component
  [data opts]
  (let [nav-event (get-in data keypaths/navigation-event)]
    (cond
      (nav/show-minimal-footer? nav-event)
      (footer-minimal/built-component data nil)

      (= (get-in data keypaths/store-slug) "shop")
      (component/build dtc-full-component (query data) {:key "dtc-full-footer"})

      :else
      (component/build full-component (query data) nil))))
