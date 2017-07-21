(ns storefront.components.leads.home
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.leads.header :as header]
            [storefront.components.leads.choose-call-slot :as leads.choose-call-slot]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn sign-up-panel [{:keys [focused field-errors first-name last-name phone email self-reg?] :as attrs}]
  [:div.rounded.bg-lighten-4.p3
   [:div.center
    [:h2 "Join over 60,000 stylists"]
    [:p.mb2 "Enter your info below to get started."]]
   [:form.col-12.flex.flex-column.items-center
    {:on-submit (utils/send-event-callback events/leads-control-sign-up-submit {})
     :data-test "leads-sign-up-form"}
    (ui/text-field {:data-test "sign-up-first-name"
                    :errors    (get field-errors ["first-name"])
                    :id        "sign-up-first-name"
                    :keypath   keypaths/leads-ui-sign-up-first-name
                    :focused   focused
                    :label     "First name"
                    :name      "first-name"
                    :required  true
                    :value     first-name})
    (ui/text-field {:data-test "sign-up-last-name"
                    :errors    (get field-errors ["last-name"])
                    :id        "sign-up-last-name"
                    :keypath   keypaths/leads-ui-sign-up-last-name
                    :focused   focused
                    :label     "Last name"
                    :name      "last-name"
                    :required  true
                    :value     last-name})
    (ui/text-field {:data-test "sign-up-phone"
                    :errors    (get field-errors ["phone"])
                    :id        "sign-up-phone"
                    :keypath   keypaths/leads-ui-sign-up-phone
                    :focused   focused
                    :label     "Mobile Phone"
                    :name      "phone"
                    :required  true
                    :type      "tel"
                    :value     phone})
    (ui/text-field {:data-test "sign-up-email"
                    :errors    (get field-errors ["email"])
                    :id        "sign-up-email"
                    :keypath   keypaths/leads-ui-sign-up-email
                    :focused   focused
                    :label     "Email"
                    :name      "email"
                    :required  true
                    :type      "email"
                    :value     email})
    (when-not self-reg?
      [:div
       [:div (leads.choose-call-slot/component {:offset-from-eastern 0 :tz-abbr "EST"} nil)]])
    (ui/teal-button {:type "submit"} "Become a Mayvenn Stylist")]])

(defn resume-self-reg-panel [{:keys [lead-id]}]
  [:div.rounded.bg-lighten-4.p3
   [:div.center
    [:h2 "Become a Mayvenn"]
    [:p.m2 "You are on your way to joining the Mayvenn Movement! Finish your registration today!"]]
   [:form
    {:on-submit (utils/send-event-callback events/leads-control-self-registration-resume-submit {:lead-id lead-id})}
    [:button.btn.btn-primary.col-12 {:type "submit" :value "Submit"} "Finish registration"]]])

(defn hero-section [{:keys [current-flow sign-up resume-self-reg] :as attrs}]
  [:section.px3.py4.bg-cover.leads-bg-hero-hair
   [:div.container
    [:div.flex-on-tb-dt.items-center
     [:div.col-7-on-tb-dt.py4
      [:div.col-9-on-tb-dt.mx-auto.white.center
       [:h1
        [:span.h0.hide-on-mb "Earn $2,000 a month selling hair with no out of pocket expenses"]
        [:span.hide-on-tb-dt "Earn $2,000 a month selling hair with no out of pocket expenses"]]]]
     [:div.col-5-on-tb-dt.py4
      (if current-flow
        (resume-self-reg-panel resume-self-reg)
        (sign-up-panel sign-up))]]]])

(def success-stories-section
  (let [video-src  "https://embedwistia-a.akamaihd.net/deliveries/2029303b76647441fe1cd35778556282f8c40e68/file.mp4"
        image-src  "https://ucarecdn.com/b2083468-3738-410f-944b-ad9ffdad0997/stylistsuccessposterplaybutton.jpg"
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
        (icon "/images/leads/icon-make-more.png")
        [:h3 "Earn more money"]
        [:p.h5.mb4 "Once your clients make a purchase from your Mayvenn store, you’ll make a 15% commission and be paid out every week. It’s that simple. "]]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        (icon "/images/leads/icon-movement.png")
        [:h3 "Be a part of a movement"]
        [:p.h5.mb4 "Mayvenn’s community is working together to change the hair industry to benefit stylists and clients. "
         "Once you sign up, we’ll provide you with marketing materials, education, and customer support – completely free of charge. "]]
       [:div.col-on-tb-dt.col-4-on-tb-dt.px3-on-tb-dt
        (icon "/images/leads/icon-30-day.png")
        [:h3 "Backed by our 30-day guarantee"]
        [:p.h5 "All of our products are quality assured thanks to our 30-day guarantee. "
         "If your clients don’t like the hair for any reason, they can exchange it. "
         "If the hair is unopened, they can return it for a full refund within 30 days."]]]]]))

(defn slide [img copy]
  [:div.mb4
   [:img.mx-auto {:style {:width "225px"}
                  :src   img}]
   [:div.my4.h5 copy]])

(def slides
  [(slide "/images/leads/how-it-works-01.jpg" "Start by sending your store link to your clients so they can start shopping.")
   (slide "/images/leads/how-it-works-02.jpg" "They’ll receive free shipping and 25% off their order with Mayvenn’s bundle discount and monthly promo code.")
   (slide "/images/leads/how-it-works-03.jpg" "Cha-ching! Once they complete their purchase on your site, we’ll deposit your commission via your chosen payment method.")])


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
                                 :dots true}}
                     {})]])


(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component data nil)
    (hero-section (:hero-data data))
    success-stories-section
    about-section
    how-it-works-section

    [:div "Imma leads component!"]]))

(defn ^:private query [data]
  {:hero {:current-flow    0
          :resume-self-reg {}
          :sign-up         {:field-errors []
                            :first-name   ""
                            :last-name    ""
                            :phone        ""
                            :email        ""
                            :self-reg?    ""}}}) 

(defn built-component [data opts]
  (component/build component (query data) opts))
