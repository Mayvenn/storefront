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
     {:tab (get-in app-state k/product-details-popup-shade-finder-tab)}))

#?(:cljs 
   (defmethod popup/component :shade-finder
     [{:as args :keys [tab]} _ _]
     (ui/modal
      {:close-attrs {:on-click #(apply m/handle-message [e/popup-hide])}}
      [:div.bg-white.p3.stretch
       [:div.flex.justify-between.items-center.mb2
        [:div.proxima.text-xl "Color Guide"]
        [:a {:on-click #(apply m/handle-message [e/popup-hide])} (svg/x-sharp {:class "black"
                                                                               :style {:width  "14px"
                                                                                       :height "14px"}})]]
       (c/build tabs/component {:tabs         [{:id                 :finding-your-color-match
                                                :title              "Finding your color match"
                                                :message            [e/popup-show-shade-finder {:tab :finding-your-color-match}]
                                                :not-selected-class "border-right"}
                                               {:id                 :color-chart
                                                :title              "Color Chart"
                                                :message            [e/popup-show-shade-finder {:tab :color-chart}]
                                                :not-selected-class "border-left"}]
                                :selected-tab tab})
       [:div.py2
        (case tab
          :finding-your-color-match 
          [:div.proxima.text-base
           [:div "To find your best color match:"]
           [:ol
            [:li.my1 "Take a picture of your hair in natural, indirect light (near a window) to use as a reference."]]
           (ui/img {:src   "https://ucarecdn.com/9e4121d9-87f2-4b43-aed3-bb7909f953bf/" ; TODO get right picture
                    :alt   ""
                    :class "col-12"})
           [:ol
            [:li.my1 {:value 2} "Compare your natural hair color in the photo to the available colors."]]
           [:div "Don’t worry, if you try on your clip-ins and they aren’t the perfect match, " 
            "we’ll exchange them within 30 days. For more information, see our " 
            [:a.bold.black.underline (utils/route-to e/navigate-content-about-us) "return & exchange policy."]]]
          
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
  (-> app-state
      (assoc-in k/popup :shade-finder)
      (assoc-in k/product-details-popup-shade-finder-tab (or tab :finding-your-color-match))))

#?(:cljs
   (defmethod trk/perform-track e/popup-show-shade-finder
     [_ _ tracking-data _]
     (stringer/track-event "shade_tracker_link_pressed" tracking-data)))