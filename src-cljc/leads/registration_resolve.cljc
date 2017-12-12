(ns leads.registration-resolve
  (:require #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.component :as component]])
            [storefront.platform.component-utils :as utils]
            [leads.header :as header]
            [cemerick.url :as url]
            [clojure.string :as string]
            [storefront.components.ui :as ui]
            [storefront.keypaths]
            [leads.keypaths]
            [storefront.config :as config]
            [storefront.components.footer :as footer]
            [storefront.components.stylist.share-your-store :as share-your-store]))

(def congratulations-section
  [:section
   [:picture
    [:source {:media "(max-width: 749px)" :srcSet "//ucarecdn.com/f06961aa-d039-4ff3-a8ec-05e9d5e1024d/-/format/auto/herocongrats.jpg 2x"}]
    [:source {:srcSet "//ucarecdn.com/cc2d502b-3402-439d-8ff7-63c2b77b7751/-/format/auto/herocongratsdsk.jpg"}]
    [:img.block.col-12 {:src "//ucarecdn.com/f06961aa-d039-4ff3-a8ec-05e9d5e1024d/-/format/auto/herocongrats.jpg"
                        :alt "Congrats. Welcome to the Mayvenn community! Your website is ready to go. You can share your store link, shop, and start selling now."}]]])

(defn- first-coupon-link [store-link token user-id target]
  (str "//" store-link "/one-time-login?sha=FIRST&token=" token "&user-id=" user-id "&target=" (url/url-encode target)))

(defn coupon-section [store-link token user-id]
  [:section.center.white.bg-cover.bg-center.bg-30-off.center.px3.py6
   [:div.max-580.mx-auto
    [:h2.h1.mt6 "Get 30% Off"]
    [:p.mb3 "To welcome you to Mayvenn, here is 30% off your first order. Use the promo code: FIRST"]
    [:a.btn.btn-primary.h5.px4.light
     {:href   (first-coupon-link store-link token user-id "/")
      :target "_blank"
      :data-test "shop-first-promo"}
     "Shop now using promo code FIRST"]]])

(defn whats-next-section [store-link token user-id]
  (let [cell :div.my4.col-on-tb-dt.col-4-on-tb-dt.px2-on-tb-dt
        icon (fn [path] [:img.m1 {:src path :height "75px"}])
        hed  :h2.h3
        dek  :p.h6
        cta  :a.my2.block.col-12.btn.btn-outline.h5]
    [:section.center.bg-teal.white.px3.pt6.pb2
     [:div.max-580.mx-auto
      [:h2 "What Next?"]
      [:p "Good question. Here are three simple (and important!) things you can do right now to get started."]]
     [:div.mx-auto {:style {:max-width "960px"}}
      [:div.clearfix.mxn2-on-tb-dt
       [cell
        (icon "//ucarecdn.com/7f4e00a1-cac3-4c79-b233-176c8809be03/-/format/auto/icontrymayvenn.png")
        [hed "Try Mayvenn Hair"]
        [dek "Becoming an expert in our products is the first step to becoming a successful seller. Use the promo code 'FIRST' to get 30% off your first order."]
        [cta {:href   (first-coupon-link store-link token user-id "/")
              :target "_blank"}
         "Shop Mayvenn hair"]]

       [cell
        (icon "//ucarecdn.com/66a2b9d9-371e-4e3d-a9af-49386e16cda8/-/format/auto/iconmeetstylists.png")
        [hed "Meet Fellow Stylists"]
        [dek "We have a vibrant community of over 60,000 stylists constantly sharing and learning from each other. Join the conversation by introducing yourself!"]
        [cta {:href   "https://community.mayvenn.com/mayvenn-general/f/all-about-you/1724/introduce-yourself-and-your-specialty"
              :target "_blank"}
         "Go to the Mayvenn stylist community"]]

       [cell
        (icon "//ucarecdn.com/024b5a65-c6e0-43f6-972b-0dd684d28e19/-/format/auto/iconpromote.png")
        [hed "Promote Your Store"]
        [dek "Don’t be shy! You now have access to the highest quality hair products in the industry. Shipping is always free and all Mayvenn products are backed by a 30-day guarantee."]
        [cta (utils/scroll-href "share-store-section") "Share your store link"]]]]]))

(defn stylist-kit-section [store-link token user-id]
  [:section.center
   [:div.bg-stylist-kit.bg-center.bg-cover.relative
    {:style {:height "480px"}}
    [:div.absolute.bottom-0.left-0.right-0.px3.py6.center
     [:div.max-580.mx-auto
      [:h2 "Your secret weapon"]
      [:p.mb3 "Our stylist kit is full of essential selling tools like business cards, hair samples, and more. "
       "For just $109, this is the best way to jumpstart your Mayvenn business (a $200 value)."]]
     [:a.h5.block.col-12.col-6-on-tb.col-4-on-dt.mx-auto.regular.btn.btn-primary.white
      {:href (first-coupon-link store-link token user-id "/products/49-rings-kits")
       :target "_blank"
       :data-test "shop-sample-kits"}
      "Learn more about stylist kits"]]]])

(def first-sale-section
  [:section.center
   [:div.bg-first-sale.bg-center.bg-cover.relative
    {:style {:height "480px"}}
    [:div.absolute.bottom-0.left-0.right-0.px3.py6.center.white
     [:div.max-580.mx-auto
      [:h2 "How to make your first sale"]
      [:p.mb3 "We understand how intimidating selling can be, but you’re not alone. "
       "Learn how other Mayvenn stylists used practical tips to make their first sale."]]
     [:a.h5.block.col-12.col-6-on-tb.col-4-on-dt.mx-auto.regular.btn.btn-primary.white
      {:href "https://community.mayvenn.com/mayvenn-general/w/mayvenn-general/4/day-1"
       :target "_blank"}
      "See how stylists are making sales"]]]])

(def q-and-as
  [{:q "Are there any samples of the hair?"
    :a "Yes! We encourage you to sample the hair before you buy or recommend it to your clients. We have stylist kits available for $109. The kit itself not only comes with sample hair, but also a poster, appointment cards, window stickers and more to get your business started."}
   {:q "How do I get my first sale?"
    :a "There are a few different ways you can get your first sale and we’re here to help you get it. Our stylist community is your number one resource for getting tips on how to approach your clients, reach out to your networks, and spread the word about your store."}
   {:q "Where can I find marketing tips?"
    :a [:span
        "Check out our stylist community! There are tons of videos, scripts, ideas and photos to help boost your Mayvenn business. You can visit the "
        [:a.white {:href "https://community.mayvenn.com" :target "_blank"}
         "community at stylist.mayvenn.com"]
        ". For hair tips, tricks, and trends, check out our blog, "
        [:a.white {:href "https://blog.mayvenn.com" :target "_blank"}
         "Real Beautiful, at blog.mayvenn.com"]
        ". You can also follow us "
        [:a.white {:href "https://www.instagram.com/mayvennmarketing/" :target "_blank"}
         "@mayvennmarketing on Instagram"]
        " for exclusive marketing photos."]}
   {:q "How and when do I get paid?"
    :a "Once you’ve made a sale, you will be paid 15% commission via your preferred payment method every Wednesday. We offer direct deposit through Venmo and Paypal, or you can opt to be paid by check."}
   {:q "How does the sales bonus work?"
    :a "Our sales bonus is one of the best perks about being a Mayvenn stylist! You’ll get $100 in Mayvenn hair credit for every $600 in sales you make or if you refer a stylist that makes $100 in sales. Use that hair credit to offer special bundle deals to your clients or treat yourself to new bundles."}
   {:q "How does the 30-day quality guarantee work?"
    :a "A common frustration that many have experienced when purchasing hair extensions is the inability to return the hair when they’re dissatisfied. Our 30-day guarantee is an unprecedented move in the industry, and it shows how confident we are in our product. If you’re having any issues with your bundles, even after you’ve dyed, cut it, or styled it, we’ll exchange it within 30 days. If you haven’t altered the hair or packaging in any way, we’ll give you a full refund within 30 days."}])

(defn faq-section [q-and-as {:keys [sms-number call-number]}]
  [:div.max-580.mx-auto
   [:h2 "Frequently asked questions"]
   [:p.mb6 "We’re always here to help! Answers to our most frequently asked questions can be found below."]
   (for [{:keys [q a]} q-and-as]
     [:div.mb4
      {:key (str (hash q))}
      [:h3 "Q: " q]
      [:p.h5 "A: " a]])
   [:div.mt6
    [:p.mb4 "If you still have questions about becoming a Mayvenn stylist, feel free to contact us! Our customer service representatives are ready to answer all of your questions. There are a few ways you can reach us:"]
    [:ul.list-reset
     [:li "Text us: " (ui/link :link/sms :a.inherit-color {} sms-number)]
     [:li "Call us: " (ui/link :link/phone :a.inherit-color {} "+" call-number)]
     [:li "Email us: " (ui/link :link/email :a.inherit-color {} "help@mayvenn.com")]
     [:li "Tweet us or DM us: " [:a.inherit-color {:href "https://twitter.com/MayvennHair" :target "_blank"} "@mayvennhair"]]]]])

(defn query [app-state]
  (let [host       (case (get-in app-state storefront.keypaths/environment)
                     "production" "mayvenn.com"
                     "acceptance" "diva-acceptance.com"
                     "storefront.localhost")
        store-slug (get-in app-state leads.keypaths/stylist-slug)]
    {:store-link       (str store-slug "." host)
     :token            (get-in app-state leads.keypaths/remote-user-token)
     :user-id          (get-in app-state leads.keypaths/remote-user-id)
     :share-your-store {:host         host
                        :store-slug   store-slug
                        :utm-campaign "resolve"}
     :faq              {:sms-number  config/mayvenn-leads-sms-number
                        :call-number config/mayvenn-leads-call-number}}))

(defn ^:private component
  [{:keys [store-link token user-id share-your-store faq]} owner opts]
  (component/create
   [:div
    (header/built-component {} nil)
    [:div
     congratulations-section
     (coupon-section store-link token user-id)
     (component/build share-your-store/component share-your-store nil)
     (whats-next-section store-link token user-id)
     (stylist-kit-section store-link token user-id)
     first-sale-section
     [:section.center.px3.py6.bg-teal.white
      (faq-section q-and-as faq)]]
    (component/build footer/minimal-component {} nil)]))


(defn built-component [data opts]
  (component/build component (query data) opts))
