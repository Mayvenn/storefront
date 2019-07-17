(ns adventure.faq
  (:require [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(defn query
  [data]
  {:expanded-index (get-in data keypaths/faq-expanded-section)})

(defn free-install-query
  [data]
  (merge (query data)
         {:sections   [{:title      "Who is going to do my hair?",
                        :paragraphs ["Mayvenn Certified Stylists have been chosen because of their professionalism, skillset, and client ratings. We’ve got a network of licensed stylists across the country who are all committed to providing you with amazing service and quality hair extensions."]}
                       {:title      "What kind of hair do you offer?"
                        :paragraphs ["We’ve got top of the line virgin hair in 8 different textures. In the event that you’d like to switch it up, we have pre-colored options available as well. The best part? All of our hair is quality-guaranteed."]}
                       {:title      "What happens after I choose my hair?"
                        :paragraphs ["After you choose your hair, you’ll be matched with a Certified Stylist of your choice. You can see the stylist’s work and their salon’s location. We’ll help you book an appointment and answer any questions you may have."]}
                       {:title      "Is Mayvenn Install really a better deal?"
                        :paragraphs ["Yes! It’s basically hair and service for the price of one. You can buy any 3 bundles, closures and frontals from Mayvenn, and we’ll pay for you to get your hair installed by a local stylist. That means that you’re paying $0 for your next sew-in, with no catch!"]}
                       {:title      "How does this process actually work?"
                        :paragraphs ["It’s super simple — after you purchase your hair, we’ll send you a pre-paid voucher that you’ll use during your appointment. When your stylist scans it, they get paid instantly by Mayvenn."]}
                       {:title      "What if I want to get my hair done by another stylist? Can I still get the free install?"
                        :paragraphs ["You must get your hair done from a Certified Stylist in order to get your hair installed for free."]}]}))

(defn component [{:keys [expanded-index sections]}]
  [:div.px6.mx-auto.col-6-on-dt
   [:h2.center "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         (map (fn [{:keys [title paragraphs]}]
                              (accordion/section [:h6 title] paragraphs))
                            sections)}
    {:opts {:section-click-event events/faq-section-selected}})])

