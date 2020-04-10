(ns homepage.default-home
  (:require [adventure.components.layered :as layered]
            adventure.handlers
            adventure.keypaths
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.categories :as categories]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [clojure.string :as string]
            [homepage.ui.mayvenn-install :as mayvenn-install]
            [homepage.ui.wig-customization :as wig-customization]
            [homepage.ui.diishan :as diishan]
            [homepage.ui.guarantees :as guarantees]
            [homepage.ui.faq :as faq]
            [homepage.ui.mayvenn-hair :as mayvenn-hair]
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

(defcomponent template
  [{:keys [homepage-hero shop-hair mayvenn-install wig-customization
           sit-back-relax-hold-hair-high mayvenn-hair faq
           guarantees diishan]} _ opts]
  [:div
   ;; TODO move to homepage ns
   (component/build ui.M/hero (merge homepage-hero
                                     {:opts {:class     "block"
                                             :style     {:min-height "300px"}
                                             :data-test "hero-link"}}))
   (component/build layered/free-standard-shipping-bar {})
   (component/build layered/box-grid shop-hair)
   horizontal-rule-atom
   (when mayvenn-install
     [:div
      (component/build mayvenn-install/organism mayvenn-install)
      horizontal-rule-atom])
   (when wig-customization
     [:div
      (component/build wig-customization/organism wig-customization)
      horizontal-rule-atom])
   (divider-atom "2d3a98e3-b49a-4f0f-9340-828d12865315")
   (when-let [{:keys [shop-text-block unified-image-block unified-text-block]}
              sit-back-relax-hold-hair-high]
     [:div
      (component/build layered/shop-text-block shop-text-block)
      (component/build layered/unified-image-block unified-image-block)
      (component/build layered/unified-text-block unified-text-block)])

   (divider-atom "7e91271e-874c-4303-bc8a-00c8babb0d77")
   (component/build mayvenn-hair/organism mayvenn-hair)
   (component/build faq/organism faq)
   (component/build guarantees/organism guarantees)
   (component/build diishan/organism diishan)])

(defn homepage-hero-query
  "TODO homepage hero query is reused and complected

  decomplect:
  - handles extraction from cms
  - schematizes accoring to reused componet"
  [cms]
  (let [hero-content
        (or
         (some-> cms :homepage :unified :hero)
         ;; TODO handle cms failure fallback
         {})]
    (homepage-hero/query hero-content)))

;; For shop hair
(defn ^:private category-image
  [{:keys [image-id filename alt]}]
  ;; Assumptions: 2 up on mobile, 3 up on tablet/desktop, within a .container. Does not account for 1px border.
  [:img.block.col-12
   {:src     (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename)
    :src-set (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/640x/-/quality/lightest/" filename " 640w, "
                  "//ucarecdn.com/" image-id "/-/format/auto/-/resize/544x/-/quality/lightest/" filename " 544w")
    :sizes   "100%"
    :alt     alt}])

;; For shop hair
(defn ^:private category->box-grid-item
  [{:keys [page/slug
           copy/title
           catalog/category-id
           unified.home/image-id]}]
  (let [[first-word & rest-of-words] (string/split title #" ")]
    {:id      slug
     :target  [e/navigate-category {:page/slug           slug
                                    :catalog/category-id category-id}]
     :content (list
               (category-image {:filename slug
                                :alt      slug
                                :image-id image-id})
               [:div.absolute.white.proxima.title-2.bottom-0.shout.ml3.mb2-on-mb.mb4-on-tb-dt
                [:span first-word [:br] (string/join " " rest-of-words)]])}))

(defn shop-hair-query
  "TODO make this solely about data"
  [categories]
  (let [categories-for-homepage (->> categories
                                     (filter :unified.home/order)
                                     (sort-by :unified.home/order)
                                     (mapv category->box-grid-item))]
    {:title        "Shop Hair"
     :aspect-ratio {:height 210 :width 171}
     :items       (conj categories-for-homepage
                        ;; TODO use the inner query here
                        {:id      "need-inspiration"
                         :target  [e/navigate-shop-by-look {:album-keyword :look}]
                         :content [:div.p2.flex.justify-around.items-center.bg-pale-purple.dark-gray.inherit-color.canela.title-2.center
                                   {:style {:height "100%"
                                            :width  "100%"}}
                                   "Need Inspiration?" [:br] "Try shop by look."]})}))

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

(def sit-back-relax-hold-hair-high-query
  {:shop-text-block
   {:header/value [:div.py1.shout
                   ;; NOTE: this is a design exception
                   [:div.title-1.proxima {:style {:font-size "34px"}} "Sit back and"]
                   [:div.light.canela.mt2.mb4 {:style {:font-size "54px"}} "relax"]]
    :body/value   "We’ve rounded up the best stylists in the country so you can be sure your hair is in really, really good hands."
    :cta/value    "Learn more"
    :cta/id       "info-certified-stylists"
    :cta/target   [e/navigate-info-certified-stylists]}
   :unified-image-block
   {:ucare?    true
    :mob-uuid  "7a58ec9e-11b2-447c-8230-de70798decf8"
    :dsk-uuid  "7a58ec9e-11b2-447c-8230-de70798decf8"
    :file-name "who-shop-hair"}
   :unified-text-block
   {:header/value [:div.py1.shout
                   ;; NOTE: this is a design exception
                   [:div.title-1.proxima {:style {:font-size "19px"}} "Hold your hair"]
                   [:div.canela.mt2.mb4 {:style {:font-size "72px"}} "high"]]
    :body/value   "With the highest industry standards in mind, we have curated a wide variety of textures and colors for you to choose from."
    :cta/id       "info-about-our-hair"
    :cta/value    "shop hair"
    :cta/target   [e/navigate-category {:page/slug           "human-hair-bundles"
                                        :catalog/category-id "27"}]}})

(defn mayvenn-hair-query
  [ugc-collection current-nav-event]
  {:looks/images (->> ugc-collection
                      :free-install-mayvenn
                      :looks
                      (mapv (partial contentful/look->homepage-social-card
                                     current-nav-event
                                     :free-install-mayvenn)))
   :cta/id       "see-more-looks"
   :cta/label    "see more looks"
   :cta/target   [e/navigate-shop-by-look {:album-keyword :look}]})

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
        video             (get-in app-state adventure.keypaths/adventure-home-video)
        ugc-collection    (get-in app-state k/cms-ugc-collection)
        current-nav-event (get-in app-state k/navigation-event)
        expanded-index    (get-in app-state k/faq-expanded-section)
        shop?             (= "shop" (get-in app-state k/store-slug))
        menu              (get-in app-state k/store-service-menu)]
    (component/build
     template
     (cond->
         {:homepage-hero                 (homepage-hero-query cms)
          :shop-hair                     (shop-hair-query categories)
          :sit-back-relax-hold-hair-high sit-back-relax-hold-hair-high-query
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


