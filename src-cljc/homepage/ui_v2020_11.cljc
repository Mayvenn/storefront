(ns homepage.ui-v2020-11
  (:require [homepage.ui.atoms :as A]
            [homepage.ui.contact-us :as contact-us]
            [homepage.ui.faq :as faq]
            [homepage.ui.hero :as hero]
            [adventure.components.layered :as l]
            [storefront.component :as c]
            [storefront.components.homepage-hero :as homepage-hero]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]))

(c/defcomponent template
  [{:keys [contact-us
           faq
           hero
           title-with-subtitle
           square-image-and-text-diptychs]} _ _]
  [:div
   (c/build hero/organism-without-shipping-bar hero)
   (A/divider-atom "7e91271e-874c-4303-bc8a-00c8babb0d77")

   (c/build l/title-with-subtitle title-with-subtitle)
   (for [diptych square-image-and-text-diptychs]
     (c/build l/square-image-and-text-diptych diptych))
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

(defn faq-query
  [faq expanded-index]
  {:faq/expanded-index expanded-index
   :list/sections      (for [{:keys [question answer]} (:question-answers faq)]
                         {:faq/title   (:text question)
                          :faq/content answer})})

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
