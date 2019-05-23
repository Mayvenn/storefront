(ns adventure.informational.about-our-hair
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
             :photo/uuid "5ab5e0a7-50f7-4d4e-ab74-dac695d72f68"}
            {:layer/type             :find-out-more
             :header/value           "We believe that quality should be accessible for all."
             :body/value             "Our bundles, closures, and frontals are crafted with the highest industry standards and come in a variety of textures and colors. The best part? All of our products are quality-guaranteed."
             :cta/value              "Get started"
             :cta/navigation-message (layered/->freeinstall-nav-event "toadventurehomepageourhairpage"
                                                                      "/adv/install-type")}
            {:layer/type      :bulleted-explainer
             :header/value    "About Our Hair"
             :subheader/value "An overview"
             :bullets         [{:icon/uuid    "8787e30c-2879-4a43-8d01-9d6790575084"
                                :icon/width   "53"
                                :header/value "Quality-Guaranteed"
                                :body/value   "Wear it, dye it, even cut it. If you're not in love with your hair, we'll exchange it within 30 days."}
                               {:icon/uuid    "e02561dd-c294-43b7-bb33-c40bfabea518"
                                :icon/width   "34"
                                :header/value "Crafted to Last"
                                :body/value   "Our hair is gently steam processed and double machine-wefted to prevent shedding, tangling, and increase longevity."}
                               {:icon/uuid    "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
                                :icon/width   "30"
                                :header/value "Care & Support"
                                :body/value   "We have a team of customer service professionals dedicated to answering your questions and giving care advice."}]}
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
            {:layer/type             :sticky-footer
             :cta/navigation-message (layered/->freeinstall-nav-event "toadventurehomepageourhairpage"
                                                                      "/adv/install-type")}]})

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-about-our-hair
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
