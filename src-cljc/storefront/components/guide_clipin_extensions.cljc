(ns storefront.components.guide-clipin-extensions
  (:require [catalog.categories :as categories]
            [mayvenn.visual.tools :as vt]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.components.tabs-v202105 :as tabs]
            [storefront.components.ui :as ui] 
            [storefront.effects :as fx]
            [storefront.events :as e] 
            [storefront.keypaths :as k]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent carousel
  [{:keys [title elements]} _ _]
  [:div
   [:div.proxima.text-3xl title]
   [:div.flex
    (for [{:keys [video-src caption]} elements]
      [:div.m2
       [:div.bg-gray {:style {:width "200px"
                              :height "400px"}}]
       [:div caption]])]])

(c/defcomponent faqs
  [{:keys [title elements]} _ _]
  [:div
   [:div.proxima.text-3xl title] 
   [:div
    (for [{:keys [title id question-answers]} elements]
      [:div.m2
       [:div {:id id} title]
       (for [{:keys [question answer]} question-answers]
         [:div
          [:div {:class "bold"} question]
          [:div answer]])])]])

(c/defcomponent component
  [data owner opts]
  [:div
   (let [{:keys [target id label]} (vt/with :header.back-navigation data)]
     (ui/button-medium-underline-black
      (merge (apply utils/route-to target)
             {:aria-label label
              :data-test  id
              :class      "block mt3"})
      [:span (svg/left-arrow {:width  "12"
                              :height "12"}) " " label]))
   [:div.proxima.text-4xl (:header/title data)]
   (c/build carousel (vt/with :carousel data))
   (c/build faqs (vt/with :faqs data))])

(def placeholder-img
  [:div.bg-gray {:style {:width "300px"
                         :height "200px"}}])

(defn ^:private query [app-state]
  (let [category (first categories/seamless-clip-ins-category)]
    (merge
     {:header/title "Clip-In Extensions Guide"}
     #:header.back-navigation{:target [e/navigate-category (select-keys category [:catalog/category-id :page/slug])]
                              :id     "navigate-back"
                              :label  "Shop All Clip-Ins"}
     #:carousel{:title    "Video Tutorials"
                :elements [{:video-src     ""
                            :caption "How to Find the Best Shade to Match Your Hair"}
                           {:video-src     ""
                            :caption "How to Install Clip-Ins"}
                           {:video-src     ""
                            :caption "How to Make Your Clip-Ins Blend with Your Hair"}
                           {:video-src     ""
                            :caption "How to Make Your Clip-Ins Stay in Place"}
                           {:video-src     ""
                            :caption "Where to Place Your Clip-Ins"}]}
     #:faqs{:title    "FAQs"
            :current-tab "choosing-your-extensions"
            :elements [{:title            "Choosing your extensions"
                        :id               "choosing-your-extensions"
                        :question-answers [{:question "What’s the difference between your different color options?"
                                            :answer   [:div
                                                       [:div "Allover color is a single allover color that is the same color from top to bottom."]
                                                       [:div "Our Balayage shades are a subtle hand-painted version of a highlight. The Balayage pieces are painted throughout the hair to add dimension."]
                                                       [:div "Our Highlighted shades are our colors that have more exaggerated highlights with a higher contrast than Balayage. The color placement is often more pronounced and has the most variation in color throughout."]
                                                       placeholder-img]}
                                           {:question "How do I know if this color will match my hair?"
                                            :answer   [:div
                                                       [:div "Color-matching your natural hair to your extensions is simpler than it sounds. Make sure you're looking at your hair in bright, natural lighting. In front of a window, or even outside, is best."]
                                                       [:div "Use the mid-shaft (middle) of your hair through your ends to color match. Our roots are sometimes a different color than the rest of our hair, and using the middle lengths will give a more accurate representation. Keep in mind: if you're in between two different shades, it's usually best to go with the lighter option."]
                                                       [:div "To figure out if your hair's undertones are warm, cool, or neutral, try this tip. Does your hair appear more red, orange, or yellow-based in natural light? You're leaning warm. Do your strands appear to have more of a blue or green hue? Cool is the way to go. If there's not a strong indication either way, you're neutral."]
                                                       [:div "And most of all, don't worry if you're not 100% sure! Our 30-day exchange policy makes it super simple to switch out your clip-ins for a different color once they arrive. Need expert help? Text 34649 to reach our Customer Support team."]
                                                       placeholder-img]}
                                           {:question "How do I know if this texture will match my hair?"
                                            :answer   [:div
                                                       [:div "Out of the package, our Straight Clip-Ins will blend with naturally straight to slight wavy hair.  For wavy and curly textures, blowing out or flat ironing your hair before using your Clip-Ins will help create a more seamless blend. Keep in mind that our Straight Clip-Ins can be curled with heat protectant to better match waves and coils."]
                                                       placeholder-img]}
                                           {:question "How many packages of clip-ins do I need for a full look?"
                                            :answer   [:div
                                                       [:div "If your hair is fine to medium density, 1 pack of Clip-Ins should work for a full look."]
                                                       placeholder-img]}
                                           {:question "What is a seamless weft?"
                                            :answer   [:div
                                                       [:div "Our Seamless Clip-Ins are created with a thin, polyurethane (PU) weft. This allows the clip-ins to lay flat and is gentle on your own strands for a damage-free, blended look."]
                                                       placeholder-img]}
                                           {:question "What does 100% remy human hair mean?"
                                            :answer   [:div
                                                       [:div "All of our products are of remy grade, which means that all cuticles are in alignment. This helps to reduce shedding, limits tangling, and maximizes the lifespan of your human hair extensions."]
                                                       placeholder-img]}]}
                       {:title            "Wearing your extensions"
                        :id               "wearing-your-extensions"
                        :question-answers [{:question "Can clip-ins be colored further?"
                                            :answer   [:div [:div "Yes, but keep in mind that pre-lightened colors should not be lifted (bleached) any further. They can be professionally colored with deposit-only products or toners."]]}
                                           {:question "How long can I keep my clip-ins in?"
                                            :answer   [:div [:div "While your Clip-Ins will stay put for a whole day (or night!) of events, make sure to remove them completely before you go to sleep. This is key for both product longevity and the health of your own hair."]]}
                                           {:question "How long do clip-ins last?"
                                            :answer   [:div [:div "With proper care and maintenance, your Clip-Ins can last for up to a year or more."]]}]}
                       {:title            "Caring for your extensions"
                        :id               "caring-for-your-extensions"
                        :question-answers [{:question "How do I store my clip-ins?"
                                            :answer   [:div
                                                       [:ul [:li "After detangling, store your clip-ins in your Mayvenn box, or inside a satin or silk pillowcase."]]
                                                       [:li "Remove clip-ins before bedtime to ensure they'll stand the test of time."]
                                                       placeholder-img]}]}]})))

(defn built-component [app-state opts]
  (c/build component (spice.core/spy (query app-state)) opts))
