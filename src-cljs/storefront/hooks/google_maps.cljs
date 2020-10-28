(ns storefront.hooks.google-maps
  (:require
   [spice.core :as spice]
   [storefront.browser.tags :as tags]
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
     #(m/handle-message events/inserted-google-maps))))

(defn attach
  ([completion-type address-elem address-keypath]
   (attach completion-type address-elem address-keypath (constantly nil) nil))
  ([completion-type address-elem address-keypath place-change-callback additional-fns]
   (when (.hasOwnProperty js/window "google")
     (let [options      (clj->js {"types"                 [completion-type]
                                  "componentRestrictions" {"country" "us"}})
           elem         (.getElementById js/document (name address-elem))
           autocomplete (google.maps.places.Autocomplete. elem options)]
       (.setFields autocomplete #js ["address_components" "geometry"])
       (.addListener autocomplete
                     "place_changed"
                     (fn [_]
                       (m/handle-message events/autocomplete-update-address
                                         {:address         (address autocomplete)
                                          :address-keypath address-keypath})
                       (place-change-callback)))
       (doseq [f additional-fns]
         (f elem autocomplete))))))

(defn remove-containers []
  (let [containers (.querySelectorAll js/document ".pac-container")]
    (dotimes [i (dec (.-length containers))]
      (let [node (aget containers i)]
        (.removeChild (.-parentNode node) node)))))

(def map-opts
  {:zoom                  13,
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
     :elementType "geometry.fill",
     :stylers     [{:color "#eeeeee"}]}
    {:featureType "road",
     :elementType "geometry.stroke",
     :stylers
     [{:visibility "on"} {:color "#dedbe5"}]}
    {:featureType "road",
     :elementType "labels.text.fill",
     :stylers     [{:color "#8b8b8b"}]}
    {:featureType "road",
     :elementType "labels.text.stroke",
     :stylers     [{:color "#ffffff"}]}
    {:featureType "road.highway",
     :elementType "geometry.stroke",
     :stylers     [{:visibility "off"}]}
    {:featureType "road.arterial",
     :elementType "geometry.stroke",
     :stylers     [{:visibility "off"}]}
    {:featureType "road.arterial",
     :elementType "labels.icon",
     :stylers     [{:visibility "off"}]}
    {:featureType "road.local",
     :elementType "geometry.stroke",
     :stylers     [{:visibility "off"}]}
    {:featureType "transit",
     :elementType "all",
     :stylers     [{:visibility "simplified"}]}
    {:featureType "transit",
     :elementType "labels",
     :stylers
     [{:visibility "simplified"}
      {:saturation "-100"}
      {:lightness "-5"}]}
    {:featureType "water",
     :elementType "all",
     :stylers
     [{:color "#46bcec"} {:visibility "on"}]}
    {:featureType "water",
     :elementType "geometry.fill",
     :stylers     [{:color "#dedbe5"}]}
    {:featureType "water",
     :elementType "labels.text.fill",
     :stylers     [{:color "#070707"}]}
    {:featureType "water",
     :elementType "labels.text.stroke",
     :stylers     [{:color "#ffffff"}]}]})

(defn- place-marker
  [lat-long map]
  (let [position-sprite-element (some-> "position" js/document.getElementById (.cloneNode true))
        sprite-element-string   (.-outerHTML (doto (js/document.createElement "svg")
                                               (.setAttribute "xmlns" "http://www.w3.org/2000/svg")
                                               (.setAttribute "xmlns:xlink" "http://www.w3.org/1999/xlink")
                                               (.setAttribute "viewBox" (.getAttribute position-sprite-element "viewBox"))
                                               (aset "innerHTML" (.-innerHTML position-sprite-element))))
        marker                  (google.maps.Marker.
                                 (clj->js {:position lat-long
                                           :icon     {:url  (str "data:image/svg+xml;charset=UTF-8,"
                                                                 (js/encodeURIComponent sprite-element-string))
                                                      :size (google.maps.Size. 36 52 "px" "px")}}))]
    (.setMap marker map)))

(defn attach-map
  [latitude longitude address-elem]
  (when (.hasOwnProperty js/window "google")
    (let [lat-long {:lat (spice/parse-double latitude)
                    :lng (spice/parse-double longitude)}
          opts     (clj->js (assoc map-opts :center lat-long))
          elem     (.getElementById js/document (name address-elem))
          map      (google.maps.Map. elem opts)]
      (place-marker lat-long map))))
