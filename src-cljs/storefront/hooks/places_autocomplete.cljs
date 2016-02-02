(ns storefront.hooks.places-autocomplete
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [clojure.set :as set]
            [storefront.messages :as m]
            [storefront.config :as config]
            [om.core :as om]))

(def key-map
  {"street_number"               :street-number
   "route"                       :street
   "locality"                    :city
   "administrative_area_level_1" :state
   "postal_code"                 :zipcode})

(defn short-names [component]
  (when-let [new-key (some key-map (:types component))]
    [new-key (({:city :long_name} new-key :short_name) component)]))

(defn extract-address [place]
  (->> place :address_components (map short-names) (into {})))

(defn address [autocomplete address-type]
  (when-let [place (js->clj (.getPlace autocomplete) :keywordize-keys true)]
    {address-type (-> (extract-address place)
                      (#(assoc % :address1 (str (:street-number %) " " (:street %))))
                      (dissoc :street :street-number))}))

(defn insert-places-autocomplete [data]
  (when-not (.hasOwnProperty js/window "google")
    (tags/insert-tag-with-callback
     (tags/src-tag (str "https://maps.googleapis.com/maps/api/js?key="
                        config/places-api-key
                        "&libraries=places")
                   "places-autocomplete")
     #(m/send data events/inserted-places))))

(defn remove-places-autocomplete []
  (tags/remove-tags-by-class "places-autocomplete"))

(defn wrapped-callback [app-state autocomplete address-key]
  (fn [e]
    (m/send app-state
          events/autocomplete-update-address
          (address autocomplete address-key))))

(defn attach [app-state address-elem]
  (when (.hasOwnProperty js/window "google")
    (let [options      (clj->js {"types" ["address"] "componentRestrictions" {"country" "us"}})
          elem         (.getElementById js/document (str (name address-elem) "1"))
          autocomplete (google.maps.places.Autocomplete. elem options)]
      (.addListener autocomplete
                    "place_changed"
                    (wrapped-callback app-state autocomplete address-elem)))))
