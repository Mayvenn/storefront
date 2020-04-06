(ns storefront.components.footer
  (:require [catalog.categories :as categories]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]))

(defn phone-uri [tel-num]
  (apply str "tel://+" (numbers/digits-only tel-num)))

(defn ^:private category->link
  [{:keys        [page/slug] :as category
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
    {:title       (:footer/title category)
     :id          slug
     :new-link?   (categories/new-category? slug)
     :nav-message nav-message}))

(defn shop-section [{:keys [link-columns]}]
  (component/html
   [:div
    [:div.content-3.proxima.shout.bold.mb2 "Shop"]
    [:nav.black.clearfix {:aria-label "Shop Products"}
     (for [link-column link-columns]
       [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :id))}
        (for [{:keys [title new-link? nav-message id]} link-column
              :let [data-test (str "footer-link-" id)]]
          [:a.inherit-color.block.py1.light.titleize
           (merge {:key       data-test
                   :data-test data-test}
                  (apply utils/route-to nav-message))
           (when new-link?
             [:span.p-color "NEW "])
           title])])]]))

(defcomponent contacts-section [{:keys [call-number sms-number contact-email]} _ _]
  [:div
   [:div.content-3.proxima.shout.bold.mb2 "Contact us"]
   [:div.flex.items-center
    [:span.py1
     [:span.hide-on-tb-dt (ui/link :link/phone :a.inherit-color {} call-number)]
     [:span.hide-on-mb call-number]]
    [:span.content-1.proxima..bold.mx3 "|"]
    (ui/link :link/email :a.block.py1.inherit-color {} contact-email)]])

(defcomponent full-component
  [{:keys [link-columns contacts essence-copy]} owner opts]
  [:div.bg-cool-gray
   [:div.bg-p-color.pt1]
   [:div.container
    [:div.col-12.clearfix.px3
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (shop-section {:link-columns link-columns})]
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build contacts-section contacts)]
     (when essence-copy
       [:div.col-on-tb-dt.col-6-on-tb-dt.pb2.content-4.dark-gray
        essence-copy])]]

   [:div.mt3
    (component/build footer-links/component {:minimal? false} nil)]])

(defn contacts-query
  [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   config/support-phone-number
   :contact-email "help@mayvenn.com"})

(defn dtc-link [{:keys [title new-link? nav-message id]}]
  (component/html
   [:a.inherit-color.block.py2.light.titleize.pointer
    ^:attrs (merge {:data-test (str id "-footer-link")
                    :key       (str id "-footer-link")}
                   (apply utils/route-to nav-message))
    (when new-link?
      [:span.p-color "NEW "])
    (str title)]))

(defcomponent dtc-shop-section [{:keys [link-columns]} _ _]
  [:div.col-12
   [:div.content-3.proxima.shout.bold.mb2 "Shop"]
   [:nav.black.clearfix {:aria-label "Shop Products"}
    (for [link-column link-columns]
      [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :id))}
       (for [link link-column]
         ^:inline (dtc-link link))])]])

(defcomponent dtc-full-component
  [{:keys [contacts link-columns essence-copy]} owner opts]
  [:div.bg-cool-gray
   [:div.bg-p-color.pt1]
   [:div.container
    [:div.col-12.clearfix.px3
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build dtc-shop-section {:link-columns link-columns})]
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build contacts-section contacts)]
     (when essence-copy
       [:div.col-on-tb-dt.col-6-on-tb-dt.pb2.content-4.dark-gray
        essence-copy])]]

   [:div.hide-on-dt {:style {:margin-bottom "90px"}}
    (component/build footer-links/component {:minimal? false} nil)]
   [:div.hide-on-mb-tb
    (component/build footer-links/component {:minimal? false} nil)]])

(defn ^:private split-evenly
  [coll]
  (when coll
    (let [coll-length    (count coll)
          half-way-point (int (Math/ceil (float (/ coll-length 2))))]
      (split-at half-way-point coll))))

(defn query
  [data]
  (let [shop?             (= (get-in data keypaths/store-slug) "shop")
        classic?          (= "mayvenn-classic" (get-in data keypaths/store-experience))
        sort-key          :footer/order
        sorted-categories (->> (get-in data keypaths/categories)
                               (into []
                                     (comp (filter sort-key)
                                           (filter (partial auth/permitted-category? data))))
                               (sort-by sort-key))

        non-category-links [(when shop?
                              {:title       "Mayvenn Install"
                               :id          "freeinstall"
                               :new-link?   false
                               :nav-message [events/navigate-adventure-match-stylist]})
                            {:title       "Shop By Look"
                             :id          "shop-by-look"
                             :new-link?   false
                             :nav-message [events/navigate-shop-by-look {:album-keyword :look}]}
                            (when (not classic?)
                              {:title       "Shop Bundle Sets"
                               :id          "shop-bundle-sets"
                               :new-link?   false
                               :nav-message [events/navigate-shop-by-look {:album-keyword :all-bundle-sets}]})]
        links              (->> sorted-categories
                                (mapv (partial category->link))
                                (concat non-category-links)
                                (remove nil?))]
    {:contacts     (contacts-query data)
     :link-columns (split-evenly links)
     :essence-copy (str "Included is a one year subscription to ESSENCE Magazine - a $10 value! "
                        "Offer and refund details will be included with your confirmation.")}))

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
