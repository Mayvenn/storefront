(ns stylist-profile.stylist
  (:require adventure.keypaths
            [spice.core :as spice]
            [spice.date :as date]
            [storefront.components.formatters :as f]
            [stylist-directory.keypaths :as keypaths]
            [stylist-directory.stylists :as stylists]))

(defn ->display-name
  [{:keys [store-nickname address] :as stylist}]
  (when stylist
    (if (= store-nickname (:firstname address))
      (str (:firstname address) " " (:lastname address))
      store-nickname)))

(def ^:private display-name<- ->display-name)

;; ----- Model

(def ^:private menu-slugs->offered-sku-ids
  ;; This defines a mapping between diva service slugs and sku-ids
  ;; NOTE
  ;; The ordering is meaningful and encodes display order to sort by.
  ;; Some of this will be necessarily refactored as service combos
  ;; become 1-1 to services. Then offerings will map to facets.
  [;; Original Discountable (Free Mayvenn)
   [:specialty-sew-in-leave-out                "SRV-LBI-000"]
   [:specialty-sew-in-closure                  "SRV-CBI-000"]
   [:specialty-sew-in-frontal                  "SRV-FBI-000"]
   [:specialty-sew-in-360-frontal              "SRV-3BI-000"]
   [:specialty-wig-customization               "SRV-WGC-000"]
   ;; Add-ons
   [:specialty-addon-natural-hair-trim         "SRV-TRMU-000"]
   [:specialty-addon-weave-take-down           "SRV-TKDU-000"]
   [:specialty-addon-hair-deep-conditioning    "SRV-DPCU-000"]
   [:specialty-addon-closure-customization     "SRV-CCU-000"]
   [:specialty-addon-frontal-customization     "SRV-FCU-000"]
   [:specialty-addon-360-frontal-customization "SRV-3CU-000"]
   ;; Custom (constructed) Wigs
   [:specialty-custom-unit-leave-out           "SRV-UPCW-000"]
   [:specialty-custom-unit-closure             "SRV-CLCW-000"]
   [:specialty-custom-unit-frontal             "SRV-LFCW-000"]
   [:specialty-custom-unit-360-frontal         "SRV-3CW-000"]
   ;; A la Carte
   [:specialty-wig-install                     "SRV-WIBI-000"]
   [:specialty-silk-press                      "SRV-SPBI-000"]
   [:specialty-weave-maintenance               "SRV-WMBI-000"]
   [:specialty-wig-maintenance                 "SRV-WGM-000"]
   [:specialty-braid-down                      "SRV-BDBI-000"]
   ;; Reinstalls
   [:specialty-reinstall-leave-out             "SRV-LRI-000"]
   [:specialty-reinstall-closure               "SRV-CRI-000"]
   [:specialty-reinstall-frontal               "SRV-FRI-000"]
   [:specialty-reinstall-360-frontal           "SRV-3RI-000"]])

(defn ^:private extend-offered-services
  [{:keys [service-menu]}]
  (let [offered-slugs (reduce-kv
                       (fn [a k v]
                         (if (true? v) (conj a k) a))
                       #{}
                       service-menu)]
    {:stylist.services/offered-sku-ids  (-> (hash-map)
                                            (into menu-slugs->offered-sku-ids)
                                            (mapv offered-slugs)
                                            set)
     ;; This should probably be a sort value on the skus (eventually facets) themselves
     :stylist.services/offered-ordering (->> menu-slugs->offered-sku-ids
                                             (map-indexed (fn [i [_ sku-id]]
                                                            [sku-id i]))
                                             (into (hash-map)))}))

(defn ^:private extend-rating
  ;; NOTE rating-star-counts type is [{rating num}]
  ;;      where rating is a symbol of the rating, e.g. :1
  ;;      num is an int quantity of that rating, e.g. 5
  [{:keys [rating rating-star-counts mayvenn-rating-publishable]}]
  (let [histogram (mapv last (sort-by first rating-star-counts))]
    ;; we produce a histogram of ratings, with 0-numbering
    {:stylist.rating/score        (spice/parse-double rating)
     :stylist.rating/histogram    histogram
     :stylist.rating/cardinality  (apply + histogram)
     :stylist.rating/maximum      (apply max histogram)
     :stylist.rating/publishable? mayvenn-rating-publishable}))

;; TODO(corey) extend reviews

(defn stylist<-
  [state diva-stylist]
  (let [salon (:salon diva-stylist)]
    (merge {:diva/stylist diva-stylist
            :stylist/id   (:stylist-id diva-stylist)
            :stylist/slug (:store-slug diva-stylist)

            :stylist/portrait (:portrait diva-stylist)
            :stylist/name     (display-name<- diva-stylist)
            :stylist/salon    (:name salon)

            :stylist/setting    (case (:salon-type salon)
                                  "salon"   "in-salon"
                                  "in-home" "in-home"
                                  nil)
            :stylist/experience (when-let [since (:stylist-since diva-stylist)]
                                  (- (date/year (date/now)) since))
            :stylist/licensed?  (:licensed diva-stylist)

            ;; unsure if phone should be considered part of an address, seems odd
            :stylist/phone-number (some-> diva-stylist :address :phone f/phone-number-parens)

            :stylist.address/city (:city salon)

            :stylist.gallery/images (:gallery-images diva-stylist)}
           (extend-rating diva-stylist)
           (extend-offered-services diva-stylist))))

;; ----- api

(defn by-id
  [app-state stylist-id]
  (get-in app-state (conj keypaths/stylists stylist-id)))

(defn current
  [state]
  (when-let [diva-stylist (get-in state adventure.keypaths/adventure-servicing-stylist)]
    (stylist<- state diva-stylist)))

(defn detailed
  [state]
  (when-let [diva-stylist (stylists/by-id state (get-in state adventure.keypaths/stylist-profile-id))]
    (stylist<- state diva-stylist)))
