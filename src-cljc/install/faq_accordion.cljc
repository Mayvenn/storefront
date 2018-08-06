(ns install.faq-accordion
  (:require [storefront.components.accordion :as accordion]
            [storefront.components.ui :as ui]))

(def free-install-sections
  [(accordion/section "How does the 30 day guarantee work?"
                      ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]
                      ["EXCHANGES"
                       [:br]
                       "Wear it, dye it, even flat iron it. If you do not love your"
                       " Mayvenn hair we will exchange it within 30 days of purchase."
                       " Just call us:"
                       (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]
                      ["RETURNS"
                       [:br]
                       "If you are not completely happy with your Mayvenn hair"
                       " before it is installed, we will refund your purchase if the"
                       " bundle is unopened and the hair is in its original condition."
                       " Just call us:"
                       (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
   (accordion/section "How does this all work? How do I get a FREE install?"
                      ["It’s easy! Mayvenn will pay a stylist to install your hair."
                       " Just purchase 3"
                       " bundles or more (frontals and closures count as bundles) and use code"
                       " FREEINSTALL at checkout. Then, Mayvenn will reach out to you via email"
                       " or text to schedule an install with one of our Mayvenn Certified"
                       " Stylists, proudly based in Fayetteville, NC."])
   (accordion/section "Who is going to do my hair?"
                      ["Our Mayvenn Certified Stylists are among the best in Fayetteville, with an"
                       " average of 15 years of professional experience. Each of our stylists specializes"
                       " in sew-in installs with leave-out, closures, frontals, and 360 frontals so you"
                       " can feel confident that we have a stylist to achieve the look you want. Have a"
                       " specific idea in mind? Just let us know and we’ll match you with a stylist's"
                       " best suited to meet your needs."])
   (accordion/section "What if I want to get my hair done by my own stylist? Can I still get the free install?"
                      ["No, you must get your hair done from one of Mayvenn’s Certified Stylists in"
                       " order to get your hair installed for free. Our stylists are licensed and"
                       " experienced - the best in Fayetteville!"])
   (accordion/section "Why should I order hair from Mayvenn?"
                      ["Mayvenn hair is 100% human. Our Virgin and Dyed Virgin hair"
                       " can be found in a variety of textures from"
                       " straight to curly. Virgin hair starts at $55 per bundle."
                       " All orders are eligible for free shipping and backed by our 30 Day"
                       " Guarantee."])])
