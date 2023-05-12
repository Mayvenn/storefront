(ns storefront.components.guide-clipin-extensions
  (:require [catalog.categories :as categories]
            [mayvenn.visual.tools :as vt]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.tabs-v202105 :as tabs]
            [storefront.components.ui :as ui] 
            [storefront.events :as e] 
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent carousel
  [{:keys [title elements]} _ _]
  [:div.proxima
   [:div.text-3xl.m3 title]
   [:div.flex.overflow-scroll.hide-scroll-bar
    (map-indexed 
     (fn [i {:keys [video-src caption]}]
       [:div.m3.flex-1 {:key   i
                        :style {:width "120px"}}
        [:div.bg-gray {:style {:height "215px"
                               :width  "120px"}}]
        [:div.mt2.text-sm caption]])
     elements)]])

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
   #_(c/build carousel (vt/with :carousel data))
   (c/build faqs (vt/with :faqs data))])

(def placeholder-img
  nil #_
  [:div.bg-gray.my1 {:style {:width "300px"
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
            :selected-tab (or (get-in app-state (conj k/tabs :guide-clipin-extensions)) 
                              :choosing-your-extensions)
            :tabs [{:title            "Choosing your extensions"
                    :id               :choosing-your-extensions
                    :message          [e/control-tab-selected {:tabs-id :guide-clipin-extensions
                                                               :tab-id  :choosing-your-extensions}]
                    :question-answers [{:question "What’s the difference between your different color options?"
                                        :answer   [:div
                                                   [:div.mb3 "Allover color is a single allover color that is the same color from top to bottom."]
                                                   [:div.mb3"Our Balayage shades are a subtle hand-painted version of a highlight. The Balayage pieces are painted throughout the hair to add dimension."]
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

