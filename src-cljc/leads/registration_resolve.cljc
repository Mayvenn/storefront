(ns leads.registration-resolve
  (:require #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.component :as component]])
            [leads.header :as header]
            [cemerick.url :as url]
            [clojure.string :as string]
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]))

(def digits (into #{} (map str (range 0 10))))

(defn phone-href [tel-num]
  (apply str "tel://+" (->> tel-num
                            (map str)
                            (filter digits))))

(defn initial-header [mayvenn-phone]
  [:div.center.px3.py2
   [:img {:src (assets/path "/images/mayvenn-logo-init.png")
          :style {:width "114px"}}]
   [:div.h6 "Have questions? Call us: "
    [:a.black {:href (phone-href mayvenn-phone)} mayvenn-phone]]])

(def congratulations-section
  [:section
   [:picture
    [:source {:media "(max-width: 749px)" :srcSet (str (assets/path "/images/leads/hero-congrats.jpg") " 2x")}]
    [:source {:srcSet (assets/path "/images/leads/hero-congrats-dsk.jpg")}]
    [:img.block.col-12 {:src (assets/path "/images/leads/hero-congrats.jpg")
                        :alt "Congrats. Welcome to the Mayvenn community! Your website is ready to go. You can share your store link, shop, and start selling now."}]]])

(defn- first-coupon-link [store-link]
  (str "//" store-link "?sha=FIRST"))

(defn coupon-section [{:keys [store-link]}]
  [:section.center.white.bg-cover.bg-center.bg-30-off.center.px3.py6
   [:div.max-580.mx-auto
    [:h2.h1.mt6 "Get 30% Off"]
    [:p.mb3 "To welcome you to Mayvenn, here is 30% off your first order. Use the promo code: FIRST"]
    [:a.btn.btn-primary.h5.px4.light
     {:href   (first-coupon-link store-link)
      :target "_blank"}
     "Shop now using promo code FIRST"]]])

(defn parse-store-link [store-link]
  ["" ""]
  #_(string/split store-link #"\." 2))

(defn facebook-link [share-url]
  (-> (url/url "https://www.facebook.com/sharer/sharer.php")
      (assoc :query {:u (assoc-in share-url [:query :utm_medium] "facebook")})
      str))

(defn twitter-link [share-url]
  (-> (url/url "https://twitter.com/intent/tweet")
      (assoc :query {:url      (assoc-in share-url [:query :utm_medium] "twitter")
                     :text     "My @MayvennHair store is open! Shop 100% virgin human hair with a 30-day quality guarantee:"
                     :hashtags "mayvennhair"})
      str))

(defn sms-link [share-url]
  ;; the ?& is to get this to work on iOS8 and Android at the same time
  (str "sms:?&body="
       (url/url-encode (str
                        "Hey! I’m now selling 100% virgin human hair through my Mayvenn hair store: "
                        (assoc-in share-url [:query :utm_medium] "sms") " "
                        "All orders ship out for free and are backed by a 30-day quality guarantee! "
                        "If you order 3 bundles or more you’ll get 25% off. "
                        "If you’re in the market for new hair, I hope you’ll consider supporting my business!"))))

(defn- social-button [options image-url content]
  [:a.h5.block.col-12.regular.btn.btn-primary.white.my4
   options
   [:img.align-bottom.mr2 {:style {:height "24px"}
                           :src   (assets/path (str "/images/leads" image-url))}]
   content])

(defn share-your-store-section [{:keys [store-link]}]
  (let [[slug host]       (parse-store-link store-link)
        share-url         (-> (url/url (str "https://" store-link))
                              (assoc :query {:utm_campaign "resolve"}))
        phone-image-width "336px"]
    [:section#share-store-section.center.px3.py6.container
     [:div.max-580.mx-auto
      [:h2 "Share your store"]
      [:p "This unique store name is all yours. Use the buttons below for quick and easy sharing across your networks."]
      [:div.relative.mx-auto
       {:style {:width phone-image-width}}
       [:img.py6.mx-auto.block {:style {:width phone-image-width}
                                :src   (assets/path "/images/leads/store-name-in-device.jpg")}]
       [:div.absolute.truncate
        {:style {:font-size   "14px"
                 :line-height "16px"
                 :top         "137px"
                 :left        "28px"
                 :right       "28px"}}
        [:span.bold slug] "." host]]
      [:h3.h5 "Share your store link"]]
     [:div.col-12.col-6-on-tb.col-4-on-dt.mx-auto
      (social-button
       {:class  "bg-fb-blue"
        :target "_blank"
        :href   (facebook-link share-url)}
       "/sprite-fb-logo.png"
       "Share on Facebook")
      (social-button
       {:class  "bg-twitter-blue"
        :target "_blank"
        :href   (twitter-link share-url)}
       "/sprite-twitter-logo.png"
       "Tweet your store link")
      (social-button
       {:class  "hide-on-dt bg-sms-green"
        :target "_blank"
        :href   (sms-link share-url)}
       "/sprite-sms-icon.png"
       "Send a text")
      [:div.h5 "Tap to select, copy and share"]
      [:input#js-share-url.input.teal.col-12.center.mt4
       {:type  "text"
        :value store-link}]]]))

(defn whats-next-section [{:keys [store-link]}]
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
        (icon "/images/leads/icon-try-mayvenn.png")
        [hed "Try Mayvenn Hair"]
        [dek "Becoming an expert in our products is the first step to becoming a successful seller. Use the promo code 'FIRST' to get 30% off your first order."]
        [cta {:href   (first-coupon-link store-link)
              :target "_blank"}
         "Shop Mayvenn hair"]]

       [cell
        (icon "/images/leads/icon-meet-stylists.png")
        [hed "Meet Fellow Stylists"]
        [dek "We have a vibrant community of over 60,000 stylists constantly sharing and learning from each other. Join the conversation by introducing yourself!"]
        [cta {:href   "https://community.mayvenn.com/mayvenn-general/f/all-about-you/1724/introduce-yourself-and-your-specialty"
              :target "_blank"}
         "Go to the Mayvenn stylist community"]]

       [cell
        (icon "/images/leads/icon-promote.png")
        [hed "Promote Your Store"]
        [dek "Don’t be shy! You now have access to the highest quality hair products in the industry. Shipping is always free and all Mayvenn products are backed by a 30-day guarantee."]
        [cta {:href "#share-store-section"} "Share your store link"]]]]]))

(defn stylist-kit-section [{:keys [store-link]}]
  [:section.center
   [:div.bg-stylist-kit.bg-center.bg-cover.relative
    {:style {:height "480px"}}
    [:div.absolute.bottom-0.left-0.right-0.px3.py6.center
     [:div.max-580.mx-auto
      [:h2 "Your secret weapon"]
      [:p.mb3 "Our stylist kit is full of essential selling tools like business cards, hair samples, and more. "
       "For just $109, this is the best way to jumpstart your Mayvenn business (a $200 value)."]
      [:a.h5.block.col-12.col-6-on-tb.col-4-on-dt.mx-auto.regular.btn.btn-primary.white
       {:href   (str "//" store-link "/categories/hair/stylist-products")
        :target "_blank"}
       "Learn more about stylist kits"]]]]])

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

(defn faq-section [q-and-as {:keys [mayvenn-phone mayvenn-text]}]
  (let [q :h3
        a :p.h5.mb4]
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
       [:li "Text us: " [:a.inherit-color {:href (phone-href mayvenn-text)} "+" mayvenn-text]]
       [:li "Call us: " [:a.inherit-color {:href (phone-href mayvenn-phone)} "+" mayvenn-phone]]
       [:li "Email us: " [:a.inherit-color {:href "mailto:help@mayvenn.com"} "help@mayvenn.com"]]
       [:li "Tweet us or DM us: " [:a.inherit-color {:href "https://twitter.com/MayvennHair" :target "_blank"} "@mayvennhair"]]]]]))

(defn query [app-state]
  {:store-link (get-in app-state (conj keypaths/leads-lead :store-url))})

(defn ^:private component
  [{:keys [store-link]} owner opts]
  (component/create
   [:div
    (header/built-component {} nil)
    [:div
     congratulations-section
     (coupon-section {})
     (share-your-store-section {})
     (whats-next-section {})
     (stylist-kit-section {})
     first-sale-section
     [:section.center.px3.py6.bg-teal.white
      (faq-section q-and-as {})]]
    #_(shared/minimal-footer {})]))


(defn built-component [data opts]
  (component/build component (query data) opts))
