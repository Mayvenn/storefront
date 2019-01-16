(ns storefront.hooks.places-autocomplete
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(def key-map
  {"street_number"               :street-number
   "route"                       :street
   "locality"                    :city
   "administrative_area_level_1" :state
   "sublocality_level_1"         :sublocality
   "postal_code"                 :zipcode})

(defn short-names [component]
  (when-let [new-key (some key-map (:types component))]
    [new-key (({:city :long_name} new-key :short_name) component)]))

(defn extract-address [place]
  (let [{:keys [lat lng]} (-> place :geometry :location .toJSON (js->clj :keywordize-keys true))]
    (->> place
         :address_components
         (map short-names)
         (into {:latitude  lat
                :longitude lng}))))

(defn address [autocomplete]
  (when-let [place (js->clj (.getPlace autocomplete) :keywordize-keys true)]
    (let [{:as extracted-address :keys [city sublocality state street street-number]} (extract-address place)]
      (-> extracted-address
          (assoc :address1 (str street-number " " street)
                 :city     (or city sublocality))
          (dissoc :street :street-number :sublocality)))))

(defn insert []
  (when-not (.hasOwnProperty js/window "google")
    (tags/insert-tag-with-callback
     (tags/src-tag (str "https://maps.googleapis.com/maps/api/js?key="
                        config/places-api-key
                        "&libraries=places")
                   "places-autocomplete")
     #(m/handle-message events/inserted-places))))

(defn- wrapped-callback [autocomplete address-keypath]
  (fn [e]
    (m/handle-message events/autocomplete-update-address
                      {:address (address autocomplete)
                       :address-keypath address-keypath})))

(defn attach [completion-type address-elem address-keypath]
  (when (.hasOwnProperty js/window "google")
    (let [options      (clj->js {"types" [completion-type] "componentRestrictions" {"country" "us"}})
          elem         (.getElementById js/document (name address-elem))
          autocomplete (google.maps.places.Autocomplete. elem options)]
      (.addListener autocomplete
                    "place_changed"
                    (wrapped-callback autocomplete address-keypath)))))

(defn remove-containers []
  (let [containers (.querySelectorAll js/document ".pac-container")]
    (dotimes [i (.-length containers)]
      (let [node (aget containers i)]
        (.removeChild (.-parentNode node) node)))))
