(ns leads.home
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
            [leads.header :as header]
            [leads.call-slot :as call-slot]
            [leads.accessors :as accessors]
            [storefront.assets :as assets]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.platform.carousel :as carousel]
            [storefront.events :as events]
            [storefront.keypaths]
            [leads.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.footer :as footer]
            [clojure.string :as string]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [spice.core :as spice]
            [storefront.request-keys :as request-keys]))

(defn sign-up-panel [{:keys [focused field-errors first-name last-name phone email call-slot self-reg? call-slot-options spinning?] :as attrs}]
  [:div.rounded.bg-lighten-4.p3
   [:div.center
    [:h2 "Join over 60,000 stylists"]
    [:p.mb2 "Enter your info below to get started."]]
   [:form.col-12.flex.flex-column.items-center
    {:on-submit (utils/send-event-callback events/leads-control-sign-up-submit {})
     :data-test "leads-sign-up-form"
     :method "POST"}
    (ui/text-field {:data-test "sign-up-first-name"
                    :errors    (get field-errors ["first-name"])
                    :id        "sign-up-first-name"
                    :keypath   keypaths/lead-first-name
                    :focused   focused
                    :label     "First name*"
                    :name      "first-name"
                    :required  true
                    :value     first-name})

    (ui/text-field {:data-test "sign-up-last-name"
                    :errors    (get field-errors ["last-name"])
                    :id        "sign-up-last-name"
                    :keypath   keypaths/lead-last-name
                    :focused   focused
                    :label     "Last name*"
                    :name      "last-name"
                    :required  true
                    :value     last-name})
    (ui/text-field {:data-test "sign-up-phone"
                    :errors    (get field-errors ["phone"])
                    :id        "sign-up-phone"
                    :keypath   keypaths/lead-phone
                    :focused   focused
                    :label     "Mobile phone*"
                    :name      "phone"
                    :required  true
                    :type      "tel"
                    :value     phone})
    (ui/text-field {:data-test "sign-up-email"
                    :errors    (get field-errors ["email"])
                    :id        "sign-up-email"
                    :keypath   keypaths/lead-email
                    :focused   focused
                    :label     "Email*"
                    :name      "email"
                    :required  true
                    :type      "email"
                    :value     email})
    (when-not self-reg?
      (ui/select-field {:data-test   "sign-up-call-slot"
                        :errors      (get field-errors ["call-slot"])
                        :id          "sign-up-call-slot"
                        :label       "Best time to call*"
                        :keypath     keypaths/lead-call-slot
                        :placeholder "Best time to call*"
                        :value       call-slot
                        :required    true
                        :options     call-slot-options
                        :div-attrs   {:class "bg-white border border-gray rounded"}}))
    (ui/submit-button "Become a Mayvenn Stylist"
                      {:data-test "sign-up-submit"
                       :spinning? spinning?})]])

(defn resume-self-reg-panel [{:keys [remote-lead]}]
  [:div.rounded.bg-lighten-4.p3
   [:div.center
    [:h2 "Become a Mayvenn"]
    [:p.m2 "You are on your way to joining the Mayvenn Movement! Finish your registration today!"]]
   (ui/teal-button
    (merge {:data-test "resume-registration"}
           (utils/route-to events/navigate-leads-registration-details {:submitted-lead remote-lead}))
    "Finish registration")])

(defn hero-section [{:keys [flow-id sign-up resume-self-reg] :as attrs}]
  [:section.px3.py4.bg-cover.leads-bg-hero-hair
   [:div.container
    [:div.flex-on-tb-dt.items-center
     [:div.col-7-on-tb-dt.py4
      [:div.col-9-on-tb-dt.mx-auto.white.center
       [:h1
        [:span.h0.hide-on-mb "Earn $2,000 a month selling hair with no out of pocket expenses"]
        [:span.hide-on-tb-dt "Earn $2,000 a month selling hair with no out of pocket expenses"]]]]
     [:div.col-5-on-tb-dt.py4
      (if flow-id
        (resume-self-reg-panel resume-self-reg)
        (sign-up-panel sign-up))]]]])

(def success-stories-section
  (let [video-src  "https://embedwistia-a.akamaihd.net/deliveries/2029303b76647441fe1cd35778556282f8c40e68/file.mp4"
        image-src  "https://ucarecdn.com/b2083468-3738-410f-944b-ad9ffdad0997/-/format/auto/stylistsuccessposterplaybutton.jpg"
        ;; TODO Figure out why playing after figwheel reloads breaks chrome (aw, snap!)
        video-html (str "<video onClick=\"this.play();\" loop muted poster=\""
                        image-src
                        "\" preload=\"none\" playsinline controls preload=\"none\" class=\"col-12\"><source src=\""
                        video-src
                        "\"></source></video>")]
    [:section.center.pt6
     [:div.max-580.mx-auto.center
      [:div.px3.pb4
       [:h2 "Mayvenn success stories"]
       [:p
        "Our ambitious group of stylists share a common goal of pursuing their passions and unlocking their full potential. "
        "Watch how Mayvenn is helping them achieve some of their biggest dreams."]]]
     [:div.container
      [:div.container.col-12.mx-auto {:dangerouslySetInnerHTML {:__html video-html}}]]]))

(def about-section
  (let [icon (fn [url] [:img.mb1 {:src url
                                  :height "70px"
                                  :width "70px"}])]
    [:section.px3.py6.center
     [:div.max-580.mx-auto.center
      [:h2 "What is Mayvenn?"]
      [:p.mb4 "Mayvenn is a black-owned hair company whose mission is to empower hair stylists to sell directly to their clients."]
      [:p.mb4 "You already suggest hair products to your clients. "
       "Become a Mayvenn stylist and start earning cash for yourself, "
       "instead of sending clients to the beauty supply store."]]

     [:div.container
      [:h2.mb4 "How can Mayvenn benefit you?"]
      [:div.clearfix.mxn3-on-tb-dt
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        (icon "//ucarecdn.com/ab25b35e-0fac-4539-bc2e-23490631dc35/-/format/auto/iconmakemore.png")
        [:h3 "Earn more money"]
        [:p.h5.mb4 "Once your clients make a purchase from your Mayvenn store, you’ll make a 15% commission and be paid out every week. It’s that simple. "]]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        (icon "//ucarecdn.com/7d1797f4-9677-4a4c-861d-fdc21b47d81b/-/format/auto/iconmovement.png")
        [:h3 "Be a part of a movement"]
        [:p.h5.mb4 "Mayvenn’s community is working together to change the hair industry to benefit stylists and clients. "
         "Once you sign up, we’ll provide you with marketing materials, education, and customer support – completely free of charge. "]]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        (icon "//ucarecdn.com/2068e6cf-32f6-4c70-baaa-b7b9f17c586c/-/format/auto/icon30day.png")
        [:h3 "Backed by our 30-day guarantee"]
        [:p.h5 "All of our products are quality assured thanks to our 30-day guarantee. "
         "If your clients don’t like the hair for any reason, they can exchange it. "
         "If the hair is unopened, they can return it for a full refund within 30 days."]]]]]))

(defn slide [img copy]
  [:div.mb4
   [:img.mx-auto {:style {:width "225px"}
                  :src   img}]
   [:div.pb4.my4.h5 copy]])

(def slides
  [(slide "//ucarecdn.com/0bd754f8-ac24-4d89-b588-b98051a765d5/-/format/auto/howitworks01.jpg" "Start by sending your store link to your clients so they can start shopping.")
   (slide "//ucarecdn.com/f6a6a617-2200-4e7d-9769-c79f037944f2/-/format/auto/howitworks02.jpg" "They’ll receive free shipping and 25% off their order with Mayvenn’s bundle discount and monthly promo code.")
   (slide "//ucarecdn.com/464c7eb4-4c0d-4365-9f1a-e775863d5cf7/-/format/auto/howitworks03.jpg" "Cha-ching! Once they complete their purchase on your site, we’ll deposit your commission via your chosen payment method.")])


(def how-it-works-section
  [:section.center.px3.pt6.pb3.bg-light-teal
   [:div.max-580.mx-auto.center
    [:h2 "How it works"]
    [:p.mb4 "Selling with Mayvenn is as easy as sharing your store link, spreading the word, and watching your income grow."]]
   [:div.container {:style {:max-width "375px"}}
    (component/build carousel/component
                     {:slides slides
                      :settings {:swipe true
                                 :arrows true
                                 :dots true
                                 :infinite false}}
                     {})]])

(def guarantee-section
  [:section.center
   [:div.bg-center.bg-top.bg-cover.bg-our-hair]
   [:div.px3.pt6.pb4
    [:div.max-580.mx-auto.center
     [:h2 "Quality Guaranteed"]
     [:p.mb1 "We sell 100% virgin human hair in a wide variety of textures and colors. "
      "We offer Peruvian, Malaysian, Indian, and Brazilian virgin hair that is machine-wefted, tangle-free and handpicked for quality."]]]])

(defn tweet [link date byline & content]
  [:div.mb1.flex.justify-center
   [:blockquote.mx-auto.twitter-tweet {:data-cards "hidden"
                                       :data-lang  "en"}
    (into [:p {:lang "en" :dir "ltr"}] content)
    (str "— " byline " ")
    [:a {:href link} date]]])

(def twitter-section
  [:section.center.mx4.mb4
   [:div.max-580.mx-auto.center
    [:h2.mb2.h3 "With over 200,000 happy customers, your clients are going to love wearing Mayvenn hair."]]

   (tweet "https://twitter.com/jusz_ashlee/status/838820759699075073"
          "March 6, 2017"
          "ashlee💙 (@jusz_ashlee)"
          "Order it Friday &' it arrived today💁🏽 "
          [:a {:href "https://twitter.com/MayvennHair"} "@MayvennHair"]
          [:a {:href "https://t.co/WVr1HftUqb"} "pic.twitter.com/WVr1HftUqb"])

   (tweet "https://twitter.com/ADITLWTOMEI/status/829373844980903938"
          "February 8, 2017"
          "Adayinthelifew/Tomei (@ADITLWTOMEI)"
          "Oh my goodness ! I'm so ready to book my hair apt.my "
          [:a {:href "https://twitter.com/MayvennHair"} "@MayvennHair"]
          " has been delivered! I just ordered and it came in less than a week 🤗")

   (tweet "https://twitter.com/ABenton/status/605158564479569920"
          "May 31, 2015"
          "Angela Benton (@ABenton)"
          [:a {:href "https://twitter.com/MayvennHair"} "@mayvennhair"]
          " for the save. My other hair and Hawaii didn't mix lol! You the real MVP 😂😂😂!… "
          [:a {:href "https://t.co/LZlwmPJ1kJ"} "https://t.co/LZlwmPJ1kJ"])])

(def your-store-section
  [:section.px3.pt6.bg-light-teal
   [:div.container
    [:div.flex-on-dt.items-center
     [:div.col-8-on-dt
      [:div.col-9-on-dt.mx-auto.center
       [:h2 "Your own beauty supply store"]
       [:p.mb4
        "Launching a website on your own is no easy feat - so we handle it all for you. "
        "As a Mayvenn stylist, you can forget worrying about tracking inventory, customer service, or website maintenance."]]]
     [:div.col-4-on-dt
      [:picture
       [:source {:media "(max-width: 999px)" :src-set "//ucarecdn.com/5ff05d23-6ef0-445d-9e76-f7e5ff5e3cf6/-/format/auto/yourbeautystore.jpg 2x"}]
       [:source {:src-set "//ucarecdn.com/fd060b43-50c8-4e34-8c3e-7ba0605c8def/-/format/auto/yourbeautystoredsk.jpg"}]
       [:img.block.mx-auto {:src "//ucarecdn.com/5ff05d23-6ef0-445d-9e76-f7e5ff5e3cf6/-/format/auto/yourbeautystore.jpg"}]]]]]])

(def numbers-section
  (let [rule [:div.hide-on-tb-dt.border-white.border-bottom]]
    [:section.px3.pt6.center
     [:div.max-580.mx-auto.center
      [:h2 "The best in the business"]
      [:p "Our stylists have sold over $35 million in hair products which has helped grow their incomes and support their communities."]]
     [:div.container
      [:div.clearfix.px2
       [:div.col-on-tb-dt.col-4-on-tb-dt
        [:div.py6
         [:h3.h2 "Member stylists"]
         [:p.h0 "60,000+"]]
        rule]
       [:div.col-on-tb-dt.col-4-on-tb-dt
        [:div.py6
         [:h3.h2 "Stylist payouts"]
         [:p.h0 "$4,000,000+"]]
        rule]
       [:div.col-on-tb-dt.col-4-on-tb-dt
        [:div.py6
         [:h3.h2 "Happy customers"]
         [:p.h0 "200,000+"]]]]]]))

(def press-section
  (let [rule [:div.hide-on-tb-dt.border-teal.border-bottom]]
    [:section.px3.pt6.center.bg-teal.white
     [:div.max-580.mx-auto.center
      [:h2 "In the press"]
      [:p "Don’t just take our word for it. As the Mayvenn movement continues to grow so do our stylists’ earnings."]]
     [:div.container
      [:div.clearfix.px2.mxn3-on-tb-dt
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        [:div.py6
         [:blockquote.m0.mb2.h6 "\"Mayvenn, the genius site that allows stylists to sell product directly to clients without having to worry about hidden costs and inventory.\""]
         [:img {:src "//ucarecdn.com/22ee98ef-927b-47be-a197-6adc0930f389/-/format/auto/presslogoessence.png" :style {:width "201px"}}]
         [:p.h6 "November 24, 2016"]]
        rule]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        [:div.py6
         [:blockquote.m0.mb2.h6 "\"They offer 100% quality virgin hair in various lengths and textures to over 50,000 women. "
          "They empower hairstylists to be their own retailers and provide an additional revenue stream.\""]
         [:img {:src "//ucarecdn.com/eaa78d5e-f5fd-4fa7-8896-658a80787cc5/-/format/auto/presslogohellobeautiful.png" :style {:width "201px"}}]
         [:p.h6 "September 8, 2016"]]
        rule]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        [:div.py6
         [:blockquote.m0.mb2.h6 "\"Mayvenn sources, purchases, warehouses, and distributes inventory on behalf of its users. It also handles customer service issues for them.\""]
         [:img {:src "//ucarecdn.com/09a568b1-6365-4620-8699-230b2eef8be0/-/format/auto/presslogowsj.png" :style {:width "201px"}}]
         [:p.h6 "June 19, 2015"]]]]]]))

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
  (let [q :h3
        a :p.h5.mb4]
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
       [:li "Tweet us or DM us: " [:a.inherit-color {:href "https://twitter.com/MayvennHair" :target "_blank"} "@mayvennhair"]]]]]))

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

(defn follow-header [{:keys [call-number]} owner opts]
  #?(:cljs
     (letfn [(handle-scroll [e] (om/set-state! owner :show? (< 750 (.-y (goog.dom/getDocumentScroll)))))]
       (reify
         om/IInitState
         (init-state [this]
           {:show? false})
         om/IDidMount
         (did-mount [this]
           (goog.events/listen js/window EventType/SCROLL handle-scroll))
         om/IWillUnmount
         (will-unmount [this]
           (goog.events/unlisten js/window EventType/SCROLL handle-scroll))
         om/IWillReceiveProps
         (will-receive-props [this next-props])
         om/IRenderState
         (render-state [this {:keys [show?]}]
           (component/html
            [:div.fixed.top-0.left-0.right-0.z4.bg-white
             (if show?
               {:style {:margin-top "0"}
                :class "transition-2"}
               {:style {:margin-top "-100px"}})
             [:div.border-bottom.border-gray.mx-auto
              {:style {:max-width "1440px"}}
              [:div.container.flex.items-center.justify-between.px3.py2
               [:div
                [:img {:src (assets/path "/images/header_logo.svg")
                       :style {:width "88px"}}]
                [:div.h6 "Questions? Call us: " call-number]]
               [:a.btn.btn-outline.teal {:href "#signup-form"} "Sign up"]]]]))))
     :clj [:span]))

(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component data nil)
    (component/build follow-header (:header data) nil)
    (hero-section (:hero data))
    success-stories-section
    about-section
    how-it-works-section
    guarantee-section
    twitter-section
    your-store-section
    numbers-section
    press-section
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
                    "storefront.dev")

        {:keys [flow-id] :as remote-lead} (get-in data keypaths/remote-lead)
        self-reg?                         (= "stylistsfb"
                                             (string/lower-case (or (get-in data keypaths/lead-utm-content)
                                                                    "")))]
    {:hero   {:flow-id         flow-id
              :resume-self-reg {:remote-lead remote-lead}
              :sign-up         {:field-errors      (get-in data storefront.keypaths/field-errors)
                                :first-name        (get-in data keypaths/lead-first-name)
                                :last-name         (get-in data keypaths/lead-last-name)
                                :phone             (get-in data keypaths/lead-phone)
                                :email             (get-in data keypaths/lead-email)
                                :call-slot         (get-in data keypaths/lead-call-slot)
                                :self-reg?         self-reg?
                                :call-slot-options (get-in data keypaths/call-slot-options)
                                :spinning?         (utils/requesting? data request-keys/create-lead)}}
     :header {:call-number config/mayvenn-leads-call-number}
     :footer {:call-number config/mayvenn-leads-call-number
              :host-name   host-name}
     :faq    {:sms-number  config/mayvenn-leads-sms-number
              :call-number config/mayvenn-leads-call-number}}))

(defn built-component [data opts]
  (component/build component (query data) opts))

(def ^:private required-keys
  #{:tracking-id
    :first-name :last-name
    :phone :email
    :call-slot
    :utm-source :utm-medium :utm-campaign :utm-content :utm-term})

(defmethod effects/perform-effects events/leads-control-sign-up-submit
  [_ _ _ _ app-state]
  #?(:cljs
     (-> (get-in app-state keypaths/lead)
         (select-keys required-keys)
         api/create-lead)))

(defmethod transitions/transition-state events/api-success-lead-created
  [_ _ {remote-lead :lead :as old-lead} app-state]
  (let [remote-lead (or remote-lead old-lead)] ;;GROT after leads deploy
    #?(:cljs (-> app-state
                 (update-in keypaths/lead select-keys [:utm-term :utm-content :utm-campaign :utm-source :utm-medium])
                 (update-in keypaths/remote-lead merge remote-lead))
       :clj  app-state)))

(defmethod effects/perform-effects events/api-success-lead-created
  [_ _ _ previous-app-state app-state]
  #?(:cljs
     (let [{:keys [flow-id] lead-id :id} (get-in app-state keypaths/remote-lead)
           lead                          (get-in previous-app-state keypaths/lead)]
       (cookie-jar/save-lead (get-in app-state storefront.keypaths/cookie)
                             {"lead-id"             lead-id
                              "onboarding-status" "lead-created"})
       (if (accessors/self-reg? flow-id)
         (history/enqueue-navigate events/navigate-leads-registration-details {:submitted-lead lead})
         (history/enqueue-navigate events/navigate-leads-resolve)))))

(defmethod effects/perform-effects events/navigate-leads
  [_ _ _ _ app-state]
  #?(:cljs
     (when-not (= "welcome"
                  (get-in app-state storefront.keypaths/store-slug))
       (effects/page-not-found))))

(defn clear-lead [app-state]
  (-> app-state
      (update-in keypaths/lead select-keys [:utm-term :utm-content :utm-campaign :utm-source :utm-medium])
      (assoc-in keypaths/stylist {})
      (assoc-in keypaths/remote-lead {})))

(def terminal-onboarding-statuses #{"awaiting-call"
                                    "stylist-created"})

(defmethod transitions/transition-state events/navigate-leads-home
  [_ _ _ app-state]
  #?(:cljs
     (let [call-slots        (call-slot/options (get-in app-state keypaths/eastern-offset))
           lead-cookie       (cookie-jar/retrieve-lead (get-in app-state storefront.keypaths/cookie))
           utm-cookies       (cookie-jar/retrieve-leads-utm-params (get-in app-state storefront.keypaths/cookie))
           onboarding-status (get lead-cookie "onboarding-status")
           app-state         (-> app-state
                                 (assoc-in keypaths/call-slot-options call-slots)
                                 (update-in keypaths/lead-utm-content
                                            (fn [existing-param]
                                              (or existing-param
                                                  (get utm-cookies "leads.utm-content")))))]
       (cond-> app-state
         (contains? terminal-onboarding-statuses onboarding-status)
         clear-lead))))

(defmethod effects/perform-effects events/navigate-leads-home
  [_ _ _ _ app-state]
  #?(:cljs
     (let [onboarding-status (-> (get-in app-state storefront.keypaths/cookie)
                                 cookie-jar/retrieve-lead
                                 (get "onboarding-status"))]
       (when (contains? terminal-onboarding-statuses
                        onboarding-status)
         (cookie-jar/clear-lead (get-in app-state storefront.keypaths/cookie))))))
