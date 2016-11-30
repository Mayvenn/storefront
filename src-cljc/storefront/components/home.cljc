(ns storefront.components.home
  (:require [storefront.platform.component-utils :as utils]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.platform.date :as date]
            [storefront.platform.video :as video]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.assets :as assets]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.platform.carousel :as carousel]))

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
   [:div.hide-on-mb.bg-center.bg-no-repeat
    {:style {:height "408px"
             :background-image (assets/css-url desktop-asset)}
     :title alt-text}]
   [:img.hide-on-tb-dt.col-12 {:src mobile-asset
                               :alt alt-text}]])

(defn link-to-search [{:keys [slug name long-name representative-images]}]
  (let [{:keys [model-circle product]} representative-images]
    [:a.p1.center.flex.flex-column.items-center
     (merge {:data-test (str "named-search-" slug)}
            (utils/route-to events/navigate-category {:named-search-slug slug}))
     [:img.unselectable (merge
                         (utils/img-attrs (:model-circle representative-images) :small)
                         {:style {:height "128px"}})]
     [:img.mt3.unselectable (merge
                             (utils/img-attrs (:product representative-images) :small)
                             {:style {:height "80px"}})]
     [:div.mb3.dark-gray.medium.f4 name]]))

(defn pick-style [named-searches]
  [:div.container.center.py3
   [:div.flex.flex-column
    [:h2.h5.order-2.gray.medium.py1 "100% virgin human hair + free shipping"]
    [:h3.h2.order-1.dark-gray.bold.py1 "pick your style"]]
   [:nav.my2
    {:role "navigation" :aria-label "Pick your style"}
    (component/build carousel/component
                     {:slides   (map link-to-search named-searches)
                      :settings (let [slide-count (count named-searches)
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
                                   {:responsive [{:breakpoint 1000
                                                  :settings   (swipe 5)}
                                                 {:breakpoint 750
                                                  :settings   (swipe 3)}
                                                 {:breakpoint 500
                                                  :settings   (swipe 2)}]}))}
                     nil)]
   [:div.col-6.col-4-on-tb-dt.mx-auto
    ;; button color should be light-silver/transparent
    (ui/large-ghost-button
     (utils/route-to events/navigate-categories)
     [:span.dark-gray.bold "shop now"])]])

(defn banner [store-slug]
  [:a
   (assoc (utils/route-to events/navigate-categories)
          :data-test "home-banner")
   (case store-slug
     "peakmill" (homepage-images (assets/path "/images/homepage/peak/mobile_banner.jpg")
                                 (assets/path "/images/homepage/peak/desktop_banner.jpg")
                                 "Get 15% Off Hair Extensions Mayvenn")
     "lovelymimi" (homepage-images (assets/path "/images/homepage/mimi/mobile_banner.jpg")
                                   (assets/path "/images/homepage/mimi/desktop_banner.jpg")
                                   "Get 15% Off Hair Extensions Mayvenn")
     "touchedbytokyo" (homepage-images (assets/path "/images/homepage/tokyo/mobile_banner.jpg")
                                       (assets/path "/images/homepage/tokyo/desktop_banner.jpg")
                                       "Get 15% Off Hair Extensions Mayvenn")
     (homepage-images (assets/path "/images/homepage/mobile_banner.jpg")
                      (assets/path "/images/homepage/desktop_banner.jpg")
                      "Get 15% Off Hair Extensions Mayvenn"))])

(def about-mayvenn
  (component/html
   [:div.container.gray.py3
    [:h2.line-length.mx-auto.center.dark-gray.bold.py1 "why people love Mayvenn hair"]

    [:div.clearfix.f5
     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.f3.center.bold.mb3 "stylist recommended"]
      [:p.line-height-5
       "Mayvenn hair is the #1 recommended hair company by over 60,000 hair stylists across the country, making it the most trusted hair brand on the market."]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.f3.center.bold.mb3 "30 day guarantee"]
      [:p.line-height-5
       "Try the best quality hair on the market risk free! Wear it, dye it, even cut it. If you’re not happy with your bundles, we will exchange it within 30 days for FREE!"]]

     [:div.col-on-tb-dt.col-4-on-tb-dt.p3
      [:h3.f3.center.bold.mb3 "fast free shipping"]
      [:p.line-height-5
       "Mayvenn offers free standard shipping on all orders, no minimum necessary. In a hurry? Expedited shipping options are available for those who just can’t wait."]]]

    [:div.col-6.col-4-on-tb-dt.mx-auto
     (ui/teal-button
      (utils/route-to events/navigate-categories)
      "shop now")]]))

(def video-popup
  (component/html
   [:div.relative {:on-click #(handle-message events/control-play-video {:video :home})}
    (homepage-images
     (assets/path "/images/homepage/mobile_video.png")
     (assets/path "/images/homepage/desktop_video.png")
     "Hair Extension Reviews Mayvenn")
    [:div.absolute.overlay.bg-darken-2
     [:div.flex.flex-column.items-center.justify-center.white.bold.bg-darken-2.center.shadow.letter-spacing-1.container-height
      [:div.mt4 svg/play-video]
      [:h2.h1.my2 "Mayvenn in action"]
      [:p.h3 "see what real customers say"]]]]))

(def talkable-banner
  (component/html
   [:div.container.py2
    [:a
     (utils/route-to events/navigate-friend-referrals)
     (homepage-images
      (assets/path "/images/homepage/mobile_talkable_banner.png")
      (assets/path "/images/homepage/desktop_talkable_banner.png")
      "refer friends, earn rewards, get 20% off")]]))

(defn component [{:keys [named-searches store-slug]} owner opts]
  (component/create
   [:div.m-auto
    [:section (banner store-slug)]
    [:section (pick-style named-searches)]
    [:section video-popup]
    [:section about-mayvenn]
    [:section talkable-banner]]))

(defn query [data]
  {:named-searches (remove named-searches/is-stylist-product? (named-searches/current-named-searches data))
   :store-slug     (get-in data keypaths/store-slug)})

(defn built-component [data opts]
  (component/build component (query data) opts))
