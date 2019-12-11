(ns adventure.shop-home
  (:require [adventure.components.layered :as layered]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            adventure.handlers ;; Needed for its defmethods
            [storefront.accessors.contentful :as contentful]
            [storefront.keypaths :as storefront.keypaths]
            [adventure.keypaths :as keypaths]
            [adventure.faq :as faq]
            #?@(:cljs [goog.dom
                       goog.style
                       goog.events
                       [storefront.browser.scroll :as scroll]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.events :as events]
            [storefront.effects :as effects]))

(defn query
  [data]
  (let [shop?                                          (= "shop" (get-in data storefront.keypaths/store-slug))
        cms-homepage-hero                              (some-> data (get-in storefront.keypaths/cms-homepage) :shop :hero)
        cms-ugc-collection                             (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event                              (get-in data storefront.keypaths/navigation-event)
        [cms-hero-event cms-hero-args :as routed-path] (-> cms-homepage-hero :path (routes/navigation-message-for nil (when shop? "shop")))]
    {:layers
     [(merge {:layer/type      :hero
              :photo/file-name "free-install-hero"}
             {:photo/alt         (-> cms-homepage-hero :alt)
              :photo/mob-url     (-> cms-homepage-hero :mobile :file :url)
              :photo/dsk-url     (-> cms-homepage-hero :desktop :file :url)
              :photo/cta-message (-> cms-homepage-hero :path)}
             (if (or (nil? cms-hero-event) (= events/navigate-not-found cms-hero-event))
               {:buttons [[{:navigation-message [events/navigate-adventure-match-stylist]
                            :data-test          "adventure-home-choice-get-started"}
                           "Browse Stylists"]]}
               {:photo/navigation-message routed-path}))
      {:layer/type :free-standard-shipping-bar}
      {:layer/type             :shop-text-block
       :header/value           "Buy 3 bundles and we’ll pay for your service"
       :cta/value              "Browse Stylists"
       :cta/button?            true
       :cta/id                 "browse-stylist"
       :cta/navigation-message [events/navigate-adventure-match-stylist]} ; TODO
      {:layer/type   :shop-framed-checklist
       :header/value "What's included?"
       :bullets      ["Shampoo"
                      "Braid down"
                      "Sew-in and style"
                      "Paid for by Mayvenn"]
       :divider-img  "url('//ucarecdn.com/2d3a98e3-b49a-4f0f-9340-828d12865315/-/resize/x24/')"}
      {:layer/type             :shop-bulleted-explainer
       :layer/id               "heres-how-it-works"
       :title/value            ["You buy the hair,"
                                "we cover the service."]
       :subtitle/value         ["Here's how it works."]
       :bullets                [{:title/value "Pick Your Dream Look"
                                 :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                                {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                                 :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                                {:title/value "Schedule Your Appointment"
                                 :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]
       :cta/value              "Watch Video"
       :cta/img                "/images/play-video.svg"
       :cta/navigation-message [events/navigate-home {:query-params {:video "free-install"}}]}
      {:layer/type             :shop-text-block
       :header/value           [:div.title-1.proxima.py1.shout
                                "Sit back and" [:br]
                                ;; TODO this is a design exception
                                [:span.title-1.shout.canela "relax"]]
       :body/value             "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
       :cta/value              "Learn more"
       :cta/id                 "info-certified-stylists"
       :cta/navigation-message [events/navigate-info-certified-stylists]}
      {:layer/type      :image-block
       :photo/mob-uuid  "a6a607e6-aeb4-4b61-8bc7-60fd17d15abe"
       :photo/dsk-uuid  "f2d82c41-2051-47d8-86c5-1c82568e324d"
       :photo/file-name "who-shop-hair"}
      {:layer/type             :shop-text-block
       :header/value           [:div.title-1.proxima.py1.shout
                                "Hold your hair" [:br]
                                ;; TODO this is a design exception
                                [:span.title-1.shout.canela "high"]]
       :body/value             "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
       :cta/id                 "info-about-our-hair"
       :cta/value              "shop hair"
       :cta/navigation-message [events/navigate-info-about-our-hair]
       :divider-img            "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"}
      {:layer/type             :shop-ugc
       :header/value           "#MayvennFreeInstall"
       :images                 (->> cms-ugc-collection
                                    :free-install-mayvenn
                                    :looks
                                    (mapv (partial contentful/look->homepage-social-card
                                                   current-nav-event
                                                   :free-install-mayvenn)))
       :cta/id                 "see-more-looks"
       :cta/value              "see more looks"
       :cta/navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
      (merge {:layer/type :faq}
             (faq/free-install-query data))
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
       :quote/img                   "befce648-98b6-45a2-90f0-6199119bfffb"
       :quote/text                  "You deserve quality extensions & exceptional service without the unreasonable price tag."
       :quote/primary-attribution   "— Diishan Imira"
       :quote/secondary-attribution "CEO of Mayvenn"}
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
                             :copy  "help@mayvenn.com"}]}
      {:layer/type             :sticky-footer
       :cta/navigation-message [events/navigate-adventure-match-stylist]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))
