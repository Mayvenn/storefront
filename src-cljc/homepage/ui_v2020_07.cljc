(ns homepage.ui-v2020-07
  (:require [homepage.ui.atoms :as A]
            [homepage.ui.contact-us :as contact-us]
            [homepage.ui.diishan :as diishan]
            [homepage.ui.faq :as faq]
            [homepage.ui.guarantees :as guarantees]
            [homepage.ui.hashtag-mayvenn-hair :as hashtag-mayvenn-hair]
            [homepage.ui.hero :as hero]
            [homepage.ui.services-section :as services-section]
            [homepage.ui.shopping-categories :as shopping-categories]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent template
  [{:keys [contact-us
           diishan
           faq
           guarantees
           hashtag-mayvenn-hair
           hero
           mayvenn-install
           quality-hair
           quality-stylists
           shopping-categories
           a-la-carte-services]} _ _]
  [:div
   (c/build hero/organism hero)
   (c/build shopping-categories/organism shopping-categories)

   A/horizontal-rule-atom

   (c/build services-section/organism a-la-carte-services)
   (c/build services-section/organism mayvenn-install)

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

(def mayvenn-install-query
  {:services-section.title/primary        "Free Mayvenn Services"
   :services-section.title/secondary      "Purchase qualifying Mayvenn hair and receive service for free!"
   :services-section.image/ucare-id       "625b63a0-5724-4a57-ad79-c9e7a72a7f5b"
   :services-section/orientation          "image-last"
   :services-section.cta/id               "browse-mayvenn-services"
   :services-section.cta/value            "Browse Mayvenn Services"
   :services-section.cta/target           [e/navigate-category {:page/slug           "free-mayvenn-services"
                                                                :catalog/category-id "31"}]
   :services-section.secondary-cta/id     "browse-stylists"
   :services-section.secondary-cta/value  "Browse Stylists"
   :services-section.secondary-cta/target [e/flow--stylist-matching--began]})

(def a-la-carte-query
  {:services-section.title/primary        "À la carte Services"
   :services-section.title/secondary      "No hair purchase needed! Now you can book à la carte salon services with your favorite Mayvenn Stylists!"
   :services-section.image/ucare-id       "8f14c17b-ffef-4178-8915-640573a8bf3a"
   :services-section/orientation          "image-first"
   :services-section.cta/id               "browse-services-section"
   :services-section.cta/value            "Browse À la carte Services"
   :services-section.cta/target           [e/navigate-category {:page/slug           "a-la-carte-salon-services"
                                                                :catalog/category-id "35"}]
   :services-section.secondary-cta/id     "browse-stylists"
   :services-section.secondary-cta/value  "Browse Stylists"
   :services-section.secondary-cta/target [e/flow--stylist-matching--began]})

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

;; TODO svg ns returns components full of undiffable data
(def guarantees-query
  {:list/icons
   [{:guarantees.icon/image (svg/heart {:class  "fill-p-color"
                                        :width  "32px"
                                        :height "29px"})
     :guarantees.icon/title "Top-Notch Customer Service"
     :guarantees.icon/body  "Our team is made up of hair experts ready to help you by phone, text, and email."}
    {:guarantees.icon/image (svg/calendar {:class  "fill-p-color"
                                           :width  "30px"
                                           :height "33px"})
     :guarantees.icon/title "30 Day Guarantee"
     :guarantees.icon/body  "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."}
    {:guarantees.icon/image (svg/worry-free {:class  "fill-p-color"
                                             :width  "35px"
                                             :height "36px"})
     :guarantees.icon/title "100% Virgin Hair"
     :guarantees.icon/body  "Our hair is gently steam-processed and can last up to a year. Available in 8 textures and 5 shades."}
    {:guarantees.icon/image (svg/mirror {:class  "fill-p-color"
                                         :width  "30px"
                                         :height "34px"})
     :guarantees.icon/title "Certified Stylists"
     :guarantees.icon/body  "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."}]})

(def diishan-query
  {:diishan.quote/text            "You deserve quality extensions & exceptional service without the unreasonable price tag."
   :diishan.attribution/ucare-ids {:desktop "3208fac6-c974-4c80-8e88-3244ee50226b"
                                   :mobile  "befce648-98b6-45a2-90f0-6199119bfffb" }
   :diishan.attribution/primary   "— Diishan Imira"
   :diishan.attribution/secondary "CEO of Mayvenn"})

;; TODO svg ns returns components full of undiffable data
(def contact-us-query
  {:contact-us.title/primary   "Contact Us"
   :contact-us.title/secondary "We're here to help"
   :contact-us.body/primary    "Have Questions?"
   :list/contact-methods
   [{:contact-us.contact-method/uri   (ui/sms-url "346-49")
     :contact-us.contact-method/svg   (svg/icon-sms {:height 51
                                                     :width  56})
     :contact-us.contact-method/title "Live Chat"
     :contact-us.contact-method/copy  "Text: 346-49"}
    {:contact-us.contact-method/uri   (ui/phone-url "1 (855) 287-6868")
     :contact-us.contact-method/svg   (svg/icon-call {:class  "bg-white fill-black stroke-black circle"
                                                      :height 57
                                                      :width  57})
     :contact-us.contact-method/title "Call Us"
     :contact-us.contact-method/copy  "1 (855) 287-6868"}
    {:contact-us.contact-method/uri   (ui/email-url "help@mayvenn.com")
     :contact-us.contact-method/svg   (svg/icon-email {:height 39
                                                       :width  56})
     :contact-us.contact-method/title "Email Us"
     :contact-us.contact-method/copy  "help@mayvenn.com"}]})
