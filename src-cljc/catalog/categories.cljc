(ns catalog.categories
  (:require [clojure.string :as string]
            [catalog.keypaths]
            [storefront.events :as events]))

(def new-facet?
  ;; [<facet-slug> <option-slug>]
  #{[:hair/family "ready-wigs"]})

(def new-category? #{})

(defn copy [& sentences]
  (string/join " " sentences))

(defn category->seo [category-name description image-url]
  {:page/title            (str category-name " | Mayvenn")
   :opengraph/title       (str category-name " - Free shipping. Free 30 day returns. Made with 100% virgin human hair.")
   :page.meta/description description
   :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee, our"
                                category-name
                                "are the best quality products on the market and ships free!")
   ;; TODO make this a proper image and place under :selector/images
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

(def clip-in-tape-in-templates
  {:page/title-template            [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Get the hair of your dreams with our "
                                    :computed/selected-facet-string
                                    " "
                                    :seo/title
                                    ". Featuring a thin, polyurethane"
                                    " weft that flawlessly blends with your own hair."]})

(def closures-templates
  {:page/title-template            [:computed/selected-facet-string " Virgin " :seo/title " | Mayvenn"]
   :page.meta/description-template ["Mayvenn's "
                                    :computed/selected-facet-string
                                    " Virgin "
                                    :seo/title
                                    " are beautifully crafted and provide a realistic part to"
                                    " close off any unit or install."]})

(def frontals-templates
  {:page/title-template            [[:computed/selected-facet-string " "] :seo/title " | Mayvenn"]
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
  [(merge {:catalog/category-id      "0"
           :copy/title               "Hair Closures"
           :page/slug                "virgin-closures"
           :seo/title                "Hair Closures"
           :legacy/named-search-slug "closures"
           :catalog/department       #{"hair"}
           :hair/family              #{"closures"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :category/tags            #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials      [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives       [:hair/origin :hair/texture :hair/color :hair/base-material]
           :header/title             "Virgin Hair Closures"
           :flyout-menu/title        "Closures"
           :flyout-menu/order        1
           :footer/order             1
           :footer/title             "Closures"
           :copy/description         (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                           "Our collection of closures blend seamlessly with our bundles"
                                           "and can be customized to fit your unique look.")}
          (category->seo "Closures"
                         (copy "Mayvenn’s hair closures allow you to close off"
                               "any unit or install and come in a variety of different"
                               "combinations. Shop now to create your look.")
                         "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")
          {:page/title "Hair Closures: Human Hair Closure Sew-Ins | Mayvenn"}
          closures-templates)
   (merge {:catalog/category-id      "10"
           :copy/title               "360 Lace Frontals"
           :page/slug                "360-frontals"
           :seo/title                "Virgin 360 Hair Frontals"
           :legacy/named-search-slug "360-frontals"
           :subcategory/image-uri    "//ucarecdn.com/e6a42693-73a2-4cda-8ad8-bc16b7d8b5f4/-/format/auto/-/resize/124x/"

           :catalog/department  #{"hair"}
           :hair/family         #{"360-frontals"}
           :hair/color.process  #{"natural" "dyed"}
           :hair/source         #{"virgin"}
           :category/tags       #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives  [:hair/origin :hair/texture :hair/color :hair/base-material]

           :copy/description "Mayvenn’s hair 360 frontals got you covered and come in different variations such as Brazilian, Malaysian, straight, and deep wave."
           :header/title     "Virgin 360 Frontals"}
          (category->seo "360 Frontals"
                         (copy "Mayvenn’s virgin hair 360 frontals got you covered and come in"
                               "different variations such as Brazilian, Malaysian, straight, "
                               "and deep wave. Order today.")
                         "//ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/")
          {:page/title "360 Lace Frontals: Virgin Hair 360 Frontals | Mayvenn"}
          frontals-templates)
   (merge {:catalog/category-id      "1"
           :copy/title               "Hair Frontals"
           :page/slug                "virgin-frontals"
           :seo/title                "Hair Frontals"
           :legacy/named-search-slug "frontals"
           :menu/hide?               true
           :page/icp?                true
           :category/description     (copy "Save your precious strands and top your look off with"
                                           "the ultimate tool in protective weave styling. Our collection of"
                                           "frontals blend seamlessly with our bundles and can be customized to"
                                           "fit your unique look.")

           :catalog/department            #{"hair"}
           :hair/family                   #{"frontals" "360-frontals"}
           :hair/color.process            #{"natural" "dyed"}
           :flyout-menu/title             "Frontals"
           :flyout-menu/order             2
           :footer/order                  2
           :footer/title                  "Frontals"
           :hair/source                   #{"virgin"}
           :category/tags                 #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials           [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives            [:hair/origin :hair/texture :hair/color]
           :copy/description              (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                                "Our collection of frontals blend seamlessly with our bundles and can be customized to fit your unique look.")
           :product-list/title            "Shop All Frontals"
           :subcategories/ids             ["10" "29"]
           :subcategories/layout          :list
           :subsections/category-selector :hair/family
           :subsections                   {"360-frontals" {:order         0
                                                           :title/primary "360 Lace Frontals"}
                                           "frontals"     {:order         1
                                                           :title/primary "Virgin Lace Frontals"}}}
          (category->seo "Frontals"
                         "Mayvenn’s hair frontals blend in seamlessly with our bundles and come in a variety of different combinations. Shop now to create your look."
                         "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/")
          {:page/title "Hair Frontals: Human Hair Lace Frontal Sew-Ins | Mayvenn"}
          frontals-templates)
   (merge {:catalog/category-id      "29"
           :copy/title               "Virgin Lace Frontals"
           :page/slug                "virgin-lace-frontals"
           :seo/title                "Virgin Hair Frontals"     ; TODO what should this be?
           :legacy/named-search-slug "frontals"
           :catalog/department       #{"hair"}
           :hair/family              #{"frontals"}
           :hair/color.process       #{"natural" "dyed"}
           :header/title             "Virgin Frontals"
           :hair/source              #{"virgin"}
           :menu/title               "Virgin Frontals"
           :category/tags            #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials      [:catalog/department :hair/family :hair/color.process :hair/source]
           :selector/electives       [:hair/origin :hair/texture :hair/color]
           :subcategory/image-uri    "//ucarecdn.com/47e29e97-821e-4c20-aeac-35eadc1a653d/-/format/auto/-/resize/124x/"
           :copy/description         "Our lace frontal human hair pieces blend in seamlessly with our bundles and come in a variety of different combinations. Shop now to create your look."}
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
  [(merge {:catalog/category-id      "2"
           :copy/title               "Virgin Straight"
           :seo/title                "Virgin Straight Hair Extensions"
           :page/slug                "virgin-straight"
           :legacy/named-search-slug "straight"

           :catalog/department  #{"hair"}
           :hair/texture        #{"straight"}
           :hair/color.process  #{"natural" "dyed"}
           :hair/source         #{"virgin"}
           :hair/family         #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives  [:hair/family :hair/origin :hair/color :hair/base-material]
           :copy/description    (copy "For those who want it given to them"
                                      "straight, our collection of 100% virgin straight hair"
                                      "is your go-to for a sleek look with"
                                      "minimal effort.")
           :images              {:home {:filename "StraightExtensionsMayvenn.jpg",
                                        :url      "//ucarecdn.com/f4addde0-3c0e-40f8-85b0-fe2e2e96a7b5/",
                                        :alt      "Straight Hair Extensions Mayvenn"}}
           :icon                "/images/categories/straight-icon.svg"
           :subcategory/title   "Straight"
           :home/order          0
           :header/title        "Virgin Straight"}
          (category->seo "Natural Straight Extensions"
                         (copy "Straight Brazilian weave, straight Indian hair and straight Peruvian hair."
                               "Our straight bundles are sleek from root to tip.")
                         "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/")
          texture-templates)
   (merge {:catalog/category-id      "3"
           :copy/title               "Virgin Yaki Straight"
           :seo/title                "Virgin Yaki Straight Hair Extensions"
           :page/slug                "virgin-yaki-straight"
           :legacy/named-search-slug "yaki-straight"

           :catalog/department  #{"hair"}
           :hair/texture        #{"yaki-straight"}
           :hair/color.process  #{"natural" "dyed"}
           :hair/source         #{"virgin"}
           :hair/family         #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives  [:hair/family :hair/origin]
           :copy/description    (copy "Tired of having to break out the hot"
                                      "tools for a textured straight look? Our Yaki"
                                      "Straight hair collection is here to save your"
                                      "strands! Yaki Straight hair matches the rhythm of"
                                      "your natural hair that's been pressed straight or"
                                      "freshly relaxed. Your flat iron has been officially"
                                      "cancelled.")
           :images              {:home {:filename "YakiStraightExtensionsMayvenn.jpg",
                                        :url      "//ucarecdn.com/d4b4aa87-fd32-4ff3-b60a-fd1118beab05/",
                                        :alt      "Yaki Straight Hair Extensions Mayvenn"}}
           :icon                "/images/categories/yaki-straight-icon.svg"
           :subcategory/title   "Yaki Straight"
           :home/order          4
           :header/title        "Virgin Yaki Straight"}
          (category->seo "Yaki Straight Extensions"
                         (copy "Our Yaki Straight hair collection features both Peruvian and Brazilian straight hair bundles."
                               "With Lace Closure or Lace Frontals in different lengths.")
                         "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/")
          texture-templates)
   (merge {:catalog/category-id      "4"
           :copy/title               "Virgin Kinky Straight"
           :seo/title                "Virgin Kinky Straight Hair Extensions"
           :page/slug                "virgin-kinky-straight"
           :legacy/named-search-slug "kinky-straight"
           :catalog/department       #{"hair"}
           :hair/texture             #{"kinky-straight"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/color :hair/base-material]
           :copy/description         (copy "Blending is a breeze with our Kinky Straight"
                                           "hair collection! Like a fresh blow out, the"
                                           "Kinky Straight hair texture moves freely and gives"
                                           "a naturally flawless look that mimics your own"
                                           "locks.")
           :icon                     "/images/categories/kinky-straight-icon.svg"
           :subcategory/title        "Kinky Straight"
           :header/title             "Virgin Kinky Straight"}
          (category->seo "Kinky Straight Extensions"
                         (copy "100% human hair bundles and extensions from Mayvenn."
                               "Peruvian and Brazilian Kinky Straight Lace Closures and Frontals.")
                         "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/")
          texture-templates)
   (merge {:catalog/category-id      "5"
           :copy/title               "Virgin Body Wave"
           :seo/title                "Virgin Body Wave Hair Extensions"
           :page/slug                "virgin-body-wave"
           :legacy/named-search-slug "body-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"body-wave"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/color :hair/base-material]
           :copy/description         (copy "Step into the spotlight with our collection of luscious Body Wave hair."
                                           "Body Wave is unbelievably soft and goes from straight to wavy and back again with ease.")
           :images                   {:home {:filename "BodyWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/cbf5424f-3bab-4c6b-9fd1-328e9d94e564/",
                                             :alt      "Body Wave Hair Extensions Mayvenn"}}
           :icon                     "/images/categories/body-wave-icon.svg"
           :subcategory/title        "Body Wave"
           :home/order               1
           :header/title             "Virgin Body Wave"}
          (category->seo "Body Wave Extensions"
                         (copy "Malaysian and Peruvian body wave silk, lace and 360 frontal bundles."
                               "Unbelievably soft and goes from straight to wavy and back again.")
                         "//ucarecdn.com/445c53df-f369-4ca6-a554-c9668c8968f1/")
          texture-templates)
   (merge {:catalog/category-id      "6"
           :copy/title               "Virgin Loose Wave"
           :seo/title                "Virgin Loose Wave Hair Extensions"
           :page/slug                "virgin-loose-wave"
           :legacy/named-search-slug "loose-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"loose-wave"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/color :hair/base-material]
           :copy/description         (copy "For hair that holds a curl beautifully,"
                                           "our collection of 100% virgin Loose Wave hair"
                                           "is the perfect foundation for all your carefree,"
                                           "flirty, wavy looks.")
           :images                   {:home {:filename "LooseWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/c935b035-3b7d-4262-a750-a5fa3b559721/",
                                             :alt      "Loose Wave Hair Extensions Mayvenn"}}
           :icon                     "/images/categories/loose-wave-icon.svg"
           :subcategory/title        "Loose Wave"
           :home/order               2
           :header/title             "Virgin Loose Wave"}
          (category->seo "Loose Wave Extensions"
                         (copy "Mayvenn’s Brazilian, Peruvian and Indian loose wave bundles."
                               "Also includes loose wave lace closures. All are 100% virgin Loose Wave hair.")
                         "//ucarecdn.com/31be9341-a688-4f03-b754-a22a0a1f267e/")
          texture-templates)
   (merge {:catalog/category-id      "7"
           :copy/title               "Virgin Water Wave"
           :seo/title                "Virgin Water Wave Hair Extensions"
           :page/slug                "Virgin-water-wave"
           :legacy/named-search-slug "water-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"water-wave"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin]
           :copy/description         (copy "Ride the lush, carefree waves of the bundles,"
                                           "closures, and frontals in our Water Wave hair"
                                           "collection. For curls you can rock everywhere from"
                                           "the office to your tropical vacation, make a"
                                           "statement with Water Wave hair.")
           :icon                     "/images/categories/water-wave-icon.svg"
           :subcategory/title        "Water Wave"
           :header/title             "Virgin Water Wave"}
          (category->seo "Water Wave Extensions"
                         (copy "Water Wave Bundles, Closures, and Frontals."
                               "Peruvian and Brazilian bundles."
                               "Mayvenn has hair extensions, bundles, closures, and frontals.")
                         "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/")
          texture-templates)
   (merge {:catalog/category-id      "8"
           :copy/title               "Virgin Deep Wave"
           :seo/title                "Virgin Deep Wave Hair Extensions"
           :page/slug                "virgin-deep-wave"
           :legacy/named-search-slug "deep-wave"
           :catalog/department       #{"hair"}
           :hair/texture             #{"deep-wave"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/color :hair/base-material]
           :copy/description         (copy "Reigning supreme in versatility, the Deep Wave hair collection features"
                                           "a soft, spiral wave full of body and bounce. Our deep wave hair is perfect"
                                           "for those who want big waves that make an even bigger splash.")
           :images                   {:home {:filename "DeepWaveExtensionsMayvenn.jpg",
                                             :url      "//ucarecdn.com/2574ca60-0336-4c42-9087-159740bafdd2/",
                                             :alt      "Deep Wave Hair Extensions Mayvenn"}}
           :icon                     "/images/categories/deep-wave-icon.svg"
           :subcategory/title        "Deep Wave"
           :home/order               3
           :header/title             "Virgin Deep Wave"}
          (category->seo "Deep Wave Extensions"
                         (copy "Deep Wave bundles and closures, including Brazilian, Peruvian and Indian Deep Wave."
                               "Soft, spiral wave full of body and bounce.")
                         "//ucarecdn.com/49cc5837-8321-4331-9cec-d299d0de1887/")
          texture-templates)
   (merge {:catalog/category-id      "9"
           :copy/title               "Virgin Curly"
           :seo/title                "Virgin Curly Hair Extensions"
           :page/slug                "virgin-curly"
           :legacy/named-search-slug "curly"
           :catalog/department       #{"hair"}
           :hair/texture             #{"curly"}
           :hair/color.process       #{"natural" "dyed"}
           :hair/source              #{"virgin"}
           :hair/family              #{"bundles" "closures" "frontals" "360-frontals"}
           :selector/essentials      [:catalog/department :hair/texture :hair/color.process :hair/source]
           :selector/electives       [:hair/family :hair/origin :hair/base-material]
           :copy/description         (copy "Let your bold, beautiful curls take center stage! Our curly hair collection is a tight,"
                                           "kinky curl perfect for creating voluminous coily styles that mimic natural 3C hair textures.")
           :icon                     "/images/categories/curly-icon.svg"
           :subcategory/title        "Curly"
           :header/title             "Virgin Curly"}
          (category->seo "Curly Extensions"
                         (copy "Shop our Brazilian curly bundle, Peruvian Curly Bundle,"
                               "Peruvian Curly Lace closures and Curly Lace frontals."
                               "Perfect for creating voluminous coily styles.")
                         "//ucarecdn.com/128b68e2-bf3a-4d72-8e39-0c71662f9c86/")
          texture-templates)])

(def wigs
  [(merge {:catalog/category-id "13"
           :footer/order        3
           :footer/title        "Wigs"
           :flyout-menu/order   3
           :flyout-menu/title   "Wigs"
           :header/title        "Wigs"

           :page/icp?            true
           :category/new?        true
           :category/description (copy "Want a fun, protective style that switches up your look,"
                                       "color or hair length instantly?"
                                       "Human hair wigs are the perfect choice.")
           :copy/title           "Human Hair Wigs"
           :page/slug            "wigs"
           :seo/title            "Wigs"

           ;; TODO: GROT once old category page is retired
           :copy/description              (copy "These units will be your go-to protective style"
                                                "for achieving a brand new look."
                                                "With options ranging from 360 to Ready to Wear,"
                                                "there’s a wig available for each of your alter egos.")
           :catalog/department            #{"hair"}
           :hair/family                   #{"360-wigs" "lace-front-wigs" "ready-wigs"}
           :selector/essentials           [:hair/family :catalog/department]
           :selector/electives            [:hair/family :hair/texture :hair/origin]
           :page/title                    "Human Hair Wigs: Natural Hair Lace Wigs | Mayvenn"
           :page.meta/description         (copy "Mayvenn’s virgin human hair wigs allow you to achieve a new look in minutes"
                                                "& come in different variations such as Brazilian, Malaysian, straight,"
                                                "& deep wave.")
           :opengraph/title               (copy "Mayvenn 360 and Lace Frontal Wigs - Free shipping."
                                                "Free 30 day returns. Made with 100% virgin human hair.")
           :opengraph/description         (copy "100% virgin human hair, machine-wefted and backed by our"
                                                "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                                "your unique look using the built-in combs and adjustable strap.")
           :product-list/title            "Shop All Wigs"
           :subcategories/ids             ["24" "26" "25"]
           :subcategories/layout          :list
           :subsections/category-selector :hair/family
           :subsections                   {"lace-front-wigs" {:order         0
                                                              :title/primary "Lace Front Wigs"}
                                           "360-wigs"        {:order         1
                                                              :title/primary "360 Wigs"}
                                           "ready-wigs"      {:order         2
                                                              :title/primary "Ready to Wear Wigs"}}
           :content-block/type            :about-attributes ;; incase we have different templates in the future
           :content-block/title           "Wigs 101:"
           :content-block/header          "How to Choose"
           :content-block/summary         (str "There are a few main factors to consider "
                                               "when you’re choosing a wig. When you have a "
                                               "good sense of the look you want to achieve, your "
                                               "lifestyle and your budget, the rest will fall "
                                               "into place. Ask yourself the density, lace color, "
                                               "length of hair you want, and if you prefer virgin "
                                               "hair or dyed hair.")
           :content-block/sections        [{:title "Cap Size"
                                            :body  "Cap size ranges between 20-21 inches. If for any reason your wig doesn’t fit, reach out to Customer Service for details to return or exchange your product."}
                                           {:title "Density"
                                            :body  "The fullest density clocks in at 200% - other measures are 180, 150 and 130. If the style you’re planning needs a lot of thickness, you should choose a higher density like 180 or 200. If you only need a little, consider 130 or 150."}
                                           {:title "Lace Color"
                                            :body  "For a wig that blends in and looks as natural as possible, you’ll want to choose a lace backing shade that most closely matches your skin tone."}
                                           {:title "Length"
                                            :body  "Short and sassy or drama down to your ankles? The choice is yours! Available in lengths ranging from 10” to 24”."}
                                           {:title "Virgin & Dyed"
                                            :body  "If you want to play with color, it helps to choose a wig that can be dyed—in other words, you’ll need a virgin wig. Or, you could choose a blonde or platinum wig and have it dyed the color you want."}]}
          wig-templates)
   (merge {:catalog/category-id "24"
           :category/new?       true

           :copy/title "Virgin Lace Front Wigs"
           :page/slug  "virgin-lace-front-wigs"
           :seo/title  "Virgin Lace Front Wigs"

           :copy/description      (copy "With the lace base in front only,"
                                        "these are ideal for exploring new ways to part your hair."
                                        "Ours are made with virgin lace & real human hair.")
           :subcategory/image-uri "//ucarecdn.com/71dcdd17-f9cc-456f-b763-2c1c047c30b4/-/format/auto/-/resize/124x/"

           :catalog/department  #{"hair"}
           :hair/family         #{"lace-front-wigs"}
           :selector/essentials [:hair/family :catalog/department]
           :selector/electives  [:hair/texture :hair/origin]

           :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our"
                                        "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                        "your unique look using the built-in combs and adjustable strap.")}
          (category->seo "Lace Front Wigs: Human Hair Lace Front Wigs"
                         (copy "Mayvenn’s human hair lace front wigs mimic a natural hairline"
                               "and come in different variations such as Brazilian, Malaysian,"
                               "straight, and deep wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/")
          wig-templates)
   (merge {:catalog/category-id "25"
           :category/new?       true

           :copy/title "Ready to Wear Wigs"
           :page/slug  "ready-wear-wigs"
           :seo/title  "Ready-to-Wear Wigs"

           :copy/description      (copy "Made of authentic and high-quality human hair,"
                                        "ready to wear wigs are a quick,"
                                        "convenient way to change up your look instantly.")
           :subcategory/image-uri "//ucarecdn.com/a4f7ad94-3c2c-41aa-be4d-94d9d83b1602/-/format/auto/-/resize/124x/"

           :catalog/department    #{"hair"}
           :hair/family           #{"ready-wigs"}
           :selector/essentials   [:hair/family :catalog/department]
           :selector/electives    [:hair/texture]
           :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our"
                                        "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                        "your unique look using the built-in combs and adjustable strap.")}
          (category->seo "Ready-to-Wear Wigs: Short, Bob, Side-Part & More"
                         (copy "Mayvenn’s ready-to-wear human hair lace wigs provide a quick style switch-up and"
                               "come in different variations such as Brazilian, straight, and loose wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/")
          wig-templates)
   (merge {:catalog/category-id "26"
           :category/new?       true

           :copy/title "Virgin 360 Lace Wigs"
           :page/slug  "virgin-360-wigs"
           :seo/title  "Virgin 360 Lace Wigs"

           :copy/description      (copy "Ideal for ponytails, these wigs are denser & fuller."
                                        "360 wigs have lace around the entire crown of your head with a cap in the middle.")
           :subcategory/image-uri "//ucarecdn.com/fe34e6e9-8927-4b62-94ac-91b37f0a137f/-/format/auto/-/resize/124x/"

           :catalog/department    #{"hair"}
           :hair/family           #{"360-wigs"}
           :selector/essentials   [:hair/family :catalog/department]
           :selector/electives    [:hair/texture :hair/origin]
           :opengraph/description (copy "100% virgin human hair, machine-wefted and backed by our"
                                        "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                        "your unique look using the built-in combs and adjustable strap.")}
          (category->seo "360 Lace Wigs: Human Hair 360 Lace Wigs"
                         (copy "Mayvenn’s human hair 360 lace wigs give you all around protection and"
                               "come in different variations such as Brazilian, Malaysian, straight,"
                               "and deep wave.")
                         "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/")
          wig-templates)])

(def mayvenn-install-eligible
  [{:catalog/category-id            "23"
    :experience/exclude             #{"mayvenn-classic"}
    :catalog/department             #{"hair"}
    :category/show-title?           true
    :category/new?                  true
    :copy/description               "Get a free Mayvenn Install by a licensed stylist when you purchase 3 or more items. "
    :copy/learn-more                [events/popup-show-consolidated-cart-free-install]
    :copy/title                     "Mayvenn Install"
    :hair/family                    #{"bundles" "closures" "frontals" "360-frontals"}
    :page/slug                      "mayvenn-install"
    :page/title                     "Mayvenn Install Eligible | Mayvenn"
    :page.meta/description          (copy "Mayvenn’s Natural Lace Front Wigs and 360 Wigs."
                                          "Comes in different variations such as Brazilian and Malaysian, straight, deep wave and loose wave.")
    :promo.mayvenn-install/eligible #{true}
    :opengraph/description          (copy "100% virgin human hair, machine-wefted and backed by our"
                                          "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                          "your unique look using the built-in combs and adjustable strap.")
    :opengraph/title                (copy "Mayvenn 360 and Lace Frontal Wigs - Free shipping."
                                          "Free 30 day returns. Made with 100% virgin human hair.")
    :selector/electives             [:hair/texture :hair/family :hair/origin :hair/color]
    :selector/essentials            [:catalog/department :promo.mayvenn-install/eligible]
    :subsections/category-selector  :hair/texture
    :subsections                    {"straight"       {:title/primary "Straight"
                                                       :order         1}
                                     "yaki-straight"  {:title/primary "Yaki Straight"
                                                       :order         2}
                                     "kinky-straight" {:title/primary "Kinky Straight"
                                                       :order         3}
                                     "body-wave"      {:title/primary "Body Wave"
                                                       :order         4}
                                     "loose-wave"     {:title/primary "Loose Wave"
                                                       :order         5}
                                     "water-wave"     {:title/primary "Water Wave"
                                                       :order         6}
                                     "deep-wave"      {:title/primary "Deep Wave"
                                                       :order         7}
                                     "curly"          {:title/primary "Curly"
                                                       :order         8}}}])

(def human-hair-bundles
  [(merge {:catalog/category-id           "27"
           :catalog/department            #{"hair"}
           :category/show-title?          true
           :category/new?                 false
           :flyout-menu/title             "Hair Bundles"
           :flyout-menu/order             0
           :footer/order                  0
           :footer/title                  "Hair Bundles"
           :category/description          (copy "Our collection of 100% Virgin hair is a must-have for when you want more volume, length and texture."
                                                "Switch up your look with these high-quality bundles. Buy three and the install is free.")
           :category/image-url            "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"
           :copy/description              "For those whom want it given to them straight, our collection of 100% virgin straight hair is your go-to for a sleek look with minimal effort."
           :copy/title                    "Hair Bundles"
           :hair/family                   #{"bundles"}
           :page/icp?                     true
           :page/slug                     "human-hair-bundles"
           :page/title                    "Hair Bundles: Sew-In Hair Bundles | Mayvenn"
           :page.meta/description         (copy "Mayvenn’s real human hair bundles come in different variations such as"
                                                "Brazilian, Malaysian, straight, deep wave, and loose wave. Create your look today.")
           :product-list/title            "Shop All Bundles"
           :opengraph/description         (copy "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee,"
                                                "our natural human hair bundles are the best quality products on the market and ship free!")
           :opengraph/title               "Natural Human Hair Bundles - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
           :selector/electives            [:hair/texture :hair/origin :hair/color]
           :selector/essentials           [:catalog/department :hair/family]
           :subcategories/layout          :grid
           :subcategories/ids             ["2" "3" "4" "5" "6" "7" "8" "9"]
           :subcategories/title           "Textures"
           :subsections/category-selector :hair/texture
           :subsections                   {"straight"       {:order         0
                                                             :title/primary "Straight"}
                                           "yaki-straight"  {:order         1
                                                             :title/primary "Yaki Straight"}
                                           "kinky-straight" {:order         2
                                                             :title/primary "Kinky Straight"}
                                           "body-wave"      {:order         3
                                                             :title/primary "Body Wave"}
                                           "loose-wave"     {:order         4
                                                             :title/primary "Loose Wave"}
                                           "water-wave"     {:order         5
                                                             :title/primary "Water Wave"}
                                           "deep-wave"      {:order         6
                                                             :title/primary "Deep Wave"}
                                           "curly"          {:order         7
                                                             :title/primary "Curly"}}
           :seo/sitemap                   true
           :seo/title                     "Virgin Hair Bundles"
           :content-block/type            :about-attributes ;; incase we have different templates in the future
           :content-block/title           "Hair Bundles 101:"
           :content-block/header          "How to Style"
           :content-block/summary         "With high quality bundles, the amount of hairstyles you can create are endless. Browse our selection of hair weaves here at Mayvenn. We feature virgin hair bundles that come in a variety of textures, such as curly, yaki straight, deep wave, and more."
           :content-block/sections        [{:title "Our Hair"
                                            :body  "We feature premium quality untreated Virgin Brazilian, Virgin Malaysian, and Virgin Peruvian hair weaves in many lengths and textures, from Straight to Yaki Straight to Wet & Wavy, in lengths including 16”, 18”, 20”, 22” and 24”."}
                                           {:title "How to Choose Bundles"
                                            :body  "It helps to have a hairstyle in mind when you choose your hair bundles. Whether you choose Virgin Hair Bundles and Dyed Virgin Hair Bundles, all our bundle styles are made with 100% virgin human hair, so your hair will have a natural look and feel. Whatever way you choose to wear it, we want you to wear your hair high with total confidence."}
                                           {:title "What to Know About Your Install"
                                            :body  "All hairpieces offer realistic, natural-looking styles. With multiple lengths and densities, our virgin hair bundles offer versatility in how to wear and style your hair."}
                                           {:title "Free Install"
                                            :body  "When you buy at least three bundles, closures, or frontals with us, we offer a free install with a Mayvenn stylist located near you. The install includes a shampoo and condition, braid down, sew-in, and style entirely paid for by us."}]}
          bundle-templates)])

(def the-only-stylist-exclusive
  {:catalog/category-id       "14"
   :auth/requires             #{:stylist}
   :footer/order              5
   :footer/title              "Stylist Exclusives"
   :header/title              "Stylist Exclusives"
   :flyout-menu/title         "Stylist Exclusives"
   :flyout-menu/order         5
   :copy/title                "Stylist Exclusives"
   :page/slug                 "stylist-exclusives"
   :direct-to-details/id      "49"
   :direct-to-details/slug    "rings-kits"
   :direct-to-details/sku-id  "SK3"
   :catalog/department        #{"stylist-exclusives"},
   :kits/contents             #{"rings"},
   :stylist-exclusives/family #{"kits"}
   :selector/electives        []
   :selector/essentials       [:catalog/department :kits/contents :stylist-exclusives/family]
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
           :page/title            ""
           :opengraph/title       (copy "" "")
           :page.meta/description (copy "")
           :opengraph/description (copy "")
           :page/redirect?        true
           :seo/sitemap           false})
   (merge {:catalog/category-id "12"
           :copy/title          "Closures & Frontals"
           :page/slug           "closures-and-frontals"
           :catalog/department  #{"hair"}
           :hair/family         #{"closures" "frontals" "360-frontals"}
           :hair/color.process  #{"dyed" "natural"}
           :hair/source         #{"virgin"}
           :category/tags       #{"closures-and-frontals"} ;; we need this to exclude virgin hair categories that include closures & frontals
           :selector/essentials [:catalog/department :hair/family :hair/color.process :hair/source :category/tags]
           :hamburger/order     1
           :selector/electives  []
           :copy/description    (copy "Save your precious strands and top your look"
                                      "off with the ultimate tool in protective weave"
                                      "styling. Our collection of closures and frontals blend"
                                      "seamlessly with our bundles and can be customized"
                                      "to fit your unique look.")
           :page/redirect?      true
           :seo/sitemap         false})])

(def seamless-clip-ins-category
  [(merge
    {:catalog/category-id "21"
     :catalog/department  #{"hair"}
     :category/new?       false

     :hair/family         #{"seamless-clip-ins"}
     :selector/essentials [:catalog/department :hair/family]
     :selector/electives  [:hair/weight :hair/texture :hair/color]

     :subcategory/image-uri "//ucarecdn.com/d255ccf8-75af-4729-86da-af6e15783fc2/-/format/auto/-/resize/124x/"
     :copy/title            "Clip-In Hair Extensions"
     :copy/description      (copy "Get the hair of your dreams in an instant with our seamless clip-in extensions."
                                  "Featuring a thin, polyurethane (PU) weft that flawlessly blends with your own hair."
                                  "Ditch the tracks for a clip-in experience that is truly seamless.")

     :page/title            "Clip-In Hair Extensions: Human Hair Clip-In Extensions | Mayvenn"
     :page/slug             "seamless-clip-ins"
     :page.meta/description (copy "Get the hair of your dreams with our seamless clip-in hair extensions."
                                  "Featuring a thin, polyurethane (PU) weft that flawlessly blends with your own hair.")
     :seo/title             "Clip-In Hair Extensions"

     :opengraph/title       "Mayvenn Clip-In Hair Extensions - Free shipping. Free 30 day returns. Made with 100% human hair extensions."
     :opengraph/description "Blending flawlessly with your own hair and backed by our 30 Day Quality Guarantee, our seamless clip-in extensions are the best quality products on the market and ships free!"

     :header/title "Seamless Clip Ins"
     :seo/sitemap  true}
    clip-in-tape-in-templates)])

(def the-only-tape-in-category
  (merge {:catalog/category-id "22"
          :catalog/department  #{"hair"}
          :category/new?       false

          :hair/family         #{"tape-ins"}
          :selector/essentials [:catalog/department :hair/family]
          :selector/electives  [:hair/color :hair/weight :hair/length]

          :copy/title       "Tape-In Hair Extensions"
          :copy/description "Our straight tape-in extensions lie completely flat against the head and blend seamlessly with your own hair."

          :page/slug "tape-ins"

          :subcategory/image-uri    "//ucarecdn.com/1998d5dd-51fa-4ee4-8fef-2ce0d8ed6f8e/-/format/auto/-/resize/124x/"
          :direct-to-details/id     "111"
          :direct-to-details/slug   "50g-straight-tape-ins"
          :direct-to-details/sku-id "TAPE-S-1-20"

          :header/title   "Tape Ins"
          :page/redirect? true
          :seo/sitemap    false}
         clip-in-tape-in-templates))

(def tape-ins-category
  [the-only-tape-in-category])

(def hair-extensions-category
  [(merge
    {:catalog/category-id "28"
     :catalog/department  #{"hair"}
     :category/new?       false

     :page/icp? true

     :hair/family         #{"seamless-clip-ins" "tape-ins"}
     :selector/essentials [:catalog/department :hair/family]
     :selector/electives  [:hair/weight :hair/color :hair/texture]

     :flyout-menu/title "Hair Extensions"
     :flyout-menu/order 4

     :footer/order 4
     :footer/title "Hair Extensions"

     :copy/title         "Hair Extensions"
     :product-list/title "Shop All Hair Extensions"

     :page/slug                     "hair-extensions"
     :copy/description              ""
     :category/description          (str "Ditch the tracks and opt for hair that blends in seamlessly. "
                                         "Mayvenn human hair extensions are made with a thin polyurethane "
                                         "weft that blends with your hair for a natural look.")
     :seo/sitemap                   false
     :seo/title                     "Hair Extensions"
     :page/title                    "Hair Extensions: Real Human Hair Extensions | Mayvenn"
     :page.meta/description         (str "Mayvenn’s real human hair extensions come in different variations"
                                         " such as Brazilian and Malaysian, straight, deep wave and loose wave."
                                         " Shop now.")
     :category/image-url            "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/"
     :opengraph/title               "Real Human Hair Extensions - Free shipping. Free 30 day returns. Made with 100% virgin human hair."
     :opengraph/description         "Blending flawlessly with your own hair and backed by our 30 Day Quality Guarantee, our seamless clip-in and tape-in extensions are the best quality products on the market and ships free!"
     :subsections/category-selector :hair/family
     :subcategories/ids             ["21" "22"]
     :subcategories/layout          :list
     :subsections                   {"seamless-clip-ins" {:order         0
                                                          :title/primary "Clip-in Hair Extensions"}
                                     "tape-ins"          {:order         1
                                                          :title/primary "Tape-in Hair Extensions"}}
     :content-block/type            :about-attributes ;; incase we have different templates in the future
     :content-block/title           "Hair Extensions 101:"
     :content-block/header          "How to Choose"
     :content-block/summary         "No matter what kind of transformation you’re looking for, our seamless clip-in & tape-in hair extensions will help you achieve your desired look in an instant. Our clip-ins & tape-ins are perfect for when you want a natural-looking appearance that complements your own hair while giving that much coveted oomph-factor."
     :content-block/sections        [{:title "Tape-In Hair Extensions"
                                      :body  "Our seamless tape-in hair extensions have a thin weft that flawlessly blends with your own hair, so you can have the hair of your dreams."}
                                     {:title "Clip-In Hair Extensions"
                                      :body  "With a thin weft that blends into your hair seamlessly, our clip-in human hair extensions help you create the hair of your dreams."}
                                     {:title "Human Hair Extensions"
                                      :body  "Our human hair extensions are a must-have for creating the hair you’ve always wanted. Our high-quality extensions are easy to install and available in many textures like Straight, Yaki Straight, Kinky Straight, Body Wave, Loose Wave, Water Wave, Deep Wave, and Curly, plus multiple lengths for all kinds of hairstyles."}]}
    clip-in-tape-in-templates)])

(def menu-categories
  (concat virgin-hair
          closures))

(def initial-categories
  (concat wigs
          hair-extensions-category
          mayvenn-install-eligible
          stylist-exclusives
          virgin-hair-nav-roots
          menu-categories
          seamless-clip-ins-category
          tape-ins-category
          human-hair-bundles))

