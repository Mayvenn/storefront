(ns adventure.informational.certified-stylists
  (:require [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [storefront.accessors.contentful :as contentful]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            storefront.keypaths))

(defn query
  [data]
  {:layers [{:layer/type :hero
             :file-name  "certified-stylists-hero"
             :mob-uuid   "0d06e8a8-123b-4265-b988-e7d55bddd7f0"
             :dsk-uuid   "3cd59dc1-8f94-4ee5-a62e-d91b5aa6c97b"
             :ucare?     true}
            {:layer/type             :find-out-more
             :header/value           "Our Certified Stylists are the best of the best."
             :body/value             (str "Our Certified Stylists are the best in your area. "
                                          "They’re chosen because of their top-rated reviews, professionalism, and amazing work.")
             :cta/value              "Get started"
             :cta/navigation-message [events/navigate-adventure-match-stylist nil]}

            {:layer/type      :shop-bulleted-explainer
             :header/value    "About Our Certified Stylists"
             :subheader/value "An overview"

             :bullets [{:icon/uuid    "6f63157c-dc3a-4bbb-abcf-e03b08d6e102"
                        :icon/width   "27"
                        :header/value "Licensed Stylists"
                        :body/value   "Our certified stylists are licensed, regulated service practitioners in each state."}
                       {:icon/uuid    "deeaa11d-c48a-4657-8d01-f477d2ea18a5"
                        :icon/width   "24"
                        :header/value "Salon Professionals"
                        :body/value   (str "We believe that quality beauty should be accessible for all. "
                                           "Our stylists work in professional, clean salon spaces.")}
                       {:icon/uuid    "b862f042-ad8d-4230-8ce4-20059dd540d7"
                        :icon/width   "34"
                        :header/value "Top Ratings & Reviews"
                        :body/value   (str "From Yelp to client surveys, we ensure that our stylists adhere"
                                           " to a top-notch code of ethics and professionalism.")}]}
            {:layer/type      :ugc
             :header/value    "#MayvennFreeInstall"
             :subheader/value "Showcase your new look by tagging #MayvennFreeInstall"
             :images          (contentful/album-kw->homepage-social-cards
                               (get-in data storefront.keypaths/cms-ugc-collection)
                               (get-in data storefront.keypaths/navigation-event)
                               :free-install-mayvenn)}
            (merge {:layer/type :faq} (faq/free-install-query data))
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
                                   :copy  "help@mayvenn.com"}]}
            {:layer/type             :sticky-footer
             :layer/id               "sticky-footer-certified-stylists"
             :sticky/content         "It’s true, we are paying for your install! "
             :cta/label              "Get started"
             :cta/navigation-message [events/navigate-adventure-match-stylist]}]})

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

