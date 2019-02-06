(ns adventure.faq
  (:require [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(def ^:private faq-section-copy
  (let [phone-link (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]
    [(accordion/section [:h6 "How does this all work? How do I get a free install?"]
                        ["It’s easy! Mayvenn will pay your stylist directly for your install."
                         " Just purchase 3 bundles or more (frontals and closures count as bundles)"
                         " and use code FREEINSTALL at checkout. You’ll receive a voucher as soon"
                         " as your order ships. Schedule an appointment with your Mayvenn stylist,"
                         " and present the voucher to them at the appointment."
                         " Your stylist will receive the full payment for your install"
                         " immediately after the voucher has been scanned!"])
     (accordion/section [:h6 "What's included in the install?"]
                        ["Typically a full install includes a wash, braid down, and simple styling."
                         " Service details may vary so it would be best to check with your stylist"
                         " to confirm what is included."])
     (accordion/section [:h6 "How does the 30 day guarantee work?"]
                        ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]
                        ["EXCHANGES" [:br] "Wear it, dye it, even cut it! If you're not satified with your"
                         " hair, we'll exchange it within 30 days of purchase. Our customer service"
                         " team is ready to answer any questions you may have. Give us a call:"
                         phone-link]
                        ["RETURNS" [:br] "If you are not completely happy with your Mayvenn hair"
                         " before it is installed, we will refund your purchase if the"
                         " bundle is unopened and the hair is in its original condition."
                         " Give us a call to start your return:"
                         phone-link])
     (accordion/section [:h6 "Who is going to do my hair?"]
                        ["The free-install offer is only valid with a Certified Mayvenn Stylist."
                         " If you are unsure if your stylist is participating in the free-install offer,"
                         " you can simply ask them or contact Mayvenn customer service: "
                         phone-link
                         [:br]
                         [:br]
                         "Our stylists specialize in sew-in installs with leave-out, closures,"
                         " frontals, and 360 frontals so you can rest assured that we have a stylist"
                         " to help you achieve the look you want."])
     (accordion/section [:h6 "What if I want to get my hair done by another stylist?"
                         " Can I still get the free install?"]
                        ["You must get your hair done from a Mayvenn stylist in"
                         " order to get your hair installed for free."])
     (accordion/section [:h6 "Why should I order hair from Mayvenn?"]
                        ["Mayvenn is a Black owned company that offers 100% virgin hair."
                         " Our Virgin and Dyed Virgin hair can be found in a variety of textures from"
                         " straight to curly. Virgin hair starts at $55 per bundle."
                         " All orders are eligible for free shipping and backed by our 30 Day"
                         " Guarantee."])]))

(defn query
  [data]
  {:expanded-index (get-in data keypaths/faq-expanded-section)})

(defn component [{:keys [expanded-index]}]
  [:div.px6.mx-auto
   [:h2.center "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         faq-section-copy}
    {:opts {:section-click-event events/faq-section-selected}})])
