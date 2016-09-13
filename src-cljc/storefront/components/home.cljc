(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.video :as video]
            [storefront.accessors.taxons :as taxons]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.assets :as assets]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.platform.carousel-two :as carousel]))

(defn homepage-images
  "Adds image effect where images are always full width. On mobile, the image
  height grows as the screen grows. On desktop, the image height is fixed. The
  image is centered and the edges are clipped at narrower screens, then are
  gradually exposed on wider screens. All meaningful content has to fit within
  the center 640px of the desktop image.

  mobile-asset can be any height but must be 640px wide
  desktop-asset should be 408px high and at least 1024px wide"
  [mobile-asset desktop-asset alt-text]
  [:div
   [:div.to-md-hide.bg-center.bg-no-repeat
    {:style {:height "408px"
             :background-image (assets/css-url desktop-asset)}
     :title alt-text}]
   [:img.md-up-hide.col-12 {:src mobile-asset
                            :alt alt-text}]])

(defn category [{:keys [slug name long-name model-image product-image]}]
  [:a.p1.center.flex.flex-column.items-center
   (merge {:data-test (str "taxon-" slug)}
          (utils/route-to events/navigate-category {:taxon-slug slug}))
   [:img.unselectable {:src   model-image
                       :alt   ""
                       :style {:height "128px"}}]
   [:img.mt3.unselectable {:src   product-image
                           :alt   (str "Shop for " long-name)
                           :style {:height "80px"}}]
   [:div.mb3.dark-black.medium.f3 name]])

(defn pick-style [taxons]
  [:div.center.py3
   [:h2.h1.dark-black.bold.py1 "pick your style"]
   [:div.dark-gray.medium.py1 "100% virgin human hair + free shipping"]
   [:div.my2
    (component/build carousel/component
                     {:slides   (map category taxons)
                      :settings (let [slide-count (count taxons)
                                      swipe       (fn [n]
                                                    {:swipe        true
                                                     :slidesToShow n
                                                     :autoplay     true
                                                     :arrows       true})
                                      show-all    {:swipe        false
                                                   :slidesToShow slide-count
                                                   :autoplay     false
                                                   :arrows       false}]
                                  ;; The breakpoints are mobile-last. That is, the
                                  ;; default values apply to the largest screens, and
                                  ;; 768 means 768 and below.
                                  (merge
                                   (if (<= slide-count 7)
                                     show-all
                                     (swipe 7))
                                   {:responsive [{:breakpoint 1024
                                                  :settings   (swipe 5)}
                                                 {:breakpoint 768
                                                  :settings   (swipe 3)}
                                                 {:breakpoint 640
                                                  :settings   (swipe 2)}]}))}
                     nil)]
   [:div.col-6.md-up-col-4.mx-auto
    ;; button color should be white/transparent
    (ui/silver-outline-button
     (utils/route-to events/navigate-categories)
     [:span.dark-black.bold "shop now"])]])

(defn banner [store-slug]
  (component/html
   [:a
    (assoc (utils/route-to events/navigate-categories)
           :data-test "home-banner")
    (case store-slug
      "peakmill" (homepage-images (assets/path "/images/homepage/peak/mobile_banner.jpg")
                                  (assets/path "/images/homepage/peak/desktop_banner.jpg")
                                  "shop now")
      "lovelymimi" (homepage-images (assets/path "/images/homepage/mimi/mobile_banner.jpg")
                                  (assets/path "/images/homepage/mimi/desktop_banner.jpg")
                                  "shop now")
      (homepage-images (assets/path "/images/homepage/mobile_banner.jpg")
                       (assets/path "/images/homepage/desktop_banner.jpg")
                       "shop now"))]))

(def about-mayvenn
  (component/html
   [:div.dark-gray.py3
    [:h2.h1.center.dark-black.bold.py1 "why people love Mayvenn hair"]

    [:div.mx3.md-flex.f4
     [:div.py4
      [:div.px3
       [:h3.f2.center.bold.mb3 "stylist recommended"]
       [:p.line-height-5
        "Mayvenn hair is the #1 recommended hair company by over 60,000 hair stylists across the country, making it the most trusted hair brand on the market."]]]

     [:div.md-up-hide.border-bottom.border-light-silver]
     [:div.py4
      [:div.to-md-hide.left.border-left.border-light-silver {:style {:height "100%"}}]
      [:div.px3
       [:h3.f2.center.bold.mb3 "30 day guarantee"]
       [:p.line-height-5
        "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange it within 30 days for FREE!"]]]

     [:div.md-up-hide.border-bottom.border-light-silver]
     [:div.py4
      [:div.to-md-hide.left.border-left.border-light-silver {:style {:height "100%"}}]
      [:div.px3
       [:h3.f2.center.bold.mb3 "fast free shipping"]
       [:p.line-height-5
        "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]]]]

    [:div.col-6.md-up-col-4.mx-auto
     (ui/green-button
      (utils/route-to events/navigate-categories)
      "shop now")]]))

(def video-popup
  (component/html
   [:div.relative {:on-click #(handle-message events/control-play-video {:video :home})}
    (homepage-images
     (assets/path "/images/homepage/mobile_video.png")
     (assets/path "/images/homepage/desktop_video.png")
     "Watch a video about what real customers have to say about Mayvenn")
    [:div.absolute.overlay.bg-darken-2
     [:div.flex.flex-column.items-center.justify-center.white.bold.bg-darken-2.center.shadow.letter-spacing-1 {:style {:height "100%"}}
      [:div.mt4 svg/play-video]
      [:div.h0.my2 "Mayvenn in action"]
      [:div.h2 "see what real customers say"]]]]))

(defn component [{:keys [taxons store-slug]} owner opts]
  (component/create
   [:div.m-auto
    (banner store-slug)
    (pick-style taxons)
    video-popup
    about-mayvenn]))

(defn query [data]
  {:taxons (remove taxons/is-stylist-product? (taxons/current-taxons data))
   :store-slug (get-in data keypaths/store-slug)})

(defn built-component [data opts]
  (component/build component (query data) opts))
