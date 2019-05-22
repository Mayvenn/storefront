(ns adventure.informational.certified-stylists
  (:require storefront.keypaths
            [storefront.accessors.pixlee :as pixlee]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [adventure.components.layered :as layered]
            #?@(:cljs [[storefront.hooks.pixlee :as pixlee.hook]])))

(defn query
  [data]
  {:layers [{:layer/type :hero
             :photo/uuid "fed7867a-4b32-44d2-b095-81274620c8eb"}
            {:layer/type   :find-out-more
             :header/value "Our Certified Stylists are the best of the best."
             :body/value   (str "Our Certified Stylists are the best in your area. "
                                "They’re chosen because of their top-rated reviews, professionalism, and amazing work.")
             :cta/value    "Get started"
             :cta/event    (layered/->freeinstall-nav-event "toadventurehomepagestylistinfopage"
                                                    "/adv/install-type")}

            {:layer/type      :bulleted-explainer
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
             :images          (pixlee/images-in-album
                               (get-in data storefront.keypaths/ugc)
                               :free-install-mayvenn)}
            {:layer/type     :faq
             :expanded-index (get-in data storefront.keypaths/faq-expanded-section)
             :sections       [{:title      "Who is going to do my hair?",
                               :paragraphs ["Mayvenn Certified Stylists have been chosen because of their professionalism, skillset, and client ratings. We’ve got a network of licensed stylists across the country who are all committed to providing you with amazing service and quality hair extensions."]}
                              {:title      "What kind of hair do you offer?",
                               :paragraphs ["We’ve got top of the line virgin hair in 8 different textures. In the event that you’d like to switch it up, we have pre-colored options available as well. The best part? All of our hair is quality-guaranteed."]}
                              {:title      "What happens after I choose my hair?",
                               :paragraphs ["After you choose your hair, you’ll be matched with a Certified Stylist of your choice. You can see the stylist’s work and their salon’s location. We’ll help you book an appointment and answer any questions you may have."]}
                              {:title      "Is Mayvenn Install really a better deal?",
                               :paragraphs ["Yes! It’s basically hair and service for the price of one. You can buy any 3 bundles (closures and frontals included) from Mayvenn, and we’ll pay for you to get your hair installed by a local stylist. That means that you’re paying $0 for your next sew-in, with no catch!"]}
                              {:title      "How does this process actually work?",
                               :paragraphs ["It’s super simple — after you purchase your hair, we’ll send you a pre-paid voucher that you’ll use during your appointment. When your stylist scans it, they get paid instantly by Mayvenn."]}
                              {:title      "What if I want to get my hair done by another stylist? Can I still get the free install?",
                               :paragraphs ["You must get your hair done from a Certified Stylist in order to get your hair installed for free."]}] }
            {:layer/type :contact}
            {:layer/type :sticky-footer
             :cta/event  (layered/->freeinstall-nav-event "toadventurehomepagehairinfopage"
                                                  "/adv/install-type")}]})

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-certified-stylists
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
