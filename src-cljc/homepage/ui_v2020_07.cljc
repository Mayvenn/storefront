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
            [homepage.ui.zip-explanation :as zip-explanation]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent template
  [{:keys [lp-data
           phone-consult-cta
           contact-us
           diishan
           faq
           guarantees
           hashtag-mayvenn-hair
           hero
           shopping-categories
           zip-explanation]} _ _]
  [:div
   (c/build layered/component lp-data nil)
   (when hero (c/build hero/organism hero))

   (when shopping-categories (c/build shopping-categories/organism shopping-categories))

   (when hashtag-mayvenn-hair (c/build hashtag-mayvenn-hair/organism hashtag-mayvenn-hair))
   (when zip-explanation (c/build zip-explanation/organism zip-explanation))
   (when faq (c/build faq/organism faq))
   (when guarantees (c/build guarantees/organism guarantees))
   (when diishan (c/build diishan/organism diishan))
   (when contact-us (c/build contact-us/organism contact-us))])

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
  {:list/boxes
   (conj
    (->> categories
         (filter :homepage.ui-v2022-09/order)
         (sort-by ::order)
         (mapv
          (fn category->box
            [{:keys [page/slug copy/title catalog/category-id homepage.ui-v2022-09/image-id]}]
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
   [{:contact-us.contact-method/uri         (ui/sms-url "346-49")
     :contact-us.contact-method/svg-symbol  [:svg/icon-sms {:height 51
                                                            :width  56}]
     :contact-us.contact-method/title       "Live Chat"
     :contact-us.contact-method/copy        "Text: 346-49"
     :contact-us.contact-method/legal-copy  "Message & data rates may apply. Message frequency varies."
     :contact-us.contact-method/legal-links [{:copy   "terms"
                                              :target [e/navigate-content-sms]}
                                             {:copy   "privacy policy"
                                              :target [e/navigate-content-privacy]}]}
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
