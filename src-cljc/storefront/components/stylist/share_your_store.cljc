(ns storefront.components.stylist.share-your-store
  #?@(:clj
      [(:require
        [cemerick.url :as url]
        [storefront.component-shim :as component]
        [storefront.components.share-links :as share-links]
        [storefront.keypaths :as keypaths]
        [storefront.platform.component-utils :as utils])]
      :cljs
      [(:require
        [cemerick.url :as url]
        [storefront.component :as component]
        [storefront.components.share-links :as share-links]
        [storefront.keypaths :as keypaths]
        [storefront.platform.component-utils :as utils])]))

(defn facebook-link [share-url]
  (share-links/facebook-link (share-links/with-utm-medium share-url "facebook")))

(defn twitter-link [share-url]
  (share-links/twitter-link (share-links/with-utm-medium share-url "twitter")
                            "My @MayvennHair store is open! Shop 100% virgin human hair with a 30-day quality guarantee:"))

(defn sms-link [share-url]
  ;; the ?& is to get this to work on iOS8 and Android at the same time
  (share-links/sms-link (str
                         "Hey! I’m now selling 100% virgin human hair through my Mayvenn hair store: "
                         (share-links/with-utm-medium share-url "sms") " "
                         "All orders ship out for free and are backed by a 30-day quality guarantee! "
                         "If you order 3 bundles or more you’ll get 25% off. "
                         "If you’re in the market for new hair, I hope you’ll consider supporting my business!")))

(defn constrain-to-720px
  "Limits width to 720px. Should not be nested inside any other width constraints"
  [& content]
  [:div.container
   (into [:div.col-9-on-dt.mx-auto] content)])

(defn constrain-copy-width
  "Limits width to 580px, which is the maximum for legible text. Should not be nested inside any other width constraints"
  [& content]
  (constrain-to-720px
   (into [:div.px2-on-tb-dt.col-10-on-tb-dt.mx-auto] content)))

(defn social-button [options img-url content]
  [:a.h5.block.col-12.regular.btn.btn-primary.white.my4.flex.justify-center.items-center
   options
   [:img.mr2 {:style {:height "18px"}
              :src   img-url}]
   content])

(defn component [{:keys [store-slug host utm-campaign]} _ _]
  (component/create
   (let [store-link        (str store-slug "." host)
         share-url         (-> (url/url (str "https://" store-link))
                               (assoc :query {:utm_campaign utm-campaign}))
         phone-image-width "336px"]
     [:div.center.px3.py6.container
      (constrain-copy-width
       [:h2 {:data-ref "share-store-section"} [:a {:name "share-store-section"} "Share your store"]]
       [:p "This unique store name is all yours. Use the buttons below for quick and easy sharing across your networks."]
       [:div.relative.mx-auto
        {:style {:width phone-image-width}}
        [:img.py6.mx-auto.block {:style {:width phone-image-width}
                                 :src   "//ucarecdn.com/da5ab6ed-d0ad-4063-a620-d933f11e0525/-/format/auto/storenameindevice.jpg"}]
        [:div.absolute.truncate
         {:style {:font-size   "14px"
                  :line-height "16px"
                  :top         "137px"
                  :left        "28px"
                  :right       "28px"}}
         [:span.bold store-slug] "." host]])
      [:h3.h5 "Share your store link"]
      [:div.col-12.col-6-on-tb.col-4-on-dt.mx-auto
       (social-button
        {:class  "bg-fb-blue"
         :target "_blank"
         :href   (facebook-link share-url)}
        "//ucarecdn.com/f9ab7fce-d25f-4854-b405-0e0c83684388/-/format/auto/fb.png"
        "Share on Facebook")
       (social-button
        {:class  "bg-twitter-blue"
         :target "_blank"
         :href   (twitter-link share-url)}
        "//ucarecdn.com/cb22b91e-5e62-47e2-a513-23435084a02b/-/format/auto/twitter.png"
        "Tweet your store link")
       (social-button
        {:class  "hide-on-dt bg-sms-green"
         :target "_blank"
         :href   (sms-link share-url)}
        "//ucarecdn.com/0bb20f76-52b6-4ad5-b094-a292352473b1/-/format/auto/sms.png"
        "Send a text")
       [:div.h5.my4 "Tap to select, copy and share"]
       [:input.border.border-gray.rounded.pl1.py1.bg-white.teal.col-12.center
        {:type     "text"
         :value    store-link
         :on-click utils/select-all-text
         :data-test "store-link"}]]])))

(defn query [data]
  {:host         (case (get-in data keypaths/environment)
                   "production" "mayvenn.com"
                   "acceptance" "diva-acceptance.com"
                   "storefront.localhost")
   :store-slug   (:store_slug (get-in data keypaths/store))
   :utm-campaign "stylist_dashboard"})

(defn built-component [data opts]
  (component/build component (query data) opts))
