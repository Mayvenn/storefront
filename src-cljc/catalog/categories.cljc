(ns catalog.categories
  (:require [clojure.string :as string]
            [catalog.keypaths]
            [storefront.events :as events]))

(def new-facet?
  ;; [<facet-slug> <option-slug>]
  #{[:hair/family "ready-wigs"]})

(defn copy [& sentences]
  (string/join " " sentences))

(defn category->seo [category-name description image-url]
  {:page/title            (str category-name " | Mayvenn")
   :opengraph/title       (str category-name " - Free shipping. Free 30 day returns. Made with 100% virgin human hair.")
   :page.meta/description description
   :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee, our"
                                category-name
                                "are the best quality products on the market and ships free!")
   ;; TODO make this a proper image and place under :selector/image-cases & images catalog
   :category/image-url    image-url
   :seo/sitemap           true})

(defn render-template
  "Evaluates a template with a variable->values map, ctx, to produce a string.

    > (render-template [\"hello, \" :person] {:person \"bob\"})
    ;; => \"hello, bob\"

  Rules of the template syntax:

    - strings        => literal values, returned as-is
    - keywords       => lookup the same keyword in ctx map. nil values are empty strings

  All parts are string concatenated together.
  "
  [tmpl ctx]
  (transduce (comp (map (fn [form]
                          (if (keyword? form)
                            (get ctx form)
                            form)))
                   (remove empty?))
             str
             tmpl))

(def texture-subsection-selectors
  [#:subsection{:title "Straight" :selector #:hair{:texture #{"straight"}}}
   #:subsection{:title "Yaki Straight" :selector #:hair{:texture #{"yaki-straight"}}}
   #:subsection{:title "Kinky Straight" :selector #:hair{:texture #{"kinky-straight"}}}
   #:subsection{:title "Body Wave" :selector #:hair{:texture #{"body-wave"}}}
   #:subsection{:title "Loose Wave" :selector #:hair{:texture #{"loose-wave"}}}
   #:subsection{:title "Water Wave" :selector #:hair{:texture #{"water-wave"}}}
   #:subsection{:title "Deep Wave" :selector #:hair{:texture #{"deep-wave"}}}
   #:subsection{:title "Curly" :selector #:hair{:texture #{"curly"}}}])

(def hair-extensions-templates
  {:page/title-template            [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Get the hair of your dreams with our "
                                    :computed/selected-facet-string
                                    " "
                                    :seo/title
                                    ". Featuring a thin, polyurethane"
                                    " weft that flawlessly blends with your own hair."]})

(def closures-templates
  {:page/title-template            [:computed/selected-facet-string " " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn's "
                                    :computed/selected-facet-string
                                    " "
                                    :seo/title
                                    " are beautifully crafted and provide a realistic part to"
                                    " close off any unit or install."]})

(def frontals-templates
  {:page/title-template            [:computed/selected-facet-string " " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn's "
                                    :computed/selected-facet-string
                                    " "
                                    :seo/title
                                    " mimic a natural hairline and offer versatile parting options"
                                    " to achieve your desired look."]})

(def texture-templates
  {:page/title-template            [:computed/selected-facet-string " " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn's "
                                    :computed/selected-facet-string
                                    " Human "
                                    :seo/title
                                    " are machine-wefted and made with virgin"
                                    " hair for unbeatable quality. Shop to achieve your desired look!"]})

(def wig-templates
  {:page/title-template            [:computed/selected-facet-string " " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn’s "
                                    :computed/selected-facet-string
                                    " "
                                    :seo/title
                                    " allow you to change up and achieve your desired look."
                                    " Shop our collection of virgin hair wigs today."]})

(def bundle-templates
  {:page/title-template            [:computed/selected-facet-string " Human " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn's "
                                    :computed/selected-facet-string
                                    " Human "
                                    :seo/title
                                    " are machine-wefted and made with virgin"
                                    " hair for unbeatable quality. Shop to achieve your desired look!"]})

(def closures
  [(merge {:catalog/category-id              "0"
           :page/icp?                        true
           :product-list/title               "Shop All Closures"
           :copy/title                       "Closures"
           :page/slug                        "virgin-closures"
           :seo/title                        "Virgin Hair Closures"
           :legacy/named-search-slug         "closures"
           :catalog/department               #{"hair"}
           :hair/family                      #{"closures"}
           :hair/color.process               #{"natural" "dyed"}
           :hair/source                      #{"virgin"}
           :category/tags                    #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :subcategories.old/ids            ["38" "39" "46"]
           :subcategories.old/layout         :list
           :subcategories/ids                ["38" "39" "46"]
           :subcategories/layout             :spotlights
           :selector/essentials              [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives               [:hair/origin :hair/texture :hair.closure/area :hair/color :hair/base-material]
           :selector/dimensions              [:hair/origin :hair/texture :hair.closure/area :hair/color :hair/base-material]
           :subsections/subsection-selectors [#:subsection{:title "Standard Lace Closures" :selector #:hair{:base-material #{"lace"}}}
                                              #:subsection{:title "Silk Closures" :selector #:hair{:base-material #{"silk"}}}
                                              #:subsection{:title "HD Lace Closures" :selector #:hair{:family #{"closures"} :base-material #{"hd-lace"}}}]
           :header/title                     "Virgin Hair Closures"
           :content-block/title              "Closures 101:"
           :content-block/header             "How to Choose Your Silk or Lace Closure"
           :content-block/summary            "Installed on the crown or side of your head, our 4x4 inch virgin human hair closures blend perfectly with Mayvenn bundles for a seamless low maintenance style. Before selecting your closure, consider these factors to make the best decision:"
           :content-block/type               :about-attributes
           :content-block/sections           [{:title "Lace Closure vs. Lace Frontal?"
                                               :body  [{:text "Both lace closures and"}
                                                       {:text "lace frontals" :nav-message [events/navigate-category {:page/slug "virgin-frontals" :catalog/category-id "1"}]}
                                                       {:text "mimic natural hairlines and close off bundles. Deciding between"}
                                                       {:text "closures vs. frontals" :external-uri "https://shop.mayvenn.com/blog/hair/closures-vs-frontals-explained/"}
                                                       {:text "really comes down to factors like budget, maintenance and which way you’re planning to style your hair."}]}
                                              {:title "Origin"
                                               :body  [{:text "The origin of your silk or lace closure determines the thickness and density of your look. From thick and smooth to light and bouncy, Mayvenn offers hair closures in Brazilian, Malaysian, Peruvian, and Indian origins."}]}
                                              {:title "Texture"
                                               :body  [{:text "With 8 different textures to choose from, Mayvenn’s closures give you the freedom to choose your dream look. From straight to curly and everything in between, select the perfect silk or lace closure to complement your bundles."}]}
                                              {:title "Want More Info on Silk and Lace Closures?"
                                               :body  [{:text "Closure or frontal? Silk or lace? Brazilian or Peruvian? So many choices. Thankfully, Customer Service is here to help. Our team is ready to answer all your questions about closures so connect with us today for more information!"}]}]


           :contentful/faq-id             :category-virgin-closures
           :desktop-menu/title            "Closures"
           :desktop-menu/order            2
           :homepage.ui-v2022-09/order    2
           :homepage.ui-v2022-09/image-id "e7d0859d-7e4f-42a5-abf0-12b3713264f7"
           :footer/order                  5
           :footer/title                  "Closures"
           :category/description          (copy "Protect your hair and complete your look with Mayvenn’s collection of 100% virgin hair closures."
                                                "Our closures are hand-tied to a lace or silk base to create a realistic part that flawlessly closes off any hairstyle.")}
          (category->seo "Closures"
                         (copy "Top off your look with the highest quality,"
                               "100% virgin hair closures that blend perfectly with"
                               "Mayvenn hair bundles to customize your style.")
                         "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")
          {:page/title "100% Virgin Hair Closures | Mayvenn"}
          closures-templates)
   (merge {:catalog/category-id              "38"
           :copy/title                       "Standard Lace Closures"
           :page/slug                        "lace-closures"
           :seo/title                        "Standard Lace Closures"
           :subcategory/image-id             "1ccd94c8-7e9b-4d92-bc74-908beb14e126"
           :subcategory/description          (copy "Lace closures mimic the color of your scalp for a natural hairline,"
                                                   "but are more transparent compared to silk bases.")
           :category/description             (copy "Lace closures mimic the color of your scalp for a natural hairline,"
                                                   "but are more transparent compared to silk bases.")
           :catalog/department               #{"hair"}
           :hair/family                      #{"closures"}
           :hair/base-material               #{"lace"}
           :hair/color.process               #{"natural" "dyed"}
           :hair/source                      #{"virgin"}
           :category/tags                    #{"closures-and-frontals"}
           :selector/essentials              [:catalog/department :hair/family :hair/color.process :hair/source :hair/base-material]
           :selector/electives               [:hair/origin :hair/texture :hair.closure/area :hair/color]
           :selector/dimensions              [:hair/origin :hair/texture :hair.closure/area :hair/color]
           :subsections/subsection-selectors texture-subsection-selectors
           :header/title                     "Lace Virgin Hair Closures"

           :contentful/faq-id :category-virgin-closures}
          (category->seo "Standard Lace Closures"
                         (copy "Get your dream hair with Mayvenn’s 100% human hair lace closures"
                               "that blend seamlessly with our bundles for a unique look that’s"
                               "all your own.")
                         "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")
          {:page/title "Standard Lace Closures: Human Hair Lace Closures | Mayvenn"}
          closures-templates)
   (merge {:catalog/category-id              "39"
           :copy/title                       "Silk Closures"
           :page/slug                        "silk-closures"
           :seo/title                        "Silk Closures"
           :subcategory/image-id             "36d00fa3-8491-4674-b2a5-7226192771d6"
           :subcategory/description          (copy "Virgin silk closures conceal the knots in the base to give you a beautiful,"
                                                   "natural-looking part without needing much customization.")
           :category/description             (copy "Virgin silk closures conceal the knots in the base to give you a beautiful,"
                                                   "natural-looking part without needing much customization.")
           :catalog/department               #{"hair"}
           :hair/family                      #{"closures"}
           :hair/base-material               #{"silk"}
           :hair/color.process               #{"natural" "dyed"}
           :hair/source                      #{"virgin"}
           :category/tags                    #{"closures-and-frontals"}
           :selector/essentials              [:catalog/department :hair/family :hair/color.process :hair/source :hair/base-material]
           :selector/electives               [:hair/origin :hair/texture :hair/color]
           :selector/dimensions              [:hair/origin :hair/texture :hair/color]
           :subsections/subsection-selectors texture-subsection-selectors
           :header/title                     "Silk Virgin Hair Closures"

           :contentful/faq-id :category-virgin-closures}
          (category->seo "Silk Closures"
                         (copy "Mayvenn’s 100% human hair silk closures blend in seamlessly with"
                               "our bundles for a unique look that’s all your own.")
                         "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")
          {:page/title "Silk Closures: Human Hair Silk Closures | Mayvenn"}
          closures-templates)
   (merge {:catalog/category-id      "10"
           :copy/title               "360 Lace Frontals"
           :page/slug                "360-frontals"
           :seo/title                "Virgin 360 Hair Frontals"
           :legacy/named-search-slug "360-frontals"
           :subcategory/image-id     "e6a42693-73a2-4cda-8ad8-bc16b7d8b5f4"

           :catalog/department               #{"hair"}
           :hair/family                      #{"360-frontals"}
           :hair/color.process               #{"natural" "dyed"}
           :hair/source                      #{"virgin"}
           :category/tags                    #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :subsections/subsection-selectors texture-subsection-selectors

           :contentful/faq-id    :category-360-frontals
           :selector/essentials  [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives   [:hair/origin :hair/texture]
           :selector/dimensions  [:hair/origin :hair/texture :hair/color]
           :category/description "Spanning your entire hairline, 360 lace frontals are the most versatile option to get your desired look, including up-dos."
           :header/title         "Virgin 360 Frontals"}
          (category->seo "360 Frontals"
                         (copy "Mayvenn’s virgin hair 360 frontals got you covered and come in"
                               "different variations such as Brazilian, Malaysian, straight, "
                               "and deep wave. Order today.")
                         "//ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/")
          {:page/title "360 Lace Frontals: Virgin Hair 360 Frontals | Mayvenn"}
          frontals-templates)
   (merge {:catalog/category-id              "1"
           :copy/title                       "Frontals"
           :page/slug                        "virgin-frontals"
           :seo/title                        "Hair Frontals"
           :legacy/named-search-slug         "frontals"
           :menu/hide?                       true
           :page/icp?                        true
           :category/description             (copy "Protect your tresses and finish off your style with 100% virgin hair"
                                                   "frontals that blend flawlessly with bundles and your natural hairline."
                                                   "Lace frontals give you the freedom to part your hair in any direction"
                                                   "with gorgeous texture and style.")
           :catalog/department               #{"hair"}
           :hair/family                      #{"frontals" "360-frontals"}
           :hair/color.process               #{"natural" "dyed"}
           :desktop-menu/title               "Frontals"
           :desktop-menu/order               3
           :homepage.ui-v2022-09/order       3
           :homepage.ui-v2022-09/image-id    "aae6c15c-00b3-43d0-be89-f4b9173212dc"
           :footer/order                     6
           :footer/title                     "Frontals"
           :hair/source                      #{"virgin"}
           :category/tags                    #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials              [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives               [:hair/origin :hair/texture :hair/color :hair/base-material]
           :selector/dimensions              [:hair/origin :hair/texture :hair/color :hair/base-material :hair/family]
           :product-list/title               "Shop All Frontals"
           :subcategories.old/ids            ["29" "10" "47"]
           :subcategories.old/layout         :list
           :subcategories/ids                ["29" "10" "47"]
           :subcategories/layout             :spotlights
           :subsections/subsection-selectors [#:subsection{:title "Standard Lace Frontals" :selector #:hair{:family #{"frontals"} :base-material #{"lace"}}}
                                              #:subsection{:title "360 frontals" :selector #:hair{:family #{"360-frontals"}}}
                                              #:subsection{:title "HD Lace Frontals" :selector #:hair{:family #{"frontals"} :base-material #{"hd-lace"}}}]

           :content-block/type     :about-attributes
           :content-block/title    "Frontals 101:"
           :content-block/header   "How to Choose Your Hair Frontal"
           :content-block/summary  "Frontals are a great solution if you want to conceal hair loss or achieve an up-do, but how do you choose the right unit? Check out different factors to look for when picking the perfect lace frontal:"
           :content-block/sections [{:title "Origin"
                                     :body  [{:text "The origin of your lace frontal will determine the thickness and density of your hair. From thick and smooth, to light and bouncy, Mayvenn offers hair frontals for every style: Brazilian, Malaysian, Peruvian, and Indian."}]}
                                    {:title "Texture"
                                     :body  [{:text "You want texture? We’ve got options. From straight to curly, deep to loose waves, our lace frontals are designed to beautifully blend with your hair bundles to create a versatile style for any occasion."}]}
                                    {:title "Need Help Choosing a Frontal?"
                                     :body  [{:text "Choosing between different lace frontals, textures, and colors can be overwhelming. Whether you’re investing in a frontal for the first time or are a pro, Mayvenn’s team is ready to answer your questions. Connect with us today to learn more!"}]}]}
          (category->seo "Frontals"
                         "Get your dream hair with Mayvenn’s 100% human hair frontals that blend seamlessly with our bundles for a unique look that’s all your own."
                         "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/")
          {:page/title "100% Human Hair Lace Frontals | Mayvenn"}
          frontals-templates)
   (merge {:catalog/category-id      "29"
           :copy/title               "Standard Lace Frontals"
           :page/slug                "virgin-lace-frontals"
           :seo/title                "Virgin Hair Frontals"
           :legacy/named-search-slug "frontals"
           :catalog/department       #{"hair"}
           :hair/family              #{"frontals"}
           :hair/color.process       #{"natural" "dyed"}
           :header/title             "Virgin Frontals"
           :hair/source              #{"virgin"}
           :hair/base-material       #{"lace"}
           :menu/title               "Virgin Frontals"
           :contentful/faq-id        :category-lace-frontals
           :category/tags            #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials      [:catalog/department :hair/family :hair/base-material]
           :selector/electives       [:hair/origin :hair/texture :hair/color]
           :selector/dimensions      [:hair/origin :hair/texture :hair/color]
           :subcategory/image-id     "47e29e97-821e-4c20-aeac-35eadc1a653d"
           :subcategory/description  "Our 13x4 inch virgin lace frontals stretch from ear-to-ear for a look that can be parted in any direction."
           :category/description     "Our 13x4 inch virgin lace frontals stretch from ear-to-ear for a look that can be parted in any direction."}
          (category->seo "Lace Frontals"
                         (copy "Mayvenn’s human hair lace frontals blend in seamlessly"
                               "with our bundles and come in a variety of different"
                               "combinations. Shop now to create your look.")
                         "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/")
          {:page/title            "Lace Frontals: Human Hair Lace Frontals | Mayvenn"
           :opengraph/title       "Lace Frontals - Free shipping. Free 30 day returns. Made with 100% human hair."
           :opengraph/description (copy "Mayvenn’s human hair lace frontals blend in seamlessly"
                                        "with our bundles and backed by our 30 Day Quality Guarantee,"
                                        "our human hair lace frontals are the best quality products on the market and ship free!")}
          frontals-templates)])

(def virgin-hair
  [(merge {:catalog/category-id           "2"
           :copy/title                    "Virgin Straight Bundles"
           :seo/title                     "Virgin Straight Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-straight"
           :legacy/named-search-slug      "straight"
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :catalog/department            #{"hair"}
           :hair/texture                  #{"straight"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/texture :hair/color.process :hair/source :hair/family]
           :selector/electives            [:hair/origin :hair/color],
           :selector/dimensions           [:hair/origin :hair/color],
           :category/description          (copy "For those who want it given to them"
                                                "straight, our collection of 100% virgin straight hair bundles"
                                                "is your go-to for a sleek look with"
                                                "minimal effort.")
           :icon                          "/images/categories/straight-icon.svg"
           :subcategory/title             "Straight"
           :header/title                  "Virgin Straight"}
          (category->seo "Straight Hair Bundles: Virgin Straight Sew-In Weaves"
                         (copy "Find the perfect virgin straight hair bundles from Mayvenn."
                               "We have everything from blonde to black, Brazilian to Indian."
                               "Browse our collection today!")
                         "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/")
          texture-templates)
   (merge {:catalog/category-id           "3"
           :copy/title                    "Virgin Yaki Straight Bundles"
           :seo/title                     "Virgin Yaki Straight Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-yaki-straight"
           :legacy/named-search-slug      "yaki-straight"
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :catalog/department            #{"hair"}
           :hair/texture                  #{"yaki-straight"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/texture :hair/color.process :hair/source :hair/family]
           :selector/electives            [:hair/origin]
           :selector/dimensions           [:hair/origin :hair/color]
           :category/description          (copy "Tired of having to break out the hot"
                                                "tools for a textured straight look? Our Yaki"
                                                "Straight hair buncdle collection is here to save your"
                                                "strands! Yaki Straight hair bundles match the rhythm of"
                                                "your natural hair that's been pressed straight or"
                                                "freshly relaxed. Your flat iron has been officially"
                                                "cancelled.")
           :icon                          "/images/categories/yaki-straight-icon.svg"
           :subcategory/title             "Yaki Straight"
           :header/title                  "Virgin Yaki Straight"}
          (category->seo "Yaki Straight Hair: Virgin Yaki Straight Bundles"
                         (copy "Looking for some top quality yaki straight hair bundles?"
                               "Mayvenn has you covered."
                               "Browse our collection of yaki straight virgin hair bundles here.")
                         "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/")
          texture-templates)
   (merge {:catalog/category-id           "4"
           :copy/title                    "Virgin Kinky Straight Bundles"
           :seo/title                     "Virgin Kinky Straight Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-kinky-straight"
           :legacy/named-search-slug      "kinky-straight"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"kinky-straight"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin :hair/color]
           :selector/dimensions           [:hair/origin :hair/color]
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :category/description          (copy "Blending is a breeze with our Kinky Straight"
                                                "hair bundle collection! Like a fresh blow out, the"
                                                "Kinky Straight hair texture moves freely and gives"
                                                "a naturally flawless look that mimics your own"
                                                "locks.")
           :icon                          "/images/categories/kinky-straight-icon.svg"
           :subcategory/title             "Kinky Straight"
           :header/title                  "Virgin Kinky Straight"}
          (category->seo "Kinky Straight Hair: Virgin Kinky Straight Bundles"
                         (copy "Like a fresh blow out, Mayvenn kinky straight bundles move freely"
                               "and give a naturally flawless look that mimics your own locks."
                               "Browse our collection here.")
                         "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/")
          texture-templates)
   (merge {:catalog/category-id           "5"
           :copy/title                    "Virgin Body Wave Bundles"
           :seo/title                     "Virgin Body Wave Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-body-wave"
           :legacy/named-search-slug      "body-wave"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"body-wave"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin :hair/color]
           :selector/dimensions           [:hair/origin :hair/color]
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :category/description          (copy "Step into the spotlight with our collection of luscious Body Wave hair bundles."
                                                "Body Wave is unbelievably soft and goes from straight to wavy and back again with ease.")
           :icon                          "/images/categories/body-wave-icon.svg"
           :subcategory/title             "Body Wave"
           :header/title                  "Virgin Body Wave"}
          (category->seo "Body Wave Hair: Virgin Body Wave Bundles"
                         (copy "Step into the spotlight with our collection of luscious virgin body wave hair bundles."
                               "Shop Mayvenn's collection of top quality body wave hair bundles here.")
                         "//ucarecdn.com/445c53df-f369-4ca6-a554-c9668c8968f1/")
          texture-templates)
   (merge {:catalog/category-id           "6"
           :copy/title                    "Virgin Loose Wave Bundles"
           :seo/title                     "Virgin Loose Wave Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-loose-wave"
           :legacy/named-search-slug      "loose-wave"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"loose-wave"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin :hair/color]
           :selector/dimensions           [:hair/origin :hair/color]
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :category/description          (copy "For hair that holds a curl beautifully,"
                                                "our collection of 100% virgin Loose Wave hair bundles"
                                                "is the perfect foundation for all your carefree,"
                                                "flirty, wavy looks.")
           :icon                          "/images/categories/loose-wave-icon.svg"
           :subcategory/title             "Loose Wave"
           :header/title                  "Virgin Loose Wave"}
          (category->seo "Loose Wave Hair: Virgin Loose Wave Bundles"
                         (copy "For hair that holds a curl beautifully,"
                               "Mayvenn's collection of virgin loose wave bundles is the"
                               "perfect foundation for all your flirty, wavy looks. Shop now.")
                         "//ucarecdn.com/31be9341-a688-4f03-b754-a22a0a1f267e/")
          texture-templates)
   (merge {:catalog/category-id           "7"
           :copy/title                    "Virgin Water Wave Bundles"
           :seo/title                     "Virgin Water Wave Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "Virgin-water-wave"
           :legacy/named-search-slug      "water-wave"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"water-wave"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin]
           :selector/dimensions           [:hair/origin :hair/color]
           :category/description          (copy "Ride the lush, carefree waves of the bundles"
                                                "in our Water Wave hair bundle"
                                                "collection. For curls you can rock everywhere from"
                                                "the office to your tropical vacation, make a"
                                                "statement with Water Wave hair.")
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :icon                          "/images/categories/water-wave-icon.svg"
           :subcategory/title             "Water Wave"
           :header/title                  "Virgin Water Wave"}
          (category->seo "Water Wave Hair: Virgin Water Wave Bundles"
                         (copy "For curls you can rock everywhere from the office to your tropical vacation,"
                               "make a statement with Mayvenn water wave hair bundles."
                               "Shop the collection here.")
                         "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/")
          texture-templates)
   (merge {:catalog/category-id           "8"
           :copy/title                    "Virgin Deep Wave Bundles"
           :seo/title                     "Virgin Deep Wave Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-deep-wave"
           :legacy/named-search-slug      "deep-wave"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"deep-wave"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin :hair/color]
           :selector/dimensions           [:hair/origin :hair/color]
           :category/description          (copy "Reigning supreme in versatility, the Deep Wave hair bundle collection features"
                                                "a soft, spiral wave full of body and bounce. Our deep wave hair is perfect"
                                                "for those who want big waves that make an even bigger splash.")
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :icon                          "/images/categories/deep-wave-icon.svg"
           :subcategory/title             "Deep Wave"
           :header/title                  "Virgin Deep Wave"}
          (category->seo "Deep Wave Hair: Virgin Deep Wave Bundles"
                         (copy "Reigning supreme in versatility,"
                               "our deep wave hair collection features a soft,"
                               "spiral wave full of body and bounce."
                               "Browse Mayvenn deep wave bundles here.")
                         "//ucarecdn.com/49cc5837-8321-4331-9cec-d299d0de1887/")
          texture-templates)
   (merge {:catalog/category-id           "9"
           :copy/title                    "Virgin Curly Bundles"
           :seo/title                     "Virgin Curly Hair Bundles"
           :seo/self-referencing-texture? true
           :page/slug                     "virgin-curly"
           :legacy/named-search-slug      "curly"
           :catalog/department            #{"hair"}
           :hair/texture                  #{"curly"}
           :hair/color.process            #{"natural" "dyed"}
           :hair/source                   #{"virgin"}
           :hair/family                   #{"bundles"}
           :selector/essentials           [:catalog/department :hair/color.process :hair/family :hair/source :hair/texture]
           :selector/electives            [:hair/origin]
           :selector/dimensions           [:hair/origin :hair/color]
           :category/description          (copy "Let your bold, beautiful curls take center stage! Our curly hair bundle collection is a tight,"
                                                "kinky curl perfect for creating voluminous coily styles that mimic natural 3C hair textures.")
           :content-block/type            :about-attributes
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                           {:title "How to Choose Bundles"
                                            :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                           {:title "What to Know About Your Hair"
                                            :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]
           :icon                          "/images/categories/curly-icon.svg"
           :subcategory/title             "Curly"
           :header/title                  "Virgin Curly"}
          (category->seo "Curly Hair Bundles: Virgin Curly Hair Weaves"
                         (copy "Let your bold,"
                               "beautiful curls take center stage with virgin curly hair bundles from Mayvenn."
                               "Shop our voluminous curly hair collection here.")
                         "//ucarecdn.com/128b68e2-bf3a-4d72-8e39-0c71662f9c86/")
          texture-templates)])

(def wigs
  [(merge {:catalog/category-id              "13"
           :homepage.ui-v2022-09/order       0
           :homepage.ui-v2022-09/image-id    "0dcffd67-bf94-4819-a2ee-a70e6b2c243a"
           :footer/order                     7
           :footer/title                     "Wigs"
           :desktop-menu/order               0
           :desktop-menu/title               "Wigs"
           :header/title                     "Wigs"

           :page/icp?            true
           :category/new?        true
           :category/description (copy "Want a fun, protective style that switches up your look,"
                                       "color or hair length instantly?"
                                       "Human hair wigs are the perfect choice.")
           :copy/title           "Human Hair Wigs"
           :page/slug            "wigs"
           :seo/title            "Wigs"
           :seo/sitemap          true

           :catalog/department               #{"hair"}
           :hair/family                      #{"360-wigs" "lace-front-wigs" "ready-wigs" "headband-wigs" "closure-wigs"}
           :selector/essentials              [:hair/family :catalog/department]
           :selector/electives               [:hair/family :hair/color :hair/texture :hair/origin :hair/base-material :style.color/features]
           :selector/dimensions              [:hair/family :hair/texture :hair/origin :hair/base-material :hair/color :wig/trait]
           :page/title                       "Human Hair Wigs: 100% Human Hair Wigs | Mayvenn"
           :page.meta/description            (copy "Mayvenn’s virgin human hair wigs allow you to achieve a new look in minutes"
                                                   "& come in different variations such as Brazilian, Malaysian, straight,"
                                                   "& deep wave.")
           :opengraph/title                  (copy "Mayvenn 360 and Lace Frontal Wigs - Free shipping."
                                                   "Free 30 day returns. Made with 100% virgin human hair.")
           :opengraph/description            (copy "100% virgin human hair, machine-wefted and backed by our"
                                                   "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                                   "your unique look using the built-in combs and adjustable strap.")
           :product-list/title               "Shop All Wigs"
           ;; GROT
           :spotlighting/title               "New Arrivals"
           :spotlighting/category-ids        ["42" "40" "41"]
           :subcategories.old/ids            ["42" "43" "40" "41" "26" "25"]
           :subcategories.old/layout         :return-address-labels
           ;; end GROT
           :subcategories/ids                ["42" "43" "40" "41" "26" "25"]
           :subcategories/layout             :spotlights

           :subsections/subsection-selectors [{:subsection/title    "Lace Front Wigs"
                                               :subsection/selector {:hair/family #{"lace-front-wigs"}}}
                                              {:subsection/title    "360 Wigs"
                                               :subsection/selector {:hair/family #{"360-wigs"}}}
                                              {:subsection/title    "Ready to Wear Wigs"
                                               :subsection/selector {:hair/family #{"ready-wigs"}}}
                                              {:subsection/title    "Headband Wigs"
                                               :subsection/selector {:hair/family #{"headband-wigs"}}}
                                              {:subsection/title    "4 x 4 Closure Wigs"
                                               :subsection/selector {:hair/family       #{"closure-wigs"}
                                                                     :hair.closure/area #{"4x4"}}}
                                              {:subsection/title    "5 x 5 Closure Wigs"
                                               :subsection/selector {:hair/family       #{"closure-wigs"}
                                                                     :hair.closure/area #{"5x5"}}}]
           :contentful/faq-id                :icp-wigs
           :content-block/type               :about-attributes
           :content-block/title              "Wigs 101:"
           :content-block/header             "How to Choose"
           :content-block/summary            (str "There are a few main factors to consider "
                                                  "when you’re choosing a wig. When you have a "
                                                  "good sense of the look you want to achieve, your "
                                                  "lifestyle and your budget, the rest will fall "
                                                  "into place. Ask yourself the density, lace color, "
                                                  "length of hair you want, and if you prefer virgin "
                                                  "hair or colored hair.")
           :content-block/sections           [{:title "Cap Size"
                                               :body  [{:text "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."}]}
                                              {:title "Density"
                                               :body  [{:text "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."}]}
                                              {:title "Lace Color"
                                               :body  [{:text "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."}]}
                                              {:title "Length"
                                               :body  [{:text "Short and sassy or drama down to your ankles? The choice is yours! Available in lengths ranging from 10” to 24”."}]}
                                              {:title "Virgin & Colored"
                                               :body  [{:text "If you want to play with color, it helps to choose a wig that can be colored—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it colored the color you want."}]}]}
          wig-templates)
   (merge {:catalog/category-id "24"
           :category/new?       true

           :copy/title "Virgin Lace Front Wigs"
           :page/slug  "virgin-lace-front-wigs"
           :seo/title  "Virgin Lace Front Wigs"

           :category/description (copy "With the lace base in front only,"
                                       "these are ideal for exploring new ways to part your hair."
                                       "Ours are made with virgin lace & real human hair.")
           :subcategory/image-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"

           :catalog/department  #{"hair"}
           :hair/family         #{"lace-front-wigs"}
           :selector/essentials [:hair/family :catalog/department]
           :selector/electives  [:hair/texture :hair/origin :hair/base-material]
           :selector/dimensions [:hair/texture :hair/origin :hair/base-material :hair/color]
           :contentful/faq-id   :category-virgin-lace-front-wigs}
          (category->seo "Lace Front Wigs: Virgin Lace Front Wigs"
                         (copy "Mayvenn’s human hair lace front wigs mimic a natural hairline"
                               "and come in different variations such as Brazilian, Malaysian,"
                               "straight, and deep wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/")
          wig-templates)
   (merge {:catalog/category-id "25"
           :category/new?       true

           :copy/title "Ready to Wear Wigs"
           :page/slug  "ready-wear-wigs"
           :seo/title  "Ready to Wear Wigs"

           :category/description (copy "Made of authentic and high-quality human hair,"
                                       "ready to wear wigs are a quick,"
                                       "convenient way to change up your look instantly.")
           :subcategory/title "Ready to Wear Wigs"
           :subcategory/image-id "a4f7ad94-3c2c-41aa-be4d-94d9d83b1602"

           :contentful/faq-id   :category-ready-wear-wigs
           :catalog/department  #{"hair"}
           :hair/family         #{"ready-wigs"}
           :selector/essentials [:hair/family :catalog/department]
           :selector/electives  [:hair/texture :wig/trait :hair/color :style.color/features]
           :selector/dimensions [:hair/color :hair/texture :wig/trait]

           :page/title-template            [:computed/selected-facet-string " Wigs: " :computed/selected-facet-string " " :seo/title " | Mayvenn"]
           :page.meta/description-template ["Mayvenn’s "
                                            :computed/selected-facet-string
                                            " "
                                            :seo/title
                                            " allow you to change up and achieve your desired look."
                                            " Shop our collection of virgin hair wigs today."]}
          (category->seo "Ready to Wear Wigs: Short, Bob, Side-Part & More"
                         (copy "Mayvenn’s Ready to Wear human hair lace wigs provide a quick style switch-up and"
                               "come in different variations such as Brazilian, straight, and loose wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/"))
   (merge {:catalog/category-id "26"
           :category/new?       true

           :copy/title "Virgin 360 Lace Wigs"
           :page/slug  "virgin-360-wigs"
           :seo/title  "Virgin 360 Lace Wigs"

           :category/description (copy "Ideal for ponytails, these wigs are denser & fuller."
                                       "360 wigs have lace around the entire crown of your head with a cap in the middle.")
           :subcategory/title    "360 Lace Wigs"
           :subcategory/image-id "fe34e6e9-8927-4b62-94ac-91b37f0a137f"

           :contentful/faq-id   :category-virgin-360-wigs
           :catalog/department  #{"hair"}
           :hair/family         #{"360-wigs"}
           :selector/essentials [:hair/family :catalog/department]
           :selector/electives  [:hair/texture :hair/origin]
           :selector/dimensions  [:hair/texture :hair/origin :hair/color]}
          (category->seo "360 Lace Wigs: Virgin 360 Lace Frontal Wigs"
                         (copy "Mayvenn’s human hair 360 lace wigs give you all around protection and"
                               "come in different variations such as Brazilian, Malaysian, straight,"
                               "and deep wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/")
          wig-templates)
   (merge {:catalog/category-id   "40"
           :category/new?         true
           :copy/title            "Virgin Headband Wigs"
           :page/slug             "virgin-headband-wigs"
           :seo/title             "Virgin Headband Wigs"
           :category/description  (copy "Our 100% virgin hair Headband Wigs rank low on effort, but high on impact."
                                        "This quick protective style option is perfect for when you’re on the go.")
           :subcategory/image-id  "4208daf0-824d-4a4e-97be-40001b1635f5"
           :subcategory/title     "Headband Wigs"
           :contentful/faq-id     :category-headband-wigs
           :catalog/department    #{"hair"}
           :hair/family           #{"headband-wigs"}
           :selector/essentials   [:hair/family :catalog/department]
           :selector/electives    [:hair/texture :hair/origin]
           :selector/dimensions   [:hair/texture :hair/origin :hair/color]
           :page/title            "100% Virgin Hair Headband Wigs | Mayvenn"
           :opengraph/title       "Mayvenn Headband Wigs - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
           :page.meta/description "Mayvenn’s Headband Wigs come in Straight, Body Wave, Loose Wave, and Deep Wave. Crafted with high-quality 100% virgin human hair."
           :opengraph/description (copy "Quick, high-quality, convenient: our versatile 100% virgin human hair"
                                        "Headband Wigs are sure to be a favorite. Easily customizable with built-in"
                                        "combs and an adjustable strap and headband.")
           :category/image-url    "//ucarecdn.com/4208daf0-824d-4a4e-97be-40001b1635f5/"
           :seo/sitemap           true}
          wig-templates)
   (merge {:catalog/category-id "41"
           :category/new?       true

           :copy/title "Virgin Closure Wigs"
           :page/slug  "virgin-closure-wigs"
           :seo/title  "Virgin Closure Wigs"

           :category/description             (copy "Available with 5x5 or 4x4 inches of parting space,"
                                                   "our virgin hair Lace Closure Wigs are ​​crafted to provide more coverage and less stress."
                                                   "Take your pick of Standard or HD Lace and 4 classic textures.")
           :subcategory/image-id             "f48ead11-9482-43f4-ae9a-2f7715a9243e"
           :subcategory/title                "Closure Wigs"
           :subsections/subsection-selectors [{:subsection/title    "4 x 4 Closure Wigs"
                                               :subsection/selector {:hair.closure/area #{"4x4"}}}
                                              {:subsection/title    "5 x 5 Closure Wigs"
                                               :subsection/selector {:hair.closure/area #{"5x5"}}}]

           :contentful/faq-id     :category-closure-wigs
           :catalog/department    #{"hair"}
           :hair/family           #{"closure-wigs"}
           :selector/essentials   [:hair/family :catalog/department]
           :selector/electives    [:hair/texture :hair/origin :hair.closure/area :hair/base-material]
           :selector/dimensions   [:hair/texture :hair/origin :hair.closure/area :hair/base-material :hair/color]
           :page/title            "100% Virgin Hair Lace Closure Wig | Mayvenn"
           :page.meta/description (copy "Mayvenn’s Lace Closure Wigs come complete with 4x4 or 5x5 inches of parting space."
                                        "Crafted with HD Lace or Standard Lace and high-quality 100% virgin human hair.")
           :opengraph/title       "Mayvenn Lace Closure Wig - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
           :opengraph/description (copy "Our Lace Closure Wigs are sure to be a favorite. Take your pick of Standard or HD Lace and 4 classic textures."
                                        "Easily customizable with built-in combs and an adjustable strap.")
           :category/image-url    "//ucarecdn.com/f48ead11-9482-43f4-ae9a-2f7715a9243e/"
           :seo/sitemap           true}
          wig-templates)
   (merge {:catalog/category-id "42"
           :header/title        "HD Lace Wigs"

           :page/icp?            false
           :category/new?        true
           :category/description (copy "Explore all of your favorite looks with our versatile HD wig options."
                                       "Our HD wigs have transparent lace for seamless blending.")
           :subcategory/image-id "e4266735-42fc-40b1-bd37-ce5a243fa681"
           :subcategory/title    "HD Lace Wigs"
           :copy/title           "HD Lace Wigs"
           :page/slug            "hd-lace-wigs"
           :seo/title            "HD Lace Wigs"
           :seo/sitemap          true

           :catalog/department     #{"hair"}
           :hair/family            #{"lace-front-wigs" "closure-wigs"}
           :hair/base-material     #{"hd-lace"}
           :selector/essentials    [:hair/family :catalog/department :hair/base-material]
           :selector/electives     [:hair/family :hair/texture :hair/origin]
           :selector/dimensions    [:hair/family :hair/texture :hair/origin :hair/color]
           :page/title             "100% Virgin Hair HD Lace Wig | Mayvenn"
           :page.meta/description  (copy "Mayvenn’s HD Lace Wigs come complete with 13x4, 4x4, or 5x5 inches of parting space."
                                         "Crafted with high-quality 100% virgin human hair.")
           :opengraph/title        (copy "Mayvenn HD Lace Wig - Free shipping."
                                         "Free 30 day returns. Made with 100% virgin human hair.")
           :opengraph/description  (copy "Our HD Lace Wigs are sure to be a favorite."
                                         "Take advantage of high-quality HD Lace and 4 classic textures."
                                         "Easily customizable with built-in combs and an adjustable strap.")
           :product-list/title     "Shop All HD Lace Wigs"
           :contentful/faq-id      :category-hd-lace-wigs
           :content-block/type     :about-attributes
           :content-block/title    "Wigs 101:"
           :content-block/header   "How to Choose"
           :content-block/summary  (str "There are a few main factors to consider "
                                        "when you’re choosing a wig. When you have a "
                                        "good sense of the look you want to achieve, your "
                                        "lifestyle and your budget, the rest will fall "
                                        "into place. Ask yourself the density, lace color, "
                                        "length of hair you want, and if you prefer virgin "
                                        "hair or colored hair.")
           :content-block/sections [{:title "Cap Size"
                                     :body  [{:text "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."}]}
                                    {:title "Density"
                                     :body  [{:text "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."}]}
                                    {:title "Lace Color"
                                     :body  [{:text "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."}]}
                                    {:title "Length"
                                     :body  [{:text "Short and sassy or drama down to your ankles? The choice is yours! Available in lengths ranging from 10” to 24”."}]}
                                    {:title "Virgin & Colored"
                                     :body  [{:text "If you want to play with color, it helps to choose a wig that can be colored—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it colored the color you want."}]}]}
          wig-templates)
   (merge {:catalog/category-id  "43"
           :header/title         "Standard Lace Wigs"
           :page/icp?            false
           :category/new?        false
           :category/description (copy "Available in 4 bestselling textures, take your pick of our Standard Lace wig options."
                                       "Ideal for switching up your style with a brand new closure or frontal unit.")
           :subcategory/title    "Standard Lace Wigs"
           :subcategory/image-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"
           :copy/title           "Standard Lace Wigs"
           :page/slug            "standard-lace-wigs"
           :seo/title            "Standard Lace Wigs"
           :seo/sitemap          true

           :catalog/department     #{"hair"}
           :hair/family            #{"lace-front-wigs" "closure-wigs"}
           :hair/base-material     #{"lace"}
           :selector/essentials    [:hair/family :catalog/department :hair/base-material]
           :selector/electives     [:hair/family :hair/texture :hair/origin :hair/color]
           :selector/dimensions    [:hair/family :hair/texture :hair/origin :hair/color]
           :page/title             "100% Virgin Hair Standard Lace Wig | Mayvenn"
           :page.meta/description  (copy "Mayvenn’s Standard Lace Wigs come complete with 13x4, 4x4, or 5x5 inches of parting space."
                                         "Crafted with high-quality 100% virgin human hair. ")
           :opengraph/title        (copy "Mayvenn Standard Lace Wig - Free shipping."
                                         "Free 30 day returns. Made with 100% virgin human hair.")
           :opengraph/description  (copy "Our Standard Lace Wigs are sure to be a favorite."
                                         "Take advantage of high-quality lace and 4 classic textures."
                                         "Easily customizable with built-in combs and an adjustable strap.")
           :product-list/title     "Shop All Standard Lace Wigs"
           :contentful/faq-id      :category-standard-lace-wigs
           :content-block/type     :about-attributes
           :content-block/title    "Wigs 101:"
           :content-block/header   "How to Choose"
           :content-block/summary  (str "There are a few main factors to consider "
                                        "when you’re choosing a wig. When you have a "
                                        "good sense of the look you want to achieve, your "
                                        "lifestyle and your budget, the rest will fall "
                                        "into place. Ask yourself the density, lace color, "
                                        "length of hair you want, and if you prefer virgin "
                                        "hair or colored hair.")
           :content-block/sections [{:title "Cap Size"
                                     :body  [{:text "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."}]}
                                    {:title "Density"
                                     :body  [{:text "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."}]}
                                    {:title "Lace Color"
                                     :body  [{:text "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."}]}
                                    {:title "Length"
                                     :body  [{:text "Short and sassy or drama down to your ankles? The choice is yours! Available in lengths ranging from 10” to 24”."}]}
                                    {:title "Virgin & Colored"
                                     :body  [{:text "If you want to play with color, it helps to choose a wig that can be colored—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it colored the color you want."}]}]}
          wig-templates)])

(def all-hair-products
  [{:catalog/category-id              "23"
    :catalog/department               #{"hair"}
    :category/show-title?             true
    :category/description             "Choose From All of Our Hair Products"
    :copy/title                       "All Products"
    :hair/family                      #{"bundles" "closures" "frontals" "360-frontals" "seamless-clip-ins"
                                        "360-wigs" "lace-front-wigs" "ready-wigs" "headband-wigs" "closure-wigs"}
    :page/slug                        "all-products"
    :page/title                       "100% Virgin Hair | Mayvenn"
    :page.meta/description            (copy "Mayvenn’s real human hair bundles come in different variations such as"
                                            "Brazilian, Malaysian, straight, deep wave, and loose wave. Create your look today.")
    :opengraph/description            (copy "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee,"
                                            "our natural human hair bundles are the best quality products on the market and ship free!")
    :opengraph/title                  "Natural Human Hair Bundles - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
    :selector/electives               [:hair/texture :hair/family :hair/origin :hair/color :hair/base-material]
    :selector/essentials              [:catalog/department]
    :selector/dimensions              [:catalog/department :hair/color :wig/trait :hair/texture :hair/origin :hair/base-material :hair.closure/area]
    :subsections/subsection-selectors texture-subsection-selectors}])

(def human-hair-bundles
  [(merge {:catalog/category-id              "27"
           :catalog/department               #{"hair"}
           :category/show-title?             true
           :category/new?                    false
           :desktop-menu/title               "Hair Bundles"
           :desktop-menu/order               1
           :homepage.ui-v2022-09/order       1
           :homepage.ui-v2022-09/image-id    "16029dd0-285c-4bc6-803c-0c201c3d402c"
           :footer/order                     4
           :footer/title                     "Hair Bundles"
           :category/description             "Have a hairstyle in mind and want more volume, length, and texture? Add our 100% virgin hair bundles."
           :category/image-url               "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"
           :copy/title                       "Virgin Hair Bundles"
           :hair/family                      #{"bundles"}
           :page/icp?                        true
           :page/slug                        "human-hair-bundles"
           :page/title                       "Hair Bundles: Sew-In Weave Bundles | Mayvenn"
           :page.meta/description            (copy "Mayvenn’s real human hair bundles come in different variations such as"
                                                   "Brazilian, Malaysian, straight, deep wave, and loose wave. Create your look today.")
           :product-list/title               "Shop All Bundles"
           :opengraph/description            (copy "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee,"
                                                   "our natural human hair bundles are the best quality products on the market and ship free!")
           :opengraph/title                  "Natural Human Hair Bundles - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
           :selector/essentials              [:catalog/department :hair/family]
           :selector/electives               [:hair/texture :hair/origin :hair/color]
           :selector/dimensions              [:hair/texture :hair/origin :hair/color]
           :subcategories.old/layout         :grid
           :subcategories.old/ids            ["2" "3" "4" "5" "6" "7" "8" "9"]
           :subcategories.old/title          "Textures"
           :subcategories/layout             :spotlights
           :subcategories/ids                ["2" "3" "4" "5" "6" "7" "8" "9"]
           :subsections/subsection-selectors texture-subsection-selectors

           :seo/sitemap            true
           :seo/title              "Virgin Hair Bundles"
           :contentful/faq-id      :icp-bundles
           :content-block/type     :about-attributes
           :content-block/title    "Hair Bundles 101:"
           :content-block/header   "How to Style"
           :content-block/summary  "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections [{:title "Our Hair"
                                     :body  [{:text "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}]}
                                    {:title "How to Choose Bundles"
                                     :body  [{:text "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Colored Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}]}
                                    {:title "What to Know About Your Install"
                                     :body  [{:text "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}]}]}
          bundle-templates)])

(def the-only-stylist-exclusive
  {:catalog/category-id       "14"
   :auth/requires             #{:stylist}
   :footer/order              11
   :footer/title              "Stylist Exclusives"
   :header/title              "Stylist Exclusives"
   :copy/title                "Stylist Exclusives"
   :page/slug                 "stylist-exclusives"
   :direct-to-details/id      "49"
   :direct-to-details/slug    "rings-kits"
   :direct-to-details/sku-id  "SK3"
   :catalog/department        #{"stylist-exclusives"},
   :kits/contents             #{"rings"},
   :stylist-exclusives/family #{"kits"}
   :selector/essentials       [:catalog/department :kits/contents :stylist-exclusives/family]
   :selector/electives        []
   :page/title                "Stylist"
   :opengraph/title           (copy "Stylist Exclusives - Free shipping."
                                    "Free 30 day returns. Made with 100% virgin human hair.")
   :page.meta/description     (copy "")
   :opengraph/description     (copy "")
   :seo/sitemap               false})

(def stylist-exclusives
  [the-only-stylist-exclusive])

(def virgin-hair-nav-roots
  [(merge {:catalog/category-id   "15"
           :copy/title            "Virgin Hair"
           :page/slug             "virgin-hair"
           :catalog/department    #{"hair"}
           :hair/color.process    #{"natural"}
           :hair/source           #{"virgin"},
           :hair/family           #{}
           :hair/texture          #{"straight"
                                    "kinky-straight"
                                    "yaki-straight"
                                    "body-wave"
                                    "deep-wave"
                                    "water-wave"
                                    "loose-wave"
                                    "curly"}
           :selector/essentials   [:catalog/department :hair/color.process :hair/texture :hair/source]
           :selector/electives    []
           :selector/dimensions   [:hair/color]
           :page/title            ""
           :opengraph/title       (copy "" "")
           :page.meta/description (copy "")
           :opengraph/description (copy "")
           :page/redirect?        true
           :seo/sitemap           false})
   (merge {:catalog/category-id  "12"
           :copy/title           "Closures & Frontals"
           :page/slug            "closures-and-frontals"
           :catalog/department   #{"hair"}
           :hair/family          #{"closures" "frontals" "360-frontals"}
           :hair/color.process   #{"dyed" "natural"}
           :hair/source          #{"virgin"}
           :category/tags        #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials  [:catalog/department :hair/family :hair/color.process :hair/source :category/tags]
           :hamburger/order      1
           :selector/electives   []
           :selector/dimensions  [:hair/color]
           :category/description (copy "Save your precious strands and top your look"
                                       "off with the ultimate tool in protective weave"
                                       "styling. Our collection of closures and frontals blend"
                                       "seamlessly with our bundles and can be customized"
                                       "to fit your unique look.")
           :page/redirect?       true
           :seo/sitemap          false})])

(def seamless-clip-ins-category
  [(merge
    {:catalog/category-id "21"
     :catalog/department  #{"hair"}
     :category/new?       false
     :desktop-menu/title  "Clip-Ins"
     :desktop-menu/order  4

     :hair/family         #{"seamless-clip-ins"}
     :selector/essentials [:catalog/department :hair/family]
     :selector/electives  [:hair/color :style.color/features]
     :selector/dimensions [:hair/texture :hair/color]
     :subcategory/image-id "d255ccf8-75af-4729-86da-af6e15783fc2"
     :copy/title           "Clip-In Hair Extensions"
     :copy/tooltip         (copy "Seamless clip-in hair extensions have a thin, silicone base that blends in with your natural hair,"
                                 " making them harder to see than classic clip-in hair extensions.")
     :category/description (copy "Get the hair of your dreams in an instant with our seamless clip-in extensions."
                                 "Featuring a thin, polyurethane (PU) weft that flawlessly blends with your own hair."
                                 "Ditch the tracks for a clip-in experience that is truly seamless.")

     :contentful/faq-id     :category-seamless-clip-ins
     :page/title            "Clip In Hair Extensions with 100% Remy Human Hair | Mayvenn"
     :page/slug             "seamless-clip-ins"
     :page.meta/description (copy "Get longer, fuller hair with Mayvenn clip in hair extensions. "
                                  "Made with 100% human hair and a thin weft to seamlessly blend with your natural hair.")
     :seo/title             "Clip-In Hair Extensions"
     :category/guide-page   [events/navigate-guide-clipin-extensions]

     :category/image-url    "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"
     :opengraph/title       "Mayvenn Clip-In Hair Extensions - Free shipping. Free 30 day returns. Made with 100% human hair extensions."
     :opengraph/description "Blending flawlessly with your own hair and backed by our 30 Day Quality Guarantee, our seamless clip-in extensions are the best quality products on the market and ships free!"

     :header/title "Seamless Clip Ins"
     :seo/sitemap  true}
    hair-extensions-templates)])

(def the-only-tape-in-category
  (merge {:catalog/category-id "22"
          :catalog/department  #{"hair"}
          :category/new?       false

          :hair/family         #{"tape-ins"}
          :selector/essentials [:catalog/department :hair/family]
          :selector/electives  [:hair/color :hair/weight :hair/length]
          :selector/dimensions [:hair/color :hair/weight :hair/length]

          :copy/title           "Tape-In Hair Extensions"
          :category/description "Our straight tape-in extensions lie completely flat against the head and blend seamlessly with your own hair."

          :page/slug "tape-ins"

          :subcategory/image-id     "1998d5dd-51fa-4ee4-8fef-2ce0d8ed6f8e"
          :direct-to-details/id     "111"
          :direct-to-details/slug   "50g-straight-tape-ins"
          :direct-to-details/sku-id "TAPE-S-1-20"

          :header/title   "Tape Ins"
          :page/redirect? true
          :seo/sitemap    false}
         hair-extensions-templates))

(def tape-ins-category
  [the-only-tape-in-category])

(def hair-extensions-category
  [(merge
    {:catalog/category-id "28"
     :catalog/department  #{"hair"}
     :category/new?       false
     :page/icp?           true
     :hair/family         #{"seamless-clip-ins"}
     :selector/essentials [:catalog/department :hair/family]
     :selector/electives  [:hair/color :hair/texture]
     :selector/dimensions [:hair/color :hair/texture]
     :desktop-menu/title  "Hair Extensions"

     :homepage.ui-v2022-09/order    4
     :homepage.ui-v2022-09/image-id "4c9af3d0-3672-4550-9e9f-1a69833b2fe3"
     :footer/order                  8
     :footer/title                  "Hair Extensions"

     :copy/title         "Hair Extensions"
     :product-list/title "Shop All Hair Extensions"

     :contentful/faq-id                :icp-hair-extensions
     :page/slug                        "hair-extensions"
     :category/description             (str "Ditch the tracks and opt for hair that blends in seamlessly. "
                                            "Mayvenn human hair extensions are made with a thin polyurethane "
                                            "weft that blends with your hair for a natural look.")
     :seo/sitemap                      true
     :seo/title                        "Hair Extensions"
     :page/title                       "Hair Extensions: Real Human Hair Extensions | Mayvenn"
     :page.meta/description            (str "Mayvenn’s real human hair extensions come in different variations"
                                            " such as Brazilian and Malaysian, straight, deep wave and loose wave."
                                            " Shop now.")
     :category/image-url               "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"
     :opengraph/title                  "Real Human Hair Extensions - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
     :opengraph/description            "Blending flawlessly with your own hair and backed by our 30 Day Quality Guarantee, our seamless clip-in extensions are the best quality products on the market and ships free!"
     :subcategories.old/ids            ["21" "22"]
     :subcategories.old/layout         :list
     :subcategories/ids                ["21" "22"]
     :subcategories/layout             :spotlights
     :subsections/subsection-selectors [{:subsection/title    "Seamless Clip-Ins"
                                         :subsection/selector {:hair/family #{"seamless-clip-ins"}}}
                                        #_{:subsection/title    "Tape-Ins"
                                           :subsection/selector {:hair/family #{"tape-ins"}}}]
     :content-block/type               :about-attributes
     :content-block/title              "Hair Extensions 101:"
     :content-block/header             "How to Choose"
     :content-block/summary            "No matter what kind of transformation you’re looking for, our seamless clip-in hair extensions will help you achieve your desired look in an instant. Our clip-ins are perfect for when you want a natural-looking appearance that complements your own hair while giving that much coveted oomph-factor."
     :content-block/sections           [#_{:title "Tape-In Hair Extensions"
                                           :body  [{:text "Our seamless tape-in hair extensions have a thin weft that flawlessly blends with your own hair, so you can have the hair of your dreams."}]}
                                        {:title "Clip-In Hair Extensions"
                                         :body  [{:text "With a thin weft that blends into your hair seamlessly, our clip-in human hair extensions help you create the hair of your dreams."}]}
                                        {:title "Human Hair Extensions"
                                         :body  [{:text "Our human hair extensions are a must-have for creating the hair you’ve always wanted. Our high-quality extensions are easy to install and available in many textures like Straight, Yaki Straight, Kinky Straight, Body Wave, Loose Wave, Water Wave, Deep Wave, and Curly, plus multiple lengths for all kinds of hairstyles."}]}]}
    hair-extensions-templates)])

(def base-materials-categories
  [{:catalog/category-id "44"
    :header/title        "HD Lace"

    :page/icp?            true
    :category/new?        true
    :category/description (copy "Explore all of your favorite looks with our versatile HD lace options."
                                "HD lace for seamless blending.")
    :subcategory/image-id "e4266735-42fc-40b1-bd37-ce5a243fa681"
    :subcategory/title    "HD Lace"
    :copy/title           "HD Lace"
    :page/slug            "hd-lace"
    :seo/title            "HD Lace"
    :seo/sitemap          true

    :catalog/department               #{"hair"}
    :hair/family                      #{"lace-front-wigs" "closure-wigs" "360-frontals" "closures" "frontals"}
    :hair/base-material               #{"hd-lace"}
    :selector/essentials              [:hair/family :catalog/department :hair/base-material]
    :selector/electives               [:hair/family :hair/texture :hair/origin :hair/color]
    :selector/dimensions              [:hair/family :hair/texture :hair/origin :hair/color]
    :page/title                       "100% Virgin Hair HD Lace Wig | Mayvenn"
    :page.meta/description            (copy "Mayvenn’s HD Lace comes complete with 13x4, 4x4, or 5x5 inches of parting space."
                                            "Crafted with high-quality 100% virgin human hair.")
    :opengraph/title                  (copy "Mayvenn HD Lace - Free shipping."
                                            "Free 30 day returns. Made with 100% virgin human hair.")
    :opengraph/description            (copy "Our HD Lace is sure to be a favorite."
                                            "Take advantage of high-quality HD Lace and 4 classic textures.")
    :product-list/title               "Shop All HD Lace"
    :contentful/faq-id                :category-hd-lace
    :content-block/type               :about-attributes
    :content-block/title              "Wigs 101:"
    :content-block/header             "How to Choose"
    :content-block/summary            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas vel ultrices eros. Vivamus vestibulum purus non tristique consectetur. Donec auctor et urna sit amet blandit. In hac habitasse platea dictumst. Vestibulum ac condimentum sapien."
    :content-block/sections           [{:title "Cap Size"
                                        :body  [{:text "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."}]}
                                       {:title "Density"
                                        :body  [{:text "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."}]}
                                       {:title "Lace Color"
                                        :body  [{:text "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."}]}
                                       {:title "Length"
                                        :body  [{:text "Short and sassy or drama down to your ankles? The choice is yours! Available in lengths ranging from 10” to 24”."}]}
                                       {:title "Virgin & Colored"
                                        :body  [{:text "If you want to play with color, it helps to choose a wig that can be colored—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it colored the color you want."}]}]
    :page/title-template              [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
    :page.meta/description-template   ["Get the hair of your dreams with our "
                                       :computed/selected-facet-string
                                       " "
                                       :seo/title
                                       ". Featuring a thin, polyurethane"
                                       " weft that flawlessly blends with your own hair."]
    :subcategories.old/ids            ["47" "46" "42"]
    :subcategories.old/layout         :list
    :subcategories/ids                ["47" "46" "42"]
    :subcategories/layout             :spotlights
    :subsections/subsection-selectors [{:subsection/title    "HD Lace Closures"
                                        :subsection/selector {:hair/family #{"closures"}}}
                                       {:subsection/title    "HD Lace Frontals"
                                        :subsection/selector {:hair/family #{"frontals"}}}
                                       {:subsection/title    "HD Lace Wigs"
                                        :subsection/selector {:hair/family #{"lace-front-wigs" "closure-wigs"}}}]}
   {:catalog/category-id  "47"
    :header/title         "HD Lace Frontals"
    :category/new?        true
    :category/description (copy "Part your hair a different way every day with our versatile 13x4 HD lace frontals, "
                                "which blend seamlessly with your scalp.")
    :subcategory/image-id "e4266735-42fc-40b1-bd37-ce5a243fa681"
    :subcategory/title    "HD Lace Frontals"
    :copy/title           "HD Lace Frontals"
    :page/slug            "hd-lace-frontals"
    :seo/title            "HD Lace Frontals"
    :seo/sitemap          true

    :catalog/department             #{"hair"}
    :hair/family                    #{"frontals"}
    :hair/base-material             #{"hd-lace"}
    :selector/essentials            [:hair/family :catalog/department :hair/base-material]
    :selector/electives             [:hair/texture :hair/origin :hair/color]
    :selector/dimensions            [:hair/texture :hair/origin :hair/color]
    :page/title                     "100% Virgin Hair HD Lace Frontals | Mayvenn"
    :page.meta/description          (copy "Mayvenn’s HD Lace comes complete with 13x4, inches of parting space."
                                          "Crafted with high-quality 100% virgin human hair.")
    :opengraph/title                (copy "Mayvenn HD Lace - Free shipping."
                                          "Free 30 day returns. Made with 100% virgin human hair.")
    :opengraph/description          (copy "Our HD Lace is sure to be a favorite."
                                          "Take advantage of high-quality HD Lace and 4 classic textures.")
    :product-list/title             "Shop All HD Lace Frontals"
    :contentful/faq-id              :category-hd-lace
    :page/title-template            [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
    :page.meta/description-template ["Get the hair of your dreams with our "
                                     :computed/selected-facet-string
                                     " "
                                     :seo/title
                                     ". Featuring a thin, polyurethane"
                                     " weft that flawlessly blends with your own hair."]}
   {:catalog/category-id  "46"
    :header/title         "HD Lace Closures"
    :category/new?        true
    :category/description (copy "Our 4x4 and 5x5 closures use HD lace for a superior match with your scalp's complexion.")
    :subcategory/image-id "e4266735-42fc-40b1-bd37-ce5a243fa681"
    :subcategory/title    "HD Lace Closures"
    :copy/title           "HD Lace Closures"
    :page/slug            "hd-lace-closures"
    :seo/title            "HD Lace Closures"
    :seo/sitemap          true

    :catalog/department             #{"hair"}
    :hair/family                    #{"closures"}
    :hair/base-material             #{"hd-lace"}
    :selector/essentials            [:hair/family :catalog/department :hair/base-material]
    :selector/electives             [:hair/texture :hair/origin :hair/color :hair.closure/area]
    :selector/dimensions            [:hair/origin :hair/texture :hair.closure/area :hair/color]
    :page/title                     "100% Virgin Hair HD Lace Closures | Mayvenn"
    :page.meta/description          (copy "Mayvenn’s HD Lace comes complete with 4x4 or 5x5 inches of parting space."
                                          "Crafted with high-quality 100% virgin human hair.")
    :opengraph/title                (copy "Mayvenn HD Lace - Free shipping."
                                          "Free 30 day returns. Made with 100% virgin human hair.")
    :opengraph/description          (copy "Our HD Lace is sure to be a favorite."
                                          "Take advantage of high-quality HD Lace and 4 classic textures.")
    :product-list/title             "Shop All HD Lace Closures"
    :contentful/faq-id              :category-hd-lace
    :page/title-template            [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
    :page.meta/description-template ["Get the hair of your dreams with our "
                                     :computed/selected-facet-string
                                     " "
                                     :seo/title
                                     ". Featuring a thin, polyurethane"
                                     " weft that flawlessly blends with your own hair."]}])

(def special-wigs-promo
  [{:catalog/category-id "45"
    :header/title        "Wigs"
    :page/icp?           false
    :page/slug           "select-wigs-promo"
    :seo/title           "Wigs"
    :seo/sitemap         true

    :copy/banner-img-dsk-id "762a38d1-6480-4a0c-a558-2c5839d9a52c"
    :copy/banner-img-mob-id "44c98bfd-bd71-4469-a321-c86bb3d713c6"
    :copy/banner-alt        "Select Wigs"

    :subcategories.old/ids    ["29" "10"]
    :subcategories.old/layout :list
    :subcategories/ids    ["29" "10"]
    :subcategories/layout :spotlights

    :subsections/subsection-selectors [{:subsection/title    "Lace Front Wigs"
                                        :subsection/selector {:hair/family #{"lace-front-wigs"}}}
                                       {:subsection/title    "360 Wigs"
                                        :subsection/selector {:hair/family #{"360-wigs"}}}]

    :catalog/department  #{"hair"}
    :hair/family         #{"lace-front-wigs" "360-wigs"}
    :selector/essentials [:hair/family :catalog/department]
    :selector/electives  [:hair/family :hair/texture :hair/origin]
    :selector/dimensions [:hair/family :hair/texture :hair/origin :hair/color]
    :page/title          "Wigs | Mayvenn"
    :page/title-template [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]}])

(def sale-clearance
  "This is not being used at the moment, but we may use category 48 for clearance in the future, so I'm leaving it here, unreferenced"
  [{:catalog/category-id  "48"
    :header/title         "Clearance Wigs"
    :category/description "Get your favorite wig styles at a fraction of the price. 30% off while supplies last. Clearance items are available online only and not eligible for additional discounts."
    :subcategory/image-id "e4266735-42fc-40b1-bd37-ce5a243fa681"
    :copy/title           "Clearance Wigs"
    :page/slug            "clearance"
    :seo/title            "Wig Sale"
    :seo/sitemap          true

    :promo.clearance/eligible         #{true}
    :selector/essentials              [:promo.clearance/eligible]
    :selector/electives               [:hair/family :hair/texture :hair/origin :hair/color]
    :selector/dimensions              [:hair/family :hair/texture :hair/origin :hair/color]
    :page/title                       "Clearance Wigs | Mayvenn"
    :page.meta/description            (copy "Mayvenn’s virgin human hair wigs are on clearance! "
                                            "These wigs allow you to achieve a new look in minutes "
                                            "& come in different variations such as Brazilian, "
                                            "Malaysian, straight, & deep wave.")
    :opengraph/title                  (copy "Mayvenn Wigs on Clearance - Free shipping. Free 30 "
                                            "day returns. Made with 100% virgin human hair.")
    :opengraph/description            (copy "100% virgin human hair, machine-wefted and backed by "
                                            "our 30 Day Quality Guarantee, our Wigs on clearance "
                                            "can be customized to fit your unique look using the "
                                            "built-in combs and adjustable strap.")
    :product-list/title               "Shop All Sale Items"
    :subsections/subsection-selectors [{:subsection/title    "Lace Front Wigs"
                                        :subsection/selector {:hair/family #{"lace-front-wigs"}}}
                                       {:subsection/title    "360 Wigs"
                                        :subsection/selector {:hair/family #{"360-wigs"}}}
                                       {:subsection/title    "Ready to Wear Wigs"
                                        :subsection/selector {:hair/family #{"ready-wigs"}}}]}])

(def nb-hyphen "‑")

(def menu-categories
  (concat virgin-hair
          closures))

(def initial-categories
  (concat wigs
          hair-extensions-category
          all-hair-products
          stylist-exclusives
          virgin-hair-nav-roots
          menu-categories
          seamless-clip-ins-category
          human-hair-bundles
          base-materials-categories
          special-wigs-promo))
