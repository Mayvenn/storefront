(ns catalog.ui.popup-shade-finder
  (:require #?@(:cljs [[storefront.components.popup :as popup]
                       [storefront.hooks.stringer :as stringer]])
            [storefront.component :as c]
            [storefront.components.tabs-v202105 :as tabs]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg] 
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as m]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]))

#?(:cljs 
   (defmethod popup/query :shade-finder [app-state]
     {:tab (or (get-in app-state (conj k/tabs :shade-finder))
               :finding-your-color-match)}))

#?(:cljs 
   (defmethod popup/component :shade-finder
     [{:keys [tab]} _ _]
     (ui/modal
      {:close-attrs {:on-click #(apply m/handle-message [e/popup-hide])}}
      [:div.bg-white.p3.stretch
       [:div.flex.justify-between.items-center.mb2
        [:div.proxima.text-xl "Color Guide"]
        [:a {:on-click #(apply m/handle-message [e/popup-hide])} (svg/x-sharp {:class "black"
                                                                               :style {:width  "14px"
                                                                                       :height "14px"}})]]
       (c/build tabs/component {:tabs         [{:id                 :finding-your-color-match
                                                :title              "Finding Your Color Match"
                                                :message            [e/control-tab-selected {:tabs-id :shade-finder
                                                                                             :tab-id  :finding-your-color-match}]
                                                :not-selected-class "border-right"}
                                               {:id                 :color-chart
                                                :title              "Color Chart"
                                                :message            [e/control-tab-selected {:tabs-id :shade-finder
                                                                                             :tab-id  :color-chart}]
                                                :not-selected-class "border-left"}]
                                :selected-tab tab})
       [:div.py2
        (case tab
          :finding-your-color-match 
          [:div.proxima.text-base 
           [:div.my2 "Color-matching your natural hair to your extensions is simpler than it sounds. Make sure you're looking at your hair in bright, natural lighting. In front of a window, or even outside, is best."]
           (ui/img {:src   "https://ucarecdn.com/177c96ce-c31b-49b7-897b-6ab06e5ba146/"
                    :alt   ""
                    :class "col-12"})
           [:div.my2 "Use the mid-shaft (middle) of your hair through your ends to color match. Our roots are sometimes a different color than the rest of our hair, and using the middle lengths will give a more accurate representation. Keep in mind: if you're in between two different shades, it's usually best to go with the lighter option. "]
           [:div.my2 "To figure out if your hair's undertones are warm, cool, or neutral, try this tip. Does your hair appear more red, orange, or yellow-based in natural light? You're leaning warm. Do your strands appear to have more of a blue or green hue? Cool is the way to go. If there's not a strong indication either way, you're neutral."]
           [:div.my2 "And most of all, don't worry if you're not 100% sure! Our 30-day exchange policy makes it super simple to switch out your clip-ins for a different color once they arrive. Need expert help? Text " 
            (ui/link :link/sms :a.inherit-color.bold {:aria-label "text us at 34649"} "34649")
            " to reach our Customer Support team."]]

          :color-chart 
          [:div.proxima.text-base 
           [:div.my2 "In the world of hair color and extensions, there are various codes and names used to describe each color. It's common to see alphanumeric codes used for different levels and tones."]
           [:div.my2 "Whether you're using a beauty supply color system (commonly used with wigs and bundles) or a standard hair color system (commonly used with clip-ins and other custom-colored products), we've got you covered."]
           [:div.my2 "Take a look at the colors we carry and what the codes mean across product types, so you can find your perfect match no matter which extensions you're wearing."]
           [:div.proxima.text-xl.my3 "Color Chart"]
           (ui/img {:src "https://ucarecdn.com/b850d68e-0222-4afd-9972-421ffeb727c8/"
                    :alt "Color Chart"
                    :class "col-12"})])]])))


(defmethod t/transition-state e/popup-show-shade-finder
  [_ _ {:keys [tab]} app-state]
  (assoc-in app-state k/popup :shade-finder))

#?(:cljs
   (defmethod trk/perform-track e/popup-show-shade-finder
     [_ _ tracking-data _]
     (stringer/track-event "shade_tracker_link_pressed" tracking-data)))