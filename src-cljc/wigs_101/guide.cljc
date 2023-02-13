(ns wigs-101.guide
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(component/defcomponent template
  [{:keys [sections] :header/keys [title copy]} _ _]
  [:div
   [:div.bg-warm-gray.center
    [:h1.canela.title-1.py8 title]
    [:div.content-2.mx-auto.max-580
     (for [paragraph copy]
       [:div.pb3 paragraph])]]

   (for [{:keys [ucare-id heading copy target id]} sections]
     [:div.max-580.mx-auto.p3
      {:key id}
      [:div.mb3 (ui/defer-ucare-img {:class      "block col-12"
                                     :smart-crop "600x400"
                                     :alt        ""}
                  ucare-id)]
      [:h2.proxima.title-3.shout heading]
      [:div copy]
      [:div.shout.my2 (ui/button-small-underline-primary
                       (merge
                        (apply utils/route-to target)
                        {:data-test (str "go-to-" id)})
                       "Read More")]])])

(defn query
  [data]
  {:header/title    "Wigs 101 Guide"
   :header/copy     ["Wigs offer you the freedom to choose how you want to express your personality, yet the options may feel daunting. From style and texture, to terminology and care, where do you begin?"
                     "Learning all about wigs shouldn’t overwhelm you, which is why we’ve created this easy-to-follow guide for beginners or experts to help you choose a wig that feels good, looks good, and most importantly, lets you confidently embrace all of your beauty."]
   :sections [{:ucare-id "5f329e01-68a8-48a5-b8a0-8950adc1950f"
               :heading  "Understanding Wigs"
               :copy     "Let’s start with wigs 101. There are SO many different types of wigs out there, but they aren’t all created equal. Before diving into the details of installation, style, and care, we want to help you understand the terminology and differences between all your different wig options so you feel good about your wig choice."
               :target   [events/navigate-wig-hair-guide {}]
               :id       "understanding-wigs"}
              {:ucare-id "cf78f8f8-703e-4330-a18c-0b01c6c891ba"
               :heading  "Buying a Wig"
               :copy     "Investing in a wig, or multiple wigs, may come with many questions: How long is it? Is it 100% virgin hair and what does that mean? Is human hair better than synthetic hair? And probably many more! In this guide, we’ll answer all your burning questions about wigs, so you can make a wig purchase you’re proud of."
               :target   [events/navigate-wig-buying-guide {}]
               :id       "buying-wigs"}
              {:ucare-id "43b18ad2-7b22-4fd9-8911-41021829eb77"
               :heading  "Installing a Wig"
               :copy     "There are a number of ways to install a wig from glue and tape, to wig grips and sewing. Which one is best for you? Each method offers different benefits and drawbacks, so our simple guide is here to help you choose the installation that works best with your hair and wig type to achieve the look you’ve always wanted."
               :target   [events/navigate-wig-installation-guide {}]
               :id       "installing-wigs"}
              {:ucare-id "188078f5-8f97-42f8-bc01-6573790e58e7"
               :heading  "Caring for a Wig"
               :copy     "Giving your wig some love and attention will ensure that it lasts. Over time, wigs can get dirty, oily, and tangled, and need to be cared for just like your natural hair. With proper washing, drying, brushing, styling, and storage, your wig will keep its vigor for many months to come–and our guide is the perfect place to learn all about wig care."
               :target   [events/navigate-wig-care-guide {}]
               :id       "wig-care"}
              {:ucare-id "008dbfab-304f-4d23-9057-5d37d3ab39ae"
               :heading  "Styling a Wig"
               :copy     "One of the benefits of owning wigs is their versatility. On any given day, you can change up your style: short, long, curly, straight, up-do, side part. But certain wigs are more suited for specific styles, so check out our wigs for beginners to advanced users guide to learn all about wigs and how to style them to achieve your desired look."
               :target   [events/navigate-wig-styling-guide {}]
               :id       "styling-wigs"}]})

(defn footer
  [extra-target]
  (let [links-to-pages [{:target [events/navigate-wigs-101-guide]
                         :id     "guide"
                         :copy   "Wig Guide"}
                        {:target [events/navigate-wig-hair-guide]
                         :id     "hair-guide"
                         :copy   "Understanding Wigs"}
                        {:target [events/navigate-wig-buying-guide]
                         :id     "buying-guide"
                         :copy   "Buying a Wig"}
                        {:target [events/navigate-wig-installation-guide]
                         :id     "install-guide"
                         :copy   "Installing a Wig"}
                        {:target [events/navigate-wig-care-guide]
                         :id     "care-guide"
                         :copy   "Wig Care"}
                        {:target [events/navigate-wig-styling-guide]
                         :id     "styling-guide"
                         :copy   "Styling a Wig"}]
        footer-links (remove #(= extra-target (:target %)) links-to-pages)]
    [:div.bg-cool-gray.mx-auto.justify-between.flex.max-960.p5.flex-wrap
     (for [{:keys [target id copy]} footer-links]
       (ui/button-small-underline-primary
        (merge
         (apply utils/route-to target)
         {:data-test id
          :class     "my2"})
        copy))]))

(component/defcomponent sub-guide-template
  [{:keys [sections] :header/keys [title copy] :footer/keys [route]} _ _]
  [:div
   [:div.bg-pale-purple.center.pb5
    [:h1.canela.title-1.py8 title]
    [:div.content-2.mx-auto.max-580
     (for [paragraph copy]
       [:div.pb3 paragraph])]]

   (for [{:keys [id copy] :as section} sections]
     [:div.max-580.mx-auto.p3
      {:key id}
      (ui/blog-preview-component section)
      [:div.content-3
       (for [paragraph copy]
         [:div.pt3 paragraph])]])
   (footer route)])

(defn hair-query
  [data]
  {:header/title "Wig Hair Guide: Understanding Wigs"
   :header/copy  ["Before you buy a wig, there are a few basics that are helpful to understand about how wigs are constructed and what type of hair is used. Because, while they may all look the same in a picture, there are some important differences."
                  "In this section of the wig buying guide, we’ll cover all the essentials you need to know about wig hair fiber, wig density, length, and texture."]
   :footer/route [events/navigate-wig-hair-guide]
   :sections     [{:blog/ucare-id "1015ce05-8a53-46af-9a13-ec7ea1957b70"
                   :blog/heading  "Complete Guide to Lace Wigs"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/lace-front-wigs-guide"
                   :copy          ["Have you spent a ton of time online or at the salon trying to figure out what are lace wigs? Or, which wig material is best for you? You’re not alone."
                                   "Lace wigs are amazing because they offer you so many options such as lace frontals and closures, full lace, and 360 lace wigs. While all are constructed with lace (rather than cotton or nylon), they each offer different benefits depending on the look you want to achieve."
                                   "In the following sections, we’ll break it all down to help you understand the essentials for choosing a lace wig including wig fibers, density, and texture."]
                   :id            "guide-to-wigs"}
                  {:blog/ucare-id "38d02014-13ec-4b11-b390-133825c9bd47"
                   :blog/heading  "Different Types of Wig Hair Fiber"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/different-types-of-wig-hair-fiber-how-to-care-for-them"
                   :copy          ["Wig hair fibers can be broken down into two categories: human and synthetic. Both are popular choices for various reasons, but they require a completely different type of care and consideration, particularly when choosing a wig."
                                   "Longevity is often a concern when investing in a wig and proper care for both human and synthetic units is vital to keeping your wig lasting for the long-term. Factors like washing, drying, how long the unit is installed, and product used all contribute to keeping your wig fresh."
                                   "Beyond the simple care of your human or synthetic wig, you also want to consider wig density. The next section will give you a glance at why hair density matters and how it can be a total game-changer to getting the look you’ve always wanted."]
                   :id            "hair-fiber"}
                  {:blog/ucare-id "7b617a6a-fe6e-4292-9a9b-092a52012532"
                   :blog/heading  "Wig Hair Density Chart"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wig-density-chart"
                   :copy          ["Investing in a high-quality unit is key to getting the best wig. One way to make sure you find just the unit you want is to understand wig density."
                                   "Wig density is expressed as a percentage and tells you how much hair is actually on the unit. The more hair in the wig, the thicker it is and the higher the percentage; the less hair, the thinner it is and the lower the percentage. Yet, even if you choose a density you love, there are still so many options for customization and styling like plucking and even cutting a wig to give it a more natural appearance. We’ll explore all of these topics in this section in addition to gearing you up to learn all about wig texture."]
                   :id            "density-chart"}
                  {:blog/ucare-id "71dff79a-1cb3-44bd-be96-99d587b043be"
                   :blog/heading  "Wig Length By Texture Guide"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wig-length-guide/"
                   :copy          ["When shopping for wigs, you’ll find that they are offered in various lengths. But what if you want a curly, or straight style–how does the wig length apply?"
                                   "The measurement listed on the wig is an approximation of where the hair on the unit will fall after it’s installed. Those measurements are generally taken from the root of the hair around the crown, to the ends. While curly and wavy styles are measured in a similar way, it’s important to remember that if you pull the hair straight, it will have a different measurement. Remember that will come in handy if you decide to straighten the wig or trim it."]
                   :id            "texture-guide"}]})

(defn buying-query
  [data]
  {:header/title "Guide to Buying a Wig"
   :header/copy  ["Buying a wig can be a big investment, especially if you’re purchasing multiple units to round out your collection of looks. So, it’s important to understand the ins-and-outs of how to choose the right wig for your head even if you’re a beginner a seasoned wearer."
                  "One of the best ways to ensure you’re getting the right wig is take complete measurements of your head. After you know your wig size, you’ll feel more empowered to ask questions and make a decision about your next wig investment."
                  "In this section, we’ll make sure you have all the step-by-step information you need including how to measure your head for wig size, texture, origin, color, and hair type."]
   :footer/route [events/navigate-wig-buying-guide]
   :sections     [{:blog/ucare-id "3817c581-1f1d-46be-a9f3-7bd3e124c784"
                   :blog/heading  "How to Measure Your Head for a Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-measure-head-for-wig-size"
                   :copy          ["Buying a wig only to discover that it doesn’t fit or stay secure is the worst. Yet, finding a wig with the perfect fit will have you looking and feeling your best. But how do you know which size to buy? That’s what this chapter is all about!"
                                   "The best way to ensure you get the right size is to measure your head for a wig. There are a number of measurements to include, and we’re here to help you figure out which ones to take and how to do it. And it only takes a few minutes."]
                   :id            "measuring-head"}
                  {:blog/ucare-id "37e28034-4e10-454f-9955-033384557ea6"
                   :blog/heading  "Wig Buying Tips for Beginners"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wig-buying-guide"
                   :copy          ["Wig size isn’t the only consideration when perusing options. You also want to think about the style, color, dying options, density, and texture–and our tips for buying a wig is the perfect place to start for the basics. If you’ve got questions, this is your go-to, quick source to understanding everything you need to know about buying a wig."]
                   :id            "beginner-tips"}
                  {:blog/ucare-id "438f06fb-43fe-472d-964d-6922653b6a7e"
                   :blog/heading  "How Long Do Human Hair and Synthetic Wigs Last?"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-long-do-wigs-last"
                   :copy          ["One of the more common questions about wigs is how long they last, especially when you’re preparing to make a major wig purchase. You definitely want to invest in a wig that’s going to give you some longevity so you can wear your favorite style for a long time."
                                   "An important reminder is that generally, human hair wigs last longer than synthetic wigs. With proper care, some human hair wigs may last over a year, while synthetic wigs may last up to six months. Ultimately, regardless of the type of hair you choose, the less styling and wear, the longer the wig will last, so check out our guide to determining how long human hair and synthetic wigs last."]
                   :id            "guide-to-wigs"}]})

(defn installation-query
  [data]
  {:header/title "Installing a Wig"
   :header/copy  ["Installing a wig can feel intimidating, especially if you’re new to wearing a wig. But we’re here to tell you all about how to install a wig (for beginners or experienced individuals) so you can rock your new unit without worry."
                  "Depending on the type of wig you purchase, there are a number of options for installing. There are lace front wigs, full lace, 360 lace, and Ready to Wear wigs, all with different benefits. Before you go at the install on your own, make sure you check out our wig installation guide, which will help you cut a lace front wig, secure it, install, and choose the best wig to get a natural look that will last."]
   :footer/route [events/navigate-wig-installation-guide]
   :sections     [{:blog/ucare-id "7187632b-3a7a-495e-8841-e1f8e1c24a37"
                   :blog/heading  "Stocking Cap Method"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wig-cap-method-foolery"
                   :copy          ["Stocking caps have seen some popularity as a way to install a lace frontal, and many still claim that it’s ideal for keeping the lace secure. But it’s also time-consuming and requires a lot of product."
                                   "Yet, if you really want a seamless look, it might be just the thing for you. So, how do you use a stocking cap to install your wig? How long does an install last? And what do you need? Check out our quick Q & A to learn more about stocking caps and then learn more about how to cut your lace front wig!"]
                   :id            "stocking-cap"}
                  {:blog/ucare-id "d4681532-7c00-4c65-a1df-c31e26f59b33"
                   :blog/heading  "How to Cut a Lace Front Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-cut-a-lace-front-wig"
                   :copy          ["Lace front wigs are easily recognizable because of the lace hairline that extends over the forehead and helps you customize the unit’s hairline. Once it’s installed, though, that lace strip is no longer needed. So, how do you cut a lace front wig?"
                                   "It’s simple to cut the lace with a few quick tips to make sure you get the best install. Using a couple of tools you probably already have, you can customize the lace front so it blends seamlessly with your hairline. Follow these steps for how to cut a lace wig and get just the look you want!"]
                   :id            "cut-lace-front"}
                  {:blog/ucare-id "bf19e11d-5877-4337-9994-e277ed3347c2"
                   :blog/heading  "3 Ways to Install a Lace Front Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-put-on-a-lace-front-wig"
                   :copy          ["When you want full coverage from ear-to-ear, a lace front wig is a great choice because it blends in with your natural hairline. There are a number of ways to install lace front wigs, including glue, tape, sew-in, and wig grips, but before you do that, you have to prep your natural hair so the wig lays flat on your head."
                                   "In this chapter, we’ll cover everything you need to know about how to prep your hair and share our favorite ways to install a lace front wig for a flawless look that will stay secure."]
                   :id            "install-lace-front"}
                  {:blog/ucare-id "8c6148a1-2127-48b1-b277-87c05d6be294"
                  :blog/heading  "How to Secure a Wig and Make it Stay On"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-secure-a-wig"
                   :copy          ["If you’ve worn a wig, you’ve probably experienced that dreaded moment when it slips off your head. It can be embarrassing and frustrating, but you don’t have to settle for a slippery wig. With a few simple tips to secure your wig, you’ll able to shake your tresses on the dance floor, or simply take a walk around the block with full confidence."
                                   "This chapter will cover our four favorite ways to secure a wig and offer advice on choosing high-quality wigs that look natural and will stay in place."]
                  :id            "guide-to-wigs"}
                  {:blog/ucare-id "fe213329-abd7-4c6f-a3f8-be98385c3ccb"
                   :blog/heading  "6 Tips for How to Make Your Wig Look Natural"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-make-a-wig-look-natural"
                   :copy          ["A natural-looking wig is essential when you’re investing in a high-quality unit and there are a number of ways to customize your wig to give it your own style and look the way you imagined. And we’re offering our six favorite tips for making a wig look natural right here."
                                   "From plucking the hairline on a lace front, to choosing a high-quality unit, you’re sure to have the most natural-looking install with a few quick adjustments."]
                   :id            "natural"}]})

(defn care-query
  [data]
  {:header/title "Wig Care Guide"
   :header/copy  ["Aside from choosing a high-quality, human hair wig, taking care of your unit is one of the best ways to make sure it lasts. But, it’s not just your wig–your natural hair needs some love, too."
                  "Taking care of both your own hair and your wig is the secret to getting the most out of your wig purchase and we’re giving you the best wig care tips for both."]
   :footer/route [events/navigate-wig-care-guide]
   :sections     [{:blog/ucare-id "b91b6d3c-c7b8-402c-9154-dcb12bbeb185"
                   :blog/heading  "How to Care for a Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wash-care-for-human-hair"
                   :copy          ["No matter which type of wig you choose–synthetic or human hair–they require proper care to make sure they last and look amazing. Of course, washing a wig is important, but how do you do it, how often, and with what products?"
                                   "To help you with proper at-home wig care, we’re giving you the do’s and don’ts to make sure your wig is always in tip-top shape and ready to go. And, don’t forget about your natural hair. Our next chapter will give you the details on how to care for the hair underneath, too!"]
                   :id            "wig-care"}
                  {:blog/ucare-id "419d1884-2821-4575-b453-ef6686b86083"
                   :blog/heading  "How to Maintain Your Hair Underneath"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/wig-regimen-to-grow-your-hair-maintainence"
                   :copy          ["Investing in a great wig is key to getting the flaunt-worthy look you love, but it’s not just about the wig. Maintaining your natural hair underneath a wig so that it’s protected from damage is equally important."
                                   "Your natural hair may experience stress from product build-up, brushing, pulling, and the pressure of always wearing a wig. Over time, you may experience thinning hair, dryness, or other discomfort if you neglect your natural hair and scalp."
                                   "Here are our ten wig care tips to protecting your hair when wearing wigs."]
                   :id            "maintain-own-hair"}]})

(defn styling-query
  [data]
  {:header/title "Complete Guide to Styling a Wig"
   :header/copy  ["One of the great things about human hair wigs is their versatility. And not just owning multiple wigs, but their ability to be styled just like you would your natural hair."
                  "Whether you want to cut, dye, curl, or trim those baby hairs, wigs give you options to change up your look from day-to-day."
                  "Yet, taking a pair of scissors or using hair dye on your beautiful wig can be a little intimidating. To help you out, we’ve put together some of our expert tips on how to style a wig just the way you like."]
   :footer/route [events/navigate-wig-styling-guide]
   :sections     [{:blog/ucare-id "e6815c05-7f67-41fc-ab30-1e870a8612db"
                   :blog/heading  "How to Cut a Wig to Look Natural"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-cut-a-wig"
                   :copy          ["Buying a wig based on a picture, only to have it look different when you put it on can be frustrating. Or, perhaps you have a wig you want to give a new style to? In either case, you may need to trim or cut your wig to freshen it up."
                                   "Cutting a wig isn’t impossible, but does take some attention to detail, especially if you’re planning to try it on your own. Even though a stylist can help customize your wig, we’ve got a few simple ways to help you do it on your own, including bang trims, texturizing, and giving your wig a fun, sassy new style. Even if you’re not a professional stylist, you can make some simple snips to give your wig a total transformation."]
                   :id            "natural-cut"}
                  {:blog/ucare-id "51e44ac7-1b45-4cb4-a880-79fb67789feb"
                   :blog/heading  "How to Dye a Human Hair Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-dye-a-human-hair-wig"
                   :copy          ["Color is a great way to change up your look from subtle and reserved one day, to bold the next. One of the benefits of investing in a 100% virgin, human hair wig is that it can be dyed just like your natural hair."
                                   "Our guide for how to dye a human hair wig will help you choose a color that suits your skin tone, show you the tools you need, and the process to getting a color that really wows before you start styling."]
                   :id            "dye"}
                  {:blog/ucare-id "98d2eb47-9f44-4863-a87f-5b244202eabd"
                   :blog/heading  "How to Style a Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-style-a-wig"
                   :copy          ["Now that you’ve bought your wig, you’ve got some options for styling. Perhaps you’ve already got the cut you love, but you still want to give it some body, a little curl, or a sleek, straight style?"
                                   "The beauty of a high-quality, human hair wig is that it acts just like natural hair, so you can style in a number of ways. Before you get to making it your own, we’ve got some tips on what tools to use and how to style your wig depending on if it’s a lace frontal or 360 lace. Then, it’s time to get to styling and curling."]
                   :id            "style"}
                  {:blog/ucare-id "0e45f56e-b34c-4fc7-9d25-73921e4afbe2"
                   :blog/heading  "How to Curl Your Wig Without Heat Damage"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-curl-your-wig-without-heat-damage"
                   :copy          ["Curling your wig is a great way to get that cute new style with little effort and cost. But you also want to avoid damaging the hair with hot tools. Thankfully, there are ways to curl a wig without causing heat damage, which can shorten the life of the unit."
                                   "Some simple prep like brushing the hair, using rollers, and using the right amount of heat can spare the hair. No more fried ends, breakage, or melted hair."]
                   :id            "curl"}
                  {:blog/ucare-id "f12bd73e-4f73-4144-ac93-b39fd52b3357"
                   :blog/heading  "How to Make Baby Hair Edges on a Lace Front Wig"
                   :blog/target   "https://shop.mayvenn.com/blog/hair/how-to-create-baby-hair-edges"
                   :copy          ["Baby hairs are a key element in making a wig look natural. Too much hair around the hairline can look dense and unnatural, but with a quick pluck, you can customize your wig just the way you like AND look super natural. With our tips, you can create and style baby hair edges like a professional."
                                   "While some wigs come pre-plucked and with baby hairs, this section is all about creating and styling your own edges to give your wig its own unique look. And it takes just a couple of simple tools to get started."]
                   :id            "edges"}]})

(defn built-component-hair
  [data opts]
  (component/build sub-guide-template (hair-query data) opts))

(defn built-component-buying
  [data opts]
  (component/build sub-guide-template (buying-query data) opts))

(defn built-component-installation
  [data opts]
  (component/build sub-guide-template (installation-query data) opts))

(defn built-component-care
  [data opts]
  (component/build sub-guide-template (care-query data) opts))

(defn built-component-styling
  [data opts]
  (component/build sub-guide-template (styling-query data) opts))

(defn built-component
  [data opts]
  (component/build template (query data) opts))
