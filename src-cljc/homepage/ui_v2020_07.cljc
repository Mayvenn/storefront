(ns homepage.ui-v2020-07
  (:require adventure.keypaths
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as k]
            [adventure.components.layered :as layered]
            [homepage.ui.atoms :as A]
            [homepage.ui.contact-us :as contact-us]
            [homepage.ui.diishan :as diishan]
            [homepage.ui.faq :as faq]
            [homepage.ui.guarantees :as guarantees]
            [homepage.ui.hashtag-mayvenn-hair :as hashtag-mayvenn-hair]
            [homepage.ui.hero :as hero]
            [homepage.ui.shopping-categories :as shopping-categories]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent install-specific-organism
  [{:keys [buy-three-bundles
           shop-framed-checklist
           video-overlay
           how-it-works
           sit-back-and-relax
           who-shop-hair
           hold-hair-high] :as data} _ _]
  (when data
    [:div
     (c/build layered/shop-text-block buy-three-bundles)
     (c/build layered/shop-framed-checklist shop-framed-checklist)
     (c/build layered/video-overlay video-overlay)
     (c/build layered/shop-bulleted-explainer how-it-works)
     (c/build layered/shop-text-block sit-back-and-relax)
     (c/build layered/image-block who-shop-hair)
     (c/build layered/shop-text-block hold-hair-high)]))

(c/defcomponent template
  [{:keys [contact-us
           diishan
           faq
           guarantees
           hashtag-mayvenn-hair
           hero
           shopping-categories
           install-specific-query]} _ _]
  [:div
   (c/build hero/organism hero)

   (c/build shopping-categories/organism shopping-categories)
   (c/build install-specific-organism install-specific-query )
   A/horizontal-rule-atom

   (A/divider-atom "7e91271e-874c-4303-bc8a-00c8babb0d77")

   (c/build hashtag-mayvenn-hair/organism hashtag-mayvenn-hair)
   (c/build faq/organism faq)
   (c/build guarantees/organism guarantees)
   (c/build diishan/organism diishan)
   (c/build contact-us/organism contact-us)])

(defn hero-query
  "TODO homepage hero query is reused and complected

  decomplect:
  - handles extraction from cms
  - schematizes according to reused component"
  [cms experience]
  (let [hero-content
        (or
         (some-> cms :homepage experience :hero)
         ;; TODO handle cms failure fallback
         {})]
    (assoc-in (homepage-hero/query hero-content)
              [:opts :data-test]
              "hero-link")))

(defn shopping-categories-query
  [categories]
  {:shopping-categories.title/primary "Shop Hair"
   :list/boxes
   (conj
    (->> categories
         (filter ::order)
         (sort-by ::order)
         (mapv
          (fn category->box
            [{:keys [page/slug copy/title catalog/category-id]
              ::keys [image-id]}]
            {:shopping-categories.box/id       slug
             :shopping-categories.box/target   [e/navigate-category
                                                {:page/slug           slug
                                                 :catalog/category-id category-id}]
             :shopping-categories.box/ucare-id image-id
             :shopping-categories.box/label    title})))
    {:shopping-categories.box/id        "need-inspiration"
     :shopping-categories.box/target    [e/navigate-shop-by-look {:album-keyword :look}]
     :shopping-categories.box/alt-label ["Need Inspiration?" "Try shop by look."]})})

(defn hashtag-mayvenn-hair-query
  [ugc]
  (let [images (->> ugc :free-install-mayvenn :looks
                    (mapv (partial contentful/look->homepage-social-card
                                   :free-install-mayvenn)))]
    {:hashtag-mayvenn-hair.looks/images images
     :hashtag-mayvenn-hair.cta/id       "see-more-looks"
     :hashtag-mayvenn-hair.cta/label    "see more looks"
     :hashtag-mayvenn-hair.cta/target   [e/navigate-shop-by-look {:album-keyword :look}]}))

(defn faq-query
  [faq expanded-index]
  {:faq/expanded-index expanded-index
   :list/sections      (for [{:keys [question answer]} (:question-answers faq)]
                         {:faq/title   (:text question)
                          :faq/content answer})})

(def guarantees-query
  {:list/icons
   [{:guarantees.icon/symbol [:svg/heart {:class  "fill-p-color"
                                          :width  "32px"
                                          :height "29px"}]
     :guarantees.icon/title  "Top-Notch Customer Service"
     :guarantees.icon/body   "Our team is made up of hair experts ready to help you by phone, text, and email."}
    {:guarantees.icon/symbol [:svg/calendar {:class  "fill-p-color"
                                             :width  "30px"
                                             :height "33px"}]
     :guarantees.icon/title  "30 Day Guarantee"
     :guarantees.icon/body   "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
    {:guarantees.icon/symbol [:svg/worry-free {:class  "fill-p-color"
                                               :width  "35px"
                                               :height "36px"}]
     :guarantees.icon/title "100% Virgin Hair"
     :guarantees.icon/body  "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
    {:guarantees.icon/symbol [:svg/mirror {:class  "fill-p-color"
                                           :width  "30px"
                                           :height "34px"}]

     :guarantees.icon/title "Certified Stylists"
     :guarantees.icon/body  "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]})

(def diishan-query
  {:diishan.quote/text            "You deserve quality extensions & exceptional service without the unreasonable price tag."
   :diishan.attribution/ucare-ids {:desktop "3208fac6-c974-4c80-8e88-3244ee50226b"
                                   :mobile  "befce648-98b6-45a2-90f0-6199119bfffb" }
   :diishan.attribution/primary   "— Diishan Imira"
   :diishan.attribution/secondary "CEO of Mayvenn"})

(def contact-us-query
  {:contact-us.title/primary   "Contact Us"
   :contact-us.title/secondary "We're here to help"
   :contact-us.body/primary    "Have Questions?"
   :list/contact-methods
   [{:contact-us.contact-method/uri        (ui/sms-url "346-49")
     :contact-us.contact-method/svg-symbol [:svg/icon-sms {:height 51
                                                           :width  56}]
     :contact-us.contact-method/title      "Live Chat"
     :contact-us.contact-method/copy       "Text: 346-49"}
    {:contact-us.contact-method/uri        (ui/phone-url "1 (855) 287-6868")
     :contact-us.contact-method/svg-symbol [:svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                            :height 57
                                                            :width  57}]
     :contact-us.contact-method/title      "Call Us"
     :contact-us.contact-method/copy       "1 (855) 287-6868"}
    {:contact-us.contact-method/uri        (ui/email-url "help@mayvenn.com")
     :contact-us.contact-method/svg-symbol [:svg/icon-email {:height 39
                                                             :width  56}]
     :contact-us.contact-method/title      "Email Us"
     :contact-us.contact-method/copy       "help@mayvenn.com"}]})

(defn install-specific-query [app-state]
  {:buy-three-bundles     (merge {:layer/type   :shop-text-block
                                  :header/value "Buy 3 bundles and we’ll pay for your install"
                                  :cta/button?  true}
                                 (if (experiments/shopping-quiz-unified-fi? app-state)
                                   {:cta/value  "Start Hair Quiz"
                                    :cta/id     "homepage-take-hair-quiz"
                                    :cta/target [e/navigate-shopping-quiz-unified-freeinstall-intro {:query-params {:location "homepage_cta"}}]}
                                   {:cta/value  "Browse Stylists"
                                    :cta/id     "browse-stylist"
                                    :cta/target [e/navigate-adventure-find-your-stylist]}))
   :shop-framed-checklist {:layer/type   :shop-framed-checklist
                           :header/value "What's included?"
                           :bullets      ["Shampoo"
                                          "Braid down"
                                          "Sew-in and style"
                                          "Paid for by Mayvenn"]
                           :divider-img  "url('//ucarecdn.com/2d3a98e3-b49a-4f0f-9340-828d12865315/-/resize/x24/')"}
   :video-overlay         {:layer/type      :video-overlay
                           :close-nav-event (get-in app-state k/navigation-event)
                           :video           (get-in app-state adventure.keypaths/adventure-home-video)}

   :how-it-works       {:layer/type     :shop-bulleted-explainer
                        :layer/id       "heres-how-it-works"
                        :title/value    ["You buy the hair,"
                                         "we cover the install."]
                        :subtitle/value ["Here's how it works."]
                        :bullets        [{:title/value "Pick Your Dream Look"
                                          :body/value  "Have a vision in mind? We’ve got the hair for it. Otherwise, peruse our site for inspiration to find your next look."}
                                         {:title/value ["Select A Mayvenn" ui/hyphen "Certified Stylist"]
                                          :body/value  "We’ve hand-picked thousands of talented stylists around the country. We’ll cover the cost of your salon appointment with them when you buy 3 or more bundles."}
                                         {:title/value "Schedule Your Appointment"
                                          :body/value  "We’ll connect you with your stylist to set up your install. Then, we’ll send you a prepaid voucher to cover the cost of service."}]
                        :cta/id         "watch-video"
                        :cta/value      "Watch Video"
                        :cta/icon       [:svg/play-video {:width  "30px"
                                                          :height "30px"}]
                        :cta/target     [(get-in app-state k/navigation-event)
                                         {:query-params {:video "free-install"}}]}
   :sit-back-and-relax {:layer/type         :shop-text-block
                        ;; NOTE: this is a design exception
                        :big-header/content [{:text "Sit back and" :attrs {:style {:font-size "34px"}}}
                                             {:text "relax" :attrs {:style {:font-size "54px"}}}]
                        :body/value         "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
                        :cta/value          "Learn more"
                        :cta/id             "info-certified-stylists"
                        :cta/target         [e/navigate-info-certified-stylists]}
   :who-shop-hair      {:layer/type :image-block
                        :ucare?     true
                        :mob-uuid   "bd8888d3-9d1a-4944-a840-2863b50ba5d6"
                        :dsk-uuid   "36bd1978-b3e2-457a-9c8d-303661f57924"
                        :file-name  "who-shop-hair"}


   :hold-hair-high {:layer/type         :shop-text-block
                    ;; NOTE: this is a design exception
                    :big-header/content [{:text "Hold your hair" :attrs {:style {:font-size "19px"}}}
                                         {:text "high" :attrs {:style {:font-size "72px"}}}]
                    :body/value         "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
                    :cta/id             "info-about-our-hair"
                    :cta/value          "shop hair"
                    :cta/target         [e/navigate-category {:page/slug           "mayvenn-install"
                                                              :catalog/category-id "23"}]}})
