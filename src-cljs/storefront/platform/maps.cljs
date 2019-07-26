(ns storefront.platform.maps
  (:require [adventure.keypaths]
            [om.core :as om]
            [sablono.core :as sablono]
            [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.hooks.places-autocomplete :as places]
            [storefront.keypaths]
            [stylist-directory.stylists :as stylists]))

(defn map-query [data]
  (let [places-loaded? (get-in data storefront.keypaths/loaded-places)
        salon          (->> (get-in data adventure.keypaths/stylist-profile-id)
                            (stylists/stylist-by-id data)
                            :salon)
        latitude       (:latitude salon)
        longitude      (:longitude salon)]
    {:salon     salon
     :latitude  latitude
     :longitude longitude
     :loaded?   (and places-loaded?
                     (some? latitude)
                     (some? longitude))}))

(defn inner-component
  [{:keys [latitude longitude]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (places/attach-map latitude longitude "stylist-profile-map"))
    om/IRender
    (render [_]
      (sablono/html
       [:div {:id    "stylist-profile-map"
              :style {:height "250px"}}]))))

(defn component
  [{:keys [loaded? salon] :as data} owner opts]
  (component/create
   [:div.mb3
    (when loaded?
      (component/build inner-component data))
    (let [{:keys [address-1 address-2 city state zipcode latitude longitude]} salon]
      [:div.bg-fate-white.p2.flex.justify-between
       [:div.flex.justfy-start
        [:div.line-height-3.pr1 (svg/position {:height "13px"
                                               :width  "10px"})]
        [:div.h6.self-center
         (clojure.string/join ", " (filter identity [address-1 address-2 city state zipcode]))]]
       [:a.self-center.navy.h6.medium
        {:href (str "https://www.google.com/maps/dir/?api=1&destination=" latitude "," longitude)}
        "DIRECTION"]])]))
