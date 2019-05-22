(ns adventure.informational.how-it-works
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
             :photo/uuid "99a8ccca-b4fe-42d2-8ac1-7ebf8e4f6559"}
            {:layer/type   :find-out-more
             :header/value "We’re offering hair + service for the price of one"
             :body/value   "We know that quality bundles can be expensive. That’s why when you buy our hair, we’re paying for your install appointment."
             :cta/value    "Get started"
             :cta/message  (layered/->freeinstall-nav-event "toadventurehomepagehowitworkspage"
                                                            "/adv/install-type")}
            {:layer/type      :bulleted-explainer
             :header/value    "How it Works"
             :subheader/value "In 3 easy steps"
             :bullets         [{:icon/uuid    "3d2b326c-7773-4672-827e-f13dedfae15a"
                                :icon/width   "22"
                                :header/value "1. Choose a Mayvenn Certified Stylist"
                                :body/value   "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."
                                :cta/value    "Learn more"
                                :cta/message  [events/navigate-info-certified-stylists nil]}
                               {:icon/uuid    "08e9d3d8-6f3d-4b3c-bc46-3590175a9a4d"
                                :icon/width   "24"
                                :header/value "2. Buy ANY Three Bundles or More"
                                :body/value   "This includes closures, frontals, and 360 frontals. Risk free - your virgin hair and service are covered by our 30 day guarantee."
                                :cta/value    "Learn more"
                                :cta/message  [events/navigate-info-about-our-hair nil]}
                               {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
                                :icon/width   "27"
                                :header/value "3. Schedule Your Appointment"
                                :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}]}
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
            {:layer/type  :sticky-footer
             :cta/message (layered/->freeinstall-nav-event "toadventurehomepagehowitworkspage"
                                                          "/adv/install-type")}]})

(defn built-component
  [data opts]
  (component/build layered/component (query data) opts))

(defmethod effects/perform-effects events/navigate-info-how-it-works
  [_ _ args prev-app-state app-state]
  #?(:cljs (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn)))
