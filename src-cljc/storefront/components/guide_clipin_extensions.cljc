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

(def placeholder-img
  nil #_
  [:div.bg-gray.my1 {:style {:width "300px"
                             :height "200px"}}])

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
                            :video-src  "//ucarecdn.com/002ece8b-f236-4714-a9f6-0830f0b95875/"
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
                            :video-src  "//ucarecdn.com/d972aae9-5c73-4297-9bf2-9eaec9973b87/"
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
                                         [:p "Title: How to Make Your Clip-Ins Blend with Your Hair"]
                                         [:p "Narrator: Make sure your clip-ins are tangle-free."]
                                         [:p ""]
                                         [:p "Narrator: Once installed, style your hair and the clip-ins together - curl or flat iron them together for a more seamless blend."]
                                         [:p ""]
                                         [:p "Narrator: Don't be afraid to have your stylist lightly trim or layer them so that they blend even better with the way your hair naturally falls."]
                                         [:p ""]]}
                           {:id         "how-keep-clip-ins-in-place"
                            :video-src  "//ucarecdn.com/5ed6bf30-02ef-4392-8c92-193e932062a4/"
                            :caption    "How to Make Your Clip-Ins Stay in Place"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "how-to-clip-ins-in-place"}}]
                            :transcript [:div
                                         [:p "Title: How to Make Your Clip-Ins Stay in Place"]
                                         [:p "Narrator: If your hair is fine, or if you’re just having trouble getting your clip-in wefts to stay put, try this tip."]
                                         [:p ""]
                                         [:p "Narrator: Lightly back comb, or “tease”, your roots along your part lines."]
                                         [:p ""]
                                         [:p "Narrator: Then, place the clip-ins on top of those sections, close to the base of your roots."]
                                         [:p ""]
                                         [:p "Narrator: The extra grip should help! Make sure to leave enough room so that they’re comfortable, and not too tight."]
                                         [:p ""]]}
                           {:id         "where-place-clip-ins"
                            :video-src  "//ucarecdn.com/22cbc48c-9a20-4e68-8b6b-abaf83127220/"
                            :caption    "Where to Place Your Clip-Ins"
                            :cta/copy   "Transcript"
                            :cta/target [e/navigate-guide-clipin-extensions {:query-params {:modal "where-to-place-clip-ins"}}]
                            :transcript [:div
                                         [:p "Title: Where to Place Your Clip-Ins"]
                                         [:p "Narrator: Focus on placing the clip-ins according to the side of your head and length of your part in each section."]
                                         [:p ""]
                                         [:p "Narrator: Place the medium-sized pieces on the very bottom, the widest pieces in the middle of the head at the widest section, and so on."]
                                         [:p ""]
                                         [:p "Narrator: Use smaller pieces at a diagonal along part lines on top of the ear and in smaller sections around your head."]
                                         [:p ""]]}]}
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
                                                           [:div.mb3 "Our Balayage shades are a subtle hand-painted version of a highlight. The Balayage pieces are painted throughout the hair to add dimension."]
                                                           [:div.mb3 "Our Highlighted shades are our colors that have more exaggerated highlights with a higher contrast than Balayage. The color placement is often more pronounced and has the most variation in color throughout."]
                                                           placeholder-img]}
                                               {:question "How do I know if this color will match my hair?"
                                                :answer   [:div
                                                           [:div.mb3 "Color-matching your natural hair to your extensions is simpler than it sounds. Make sure you're looking at your hair in bright, natural lighting. In front of a window, or even outside, is best."]
                                                           [:div.mb3 "Use the mid-shaft (middle) of your hair through your ends to color match. Our roots are sometimes a different color than the rest of our hair, and using the middle lengths will give a more accurate representation. Keep in mind: if you're in between two different shades, it's usually best to go with the lighter option."]
                                                           [:div.mb3 "To figure out if your hair's undertones are warm, cool, or neutral, try this tip. Does your hair appear more red, orange, or yellow-based in natural light? You're leaning warm. Do your strands appear to have more of a blue or green hue? Cool is the way to go. If there's not a strong indication either way, you're neutral."]
                                                           [:div.mb3 "And most of all, don't worry if you're not 100% sure! Our 30-day exchange policy makes it super simple to switch out your clip-ins for a different color once they arrive. Need expert help? Text 34649 to reach our Customer Support team."]
                                                           placeholder-img]}
                                               {:question "How do I know if this texture will match my hair?"
                                                :answer   [:div
                                                           [:div.mb3 "Out of the package, our Straight Clip-Ins will blend with naturally straight to slight wavy hair.  For wavy and curly textures, blowing out or flat ironing your hair before using your Clip-Ins will help create a more seamless blend. Keep in mind that our Straight Clip-Ins can be curled with heat protectant to better match waves and coils."]
                                                           placeholder-img]}
                                               {:question "How many packages of clip-ins do I need for a full look?"
                                                :answer   [:div
                                                           [:div.mb3 "If your hair is fine to medium density, 1 pack of Clip-Ins should work for a full look."]
                                                           [:div.mb3 "For medium to thick hair 2 packs will create a better blend and match your natural density. You can always mix and match packs for a truly customized look!"]
                                                           placeholder-img]}
                                               {:question "What is a seamless weft?"
                                                :answer   [:div
                                                           [:div.mb3 "Our Seamless Clip-Ins are created with a thin, polyurethane (PU) weft. This allows the clip-ins to lay flat and is gentle on your own strands for a damage-free, blended look."]
                                                           placeholder-img]}
                                               {:question "What does 100% remy human hair mean?"
                                                :answer   [:div
                                                           [:div.mb3 "All of our products are of remy grade, which means that all cuticles are in alignment. This helps to reduce shedding, limits tangling, and maximizes the lifespan of your human hair extensions."]
                                                           placeholder-img]}]}
                           {:title            "Wearing your extensions"
                            :id               :wearing-your-extensions
                            :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                                       :tab-id  :wearing-your-extensions}]
                            :question-answers [{:question "Can clip-ins be colored further?"
                                                :answer   [:div [:div.mb3 "Yes, but keep in mind that pre-lightened colors should not be lifted (bleached) any further. They can be professionally colored with deposit-only products or toners."]]}
                                               {:question "How long can I keep my clip-ins in?"
                                                :answer   [:div [:div.mb3 "While your Clip-Ins will stay put for a whole day (or night!) of events, make sure to remove them completely before you go to sleep. This is key for both product longevity and the health of your own hair."]]}
                                               {:question "How long do clip-ins last?"
                                                :answer   [:div [:div.mb3 "With proper care and maintenance, your Clip-Ins can last for up to a year or more."]]}]}
                           {:title            "Caring for your extensions"
                            :id               :caring-for-your-extensions
                            :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                                       :tab-id  :caring-for-your-extensions}]
                            :question-answers [{:question "How do I store my clip-ins?"
                                                :answer   [:div
                                                           [:ul
                                                            [:li.mb3 "After detangling, store your clip-ins in your Mayvenn box, or inside a satin or silk pillowcase."]
                                                            [:li.mb3 "Remove clip-ins before bedtime to ensure they'll stand the test of time."]]
                                                           placeholder-img]}]}]})))

(defn built-component [app-state opts]
  (c/build component (query app-state) opts))

