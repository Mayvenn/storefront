(ns storefront.components.new-home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.accessors.taxons :as taxons]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.assets :as assets]))

(defn category [{:keys [slug name long-name model-image product-image]}]
  [:a.p1.center.flex.flex-column.items-center
   (merge {:key slug
           :data-test (str "taxon-" slug)}
          (utils/route-to events/navigate-category {:taxon-slug slug}))
   [:img.my1 {:src   model-image
              :alt   (str "A stylish model wearing " long-name)
              :style {:height "128px"}}]
   [:img.my1 {:src   product-image
              :alt   (str "A close-up of " long-name)
              :style {:width "64px"}}]
   [:div.my1.dark-black.medium.f3 name]])

(defn pick-style [taxons]
  [:div.center.py3
   [:h2.h1.dark-black.bold.py1 "pick your style"]
   [:div.dark-gray.medium.py1 "100% virgin human hair + free shipping"]
   [:div.my1.flex.flex-wrap.items-center.justify-around
    (for [taxon taxons]
      (category taxon))]
   [:div.col-6.md-up-col-4.mx-auto
    ;; button color should be white/transparent
    (ui/silver-outline-button
     (utils/route-to events/navigate-categories)
     [:span.dark-black.bold "shop now"])]])

(def banner
  (component/html
   [:a
    (utils/route-to events/navigate-categories)
    [:img.col-12 {:src (assets/path "/images/homepage/mobile_banner.jpg")
                  :alt "shop now"}]]))

(def about-mayvenn
  (component/html
   [:div.dark-gray.py3
    [:h2.h1.center.dark-black.bold.py1 "why people love Mayvenn hair"]

    [:div.mx3.p3.border-bottom.border-light-silver
     [:h3.h2.center.bold.py3 "stylist recommended"]
     [:p.line-height-5
      "Mayvenn hair is the #1 recommended hair company by over 60,000 hair stylists across the country, making it the most trusted hair brand on the market."]]

    [:div.mx3.p3.border-bottom.border-light-silver
     [:h3.h2.center.bold.py3 "30 day guarantee"]
     [:p.line-height-5
      "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange it within 30 days for FREE!"]]

    [:div.mx3.p3
     [:h3.h2.center.bold.py3 "fast free shipping"]
     [:p.line-height-5
      "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]]

    [:div.col-6.md-up-col-4.mx-auto
     (ui/green-button
      (utils/route-to events/navigate-categories)
      "shop now")]]))

(defn component [{:keys [taxons]} owner opts]
  (component/create
   [:div.m-auto
    banner
    (pick-style taxons)
    about-mayvenn]))

(defn query [data]
  {:taxons (remove taxons/is-stylist-product? (taxons/current-taxons data))})

(defn built-component [data opts]
  (component/build component (query data) opts))
