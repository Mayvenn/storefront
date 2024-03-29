(ns storefront.components.footer
  (:require [api.catalog :refer [select ?discountable]]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.sites :as sites]
            [storefront.component :as component :refer [defcomponent]]
            storefront.components.footer-v2022-11
            [storefront.components.footer-links :as footer-links]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]
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
     :nav-message nav-message}))

(defn shop-section [{:keys [link-columns]}]
  (component/html
   [:div
    [:div.content-3.proxima.shout.bold.mb2 "Shop"]
    [:nav.black.clearfix {:aria-label "Shop Products"}
     (for [link-column link-columns]
       [:div.col.col-6 {:key (str "footer-column-" (-> link-column first :id))}
        (for [{:keys [title nav-message id]} link-column
              :let [data-test (str "footer-link-" id)]]
          [:a.inherit-color.block.py1.light.titleize
           (merge {:key       data-test
                   :data-test data-test}
                  (apply utils/route-to nav-message))
           title])])]]))

(defcomponent contacts-section [{:keys [call-number sms-number contact-email business-hours]} _ _]
  [:div
   [:div.content-3.proxima.shout.bold.mb2 "Contact us"]
   [:div.flex.items-center
    [:span.py1
     [:span.hide-on-tb-dt (ui/link :link/phone :a.inherit-color {} call-number)]
     [:span.hide-on-mb call-number]]
    [:span.content-1.proxima..bold.mx3 "|"]
    (ui/link :link/email :a.block.py1.inherit-color {} contact-email)]
   [:div.py1
    business-hours]])

(defn- social-link
  ([uri icon] (social-link {:height "20px" :width "20px"} uri icon))
  ([{:keys [height width]} uri icon]
   (component/html
    ;; https://web.dev/external-anchors-use-rel-noopener/
    [:a.block.px1.mx1.flex.items-center {:href uri :rel "noopener" :target "_blank"}
     [:div {:style {:width width :height height}}
      ^:inline icon]])))

(defn social-links
  []
  (component/html
   [:div.flex.items-center.py3
    ^:inline (social-link {:height "28px" :width "28px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter))
    ^:inline (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram))
    ^:inline (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook))
    ^:inline (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest))]))

(defcomponent full-component
  [{:keys [link-columns contacts footer-links]} owner opts]
  [:div.bg-cool-gray
   [:div.bg-p-color.pt1]
   [:div.container
    [:div.col-12.clearfix.px3
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (shop-section {:link-columns link-columns})]
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build contacts-section contacts)]
     [:div.col-on-tb-dt.col-6-on-tb-dt
      (social-links)]]]

   [:div.mt3
    (footer-links/built-component footer-links opts)]])

(defn contacts-query
  [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :call-number   config/support-phone-number
   :business-hours "Monday - Friday, 11am - 8pm ET"
   :contact-email "help@mayvenn.com"})

(defn dtc-link [{:keys [title nav-message nav-href id]}]
  (component/html
   [:a.inherit-color.block.py2.light.pointer
    ^:attrs (merge {:data-test (str id "-footer-link")
                    :key       (str id "-footer-link")}
                   (when nav-message (apply utils/route-to nav-message))
                   (when nav-href {:href nav-href}))
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
  [{:keys [contacts link-columns footer-links]} _ opts]
  [:div.bg-cool-gray
   [:div.bg-p-color.pt1]
   [:div.container
    [:div.col-12.clearfix.px3
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build dtc-shop-section {:link-columns link-columns})]
     [:div.col-on-tb-dt.col-6-on-tb-dt.mt6
      (component/build contacts-section contacts)]
     [:div.col-on-tb-dt.col-6-on-tb-dt
      (social-links)]]]

   [:div.hide-on-dt
    (footer-links/built-component footer-links opts)]
   [:div.hide-on-mb-tb
    (footer-links/built-component footer-links opts)]])

(defn ^:private split-evenly
  [coll]
  (when coll
    (let [coll-length    (count coll)
          half-way-point (int (Math/ceil (float (/ coll-length 2))))]
      (split-at half-way-point coll))))

;; We filter out ICP (instead of displaying it with other categories)
(defn not-services-icp?
  [{:catalog/keys [category-id]}]
  (not (#{"30"} category-id )))

(defn query
  [data]
  (let [shop?                (or (= "shop" (get-in data keypaths/store-slug))
                               (= "retail-location" (get-in data keypaths/store-experience)))
        classic?             (#{"mayvenn-classic" "classic2.1"} (get-in data keypaths/store-experience))
        remove-free-install? (:remove-free-install (get-in data keypaths/features))
        sort-key             :footer/order
        categories           (->> (get-in data keypaths/categories)
                                (into []
                                      (comp (filter not-services-icp?)
                                            (filter sort-key)
                                            (filter (partial auth/permitted-category? data)))))
        non-category-links   (when (not classic?)
                               [{:title       "Shop By Look"
                                 :sort-order  3
                                 :id          "shop-by-look"
                                 :nav-message [events/navigate-shop-by-look {:album-keyword :look}]}])
        links                (->> categories
                                (mapv (partial category->link))
                                (concat non-category-links)
                                (remove nil?)
                                (sort-by :sort-order))]
    {:contacts     (contacts-query data)
     :link-columns (split-evenly links)
     :footer-links {:minimal-footer?                (nav/show-minimal-footer? (get-in data keypaths/navigation-event))
                    :footer-email-input-value       (get-in data keypaths/footer-email-value)
                    :footer-email-submitted?        (get-in data keypaths/footer-email-submitted)
                    :footer-ready-for-email-signup? (get-in data keypaths/footer-email-ready)
                    :footer-email-capture?          (experiments/footer-email-capture? data)}}))

(defn built-component
  [data _]
  (let [nav-event  (get-in data keypaths/navigation-event)]
    (cond
      (nav/hide-footer? nav-event)
      nil

      (nav/show-minimal-footer? nav-event)
      (footer-minimal/built-component data nil)

      (experiments/footer-v22? data)
      (component/build storefront.components.footer-v2022-11/component
                       (storefront.components.footer-v2022-11/query data) nil)

      (= :shop (sites/determine-site data))
      (component/build dtc-full-component (query data) {:key "dtc-full-footer"})

      :else
      (component/build full-component (query data) nil))))
