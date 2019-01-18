(ns adventure.matching-stylist-wait
  (:require #?@(:cljs [[om.core :as om]
                       [storefront.components.ugc :as ugc]
                       [storefront.hooks.pixlee :as pixlee-hook]
                       [storefront.config :as config]])
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.platform.messages :as messages]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [adventure.keypaths :as adventure-keypaths]
            [spice.maps :as maps]
            [adventure.components.header :as header]))

#_(defmethod effects/perform-effects events/navigate-adventure-matching-stylist-wait [_ _ _ _ app-state]
  #?(:cljs
     (let [{:keys [latitude longitude]} (get-in app-state keypaths/adventure-stylist-match-location)
           {:keys [how-far]} (get-in app-state keypaths/adventure-choices)]
       (api/fetch-stylists-within-radius (get-in app-state keypaths/api-cache)
                                         {:lat latitude :lon longitude}
                                         how-far))))

(defn ^:private query [data]
  (let []
    {}))

(defn ^:private component
  [{:keys [] :as data} _ _]
  (component/create
   [:div.white.flex.flex-auto.flex-column.items-center.pt4
    {:style {:background "linear-gradient(#cdb8d9,#9a8fb4)"}}
    [:div.mt10.mb2
     [:img {:src "https://ucarecdn.com/4e41237c-fd87-48ac-89dd-8bbde45ee569/matching-stylist-wait.gif"}]]
    [:div.col-8.h3.my2.medium.center "Matching you with a" [:br] " Mayvenn Certified Stylist..."]
    [:ul.col-7.h6 {:style {:list-style-image "url(https://ucarecdn.com/2560cee9-9ac7-4706-ade4-2f92d127b565/-/resize/12x/checkmark.png)"}}
     (mapv (fn [%] [:li.mb1 %])
           ["Licenced Salon Stylist" "Mayvenn Certified" "In your area"])]]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))


