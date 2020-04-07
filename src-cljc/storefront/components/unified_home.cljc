(ns storefront.components.unified-home
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
            [storefront.keypaths :as storefront.keypaths]
            [clojure.string :as string]))

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
                        {:url   (ui/phone-url "1 (855) 287-6868")
                         :svg   (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                :height 57
                                                :width  57})
                         :title "Call Us"
                         :copy  "1 (855) 287-6868"}
                        {:url   (ui/email-url "help@mayvenn.com")
                         :svg   (svg/icon-email {:height 39
                                                 :width  56})
                         :title "Email Us"
                         :copy  "help@mayvenn.com"}]})

(defn ^:private category-image
  [{:keys [image-id filename alt]}]
  (component/html
   ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
   [:img.block.col-12
    {:src     (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename)
     :src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w, "
                   "//ucarecdn.com/" image-id "/-/format/auto/-/resize/544x/-/quality/lightest/" filename " 544w")
     :sizes   "100%"
     :alt     alt}]))

(defn ^:private category->box-grid-item
  [{:keys [page/slug
           copy/title
           catalog/category-id
           unified.home/image-id]}]
  (let [[first-word & rest-of-words] (string/split title #" ")]
    {:id      slug
     :target  [events/navigate-category {:page/slug           slug
                                         :catalog/category-id category-id}]
     :content (list
               (category-image {:filename slug
                                :alt      slug
                                :image-id image-id})
               [:div.absolute.white.proxima.title-2.bottom-0.shout.ml3.mb2-on-mb.mb4-on-tb-dt
                [:span first-word [:br] (string/join " " rest-of-words)]])}))

(defn query
  [data]
  (let [cms-homepage-hero  (some-> data (get-in storefront.keypaths/cms-homepage) :unified :hero)
        cms-ugc-collection (get-in data storefront.keypaths/cms-ugc-collection)
        current-nav-event  (get-in data storefront.keypaths/navigation-event)]
    {:layers
     [(merge {:layer/type :hero}
             (assoc (homepage-hero/query cms-homepage-hero)
                    :file-name "free-install-hero"))
      {:layer/type :free-standard-shipping-bar}
      {:layer/type   :box-grid
       :title        "Shop Hair"
       :aspect-ratio {:height 210 :width 171}
       :items        (conj (->> (get-in data storefront.keypaths/categories)
                                (filter :unified.home/order)
                                (sort-by :unified.home/order)
                                (mapv category->box-grid-item))
                           {:id      "need-inspiration"
                            :target  [events/navigate-shop-by-look {:album-keyword :look}]
                            :content [:div.p2.flex.justify-around.items-center.bg-pale-purple.dark-gray.inherit-color.canela.title-2.center
                                      {:style {:height "100%"
                                               :width  "100%"}}
                                      "Need Inspiration?" [:br] "Try shop by look."]})}
      {:layer/type :horizontal-rule}
      {:layer/type   :unified-text-block
       :header/value "Free Mayvenn Install"
       :body/value   (str "Purchase 3+ bundles or closure and get a mayvenn install "

                          "valued up to $200 for absolutely free!")}
      {:layer/type :unified-image-block
       :unboxed?   true
       :ucare?     true

       :mob-uuid  "625b63a0-5724-4a57-ad79-c9e7a72a7f5b"
       :dsk-uuid  "625b63a0-5724-4a57-ad79-c9e7a72a7f5b"
       :file-name "who-shop-hair"}
      {:layer/type   :unified-framed-checklist
       :header/value "What's included?"
       :unboxed?     true
       :bullets      ["Shampoo"
                      "Braid down"
                      "Sew-in and style"]
       :cta/button?  true
       :cta/value    "Browse Stylists"
       :cta/id       "browse-stylists"
       :cta/target   [events/navigate-adventure-find-your-stylist]}
      {:layer/type :horizontal-rule}
      {:layer/type      :video-overlay
       :close-nav-event events/navigate-home
       :video           (get-in data adventure.keypaths/adventure-home-video)}
      {:layer/type   :unified-text-block
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
      {:layer/type   :unified-text-block
       :header/value [:div.py1.shout
                      ;; NOTE: this is a design exception
                      [:div.title-1.proxima {:style {:font-size "19px"}} "Hold your hair"]
                      [:div.canela.mt2.mb4 {:style {:font-size "72px"}} "high"]]
       :body/value   "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
       :cta/id       "info-about-our-hair"
       :cta/value    "shop hair"
       :cta/target   [events/navigate-category {:page/slug           "mayvenn-install"
                                                :catalog/category-id "23"}]}
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
