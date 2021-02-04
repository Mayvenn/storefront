(ns storefront.components.footer
  (:require catalog.services
            promotion-helper.ui
            [storefront.accessors.auth :as auth]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            storefront.utils
            [storefront.platform.numbers :as numbers]
            [storefront.accessors.experiments :as experiments]
            [api.orders :as api.orders]))

(defn phone-uri [tel-num]
  (apply str "tel://+" (numbers/digits-only tel-num)))

(defn ^:private category->link
  [{:keys        [page/slug category/new?] :as category
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
     :sort-order  (:footer/order category)
     :id          slug
     :new-link?   new?
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
   [:a.inherit-color.block.py2.light.pointer
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
  [{:keys [additional-margin contacts link-columns essence-copy]} owner opts]
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

   [:div.hide-on-dt
    (when additional-margin
      {:style {:margin-bottom additional-margin}})
    (component/build footer-links/component {:minimal? false} nil)]
   [:div.hide-on-mb-tb
    (component/build footer-links/component {:minimal? false} nil)]])

(defn ^:private split-evenly
  [coll]
  (when coll
    (let [coll-length    (count coll)
          half-way-point (int (Math/ceil (float (/ coll-length 2))))]
      (split-at half-way-point coll))))

;; We filter out ICP (instead of displaying it with other categories)
;; and add it later by hand because
;; it needs to be displayed with "NEW" sometimes.
(defn not-services-icp?
  [{:catalog/keys [category-id]}]
  (not (#{"30"} category-id )))

(defn query
  [data]
  (let [shop?                (= (get-in data keypaths/store-slug) "shop")
        classic?             (= "mayvenn-classic" (get-in data keypaths/store-experience))
        homepage-revert?     (experiments/homepage-revert? data)
        sort-key             :footer/order
        categories           (->> (get-in data keypaths/categories)
                                (into []
                                      (comp (filter not-services-icp?)
                                            (filter sort-key)
                                            (filter (partial auth/permitted-category? data)))))
        service-icp-category (->> (get-in data keypaths/categories)
                                  (remove not-services-icp?)
                                  first)
        non-category-links   (concat (when shop?
                                     (if homepage-revert?
                                       [{:title       "Find a Stylist"
                                         :sort-order  1
                                         :id          "find-a-stylist"
                                         :new-link?   false
                                         :nav-message [events/navigate-adventure-find-your-stylist]}
                                        {:title       "Browse Services"
                                         :sort-order  2
                                         :id          "browse-services"
                                         :new-link?   true
                                         :nav-message [events/navigate-category service-icp-category]}]
                                       [{:title       "Get a Mayvenn Install"
                                         :sort-order  1
                                         :id          "find-a-stylist"
                                         :new-link?   true
                                         :nav-message [events/navigate-adventure-find-your-stylist]}
                                        {:title       "Browse Services"
                                         :sort-order  2
                                         :id          "browse-services"
                                         :new-link?   false
                                         :nav-message [events/navigate-category service-icp-category]}]))
                                   (when (not classic?)
                                     [{:title       "Shop By Look"
                                       :sort-order  3
                                       :id          "shop-by-look"
                                       :new-link?   false
                                       :nav-message [events/navigate-shop-by-look {:album-keyword :look}]}
                                      {:title       "Shop Bundle Sets"
                                       :sort-order  4
                                       :id          "shop-bundle-sets"
                                       :new-link?   false
                                       :nav-message [events/navigate-shop-by-look {:album-keyword :all-bundle-sets}]}]))
        links                (->> categories
                                (mapv (partial category->link))
                                (concat non-category-links)
                                (remove nil?)
                                (sort-by :sort-order))]
    {:contacts          (contacts-query data)
     :link-columns      (split-evenly links)
     ;; NOTE: necessary only when promo helper exists. Can remove if it goes away.
     :additional-margin (when (:promotion-helper/exists?
                               (promotion-helper.ui/promotion-helper-model<-
                                data
                                (->> (api.orders/current data)
                                     :order/items
                                     (storefront.utils/select catalog.services/discountable)
                                     first)))
                          "79px")
     :essence-copy      (str "All orders include a one year subscription to ESSENCE Magazine - a $10 value! "
                             "Offer and refund details will be included with your confirmation.")}))

(defn built-component
  [data _]
  (let [nav-event (get-in data keypaths/navigation-event)]
    [:div
     (cond
       (nav/hide-footer? nav-event)
       nil

       (nav/show-minimal-footer? nav-event)
       (footer-minimal/built-component data nil)

       (= :shop (sites/determine-site data))
       (component/build dtc-full-component (query data) {:key "dtc-full-footer"})

       :else
       (component/build full-component (query data) nil))
     (promotion-helper.ui/promotion-helper data)]))
