(ns adventure.shop-home
  (:require [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            adventure.handlers
            adventure.keypaths
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as component]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]))

(def shop-contact-query
  {:layer/type         :shop-contact
   :title/value        "Contact Us"
   :sub-subtitle/value "We're here to help"
   :subtitle/value     "Have Questions?"
   :contact-us-blocks  [{:url   (ui/sms-url "346-49")
                         :svg   (svg/icon-sms {:height 51
                                               :width  56})
                         :title "Live Chat"
                         :copy  "Text: 346-49"}
                        {:url   (ui/phone-url "1 (888) 562-7952")
                         :svg   (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                :height 57
                                                :width  57})
                         :title "Call Us"
                         :copy  "1 (888) 562-7952"}
                        {:url   (ui/email-url "help@mayvenn.com")
                         :svg   (svg/icon-email {:height 39
                                                 :width  56})
                         :title "Email Us"
                         :copy  "help@mayvenn.com"}]})

(defn query
  [data]
  (let [cms-homepage-hero  (some-> data (get-in storefront.keypaths/cms-homepage) :shop :hero)
        cms-ugc-collection (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event  (get-in data storefront.keypaths/navigation-event)]
    {:layers
     [(merge {:layer/type :hero}
             (assoc (homepage-hero/query cms-homepage-hero)
                    :file-name "free-install-hero"))
      {:layer/type :free-standard-shipping-bar}
      {:layer/type   :shop-text-block
       :header/value "Buy 3 bundles and we’ll pay for your service"
       :cta/value    "Browse Stylists"
       :cta/button?  true
       :cta/id       "browse-stylist"
       :cta/target   [events/navigate-adventure-match-stylist]}
      {:layer/type   :shop-framed-checklist
       :header/value "What's included?"
       :bullets      ["Shampoo"
                      "Braid down"
                      "Sew-in and style"
                      "Paid for by Mayvenn"]
       :divider-img  "url('//ucarecdn.com/2d3a98e3-b49a-4f0f-9340-828d12865315/-/resize/x24/')"}
      {:layer/type :video-overlay
       :video      (get-in data adventure.keypaths/adventure-home-video)}
      {:layer/type     :shop-bulleted-explainer
       :layer/id       "heres-how-it-works"
       :title/value    ["You buy the hair,"
                        "we cover the service."]
       :subtitle/value ["Here's how it works."]
       :bullets        [{:title/value "Pick Your Dream Look"
                         :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                        {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                         :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                        {:title/value "Schedule Your Appointment"
                         :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]
       :cta/id         "watch-video"
       :cta/value      "Watch Video"
       :cta/icon       (svg/play-video {:width  "30px"
                                        :height "30px"})
       :cta/target     [events/navigate-home {:query-params {:video "free-install"}}]}
      {:layer/type   :shop-text-block
       :header/value [:div.py1.shout
                      ;; NOTE: this is a design exception
                      [:div.title-1.proxima {:style {:font-size "34px"}} "Sit back and"]
                      [:div.light.canela.mt2.mb4 {:style {:font-size "54px"}} "relax"]]
       :body/value   "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
       :cta/value    "Learn more"
       :cta/id       "info-certified-stylists"
       :cta/target   [events/navigate-info-certified-stylists]}
      {:layer/type :image-block
       :ucare?     true
       :mob-uuid   "a6a607e6-aeb4-4b61-8bc7-60fd17d15abe"
       :dsk-uuid   "f2d82c41-2051-47d8-86c5-1c82568e324d"
       :file-name  "who-shop-hair"}
      {:layer/type   :shop-text-block
       :header/value [:div.py1.shout
                      ;; NOTE: this is a design exception
                      [:div.title-1.proxima {:style {:font-size "19px"}} "Hold your hair"]
                      [:div.canela.mt2.mb4 {:style {:font-size "72px"}} "high"]]
       :body/value   "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
       :cta/id       "info-about-our-hair"
       :cta/value    "shop hair"
       :cta/target   [events/navigate-category {:page/slug           "mayvenn-install"
                                                :catalog/category-id "23"}]
       :divider-img  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"}
      {:layer/type   :shop-ugc
       :header/value "#MayvennFreeInstall"
       :images       (->> cms-ugc-collection
                          :free-install-mayvenn
                          :looks
                          (mapv (partial contentful/look->homepage-social-card
                                         current-nav-event
                                         :free-install-mayvenn)))
       :cta/id       "see-more-looks"
       :cta/value    "see more looks"
       :cta/target   [events/navigate-shop-by-look {:album-keyword :look}]}
      (merge {:layer/type :faq} (faq/free-install-query data) {:background-color "bg-pale-purple"})
      {:layer/type     :shop-iconed-list
       :layer/id       "more-than-a-hair-company"
       :title/value    [[:div.img-logo.bg-no-repeat.bg-center.bg-contain {:style {:height "29px"}}]]
       :subtitle/value ["guarantees"]
       :bullets        [{:icon/body    (svg/heart {:class  "fill-p-color"
                                                   :width  "32px"
                                                   :height "29px"})
                         :header/value "Top-Notch Customer Service"
                         :body/value   "Our team is made up of hair experts ready to help you by phone, text, and email."}
                        {:icon/body    (svg/calendar {:class  "fill-p-color"
                                                      :width  "30px"
                                                      :height "33px"})
                         :header/value "30 Day Guarantee"
                         :body/value   "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
                        {:icon/body    (svg/worry-free {:class  "fill-p-color"
                                                        :width  "35px"
                                                        :height "36px"})
                         :header/value "100% Virgin Hair"
                         :body/value   "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
                        {:icon/body    (svg/mirror {:class  "fill-p-color"
                                                    :width  "30px"
                                                    :height "34px"})
                         :header/value "Certified Stylists"
                         :body/value   "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]}
      {:layer/type                  :shop-quote-img
       :quote/dsk-ucare-id          "3208fac6-c974-4c80-8e88-3244ee50226b"
       :quote/mob-ucare-id          "befce648-98b6-45a2-90f0-6199119bfffb"
       :quote/text                  "You deserve quality extensions & exceptional service without the unreasonable price tag."
       :quote/primary-attribution   "— Diishan Imira"
       :quote/secondary-attribution "CEO of Mayvenn"}
      shop-contact-query
      {:layer/type             :sticky-footer
       :layer/id               "sticky-footer-get-started"
       :sticky/content         "It’s true, we are paying for your install! "
       :cta/label              "Get started"
       :cta/navigation-message [events/navigate-adventure-match-stylist]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))
