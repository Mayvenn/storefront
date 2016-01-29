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
    [new-key (:short_name component)]))

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

(defn attach [ref callback-cons]
  (when (.hasOwnProperty js/window "google")
    (let [autocomplete (google.maps.places.Autocomplete. (.getElementById js/document ref))]
      (.addListener autocomplete "place_changed" (callback-cons autocomplete)))))
