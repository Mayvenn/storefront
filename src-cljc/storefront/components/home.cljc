(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.keypaths :as keypaths]
            [storefront.accessors.taxons :as taxons]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.components.new-home :as new-home]))

(defn color-option-home-grid [taxons]
  (let [rem #(str % "rem")
        height 10]
    (for [[index {:keys [slug]}] (map-indexed vector taxons)]
      [:a.col (merge {:key       slug
                      :data-test (str "taxon-" slug)
                      :class     "col-4"}
                     (utils/route-to events/navigate-category {:taxon-slug slug}))
       [:div.mp1.flex.items-center.justify-center.bg-cover.bg-no-repeat.bg-center
        {:class (str "img-homepage-" slug)
         :style (if (> index 4)
                  {:height (rem (float (- (/ height 2) 0.083333333)))}
                  {:height (rem height)})}
        ;; duplicate element to emulate old styles without custom special-cased classes
        [:div.bg-contain.bg-no-repeat.bg-center.col-10.md-up-hide
         {:class (str "img-text-" slug)
          :style {:height "1.33333rem"}}]
        [:div.bg-contain.bg-no-repeat.bg-center.col-10.to-md-hide
         {:class (str "img-text-" slug)
          :style {:height "25px"}}]]])))

(defn component [{:keys [taxons]} owner opts]
  (component/create
   [:div.home-container.m-auto.clearfix
    [:a (assoc (utils/route-to events/navigate-categories)
               :data-test "home-banner")
     [:div.to-lg-hide.img-lg-home-banner.bg-no-repeat.bg-full.bg-center.col-12.block.banner-container]
     [:div.lg-up-hide.img-md-home-banner.bg-no-repeat.bg-full.bg-center.col-12.block.banner-container]]

    [:div.border.border-width-2.my3.py2.center.medium.green.border-green
     "Free Shipping + 30 Day Money Back Guarantee"]

    [:div.col-12.lg-up-col-6 [:div.h3.center.black.mb1 "Pick your style"]]
    [:div.mb2.clearfix
     [:div.col.col-12.lg-up-col-6.clearfix
      (color-option-home-grid taxons)]
     [:div.col.col-6.to-lg-hide
      [:a.block.img-featured.col-12.bg-no-repeat.bg-center.bg-cover.mtp1
       (merge {:style {:height "300px"}}
              (utils/route-to events/navigate-categories))]
      [:p.bg-pink-gradient.col-12.white.italic.flex.items-center.justify-center.mtn1
       {:style {:height "29px"}}
       "Introducing Peruvian In All Textures"]]]]))

(defn query [data]
  {:taxons (remove taxons/is-stylist-product? (taxons/current-taxons data))})

(defn built-component [data opts]
  (if (experiments/new-homepage? data)
    (new-home/built-component data nil)
    (component/build component (query data) nil)))
