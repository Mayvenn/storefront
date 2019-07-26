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
  (when-let [location (some-> place :geometry :location)]
    (let [{:keys [lat lng]} (js->clj (.toJSON location)
                                     :keywordize-keys true)]
      (->> place
           :address_components
           (map short-names)
           (into {:latitude  lat
                  :longitude lng})))))

(defn address [autocomplete]
  (when-let [place (js->clj (.getPlace autocomplete) :keywordize-keys true)]
    (let [{:as extracted-address :keys [city sublocality state street street-number]} (extract-address place)]
      (some-> extracted-address
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
    (let [options      (clj->js {"types"                 [completion-type]
                                 "componentRestrictions" {"country" "us"}})
          elem         (.getElementById js/document (name address-elem))
          autocomplete (google.maps.places.Autocomplete. elem options)]
      (.setFields autocomplete #js ["address_components" "geometry"])
      (.addListener autocomplete
                    "place_changed"
                    (wrapped-callback autocomplete address-keypath)))))

(defn remove-containers []
  (let [containers (.querySelectorAll js/document ".pac-container")]
    (dotimes [i (.-length containers)]
      (let [node (aget containers i)]
        (.removeChild (.-parentNode node) node)))))

(def map-opts
  {:center                {:lat 40.674, :lng -73.945},
   :zoom                  13,
   :streetViewControl     false,
   :fullscreenControl     false,
   :mapTypeControlOptions {:mapTypeIds []},
   :styles
   [{:featureType "all",
     :elementType "geometry.fill",
     :stylers     [{:weight "2.00"}]}
    {:featureType "all",
     :elementType "geometry.stroke",
     :stylers     [{:color "#9c9c9c"}]}
    {:featureType "all",
     :elementType "labels.text",
     :stylers     [{:visibility "on"}]}
    {:featureType "administrative.land_parcel",
     :elementType "geometry.fill",
     :stylers
     [{:visibility "on"}
      {:color "#372305"}
      {:saturation "-25"}]}
    {:featureType "landscape",
     :elementType "all",
     :stylers     [{:color "#f2f2f2"}]}
    {:featureType "landscape",
     :elementType "geometry.fill",
     :stylers     [{:color "#ffffff"}]}
    {:featureType "landscape.man_made",
     :elementType "geometry.fill",
     :stylers     [{:color "#ffffff"}]}
    {:featureType "poi",
     :elementType "all",
     :stylers     [{:visibility "off"}]}
    {:featureType "road",
     :elementType "all",
     :stylers
     [{:saturation -100} {:lightness 45}]}
    {:featureType "road",
     :elementType "geometry.fill",
     :stylers     [{:color "#F1F0F2"}]}
    {:featureType "road",
     :elementType "labels.text.fill",
     :stylers     [{:color "#999999"}]}
    {:featureType "road",
     :elementType "labels.text.stroke",
     :stylers     [{:color "#ffffff"}]}
    {:featureType "road.highway",
     :elementType "all",
     :stylers     [{:visibility "simplified"}]}
    {:featureType "road.arterial",
     :elementType "labels.icon",
     :stylers     [{:visibility "off"}]}
    {:featureType "transit",
     :elementType "all",
     :stylers     [{:visibility "off"}]}
    {:featureType "water",
     :elementType "all",
     :stylers
     [{:color "#46bcec"} {:visibility "on"}]}
    {:featureType "water",
     :elementType "geometry.fill",
     :stylers     [{:color "#e4e1e9"}]}
    {:featureType "water",
     :elementType "labels.text.fill",
     :stylers     [{:color "#070707"}]}
    {:featureType "water",
     :elementType "labels.text.stroke",
     :stylers     [{:color "#ffffff"}]}]})

(def marker-icon {:path "M18.002 0C8.077 0 0 8.17 0 18.216c0 7.363 4.34 13.961
  11.06 16.808.476.203.846.6 1.017 1.096L17.535 52l6.034-15.801c.184-.48.56-.863
  1.03-1.051C31.525 32.379 36 25.734 36 18.216 36.005 8.173 27.928 0 18.003 0zm0
  25.8c-4.134 0-7.495-3.402-7.495-7.583 0-4.184 3.364-7.587 7.495-7.587 4.135 0
  7.496 3.403 7.496 7.587 0 4.183-3.361 7.584-7.496 7.584z"
   :fillColor     "#7E006D"
   :fillOpacity   1.0
   :strokeOpacity 0.0})

(defn- place-marker
  [lat-long map]
  (let [marker (google.maps.Marker. (clj->js {:position lat-long
                                              :icon     (assoc marker-icon :anchor (google.maps.Point. 18 52))}))]
    (.setMap marker map)))

(defn attach-map
  [latitude longitude address-elem]
  (when (.hasOwnProperty js/window "google")
    (let [lat-long {:lat (spice.core/parse-double latitude)
                    :lng (spice.core/parse-double longitude)}
          opts (clj->js (assoc map-opts :center lat-long))
          elem (.getElementById js/document (name address-elem))
          map  (google.maps.Map. elem opts)]
      (place-marker lat-long map)))) 


