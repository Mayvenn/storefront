(ns leads.home-a1
  (:require #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[om.core :as om]
                       [goog.events]
                       [goog.dom]
                       [goog.style]
                       [goog.events.EventType :as EventType]
                       [storefront.component :as component]
                       [storefront.api :as api]
                       [storefront.browser.cookie-jar :as cookie-jar]
                       [storefront.browser.tags :as tags]
                       [storefront.history :as history]])
            [clojure.string :as string]
            [leads.call-slot :as call-slot]
            [leads.flows :as flows]
            [leads.header :as header]
            [leads.keypaths :as keypaths]
            [storefront.assets :as assets]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths]
            [leads.a1-self-reg]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as transitions]))

(def become-a-mayvenn-stylist-section
  [:div.bg-light-gray
   [:section.center.hide-on-mb
    [:div.relative
     [:div.absolute.right-0.top-0.bottom-0.col-6
      (ui/aspect-ratio 720 600
                       {:class "flex flex-center"}
                       [:div.flex-auto.self-center.mr4
                        [:h2.h1.mb4 "Become a Mayvenn stylist and join the movement"]
                        [:p.h4 "We're a black-owned hair company with hustle, that empowers "
                         "hair stylists to think bigger, build their businesses, and achieve "
                         "their dreams. Instead of sending clients to beauty supply shops, we "
                         "provide a custom platform for stylists to sell the high-quality hair "
                         "directly, earn commissions from these sales, and be part of a movement."]])]]
    [:img.col-12.mbnp6
     {:src     "//ucarecdn.com/e58b55dc-4c02-4d40-8633-0012a5469a4f/-/format/auto/BecomeAMayvennStylist.jpg"
      :src-set "//ucarecdn.com/e58b55dc-4c02-4d40-8633-0012a5469a4f/-/format/auto/-/quality/lightest/BecomeAMayvennStylist.jpg 2x"}]]

   [:section.center.hide-on-tb-dt
    [:div.px3.pt6
     [:div.container
      [:h2.mb4 "Become a Mayvenn stylist and join the movement"]
      [:p.h6 "We're a black-owned hair company with hustle, that empowers "
       "hair stylists to think bigger, build their businesses, and achieve "
       "their dreams. Instead of sending clients to beauty supply shops, we "
       "provide a custom platform for stylists to sell the high-quality hair "
       "directly, earn commissions from these sales, and be part of a movement."]]
     [:img.block.col-12 {:src     "//ucarecdn.com/36c98d9b-f015-4c65-9513-4554edc0ee48/-/format/auto/BecomeAMayvennStylist.jpg"
                         :src-set "//ucarecdn.com/36c98d9b-f015-4c65-9513-4554edc0ee48/-/format/auto/-/quality/lightest/BecomeAMayvennStylist.jpg 2x"}]]]])

(defn sign-up-panel [{:keys [focused field-errors first-name last-name phone email website-url facebook-url instagram-handle number-of-clients number-of-clients-options pro? spinning?]}]
  [:div.rounded.bg-lighten-4.p3
   [:form.col-12.flex.flex-column
    {:on-submit (utils/send-event-callback events/leads-a1-control-sign-up-submit {})
     :data-test "leads-sign-up-form"
     :data-ref  "leads-sign-up-form"
     :method    "POST"}
    [:p.mb2 "About You"]
    (ui/text-field {:data-test "sign-up-first-name"
                    :errors    (get field-errors ["first-name"])
                    :id        "sign-up-first-name"
                    :keypath   keypaths/lead-first-name
                    :focused   focused
                    :label     "First name *"
                    :name      "first-name"
                    :required  true
                    :value     first-name})

    (ui/text-field {:data-test "sign-up-last-name"
                    :errors    (get field-errors ["last-name"])
                    :id        "sign-up-last-name"
                    :keypath   keypaths/lead-last-name
                    :focused   focused
                    :label     "Last name *"
                    :name      "last-name"
                    :required  true
                    :value     last-name})
    (ui/text-field {:data-test "sign-up-phone"
                    :errors    (get field-errors ["phone"])
                    :id        "sign-up-phone"
                    :keypath   keypaths/lead-phone
                    :focused   focused
                    :label     "Phone *"
                    :name      "phone"
                    :required  true
                    :type      "tel"
                    :value     phone})
    (ui/text-field {:data-test "sign-up-email"
                    :errors    (get field-errors ["email"])
                    :id        "sign-up-email"
                    :keypath   keypaths/lead-email
                    :focused   focused
                    :label     "Email *"
                    :name      "email"
                    :required  true
                    :type      "email"
                    :value     email})

    [:p.mb2 "About Your Business"]
    (ui/text-field {:data-test "sign-up-website-url"
                    :errors    (get field-errors ["website-url"])
                    :id        "sign-up-website-url"
                    :keypath   keypaths/lead-website-url
                    :focused   focused
                    :label     "Website URL"
                    :name      "website-url"
                    :value     website-url})
    (ui/text-field {:data-test "sign-up-facebook-url"
                    :errors    (get field-errors ["facebook-url"])
                    :id        "sign-up-facebook-url"
                    :keypath   keypaths/lead-facebook-url
                    :focused   focused
                    :label     "Facebook URL"
                    :name      "facebook-url"
                    :value     facebook-url})
    (ui/text-field {:data-test "sign-up-instagram-handle"
                    :errors    (get field-errors ["instagram-handle"])
                    :id        "sign-up-instagram-handle"
                    :keypath   keypaths/lead-instagram-handle
                    :focused   focused
                    :label     "Instagram Handle"
                    :name      "instagram-handle"
                    :value     instagram-handle})
    (ui/select-field {:data-test   "sign-up-number-of-clients"
                      :errors      (get field-errors ["number-of-clients"])
                      :id          "sign-up-number-of-clients"
                      :label       "Number of Clients *"
                      :placeholder "Number of Clients *"
                      :keypath     keypaths/lead-number-of-clients
                      :value       number-of-clients
                      :options     number-of-clients-options
                      :required    true
                      :div-attrs   {:class "bg-white border border-gray rounded"}})

    (ui/check-box {:label     "I am a professional stylist *"
                   :errors    (get field-errors ["professional"])
                   :data-test "sign-up-professional"
                   :id        "sign-up-professional"
                   :keypath   keypaths/lead-professional
                   :value     pro?})

    (ui/submit-button "Begin Application"
                      {:data-test "sign-up-submit"
                       :spinning? spinning?})]])

(defn signup-section [{:keys [flow-id sign-up]}]
  [:section.px3.py4.bg-cover.leads-bg-hero-hair
   [:div.container
    [:div.flex-on-tb-dt.items-center
     [:a {:name "signup-form"}]
     [:div.col-7-on-tb-dt.py4
      [:div.col-9-on-tb-dt.mx-auto.white.center
       [:h1
        [:span.h0.hide-on-mb "Apply to become a Mayvenn Stylist"]
        [:span.hide-on-tb-dt "Apply to become a Mayvenn Stylist"]]]]
     [:div.col-5-on-tb-dt.py4
      (sign-up-panel sign-up)]]]])

(def success-stories-section
  (let [mobile-url "//ucarecdn.com/b9297a82-e947-486a-8f95-73ba18250269/"
        desktop-url "//ucarecdn.com/878349cd-9510-4493-b990-c83aec10e41b/"
        file-name "hairbundles.png"
        alt "Hair Bundles"]
    [:section.center.pt6.bg-light-teal
     [:div.max-580.mx-auto.center
      [:div.px3.pb2
       [:h2 "Success Stories"]
       [:p "Don't take our word for it. Our community speaks for itself."]]]
     [:div.py2
      [:p.h1.bold.white "200,000+"]
      [:p.h5 "happy customers"]]
     [:picture.mbnp6
      ;; Desktop
      [:source {:media   "(min-width: 1000px)"
                :src-set (str desktop-url "-/format/auto/" file-name " 1x, "
                              desktop-url "-/format/auto/-/quality/lightest/" file-name " 2x")}]
      ;; Tablet
      [:source {:media   "(min-width: 750px)"
                :src-set (str desktop-url "-/format/auto/-/resize/768x/" file-name " 1x, "
                              desktop-url "-/format/auto/-/resize/720x/-/quality/lightest/" file-name " 2x")}]
      ;; Mobile
      [:img.block.col-12 {:src     (str mobile-url "-/format/auto/-/resize/375x/" file-name)
                          :src-set (str mobile-url "-/format/auto/-/resize/750x/-/quality/lightest/" file-name " 2x")
                          :alt     alt}]]]))

(def our-stylists-thrive-section
  (let [section (fn [icon copy]
                  [:div.col-on-tb-dt.col-3-on-tb-dt.px3-on-tb-dt.mb8
                   icon
                   [:p.h6.mb4 copy]])]
    [:section.px3.py6.center

     [:div.container
      [:h2.mb4 "Our Stylists Thrive"]
      [:p "Our ambitious community of stylists share a common goal of pursuing their passions and unlocking their business' full potential."]
      [:div.clearfix.mxn3-on-tb-dt.mt4.pt4
       (section [:img.mb1 {:src "//ucarecdn.com/ab25b35e-0fac-4539-bc2e-23490631dc35/-/format/auto/iconmakemore.png"
                           :height "75px"
                           :width "75px"}]
                "Mayvenn stylists earn more money")
       (section svg/no-expenses
                "They have no out of pocket expenses")
       (section [:img.mb1 {:src "//ucarecdn.com/2068e6cf-32f6-4c70-baaa-b7b9f17c586c/-/format/auto/icon30day.png"
                           :height "75px"
                           :width "75px"}]
                "Hair quality is guaranteed for 30 days risk-free")
       (section [:img.mt3.mb3.align-bottom {:src "//ucarecdn.com/647b33d9-a175-406c-82e0-3723c6767757/iconfastfreeshipping.png"
                                        :height "44px"
                                        :width "70px"}]
                "Fast and free shipping to clients or to stylists")]]]))

(defn slide [img copy]
  [:div.mb4
   [:img.mx-auto {:style {:width "225px"}
                  :src   img}]
   [:div.pb4.my4.h5 copy]])

(def slides
  [(slide "//ucarecdn.com/0bd754f8-ac24-4d89-b588-b98051a765d5/-/format/auto/howitworks01.jpg" "Start by sending your store link to your clients so they can start shopping.")
   (slide "//ucarecdn.com/f6a6a617-2200-4e7d-9769-c79f037944f2/-/format/auto/howitworks02.jpg" "They’ll receive free shipping and 25% off their order with Mayvenn’s bundle discount and monthly promo code.")
   (slide "//ucarecdn.com/464c7eb4-4c0d-4365-9f1a-e775863d5cf7/-/format/auto/howitworks03.jpg" "Cha-ching! Once they complete their purchase on your site, we’ll deposit your commission via your chosen payment method.")])


(def how-mayvenn-works-section
  (let [segment (fn [num copy]
                  [:div.pb4.col-on-tb-dt.col-3-on-tb-dt
                   [:div.h1.bold.bg-white.light-teal.mx-auto.capped.my2
                    {:style {:width "75px" :height "75px"}}
                    [:div.table-cell.align-middle
                     {:style {:width "75px" :height "75px"}}
                     num]]
                   [:p.h6 copy]])]
    [:section.center.px3.pt6.pb3.bg-light-teal
     [:div.max-580.mx-auto.center
      [:h2 "How Mayvenn Works"]
      [:p.mb4 "We build your beauty supply store for you. From tracking inventory, to customer service, and website maintenance, we support you."]]
     [:div.mx-auto.center.clearfix
      (segment "1" "Apply to become part of our expert stylist community.")
      (segment "2" "Work with our team to get onboarded, and activate your custom website.")
      (segment "3" "Start sharing your website provided by Mayvenn with clients.")
      (segment "4" "Grow your business, earn more money, and delight clients.")]]))

(def stylist-success-story-video
  (let [video-src  "https://embedwistia-a.akamaihd.net/deliveries/2029303b76647441fe1cd35778556282f8c40e68/file.mp4"
        image-src  "https://ucarecdn.com/b2083468-3738-410f-944b-ad9ffdad0997/-/format/auto/stylistsuccessposterplaybutton.jpg"
        video-html (str "<video onClick=\"this.play();\" loop muted poster=\""
                        image-src
                        "\" preload=\"none\" playsinline controls preload=\"none\" class=\"col-12\"><source src=\""
                        video-src
                        "\"></source></video>")]
    [:div.container
     [:div.container.col-12.mx-auto {:dangerouslySetInnerHTML {:__html video-html}}]]))

(defn ^:private press-quote [copy img-src]
  [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
   [:div.py3
    [:blockquote.m0.mb2.h4 copy]
    [:img.mt1 {:src img-src :style {:width "201px"}}]]])

(def clients-love-section
  [:section.pt6.center
   [:div.px3.max-580.mx-auto.center.mb6
    [:h2 "Your clients will love wearing Mayvenn Hair"]]
   stylist-success-story-video
   [:div.px3.mt3
    [:div.clearfix.px2.mxn3-on-tb-dt
      (press-quote
       "\"Mayvenn, the genius site that allows stylists to sell product directly to clients without having to worry about hidden costs and inventory.\""
       "//ucarecdn.com/e99ef6bb-c91f-44de-b4ff-ad69b843adb2/-/format/auto/presslogoessence.png")
      (press-quote
       (str "\"They offer 100% quality virgin hair in various lengths and textures to over 50,000 women. "
            "They empower hairstylists to be their own retailers and provide an additional revenue stream.\"")
       "//ucarecdn.com/1089bc49-af30-4182-9059-a4e710b95ddb/-/format/auto/presslogohellobeautiful.png")
      (press-quote
       "\"Mayvenn sources, purchases, warehouses, and distributes inventory on behalf of its users. It also handles customer service issues for them.\""
       "//ucarecdn.com/65e43a97-c265-46f8-a023-2476f8a60ea0/-/format/auto/presslogowsj.png")]]])

(def q-and-as
  [{:q "Is there a cost to be a Mayvenn Stylist and to sell Mayvenn products?"
    :a "Nope. Becoming a Mayvenn stylist is 100% free. There are no out-of-pocket costs to join."}
   {:q "Is there a special stylist discount I’ll get for signing up?"
    :a "Yes! We want you to get to know Mayvenn’s products so you can be well-equipped to sell the hair to your clients. You’ll receive 30% off your first order with a promo code that will be sent to you after you register as a stylist."}
   {:q "How and when do I get paid?"
    :a "Once you’ve made a sale, you will be paid 15% commission via your preferred payment method every Wednesday. We offer direct deposit through Venmo and Paypal, or you can opt to be paid by check."}
   {:q "How does the sales bonus work?"
    :a "Our sales bonus is one of the best perks about being a Mayvenn stylist! You’ll get $100 in Mayvenn hair credit for every $600 in sales you make or if you refer a stylist that makes $100 in sales. Use that hair credit to offer special bundle deals to your clients or treat yourself to new bundles."}
   {:q "What kind of hair do you sell?"
    :a "Our hair is 100% virgin human hair lightly steamed to create our different curl patterns. We offer Brazilian, Malaysian, Peruvian, and Indian hair textures in Straight, Body Wave, Loose Wave, Deep Wave, and Curly."}
   {:q "How much does Mayvenn hair cost?"
    :a "It all depends on your desired length, texture, and color. Our bundles start at $55. Closures start at $94. Frontals start at $149."}
   {:q "How does the 30 days quality guarantee work?"
    :a "A common frustration that many have experienced when purchasing hair extensions is the inability to return the hair when they’re dissatisfied. Our 30-day guarantee is an unprecedented move in the industry, and it shows how confident we are in our product. If you’re having any issues with your bundles, even after you’ve dyed, cut it, or styled it, we’ll exchange it within 30 days. If you haven’t altered the hair or packaging in any way, we’ll give you a full refund within 30 days."}])

(defn faq-section [q-and-as {:keys [sms-number call-number]}]
  [:div.max-580.mx-auto.center
   [:h2 "Frequently asked questions"]
   [:p.mb6 "We’re always here to help! Answers to our most frequently asked questions can be found below."]
   (map-indexed (fn [idx {:keys [q a]}]
                  [:div.mb4
                   {:key (str "q-" idx "-a")}
                   [:h3 "Q: " q]
                   [:p.h5 "A: " a]])
                q-and-as)
   [:div.mt6
    [:p.mb4 "If you still have questions about becoming a Mayvenn stylist, feel free to contact us! Our customer service representatives are ready to answer all of your questions. There are a few ways you can reach us:"]
    [:ul.list-reset
     [:li "Text us: " (ui/link :link/sms :a.inherit-color {} sms-number)]
     [:li "Call us: " (ui/link :link/phone :a.inherit-color {} "+" call-number)]
     [:li "Email us: " (ui/link :link/email :a.inherit-color {} "help@mayvenn.com")]
     [:li "Tweet us or DM us: " [:a.inherit-color {:href "https://twitter.com/MayvennHair" :target "_blank"} "@mayvennhair"]]]]])

(defn footer [{:keys [call-number host-name]}]
  (let [policy-url  (str "//shop." host-name "/policy")
        privacy-url (str policy-url "/privacy")]
    [:div.border-top.border-gray.bg-white
     [:div.container
      [:div.center.px3.my2
       [:div.my1.medium.dark-gray "Need Help?"]
       [:div.dark-gray.light.h5
        [:span.hide-on-tb-dt (ui/link :link/phone :a.dark-gray {} call-number)]
        [:span.hide-on-mb call-number]
        " | 9am-5pm PST M-F"]
       [:div.my1.dark-silver.h6
        [:div.center
         [:a.inherit-color
          {:href privacy-url} "Privacy"]
         " - "
         [:a.inherit-color
          {:href (str privacy-url "#ca-privacy-rights")} "CA Privacy Rights"]
         " - "
         [:a.inherit-color
          {:href (str policy-url "/tos")} "Terms"]
         " - "
         [:a.inherit-color
          {:href (str privacy-url "#our-ads")} "Our Ads"]]]]]]))


(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component (:header data) nil)
    become-a-mayvenn-stylist-section
    how-mayvenn-works-section
    our-stylists-thrive-section
    (signup-section (:hero data))
    success-stories-section
    clients-love-section
    [:section.center.px3.py6
     (faq-section q-and-as (:faq data))]
    (footer (:footer data))]))

(defn ->hr [hour]
  (let [tod    (if (< hour 12) "AM" "PM")
        result (mod hour 12)
        result (if (zero? result)
                 12
                 result)]
    (str result " " tod)))

(defn ^:private query [data]
  (let [host-name (case (get-in data storefront.keypaths/environment)
                    "production" "mayvenn.com"
                    "acceptance" "diva-acceptance.com"
                    "storefront.localhost")

        {:keys [flow-id] :as remote-lead} (get-in data keypaths/remote-lead)]
    {:hero   {:flow-id flow-id
              :sign-up {:field-errors              (get-in data storefront.keypaths/field-errors)
                        :first-name                (get-in data keypaths/lead-first-name)
                        :last-name                 (get-in data keypaths/lead-last-name)
                        :phone                     (get-in data keypaths/lead-phone)
                        :email                     (get-in data keypaths/lead-email)
                        :flow-id                   (get-in data keypaths/lead-flow-id)
                        :focused                   (get-in data storefront.keypaths/ui-focus)
                        :website-url               (get-in data keypaths/lead-website-url)
                        :facebook-url              (get-in data keypaths/lead-facebook-url)
                        :instagram-handle          (get-in data keypaths/lead-instagram-handle)
                        :number-of-clients         (get-in data keypaths/lead-number-of-clients)
                        :number-of-clients-options [["None" "0"]
                                                    ["1 to 5" "1-5"]
                                                    ["6 to 10" "6-10"]
                                                    ["11 to 15" "11-15"]
                                                    ["16 to 19" "16-19"]
                                                    ["20+" "20+"]]
                        :pro?                      (get-in data keypaths/lead-professional)
                        :spinning?                 (utils/requesting? data request-keys/create-lead)}}
     :header {:call-number config/mayvenn-leads-a1-call-number}
     :footer {:call-number config/mayvenn-leads-a1-call-number
              :host-name   host-name}
     :faq    {:sms-number  config/mayvenn-leads-sms-number
              :call-number config/mayvenn-leads-a1-call-number}}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/leads-a1-control-sign-up-submit
  [_ _ _ _ app-state]
  (flows/create-lead app-state
                     #(messages/handle-message events/api-success-leads-a1-lead-created %)))

(defmethod transitions/transition-state events/api-success-leads-a1-lead-created
  [_ _ {remote-lead :lead} app-state]
  #?(:cljs (-> app-state
               (update-in keypaths/lead select-keys [:utm-term :utm-content :utm-campaign :utm-source :utm-medium])
               (update-in keypaths/remote-lead merge remote-lead))
     :clj  app-state))

(defmethod effects/perform-effects events/api-success-leads-a1-lead-created
  [_ _ _ previous-app-state app-state]
  #?(:cljs
     (let [remote-lead (get-in app-state keypaths/remote-lead)]
       (cookie-jar/save-lead (get-in app-state storefront.keypaths/cookie)
                             {"lead-id" (:id remote-lead)})
       (history/enqueue-navigate events/navigate-leads-a1-receive))))
