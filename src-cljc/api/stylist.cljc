(ns api.stylist
  "Business Domain: Stylist

   Represents the stylist that can perform the services.

   Read API:

   - by-id

   Cache Domain: Current Stylist

   behavior:

   - requested
     Requests the stylist model for caching
   - fetched
     The stylist model is cached locally "
  (:require [spice.core :as spice]
            [spice.date :as date]
            [storefront.components.formatters :as f]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.transitions :as t]
            [storefront.keypaths :as storefront.k]
            [storefront.platform.messages :as m]
            [stylist-directory.keypaths :as k]
            #?@(:cljs [[storefront.api :as api]])))

(defn ->display-name
  [{:keys [store-nickname address] :as stylist}]
  (when stylist
    (if (= store-nickname (:firstname address))
      (str (:firstname address) " " (:lastname address))
      store-nickname)))

(def ^:private display-name<- ->display-name)

(def ^:private service-menu-join
  ;; This defines a mapping between diva service slugs and sku-ids
  ;; NOTE
  ;; The ordering is meaningful and encodes display order to sort by.
  ;; Some of this will be necessarily refactored as service combos
  ;; become 1-1 to services. Then offerings will map to facets.
  [;; Original Discountable (Free Mayvenn)
   [:specialty-sew-in-leave-out                "SRV-LBI-000"  nil "LBI"]
   [:specialty-sew-in-closure                  "SRV-CBI-000"  nil "CBI"]
   [:specialty-sew-in-frontal                  "SRV-FBI-000"  nil "FBI"]
   [:specialty-sew-in-360-frontal              "SRV-3BI-000"  nil "3BI"]
   [:specialty-wig-customization               "SRV-WGC-000"  nil "WGC"]
   ;; Add-ons
   [:specialty-addon-natural-hair-trim         "SRV-TRMU-000" :service/natural-hair-trim                 nil]
   [:specialty-addon-weave-take-down           "SRV-TKDU-000" :service/weave-take-down                   nil]
   [:specialty-addon-hair-deep-conditioning    "SRV-DPCU-000" :service/hair-deep-conditioning            nil]
   [:specialty-addon-closure-customization     "SRV-CCU-000"  :service/closure-customization             nil]
   [:specialty-addon-frontal-customization     "SRV-FCU-000"  :service/frontal-customization             nil]
   [:specialty-addon-360-frontal-customization "SRV-3CU-000"  :service/three-sixty-frontal-customization nil]
   ;; Custom (constructed) Wigs
   [:specialty-custom-unit-leave-out           "SRV-UPCW-000" nil "UPCW"]
   [:specialty-custom-unit-closure             "SRV-CLCW-000" nil "CLCW"]
   [:specialty-custom-unit-frontal             "SRV-LFCW-000" nil "LFCW"]
   [:specialty-custom-unit-360-frontal         "SRV-3CW-000"  nil "3CW"]
   ;; A la Carte
   [:specialty-wig-install                     "SRV-WIBI-000" nil "WIBI"]
   [:specialty-silk-press                      "SRV-SPBI-000" nil "SPBI"]
   [:specialty-weave-maintenance               "SRV-WMBI-000" nil "WMBI"]
   [:specialty-wig-maintenance                 "SRV-WGM-000"  nil "WGM"]
   [:specialty-braid-down                      "SRV-BDBI-000" nil "BDBI"]
   ;; Reinstall
   [:specialty-reinstall-leave-out             "SRV-LRI-000"  nil "LRI"]
   [:specialty-reinstall-closure               "SRV-CRI-000"  nil "CRI"]
   [:specialty-reinstall-frontal               "SRV-FRI-000"  nil "FRI"]
   [:specialty-reinstall-360-frontal           "SRV-3RI-000"  nil "3RI"]])

(def service-menu-slug  #(nth % 0))
(def legacy-sku-id      #(nth % 1))
(def facet-slug         #(nth % 2))
(def essential-sku-part #(nth % 3))

(defn filter-keys
  [pred m]
  (->> m
       (filter (fn [[_ v]] (pred v)))
       (into {})))

;; TODO(corey) clarify/refine lexicon
;; "Ordering" should probably be a sort value on the skus (eventually facets) themselves
(defn ^:private extend-offered-services
  [{:keys [service-menu]}]
  (let [table (-> (filter-keys true? service-menu)
                  keys
                  set ; at this point we have the stylists' offered slugs
                  (comp service-menu-slug)
                  (filter service-menu-join))]
    {:stylist.services/offered-facet-slugs (set (keep facet-slug table))
     :stylist.services/offerings           (set (keep essential-sku-part table))
     :stylist.services/ordering            (into {}
                                                 (comp
                                                  (keep essential-sku-part)
                                                  (map-indexed (fn [i esp] [esp i])))
                                                 table)
     ;; GROT(SRV) These two below should be superfluous
     :stylist.services/offered-sku-ids     (set (keep legacy-sku-id table))
     :stylist.services/offered-ordering    (into {}
                                                 (comp
                                                  (keep legacy-sku-id)
                                                  (map-indexed (fn [i lsi] [lsi i])))
                                                 table)}))

(defn ^:private extend-rating
  ;; NOTE rating-star-counts type is [{rating num}]
  ;;      where rating is a symbol of the rating, e.g. :1
  ;;      num is an int quantity of that rating, e.g. 5
  [{:keys [rating rating-star-counts mayvenn-rating-publishable]}]
  (let [histogram (mapv last (sort-by first rating-star-counts))]
    ;; we produce a histogram of ratings, with 0-numbering
    {:stylist.rating/score        (some-> rating spice/parse-double)
     :stylist.rating/decimal-score rating
     :stylist.rating/histogram    histogram
     :stylist.rating/cardinality  (some->> histogram (apply +))
     :stylist.rating/maximum      (some->> histogram seq (apply max))
     :stylist.rating/publishable? mayvenn-rating-publishable}))

;; TODO(corey) extend reviews
(defn stylist<-
  "Stylist schema situated for Storefront"
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

            :stylist.address/city  (:city salon)
            :stylist.address/state (:state salon)

            :stylist.gallery/images (:gallery-images diva-stylist)}
           (extend-rating diva-stylist)
           (extend-offered-services diva-stylist))))

;; - Read API

(defn by-id
  "Get a stylist by id"
  [state stylist-id]
  (when stylist-id
    (get-in state (conj storefront.k/models-stylists stylist-id))))

;; - Behavior API

(defmethod fx/perform-effects e/cache|stylist|requested
  [_ _ {:stylist/keys [id] :on/keys [success failure] :keys [forced?]} _ state]
  (let [cache (get-in state storefront.k/api-cache)]
    (->> {:error-handler   failure
          :cache/bypass?   forced?
          :success-handler #(m/handle-message e/cache|stylist|fetched
                                              (assoc % :on/success success))}
         #?(:cljs
            (api/fetch-matched-stylist cache id)))))

(defmethod t/transition-state e/cache|stylist|fetched
  [_ _ {diva-stylist :stylist} state]
  (when diva-stylist
    (let [{:as stylist-model :stylist/keys [id]} (stylist<- state diva-stylist)]
      (-> state
          ;; Store stylist in the original diva format, removal soon
          (assoc-in (conj k/stylists id)
                    diva-stylist)
          ;; store stylist baked for storefront
          (assoc-in (conj storefront.k/models-stylists id)
                    stylist-model)))))

(defmethod fx/perform-effects e/cache|stylist|fetched
  [_ _ {:on/keys [success]} _ _]
  (when-not (nil? success)
    (success)))
