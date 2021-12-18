(ns adventure.stylist-matching.maps
  (:require #?@(:cljs [[storefront.hooks.google-maps :as maps]])
            api.stylist
            adventure.keypaths
            [clojure.string :as string]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.keypaths]
            [storefront.components.ui :as ui]))

(defn map-query
  [data]
  (let [loaded-google-maps? (get-in data storefront.keypaths/loaded-google-maps)
        salon               (some->> (get-in data adventure.keypaths/stylist-profile-id)
                                     (api.stylist/by-id data)
                                     :diva/stylist
                                     :salon)
        latitude            (:latitude salon)
        longitude           (:longitude salon)]
    {:salon     salon
     :latitude  latitude
     :longitude longitude
     :loaded?   (and loaded-google-maps?
                     (some? latitude)
                     (some? longitude))}))

(defdynamic-component inner-component-v2
  (did-mount [this]
             (let [{:keys [latitude longitude]} (component/get-props this)]
               #?(:cljs (maps/attach-map latitude longitude "stylist-profile-map"))))
  (render [_]
          (component/html
           [:div {:id    "stylist-profile-map"
                  :style {:height (if (> #?(:cljs (.-innerWidth js/window)
                                            :clj 0) 749)
                                    "575px"
                                    "250px")}}])))

(defdynamic-component inner-component
  (did-mount [this]
             (let [{:keys [latitude longitude]} (component/get-props this)]
               #?(:cljs (maps/attach-map latitude longitude "stylist-profile-map"))))
  (render [_]
          (component/html
           [:div {:id    "stylist-profile-map"
                  :style {:height "250px"}}])))

(defcomponent component
  [{:keys [loaded? salon] :as data} owner opts]
  [:div.mb3
   (if loaded?
     (component/build inner-component data)
     [:div.flex.items-center.bg-cool-gray {:style {:min-height "250px"}} ui/spinner])
   (let [{:keys [address-1 address-2 city state zipcode latitude longitude]} salon]
     [:div.bg-cool-gray.p2.flex.justify-between
      [:div.flex.justfy-start.mr2
       [:div.line-height-3.pr1 (svg/position {:height "18px"
                                              :width  "18px"})]
       [:div.content-3.self-center
        (string/join ", " (remove nil? [address-1 address-2 city state zipcode]))]]
      (ui/button-small-underline-primary
       {:class "self-center"
        :href  (str "https://www.google.com/maps/dir/?api=1&destination=" latitude "," longitude)}
       "DIRECTIONS")])])

(defcomponent component-v2
  [{:keys [loaded? salon] :as data} owner opts]
  [:div.mb3
   (if loaded?
     (component/build inner-component data)
     [:div.flex.items-center.bg-cool-gray {:style {:min-height "250px"}} ui/spinner])
   (let [{:keys [name address-1 address-2 city state zipcode latitude longitude]} salon]
     [:div.bg-cool-gray.py3.px4.flex.items-center
      [:div.content-2.flex-grow-1
       [:div.bold.mb1 name]
       [:div address-1]
       [:div (string/join ", " (remove nil? [address-2 city state zipcode]))]]
      [:div.self-start
       (ui/button-small-underline-primary
        {:href  (str "https://www.google.com/maps/dir/?api=1&destination=" latitude "," longitude)}
        "DIRECTIONS")]])])

(defcomponent component-v3
  [{:keys [loaded? salon] :as data} owner opts]
  [:div.mb3.col-10.mx-auto
   (if loaded?
     (component/build inner-component-v2 data)
     [:div.flex.items-center.bg-cool-gray {:style {:min-height "250px"}} ui/spinner])
   (let [{:keys [name address-1 address-2 city state zipcode latitude longitude]} salon]
     [:div.bg-cool-gray.py3.px4.flex.items-center
      [:div.content-2.flex-grow-1
       [:div.bold.mb1 name]
       [:div address-1]
       [:div (string/join ", " (remove nil? [address-2 city state zipcode]))]]
      [:div.self-start
       (ui/button-small-underline-primary
        {:href  (str "https://www.google.com/maps/dir/?api=1&destination=" latitude "," longitude)}
        "DIRECTIONS")]])])
