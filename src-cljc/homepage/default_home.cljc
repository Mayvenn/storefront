(ns homepage.default-home
  (:require [adventure.components.layered :as layered]
            adventure.keypaths
            [clojure.string :as string]
            [homepage.ui.diishan :as diishan]
            [homepage.ui.faq :as faq]
            [homepage.ui.guarantees :as guarantees]
            [homepage.ui.mayvenn-hair :as mayvenn-hair]
            [homepage.ui.mayvenn-install :as mayvenn-install]
            [homepage.ui.quality-hair :as quality-hair]
            [homepage.ui.quality-image :as quality-image]
            [homepage.ui.quality-stylists :as quality-stylists]
            [homepage.ui.shopping-categories :as shopping-categories]
            [homepage.ui.wig-customization :as wig-customization]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [clojure.string :as string]
            [ui.molecules :as ui.M]))

;; TODO can this use ucare-img utilities / picture tag?
(defn ^:private divider-atom
  [ucare-id]
  (let [uri (str "url('//ucarecdn.com/" ucare-id "/-/resize/x24/')")]
    [:div {:style {:background-image    uri
                   :background-position "center"
                   :background-repeat   "repeat-x"
                   :height              "24px"}}]))

(def ^:private horizontal-rule-atom
  [:div.border-bottom.border-width-1.hide-on-dt
   {:style {:border-color "#EEEEEE"}}])

(c/defcomponent template
  [{:keys [diishan
           faq
           guarantees
           quality-hair
           homepage-hero
           mayvenn-hair
           mayvenn-install
           quality-image
           shopping-categories
           quality-stylists
           wig-customization]} _ _]
  [:div
   ;; TODO move to homepage ns
   (c/build ui.M/hero (merge homepage-hero
                             {:opts {:class     "block"
                                     :style     {:min-height "300px"}
                                     :data-test "hero-link"}}))
   (c/build layered/free-standard-shipping-bar {})
   (c/build shopping-categories/organism shopping-categories)
   horizontal-rule-atom
   (when mayvenn-install
     [:div
      (c/build mayvenn-install/organism mayvenn-install)
      horizontal-rule-atom])
   (when wig-customization
     [:div
      (c/build wig-customization/organism wig-customization)
      horizontal-rule-atom])
   (divider-atom "2d3a98e3-b49a-4f0f-9340-828d12865315")
   (c/build quality-stylists/organism quality-stylists)

   (c/build quality-image/molecule quality-image)
   (c/build quality-hair/organism quality-hair)

   (divider-atom "7e91271e-874c-4303-bc8a-00c8babb0d77")
   (c/build mayvenn-hair/organism mayvenn-hair)
   (c/build faq/organism faq)
   (c/build guarantees/organism guarantees)
   (c/build diishan/organism diishan)])

(defn homepage-hero-query
  "TODO homepage hero query is reused and complected

  decomplect:
  - handles extraction from cms
  - schematizes according to reused component"
  [cms]
  (let [hero-content
        (or
         (some-> cms :homepage :unified :hero)
         ;; TODO handle cms failure fallback
         {})]
    (homepage-hero/query hero-content)))

(defn shopping-categories-query
  [categories]
  {:shopping-categories.title/primary "Shop Hair"
   :list/boxes
   (conj
    ;; TODO rename category keys for consistency
    (->> categories (filter :unified.home/order) (sort-by :unified.home/order)
         (mapv
          (fn category->box
            [{:keys [page/slug copy/title catalog/category-id unified.home/image-id]}]
            {:shopping-categories.box/id       slug
             :shopping-categories.box/target   [e/navigate-category
                                                {:page/slug           slug
                                                 :catalog/category-id category-id}]
             :shopping-categories.box/ucare-id image-id
             :shopping-categories.box/label    title})))
    {:shopping-categories.box/id       "need-inspiration"
     :shopping-categories.box/target   [e/navigate-shop-by-look {:album-keyword :look}]
     :shopping-categories.box/label    ["Need Inspiration?" "Try shop by look."]})})

(def mayvenn-install-query
  {:mayvenn-install.title/primary   "Free Mayvenn Install"
   :mayvenn-install.title/secondary (str "Purchase 3+ bundles or closure and get a mayvenn install "
                                         "valued up to $200 for absolutely free!")
   :mayvenn-install.image/ucare-id  "625b63a0-5724-4a57-ad79-c9e7a72a7f5b"
   :mayvenn-install.list/primary    "What's included?"
   :list/bullets                    ["Shampoo" "Braid down" "Sew-in and style"]
   :mayvenn-install.cta/id          "browse-stylists"
   :mayvenn-install.cta/value       "Browse Stylists"
   :mayvenn-install.cta/target      [e/navigate-adventure-find-your-stylist]})

(def wig-customization-query
  {:wig-customization.title/primary   "Free Wig Customization"
   :wig-customization.title/secondary (str "Purchase any of our virgin lace front wigs or virgin 360 "
                                           "lace wigs and we’ll customize it for free.")
   :wig-customization.image/ucare-id  "beaa9641-35dd-4811-8f57-a10481c5132d"
   :wig-customization.list/primary    "What's included?"
   :list/bullets                      ["Bleaching the knots"
                                       "Tinting the lace"
                                       "Cutting the lace"
                                       "Customize your hairline"]
   :wig-customization.cta/id          "show-wigs"
   :wig-customization.cta/value       "Shop Wigs"
   :wig-customization.cta/target      [e/navigate-category
                                       {:catalog/category-id "13"
                                        :page/slug           "wigs"
                                        :query-params        {:family
                                                              (string/join
                                                               categories/query-param-separator
                                                               ["360-wigs" "lace-front-wigs"])}}]})

(def quality-image-query
  {:ucare?    true
   :mob-uuid  "7a58ec9e-11b2-447c-8230-de70798decf8"
   :dsk-uuid  "484cc089-8aa1-4199-af07-05d72271d3a3"
   :file-name "who-shop-hair"})

(def quality-hair-query
  {:quality-hair.title/primary   "Hold your hair"
   :quality-hair.title/secondary "high"
   :quality-hair.body/primary    "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
   :quality-hair.cta/id          "info-about-our-hair"
   :quality-hair.cta/label       "shop hair"
   :quality-hair.cta/target      [e/navigate-category {:page/slug           "human-hair-bundles"
                                                    :catalog/category-id "27"}]})

(def quality-stylists-query
  {:quality-stylists.title/primary   "Sit back and"
   :quality-stylists.title/secondary "relax"
   :quality-stylists.body/primary    "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
   :quality-stylists.cta/label       "Learn more"
   :quality-stylists.cta/id          "info-certified-stylists"
   :quality-stylists.cta/target      [e/navigate-info-certified-stylists]})

(defn mayvenn-hair-query
  [ugc-collection current-nav-event]
  {:mayvenn-hair.looks/images (->> ugc-collection
                      :free-install-mayvenn
                      :looks
                      (mapv (partial contentful/look->homepage-social-card
                                     current-nav-event
                                     :free-install-mayvenn)))
   :mayvenn-hair.cta/id       "see-more-looks"
   :mayvenn-hair.cta/label    "see more looks"
   :mayvenn-hair.cta/target   [e/navigate-shop-by-look {:album-keyword :look}]})

(defn faq-query
  [expanded-index]
  {:faq/expanded-index expanded-index
   :list/sections
   [{:faq/title      "Who is going to do my hair?",
     :faq/paragraphs ["Mayvenn Certified Stylists have been chosen because of their professionalism, skillset, and client ratings. We’ve got a network of licensed stylists across the country who are all committed to providing you with amazing service and quality hair extensions."]}
    {:faq/title      "What kind of hair do you offer?"
     :faq/paragraphs ["We’ve got top of the line virgin hair in 8 different textures. In the event that you’d like to switch it up, we have pre-colored options available as well. The best part? All of our hair is quality-guaranteed."]}
    {:faq/title      "What happens after I choose my hair?"
     :faq/paragraphs ["After you choose your hair, you’ll be matched with a Certified Stylist of your choice. You can see the stylist’s work and their salon’s location. We’ll help you book an appointment and answer any questions you may have."]}
    {:faq/title      "Is Mayvenn Install really a better deal?"
     :faq/paragraphs ["Yes! It’s basically hair and service for the price of one. You can buy any 3 bundles, closures and frontals from Mayvenn, and we’ll pay for you to get your hair installed by a local stylist. That means that you’re paying $0 for your next sew-in, with no catch!"]}
    {:faq/title      "How does this process actually work?"
     :faq/paragraphs ["It’s super simple — after you purchase your hair, we’ll send you a pre-paid voucher that you’ll use during your appointment. When your stylist scans it, they get paid instantly by Mayvenn."]}
    {:faq/title      "What if I want to get my hair done by another stylist? Can I still get the Mayvenn Install?"
     :faq/paragraphs ["You must get your hair done from a Certified Stylist in order to get your hair installed for free."]}]})

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

;;;; TODO -> model.stylists

(def ^:private wig-customizations
  [:specialty-wig-customization])
(def ^:private mayvenn-installs
  [:specialty-sew-in-360-frontal
   :specialty-sew-in-closure
   :specialty-sew-in-frontal
   :specialty-sew-in-leave-out])
(def ^:private services
  (into mayvenn-installs wig-customizations))

(defn ^:private offers?
  [services menu]
  (->> ((apply juxt services) menu) (some identity) boolean))

;;;;

(defn page
  [app-state]
  (let [cms               (get-in app-state k/cms)
        categories        (get-in app-state k/categories)
        ;; TODO ?
        video             (get-in app-state adventure.keypaths/adventure-home-video)
        ugc-collection    (get-in app-state k/cms-ugc-collection)
        current-nav-event (get-in app-state k/navigation-event)
        expanded-index    (get-in app-state k/faq-expanded-section)
        shop?             (= "shop" (get-in app-state k/store-slug))
        menu              (get-in app-state k/store-service-menu)]
    (c/build
     template
     (cond->
         {:homepage-hero                 (homepage-hero-query cms)
          :shopping-categories           (shopping-categories-query categories)
          :quality-hair                  quality-hair-query
          :quality-image                 quality-image-query
          :quality-stylists              quality-stylists-query
          :mayvenn-hair                  (mayvenn-hair-query ugc-collection
                                                             current-nav-event)
          :guarantees                    guarantees-query
          :diishan                       diishan-query}

       (or shop? (offers? mayvenn-installs menu))
       (merge {:mayvenn-install mayvenn-install-query})

       (or shop? (offers? wig-customizations menu))
       (merge {:wig-customization wig-customization-query})

       (or shop? (offers? services menu))
       (merge {:faq (faq-query expanded-index)})))))
