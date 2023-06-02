(ns storefront.components.guide-clipin-extensions
  (:require  #?@(:cljs [[storefront.history :as history]])
             [catalog.categories :as categories]
             [mayvenn.visual.tools :as vt]
             [storefront.component :as c]
             [storefront.components.svg :as svg]
             [storefront.components.tabs-v202105 :as tabs]
             [storefront.components.ui :as ui]
             [storefront.events :as e]
             [storefront.keypaths :as k]
             [storefront.platform.component-utils :as utils]))

;; If other categories get guides as well, consider generalizing this (and driving it from Contentful!)
;; rather than duplicating it.

(c/defcomponent video-transcript-modal
  [{:keys [transcript]} owner opts]
  (let [close-attrs (utils/route-to e/navigate-guide-clipin-extensions)]
    (ui/modal
     {:close-attrs close-attrs}
     [:div.bg-white.stretch.p4
      [:div.bg-white.col-12.flex.justify-between.items-center
       [:div {:style {:min-width "42px"}}]
       [:div "Transcript"]
       [:a.p3
        (merge close-attrs
               {:data-test "close-transcript-modal"})
        (svg/x-sharp {:style {:width  "12px"
                              :height "12px"}})]]
      [:div.m3 transcript]])))

(c/defcomponent carousel
  [{:keys [title modal-id elements]} _ _]
  [:div
   (when-let [selected-element (first (filter #(= (:id %) modal-id) elements))]
     (c/build video-transcript-modal selected-element))
   (let [element-width "240px"]
     [:div.proxima
      [:div.text-3xl.m3 title]
      [:div.flex.overflow-scroll.hide-scroll-bar
       (map-indexed
        (fn [i {:keys [video-src caption cta/copy cta/target]}]
          [:div.m3.flex-1 {:key   i
                           :style {:width element-width}}
           [:video
            {:controls         "controls"
             :width            element-width
             :aria-describedby (str "video-" i "-caption")
             :src              video-src}]
           [:div.mt2.text-sm {:id (str "video-" i "-caption")} caption]
           (when target
             (ui/button-medium-underline-black
              (apply utils/route-to target)
              copy))])
        elements)]])])

(c/defcomponent faqs
  [{:keys [title selected-tab tabs]} _ _]
  [:div.m3.proxima
   [:div.text-3xl.mb6 title]
   (c/build tabs/component {:tabs         tabs
                            :selected-tab selected-tab})
   (let [{:keys [title id question-answers]} (first (filter #(= (:id %) selected-tab) tabs))]
     [:div.m2.text-base {:key id}
      [:div.my1.text-2xl title]
      (map-indexed
       (fn [i {:keys [question answer]}]
         [:div {:key (str id "-" i)}
          [:div {:class "bold"} question]
          [:div answer]])
       question-answers)])])

(c/defcomponent component
  [data owner opts]
  [:div.py3.text-base.max-960.mx-auto
   (let [{:keys [target id label]} (vt/with :header.back-navigation data)]
     (ui/button-medium-underline-black
      (merge (apply utils/route-to target)
             {:aria-label label
              :data-test  id
              :class      "block mt3 mx3"})
      [:span (svg/left-arrow {:width  "12"
                              :height "12"}) " " label]))
   [:div.proxima.text-4xl.m3 (:header/title data)]
   (c/build carousel (vt/with :carousel data))
   (c/build faqs (vt/with :faqs data))])

(defn ^:private query [app-state]
  (let [category (first categories/seamless-clip-ins-category)]
    (merge
     {:header/title "Clip-In Extensions Guide" }
     #:header.back-navigation{:target [e/navigate-category (select-keys category [:catalog/category-id :page/slug])]
                              :id     "navigate-back"
                              :label  "Shop All Clip-Ins"}

     #:carousel{:title    "Video Tutorials"
                :modal-id (:modal (get-in app-state k/navigation-query-params))
                :elements [{:id         "how-to-find-the-best-shade-to-match-your-hair"
                            :video-src  "//ucarecdn.com/f792edd1-03f8-4dd9-86d7-70b9928a9a7f/"
                            :caption    "How to Find the Best Shade to Match Your Hair"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "how-to-find-the-best-shade-to-match-your-hair"}}]
                            :transcript [:div
                                         [:p "Narrator: How to find the best shade to match your hair."]
                                         [:p "Title: Step 1: Use Natural Lighting"]
                                         [:p "A woman grabs the mids and ends of her hair to examine the color."]
                                         [:p "Narrator: Use natural lighting. Stand in front of a window, or if possible, go outside for the best lighting."]
                                         [:p "Title: Step 2: Look at the Mid-Lengths to Ends of Your Hair"]
                                         [:p "The woman grabs a weft of clip-in hair extensions and compares the color to the mids and ends of her hair."]
                                         [:p "Narrator: Take a close look at the mid-lengths to ends of your hair."]
                                         [:p "Title: Step 3: Figure out if Your Hair Has Warm, Cool, or Neutral Undertones"]
                                         [:p "Narrator: Determine your hair's undertones. Look for red, orange or yellow undertones, for warm. Blues or greens indicate cool undertones. If nothing stands out, you're likely neutral. Identify your tones and choose accordingly."]
                                         [:p "The woman has installed the clip-in hair extensions. The woman moves and flips her hair to show the results. The clip-ins blend seamlessly with her natural hair."]
                                         [:p "Narrator: If you're in between shades, opt for the lighter color for a closer match. Remember, since Mayvenn clip-ins are made of human hair, your stylist can easily tone them for more customization."]]}
                           {:id         "how-to-install-clip-ins"
                            :video-src  "//ucarecdn.com/b123c0c2-0f48-482f-a024-ea6f3190e26a/"
                            :caption    "How to Install Clip-Ins"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "how-to-install-clip-ins"}}]
                            :transcript [:div
                                         [:p "Title: How to Install Your Clip-Ins"]
                                         [:p "Narrator: Want to learn how to install clip-ins with Mayvenn hair? Lay out your weft pieces, ensuring they're tangle free."]
                                         [:p "A woman has the top half of her hair pinned up with a hair clip. The lower half is left down. She grabs a weft of clip-in hair extensions and lays it across the root of the bottom half of her hair, directly below the part line."]
                                         [:p "Narrator: Use a clip or hair tie to keep your hair out of the way as you section. Match the weft size with each hair section, starting from the bottom and working your way up. Begin with a medium sized piece at the nape, and then place wider clip-in wefts around the widest parts of your head. "]
                                         [:p "The woman presses her thumb and index finger along each weft to snap each clip securely into her hair."]
                                         [:p "Narrator: Clip in the weft right below your part line in each section. It should feel secure, allowing easy movement and styling, and ensure enough room around your hairline so the clip-ins remain invisible. And there you have it. Achieve flawless hair extensions with these quick and easy steps."]
                                         [:p "The woman poses with the clip-in hair extensions fully installed and blended into her hair."]]}
                           {:id         "how-to-blend-clip-ins"
                            :video-src  "//ucarecdn.com/3b92a8bd-4b60-4425-a3f8-a42b66646ef5/"
                            :caption    "How to Make Your Clip-Ins Blend with Your Hair"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "how-to-blend-clip-ins"}}]
                            :transcript [:div
                                         [:p "Title: How to Install, Blend Your Clip-ins"]
                                         [:p "A woman wearing clip-in extensions and flipping her hair is shown."]
                                         [:p "Narrator: To achieve a flawless blend with your clip-in extensions, follow these tips."]
                                         [:p "A stylist brushes the woman's hair. "]
                                         [:p "Narrator: First, ensure your clip-ins are tangle-free before installation."]
                                         [:p "A stylist styles the woman's hair with a curling iron."]
                                         [:p "Narrator: Then, style your hair and the clip-ins together using a curling or flatiron for a seamless look."]
                                         [:p "A stylist trims the woman's hair, blending the clip-ins with her natural hair."]
                                         [:p "Narrator: Lastly, don't be afraid to consult with your stylist to lightly trim or lay the extensions for a better natural blend."]
                                         [:p "A woman wearing clip-in extensions and flipping her hair is shown again."]
                                         [:p "Enjoy an effortless hairstyle with these simple steps from Mayvenn Hair."]]}
                           {:id         "how-to-clip-ins-in-place"
                            :video-src  "//ucarecdn.com/d800b421-5605-4c08-acbc-7ba2a60e77bd/"
                            :caption    "How to Make Your Clip-Ins Stay in Place"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "how-to-clip-ins-in-place"}}]
                            :transcript [:div
                                         [:p "Title: How to Make Your Clip-ins Stay in Place"]
                                         [:p "Narrator: If your hair is fine or if you're just having trouble getting clip-in wefts to stay put, try this tip."]
                                         [:p "A stylist parts the woman's hair horizontally on one side of her head and clips the top section of hair off, leaving the bottom half down. She then takes a thin portion of the bottom section of hair and backcombs it towards the roots of the woman's hair."]
                                         [:p "Narrator: Lightly backcomb or tease your roots along your part lines. Remember to backcomb in one direction, only to minimize any unecessary friction."]
                                         [:p "The stylist places a weft of clip-ins over this section of hair at the root and clips it in place."]
                                         [:p "Narrator: Once you've done that, place the clip-ins on top of those sections, close to the base of your roots. This added texture and grip should provide extra support to keep clip-ins secure throughout the day."]
                                         [:p "The woman twirls and flips her hair to show the results of the clip-ins."]
                                         [:p "Narrator: It's important to ensure that the clip-ins are comfortable and not too tight, so leave enough room for a comfortable fit. Give it a try and enjoy a secure look with your Mayvenn Hair Clip-ins."]]}
                           {:id         "where-to-place-clip-ins"
                            :video-src  "//ucarecdn.com/22cbc48c-9a20-4e68-8b6b-abaf83127220/"
                            :caption    "Where to Place Your Clip-Ins"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "where-to-place-clip-ins"}}]
                            :transcript [:div
                                         [:p "Title: Where to Place Your Clip-ins"]
                                         [:p "A woman has her hair parted horizontally across her head, the top half sectioned into a bun. The bottom section of her hair is left down. Three wefts of clip-in hair extensions are clipped into the bottom section of her hair."]
                                         [:p "Narrator: When applying clip-ins, remember to place them according to the side of your head and the length of your part in each section. Start with medium sized pieces at the bottom, widest pieces in the middle, and smaller pieces diagonally along your parted line and in smaller sections."]
                                         [:p "The woman slowly turns her head back and forth, showing a wide weft of hair in the middle and small wefts closer to her face"]
                                         [:p "Enjoy a flawless and natural look with these placement tips with Mayvenn Hair Clip-ins."]]}]}
     #:faqs{:title        "FAQs"
            :selected-tab (or (get-in app-state (conj k/tabs :guide-clipin-extensions))
                              :choosing-your-extensions)
            :tabs         [{:title            "Choosing your extensions"
                            :id               :choosing-your-extensions
                            :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                                       :tab-id  :choosing-your-extensions}]
                            :question-answers [{:question "What’s the difference between your different color options?"
                                                :answer   [:div
                                                           [:div.mb3 "Allover color is a single allover color that is the same color from top to bottom."]
                                                           (ui/ucare-img {:width  "275px"
                                                                          :height "200px"
                                                                          :alt    "A close-up of black hair with allover color. The color is uniform throughout the hair."}
                                                                         "a88d0355-bdd8-4dc7-959f-1ecef669c76f")
                                                           [:div.mb3 "Our Balayage shades are a subtle hand-painted version of a highlight. The Balayage pieces are painted throughout the hair to add dimension."]
                                                           (ui/ucare-img {:width  "275px"
                                                                          :height "200px"
                                                                          :alt    "A close-up of brown hair with balayage. The hair has a subtle, graduated color that is a medium brown at the roots and is a lighter, caramel brown towards the ends."}
                                                                         "2c09c3aa-80ef-4f06-9346-fb9fd7b755f9")
                                                           [:div.mb3 "Our Highlighted shades are our colors that have more exaggerated highlights with a higher contrast than Balayage. The color placement is often more pronounced and has the most variation in color throughout."]
                                                           (ui/ucare-img {:width  "275px"
                                                                          :height "200px"
                                                                          :alt    "A close-up of blonde hair with highlights. There is a contrast between the lighter blonde and darker blonde strands, giving a multi-tonal effect to the hair."}
                                                                         "0c082a25-fc98-4fd7-bfef-38e0109f8be9")]}
                                               {:question "How do I know if this color will match my hair?"
                                                :answer   [:div
                                                           [:div.mb3 "Color-matching your natural hair to your extensions is simpler than it sounds. Make sure you're looking at your hair in bright, natural lighting. In front of a window, or even outside, is best."]
                                                           (ui/ucare-img {:width "275px"
                                                                          :alt   "A woman standing indoors in bright, natural lighting."}
                                                                         "2ec39103-4e23-4363-8f31-6905c915b35b")
                                                           [:div.mb3 "Use the mid-shaft (middle) of your hair through your ends to color match. Our roots are sometimes a different color than the rest of our hair, and using the middle lengths will give a more accurate representation. Keep in mind: if you're in between two different shades, it's usually best to go with the lighter option."]
                                                           [:div.mb3 "To figure out if your hair's undertones are warm, cool, or neutral, try this tip. Does your hair appear more red, orange, or yellow-based in natural light? You're leaning warm. Do your strands appear to have more of a blue or green hue? Cool is the way to go. If there's not a strong indication either way, you're neutral."]
                                                           [:div.mb3 "And most of all, don't worry if you're not 100% sure! Our 30-day exchange policy makes it super simple to switch out your clip-ins for a different color once they arrive. Need expert help? Text 34649 to reach our Customer Support team."]]}
                                               {:question "How do I know if this texture will match my hair?"
                                                :answer   [:div
                                                           [:div.mb3 "Out of the package, our Straight Clip-Ins will blend with naturally straight to slight wavy hair.  For wavy and curly textures, blowing out or flat ironing your hair before using your Clip-Ins will help create a more seamless blend. Keep in mind that our Straight Clip-Ins can be curled with heat protectant to better match waves and coils."]
                                                           (ui/ucare-img {:width "275px"
                                                                          :alt   "A model wearing clip-in hair extensions styled into loose curls."}
                                                                         "fecc4a1f-057c-4d23-a9f1-c2049a42fd72")]}
                                               {:question "How many packages of clip-ins do I need for a full look?"
                                                :answer   [:div
                                                           [:div.mb3 "If your hair is fine to medium density, 1 pack of Clip-Ins should work for a full look."]
                                                           [:div.mb3 "For medium to thick hair 2 packs will create a better blend and match your natural density. You can always mix and match packs for a truly customized look!"]
                                                           (ui/ucare-img {:width "275px"
                                                                          :alt   "A model wearing clip-in hair extensions styled into waves."}
                                                                         "a0658a72-e7fd-4d74-a9ed-e8a7ba8d19f7")]}
                                               {:question "What is a seamless weft?"
                                                :answer   [:div
                                                           [:div.mb3 "Our Seamless Clip-Ins are created with a thin, polyurethane (PU) weft. This allows the clip-ins to lay flat and is gentle on your own strands for a damage-free, blended look."]
                                                           (ui/ucare-img {:width "275px"
                                                                          :alt   "A model putting in clip-in hair extensions. The top half of her natural hair is sectioned off into a bun, and the weft is clipped in below, blending effortlessly with her hair. The seamless polyurethane weft is a thin, flexible strip that runs along the top of the clip-in hair extension."}
                                                                         "0b38d218-913e-42f1-acc6-9741015bd2fd")]}
                                               {:question "What does 100% remy human hair mean?"
                                                :answer   [:div
                                                           [:div.mb3 "All of our products are of remy grade, which means that all cuticles are in alignment. This helps to reduce shedding, limits tangling, and maximizes the lifespan of your human hair extensions."]]}]}
                           {:title            "Wearing your extensions"
                            :id               :wearing-your-extensions
                            :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                                       :tab-id  :wearing-your-extensions}]
                            :question-answers [{:question "Can clip-ins be colored further?"
                                                :answer   [:div [:div.mb3 "Yes, but keep in mind that pre-lightened colors should not be lifted (bleached) any further. They can be professionally colored with deposit-only products or toners."]]}
                                               {:question "How long can I keep my clip-ins in?"
                                                :answer   [:div [:div.mb3 "While your Clip-Ins will stay put for a whole day (or night!) of events, make sure to remove them completely before you go to sleep. This is key for both product longevity and the health of your own hair."]]}
                                               {:question "How long do clip-ins last?"
                                                :answer   [:div [:div.mb3 "With proper care and maintenance, your Clip-Ins can last for up to a year or more."]]}
                                               {:question "What does 100% remy human hair mean?"
                                                :answer   [:div [:div.mb3 "All of our products are of remy grade, which means that all cuticles are in alignment. This helps to reduce shedding, limits tangling, and maximizes the lifespan of your human hair extensions."]]}]}
                           {:title            "Caring for your extensions"
                            :id               :caring-for-your-extensions
                            :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                                       :tab-id  :caring-for-your-extensions}]
                            :question-answers [{:question "How do I store my clip-ins?"
                                                :answer   [:div
                                                           [:ul
                                                            [:li.mb3 "After detangling, store your clip-ins in your Mayvenn box, or inside a satin or silk pillowcase."]
                                                            [:li.mb3 "Remove clip-ins before bedtime to ensure they'll stand the test of time."]]
                                                           (ui/ucare-img {:width "275px"
                                                                          :alt   "Mayvenn Seamless Clip-Ins product packaging. The Mayvenn logo and the product name \"Seamless Clip-Ins\" are printed in white lettering on a small, purple and gray box."}
                                                                         "665b3c79-2b8a-46a4-8f59-ac1331c7703a")]}]}]})))

(defn built-component [app-state opts]
  (c/build component (query app-state) opts))

