(ns mayvenn-install.about
  (:require [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            adventure.keypaths
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]))

(defn query
  [data]
  (let [cms-ugc-collection (get-in data storefront.keypaths/cms-ugc-collection)]
    {:layers
     [{:layer/type :hero
       :opts       {:href      "/adv/match-stylist"
                    :data-test "home-banner"}
       :dsk-url    "//images.ctfassets.net/76m8os65degn/6qzfvE4tjQApY5shHJU7gw/d946312226ad40efe3445c2f3e9ac91e/site-homepage-hero-020420-dsk-03.jpg"
       :mob-url    "//images.ctfassets.net/76m8os65degn/49QbvcP6B3Ow3g08qM3u3g/1a4567b56ab380a70e8889cec983fef8/site-homepage-hero-020420-mob-03.jpg"
       :alt        "Buy 3 bundles and we'll pay for your install! Choose any Mayvenn Stylist in your area. Browse Stylists."
       :file-name  "free-install-hero"}
      {:layer/type :free-standard-shipping-bar}
      {:layer/type   :shop-text-block
       :header/value "Buy 3 bundles and we’ll pay for your service"
       :cta/value    "Browse Stylists"
       :cta/button?  true
       :cta/id       "browse-stylist"
       :cta/target   [events/navigate-adventure-find-your-stylist]}
      {:layer/type   :shop-framed-checklist
       :header/value "What's included?"
       :bullets      ["Shampoo"
                      "Braid down"
                      "Sew-in and style"
                      "Paid for by Mayvenn"]
       :divider-img  "url('//ucarecdn.com/2d3a98e3-b49a-4f0f-9340-828d12865315/-/resize/x24/')"}
      {:layer/type      :video-overlay
       :close-nav-event events/navigate-about-mayvenn-install
       :video           (get-in data adventure.keypaths/adventure-home-video)}
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
       :cta/target     [events/navigate-about-mayvenn-install {:query-params {:video "free-install"}}]}
      {:layer/type         :shop-text-block
       ;; NOTE: this is a design exception
       :big-header/content [{:text "Sit back and" :attrs {:style {:font-size "34px"}}}
                            {:text "relax" :attrs {:style {:font-size "54px"}}}]
       :body/value         "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
       :cta/value          "Learn more"
       :cta/id             "info-certified-stylists"
       :cta/target         [events/navigate-info-certified-stylists]}
      {:layer/type :image-block
       :ucare?     true
       :mob-uuid   "a6a607e6-aeb4-4b61-8bc7-60fd17d15abe"
       :dsk-uuid   "f2d82c41-2051-47d8-86c5-1c82568e324d"
       :file-name  "who-shop-hair"}
      {:layer/type         :shop-text-block
       ;; NOTE: this is a design exception
       :big-header/content [{:text "Hold your hair" :attrs {:style {:font-size "19px"}}}
                            {:text "high" :attrs {:style {:font-size "72px"}}}]
       :body/value         "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
       :cta/id             "info-about-our-hair"
       :cta/value          "shop hair"
       :cta/target         [events/navigate-category {:page/slug           "mayvenn-install"
                                                      :catalog/category-id "23"}]
       :divider-img        "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"}
      {:layer/type   :shop-ugc
       :header/value "#MayvennFreeInstall"
       :images       (->> cms-ugc-collection
                          :free-install-mayvenn
                          :looks
                          (mapv (partial contentful/look->homepage-social-card :free-install-mayvenn)))
       :cta/id       "see-more-looks"
       :cta/value    "see more looks"
       :cta/target   [events/navigate-shop-by-look {:album-keyword :look}]}
      (merge {:layer/type :faq}
             (faq/free-install-query data))
      {:layer/type     :shop-iconed-list
       :layer/id       "more-than-a-hair-company"
       :subtitle/value ["guarantees"]
       :bullets        [{:icon/symbol  [:svg/heart {:class  "fill-p-color"
                                                    :width  "32px"
                                                    :height "29px"}]
                         :header/value "Top-Notch Customer Service"
                         :body/value   "Our team is made up of hair experts ready to help you by phone, text, and email."}
                        {:icon/symbol  [:svg/calendar {:class  "fill-p-color"
                                                       :width  "30px"
                                                       :height "33px"}]
                         :header/value "30 Day Guarantee"
                         :body/value   "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
                        {:icon/symbol  [:svg/worry-free {:class  "fill-p-color"
                                                         :width  "35px"
                                                         :height "36px"}]
                         :header/value "100% Virgin Hair"
                         :body/value   "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
                        {:icon/symbol  [:svg/mirror {:class  "fill-p-color"
                                                     :width  "30px"
                                                     :height "34px"}]
                         :header/value "Certified Stylists"
                         :body/value   "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]}
      {:layer/type                  :shop-quote-img
       :quote/dsk-ucare-id          "3208fac6-c974-4c80-8e88-3244ee50226b"
       :quote/mob-ucare-id          "befce648-98b6-45a2-90f0-6199119bfffb"
       :quote/text                  "You deserve quality extensions & exceptional service without the unreasonable price tag."
       :quote/primary-attribution   "— Diishan Imira"
       :quote/secondary-attribution "CEO of Mayvenn"}
      (merge {:layer/type :shop-contact} layered/shop-contact-query)
      {:layer/type             :sticky-footer
       :layer/id               "sticky-footer-get-started"
       :sticky/content         "It’s true, we are paying for your install! "
       :cta/label              "Get started"
       :cta/navigation-message [events/navigate-adventure-find-your-stylist]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))
