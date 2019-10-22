(ns storefront.platform.maps
  (:require [adventure.keypaths]
            [clojure.string :as string]
            [om.core :as om]
            [sablono.core :as sablono]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.svg :as svg]
            [storefront.hooks.google-maps :as maps]
            [storefront.keypaths]
            [storefront.components.ui :as ui]
            [stylist-directory.stylists :as stylists]
            ))

(defn map-query [data]
  (let [loaded-google-maps? (get-in data storefront.keypaths/loaded-google-maps)
        salon          (->> (get-in data adventure.keypaths/stylist-profile-id)
                            (stylists/by-id data)
                            :salon)
        latitude       (:latitude salon)
        longitude      (:longitude salon)]
    {:salon     salon
     :latitude  latitude
     :longitude longitude
     :loaded?   (and loaded-google-maps?
                     (some? latitude)
                     (some? longitude))}))

(defdynamic-component inner-component
  [data owner opts]
  (did-mount [this]
    (let [{:keys [latitude longitude]} (component/get-props this)]
      (maps/attach-map latitude longitude "stylist-profile-map")))
  (render [_]
    (component/html
      [:div {:id    "stylist-profile-map"
            :style {:height "250px"}}])))

(defn component
  [{:keys [loaded? salon] :as data} owner opts]
  (component/create
   [:div.mb3
    (if loaded?
      (component/build inner-component data)
      [:div.flex.items-center {:style {:height "250px"}} ui/spinner])
    (let [{:keys [address-1 address-2 city state zipcode latitude longitude]} salon]
      [:div.bg-fate-white.p2.flex.justify-between
       [:div.flex.justfy-start.mr2
        [:div.line-height-3.pr1 (svg/position {:height "13px"
                                               :width  "10px"})]
        [:div.h6.self-center
         (string/join ", " (filter identity [address-1 address-2 city state zipcode]))]]
       [:a.self-center.navy.h6.medium
        {:href (str "https://www.google.com/maps/dir/?api=1&destination=" latitude "," longitude)}
        "DIRECTIONS"]])]))
