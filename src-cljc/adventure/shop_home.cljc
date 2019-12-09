(ns adventure.shop-home
  (:require [adventure.components.layered :as layered]
            [storefront.components.ui :as ui]
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
       :cta/value              "shop hair"
       :cta/id                 "info-about-our-hair"
       :cta/navigation-message [events/navigate-info-about-our-hair]
       :divider-img            "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"}
      {:layer/type      :ugc
       :header/value    "#MayvennFreeInstall"
       :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
       :images          (mapv (partial contentful/look->homepage-social-card
                                       current-nav-event
                                       :free-install-mayvenn)
                              (->> cms-ugc-collection :free-install-mayvenn :looks))}
      (merge {:layer/type :faq} (faq/free-install-query data))
      (when shop? {:layer/type :escape-hatch})
      {:layer/type      :bulleted-explainer
       :layer/id        "more-than-a-hair-company"
       :header/value    "Mayvenn is More than a Hair Company"
       :subheader/value "It's a movement"
       :bullets         [{:icon/uuid    "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                          :icon/width   "29"
                          :header/value "Top-Notch Customer Service"
                          :body/value   "Our team is made up of hair experts ready to help you by phone, text, and email."}
                         {:icon/uuid    "8787e30c-2879-4a43-8d01-9d6790575084"
                          :icon/width   "52"
                          :header/value "30 Day Guarantee"
                          :body/value   "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
                         {:icon/uuid    "e02561dd-c294-43b7-bb33-c40bfabea518"
                          :icon/width   "35"
                          :header/value "100% Virgin Hair"
                          :body/value   "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
                         {:icon/uuid    "5a04ea88-b0f8-416b-a380-1da0baa8a114"
                          :icon/width   "35"
                          :header/value "Certified Stylists"
                          :body/value   "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]}
      {:layer/type             :homepage-were-changing-the-game
       :cta/navigation-message [events/navigate-home
                                {:query-params {:video "we-are-mayvenn"}}]}
      {:layer/type :contact}
      {:layer/type             :sticky-footer
       :cta/navigation-message [events/navigate-adventure-match-stylist]}]}))

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/control-open-shop-escape-hatch
  [_ _ _ _ _]
  (messages/handle-message events/control-menu-expand-hamburger
                           {:keypath storefront.keypaths/menu-expanded})
  #?(:cljs (scroll/snap-to-top)))
