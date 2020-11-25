(ns homepage.ui-v2020-11
  (:require [homepage.ui.atoms :as A]
            [homepage.ui.contact-us :as contact-us]
            [homepage.ui.faq :as faq]
            [homepage.ui.square-image-and-text-diptych :as square-image-and-text-diptych]
            [homepage.ui.triptychs :as triptychs]
            [homepage.ui.hero :as hero]
            [adventure.components.layered :as l]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent x-organism
  [{:sit-back-and-relax.title/keys
    [primary secondary]
    items :list/items} _ _]
  [:div.bg-deep-purple.white.center.pj3-on-mb.pyj3-on-tb-dt
   [:div.shout.title-2.proxima.mb3 secondary]
   [:div.shout.title-1.canela.mt2.pbj3-on-mb.pbj2-on-tb-dt
    ;; NOTE: this is a design exception
    {:style {:font-size "56px"}} primary]
   [:div.flex-on-tb-dt.justify-around.mx-auto.px2-on-tb-dt
    {:style {:max-width "750px"}}
    (interpose
     [:div.pbj3-on-mb]
     (for [{:sit-back-and-relax.items/keys [caption icon]} items]
       [:div.flex.flex-column.justify-start.items-center {:key (str icon)}
        [:div.flex.items-top {:style {:height "50px" :width "50px"}}
         (svg/symbolic->html [icon {:height "42px" :width "42px" :class "fill-mayvenn-pink"}])]
        [:div.shout.title-3.proxima.mt1.mx-auto
         {:style {:line-height "18px"
                  :width       "150px"}} caption]]))]])

(def purple-pink-divider-id
  "937451d3-070b-4f2c-b839-4f5b621ef661")
(c/defcomponent template
  [{:keys [contact-us
           faq
           hero
           title-with-subtitle
           square-image-and-text-diptychs
           portrait-triptychs
           sit-back-and-relax]} _ _]
  [:div
   (c/build hero/organism-without-shipping-bar hero)
   (A/divider-atom "7e91271e-874c-4303-bc8a-00c8babb0d77")
   (c/build l/title-with-subtitle title-with-subtitle)
   (into [:div.bg-refresh-gray.pbj3-on-mb.pbj3-on-tb-dt]
         (for [d square-image-and-text-diptychs]
           (c/build square-image-and-text-diptych/organism d)))
   (c/build triptychs/organism portrait-triptychs)
   (A/divider-atom purple-pink-divider-id)
   (c/build x-organism sit-back-and-relax)
   (c/build faq/organism faq)
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

(def title-with-subtitle-query
  {:primary   "Revolutionize Your Salon Experience"
   :secondary "Get amazing hair and book appointments with top stylists near you. All in one place, for an affordable price."})

(def square-image-and-text-diptychs
  [{:id                    "cta-human-hair-bundles"
    :ucare-id              "48fb1b15-c50c-41e2-8c16-a80d815df0fd"
    :primary               "luxury hair + service, all in one place"
    :secondary             "Hair + Service for the price of one. Get the best value for your money with our hair plus Free Service pairings."
    :target                [e/navigate-category {:page/slug           "human-hair-bundles"
                                                 :catalog/category-id "27"}]
    :link-text             "Pick your dream look"
    :pic-right-on-desktop? false}
   {:id                    "cta-salon-services"
    :ucare-id              "045c8b5b-1230-41f1-8b4e-0a712b802ec9"
    :primary               "take a seat on your throne "
    :secondary             "We know that a woman’s hair is her crown - and the means to flaunt it, however she chooses, should be easily attainable.  "
    :target                [e/navigate-category {:page/slug           "salon-services"
                                                 :catalog/category-id "30"}]
    :link-text             "View services"
    :pic-right-on-desktop? true}
   {:id                    "browse-stylists"
    :ucare-id              "82a47ad2-7e42-4594-b011-b6f497e55885"
    :primary               "your wish is our command"
    :secondary             "Our licensed stylists are chosen for their commitment to hair health and total client satisfaction."
    :target                [e/navigate-adventure-find-your-stylist nil]
    :link-text             "Browse stylists near you"
    :pic-right-on-desktop? false}])

(def portrait-triptychs
  {:title    "Need Inspiration? Look No Further."
   :subtitle "#mayvennmade"
   :target   [e/navigate-shop-by-look {:album-keyword :look}]
   :data     [{:id                         "triptych-1"
               :large-pic-right-on-mobile? false
               :image-ids                  ["eb70fa1b-449f-4415-b038-4fe25952cf43"
                                            "ea1ccf14-276e-4da1-a449-c5cfb6055bf0"
                                            "54f0180b-ce09-4f14-a5f0-c8e5c7a3d5b9"]}
              {:id                         "triptych-2"
               :large-pic-right-on-mobile? true
               :image-ids                  ["4b8c6087-96f3-41ac-b12a-63e06695ee2d"
                                            "b026b457-50bb-4548-8c29-22038a9a5ad1"
                                            "d5a56ee3-86e3-4932-b832-2b98b497a6f7"]}]})

(defn faq-query
  [faq expanded-index]
  {:faq/expanded-index expanded-index
   :list/sections      (for [{:keys [question answer]} (:question-answers faq)]
                         {:faq/title   (:text question)
                          :faq/content answer})})
;; TODO: set min-ish height for images to prevent content reflow

(def sit-back-and-relax
  {:sit-back-and-relax.title/primary   "Relax"
   :sit-back-and-relax.title/secondary "Sit Back And"
   :list/items
   [{:sit-back-and-relax.items/icon    :svg/shaded-shipping-package
     :sit-back-and-relax.items/caption "Fast, Free Shipping"}
    {:sit-back-and-relax.items/icon    :svg/customer-service-representative
     :sit-back-and-relax.items/caption "Easy Returns & Exchanges"}
    {:sit-back-and-relax.items/icon    :svg/heart
     :sit-back-and-relax.items/caption "Enhanced Safety Standards"}]})

;; TODO svg ns returns components full of undiffable data
(def contact-us-query
  {:contact-us.title/primary   "Contact Us"
   :contact-us.title/secondary "Have Questions?"
   :contact-us.body/primary    "We're here to help"
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
