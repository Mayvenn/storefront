(ns api.orders
  (:require api.stylist
            [spice.selector :refer [match-all]]
            [storefront.accessors.experiments :as ff]
            [storefront.accessors.images :as images]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.sites :as sites]
            storefront.keypaths
            [stylist-matching.search.accessors.filters :as stylist-filters]
            [spice.maps :as maps]))

;;; Utils

(defn- ns!
  [ns m]
  (->> m (mapv (fn [[k v]] [(keyword ns (name k)) v])) (into {})))

(def ^:private select
  (comp seq (partial match-all {:selector/strict? true})))

(def ^:private ?recent
  {:item/recent? #{true}})

(def ^:private ?service
  {:catalog/department #{"service"}})

(def ^:private ?new-world-service
  {:service/world #{"SV2"}})

(def ^:private ?physical
  {:catalog/department #{"hair" "stylist-exclusives"}})

(def ^:private ?addons
  {:catalog/department #{"service"}
   :service/type       #{"addon"}})

(def ^:private ?discountable
  {:catalog/department                 #{"service"}
   :service/type                       #{"base"}
   :promo.mayvenn-install/discountable #{true}})

;;; Not quite sure why this is needed, its just reverse lookups :hair/family
;;; Why not query?

(defn hair-family->service-sku-ids [hair-family]
  (get {"bundles"         #{"SRV-LBI-000" "SRV-UPCW-000"}
        "closures"        #{"SRV-CBI-000" "SRV-CLCW-000"}
        "frontals"        #{"SRV-FBI-000" "SRV-LFCW-000"}
        "360-frontals"    #{"SRV-3BI-000" "SRV-3CW-000"}
        "360-wigs"        #{"SRV-WGC-000"}
        "lace-front-wigs" #{"SRV-WGC-000"}}
       hair-family))

;;; Discountable promotions
(def ^:private ?bundles
  {:catalog/department #{"hair"}
   :hair/family        #{"bundles"}} )

(def ^:private ?closures
  {:catalog/department #{"hair"}
   :hair/family        #{"closures"}})

(def ^:private ?frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"frontals"}})

(def ^:private ?360-frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"360-frontals"}})

(def ^:private ?wigs
  {:catalog/department #{"hair"}
   :hair/family        #{"lace-front-wigs" "360-wigs"}})

(def rules
  {"SRV-LBI-000"  [["bundle" ?bundles 3]]
   "SRV-UPCW-000" [["bundle" ?bundles 3]]
   "SRV-CBI-000"  [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "SRV-CLCW-000" [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "SRV-FBI-000"  [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "SRV-LFCW-000" [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "SRV-3BI-000"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "SRV-3CW-000"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "SRV-WGC-000"  [["Virgin Lace Front or a Virgin 360 Wig" ?wigs 1]]})

(def SV2-rules
  {"LBI"  [["bundle" ?bundles 3]]
   "UPCW" [["bundle" ?bundles 3]]
   "CBI"  [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "CLCW" [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "FBI"  [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "LFCW" [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "3BI"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "3CW"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "WGC"  [["Virgin Lace Front or a Virgin 360 Wig" ?wigs 1]]})

(defn free-mayvenn-service
  "
  Free Mayvenn Services

  Requirements:
  - A service item in cart of a sku that is discountable
  - Other physical items in the cart associated with the service item

  Optional:
  - A servicing stylist (that can provide those services)

  Caveats
  - service items should have a stylist associated, but that is broken 2020 June
  "
  [servicing-stylist waiter-order]
  (let [service-line-item (first (filter
                                  (comp :promo.mayvenn-install/discountable :variant-attrs)
                                  (orders/service-line-items waiter-order)))
        rules-for-service (or (get rules (:sku service-line-item))
                              (get SV2-rules (-> service-line-item :service-attrs :sku/base-part)))
        physical-items    (->> waiter-order :shipments (mapcat :line-items)
                               (filter (fn [item]
                                         (= "spree" (:source item))))
                               (map (fn [item]
                                      (merge
                                       (dissoc item :variant-attrs)
                                       (:variant-attrs item)))))
        failed-rules      (keep (fn [[word essentials rule-quantity]]
                                  (let [cart-quantity    (->> physical-items
                                                              (match-all
                                                               {:selector/strict? true}
                                                               essentials)
                                                              (map :quantity)
                                                              (apply +))
                                        missing-quantity (- rule-quantity cart-quantity)]
                                    (when (pos? missing-quantity)
                                      {:word             word
                                       :cart-quantity    cart-quantity
                                       :missing-quantity missing-quantity
                                       :essentials       essentials})))
                                rules-for-service)]
    (when (seq service-line-item)
      #:free-mayvenn-service
      {;; Some sort of key indicating you can add more after completion
       :failed-criteria-count (->> [(seq service-line-item)
                                    (empty? failed-rules)
                                    (seq servicing-stylist)]
                                   (remove boolean)
                                   count)
       ;; criterion 1
       :service-item          service-line-item
       ;; criterion 2
       :hair-missing          (seq failed-rules)
       :hair-missing-quantity (apply + (map :missing-quantity failed-rules))
       :hair-success-quantity (apply + (map last rules-for-service))
       ;; criterion 3
       :stylist               (not-empty servicing-stylist)
       ;; result
       :discounted?           (and service-line-item (not (seq failed-rules)))})))

;;; Some ad-hoc transforms?

(defn ->service
  [waiter-line-item]
  {:title  (:variant-name waiter-line-item)
   :sku-id (:sku waiter-line-item)
   :price  (:unit-price waiter-line-item)})

(defn ->addon-service
  [waiter-addon-line-item]
  (->service waiter-addon-line-item))

(defn ->base-service
  [waiter-order waiter-base-line-item]
  (let [addon-services (->> waiter-order
                            orders/service-line-items
                            (filter line-items/addon-service?)
                            (filter #(= (:line-item-group waiter-base-line-item) (:line-item-group %)))
                            (map ->addon-service))]
    (assoc (->service waiter-base-line-item) :addons addon-services)))

(declare items<-)

(defn offered-services-sku-ids
  [stylist]
  (->> stylist-filters/service-filter-data
       (map :sku-id)
       (filter (partial stylist-filters/stylist-provides-service-by-sku-id? stylist))
       set))

(defn ->order
  [app-state waiter-order]
  (let [recents           (get-in app-state storefront.keypaths/cart-recently-added-skus)
        base-services     (->> waiter-order
                               orders/service-line-items
                               (filter line-items/base-service?))
        products-db       (get-in app-state storefront.keypaths/v2-products)
        skus-db           (get-in app-state storefront.keypaths/v2-skus)
        images-db         (get-in app-state storefront.keypaths/v2-images)
        servicing-stylist (when-let [stylist-id (:servicing-stylist-id waiter-order)]
                            (when-let [stylist (:diva/stylist (api.stylist/by-id app-state stylist-id))]
                              (assoc stylist :offered-skus (offered-services-sku-ids stylist))))
        facets-db         (->> storefront.keypaths/v2-facets
                               (get-in app-state)
                               (maps/index-by (comp keyword :facet/slug))
                               (maps/map-values (fn [facet]
                                                  (update facet :facet/options (partial maps/index-by :option/slug)))))
        items             (items<- waiter-order recents products-db skus-db images-db facets-db servicing-stylist)]
    {:waiter/order         waiter-order
     :order/submitted?     (= "submitted" (:state waiter-order))
     :order.shipping/phone (get-in waiter-order [:shipping-address :phone])
     :order.items/quantity (orders/displayed-cart-count waiter-order)
     :order.items/services (map (partial ->base-service waiter-order) base-services)
     :order/items          items
     :service/world        (cond ; Consider current order contents first, then ff, default old
                             (select ?new-world-service items)        "SV2"
                             (select ?service items)                  "SRV"
                             (ff/service-skus-with-addons? app-state) "SV2"
                             :else                                    "SRV")}))

(defn completed
  [app-state]
  (->order app-state (get-in app-state storefront.keypaths/completed-order)))

;; TODO(corey) what should an order model look like if the order doesn't exist
(defn current
  [app-state]
  (when-let [waiter-order (not-empty (get-in app-state storefront.keypaths/order))]
    (->order app-state waiter-order)))

(defn services
  "Model: Services on an order

  In Scope:
  - services in on the order
  - servicing stylist for those services

  Issues:
  - We don't have a way to store and retrieve different stylists, so if we don't
    have the stylist cache loaded. Sorry for now, you only get an id.

  - How do line item groups show up here?
  "
  [app-state waiter-order]
  ;; Ideally: [stylist-db waiter-order]
  (when (= :shop (sites/determine-site app-state))
    (let [servicing-stylist-id (:servicing-stylist-id waiter-order)
          cached-stylist       (when-let [stylist-id (:servicing-stylist-id waiter-order)]
                                 (:diva/stylist (api.stylist/by-id app-state stylist-id)))]
      (merge
       #:services{:stylist-id servicing-stylist-id
                  :items      (orders/service-line-items waiter-order)}
       (when cached-stylist
         #:services{:stylist                  cached-stylist
                    :offered-services-sku-ids (offered-services-sku-ids cached-stylist)})))))

;;; ---- API in progress; Please chat Corey

(defn ^:private as-sku
  "
  Items are extensions of skus
  "
  [skus-db images-db facets-db {sku-id :item/sku :as item}]
  (merge item
         (let [sku    (get skus-db sku-id)
               images (images/for-skuer images-db sku)]
           (-> sku
               (assoc :join/facets
                      (->> sku
                           :selector/essentials
                           (select-keys facets-db)
                           (map (fn [[essential-key facet]]
                                  [essential-key (get-in facet [:facet/options (first (get sku essential-key))])]))
                           (into {})))
               ;; TODO(corey) I'm a bit skeptical of the specialness of addon-facets
               ;; Trouble is that we can't easily select the addons in the above representation
               (assoc :join/addon-facets
                      (->> sku
                           :selector/essentials
                           (select-keys facets-db)
                           (keep (fn addon-facet-and-true? [[essential-key facet]]
                                   (let [addon? (= #{true} (get sku essential-key))]
                                     (when (and (:service/addon? facet)
                                                addon?)
                                       facet))))
                           (sort-by :filter/order)
                           (into [])))
               (assoc :selector/images images)))))

(defn ^:private extend-recency
  "
  Extend items to have recency information
  "
  [recents {sku-id :item/sku :as item}]
  (merge item
         (when-let [recent-quantity (get recents sku-id)]
           {:item/recent?         #{true}
            :item/recent-quantity recent-quantity})))

(defn ^:private extend-servicing
  "
  Services combine into one item

  Addons - three levels:
  - A. base compatibility
  - B. in cart?
  - C. stylist serviceable?

  Rough Schema Plan:
  :service/addons or :selector/addons ;; A.
  :item.service/stylist {... :stylist-menu } ;; c.
  :item.service/stylist-offered?  #{};; C.
  :item.service/addons  [{... :item.service/stylist-offered? #{}}] ;; B., C.
  "
  [stylist items item]
  (when-not (= #{"addon"} (:service/type item))
    (merge item
           (when stylist
             (let [offered? (contains? (:offered-skus stylist)
                                       (:catalog/sku-id item))]
               {:item.service/stylist          stylist
                :item.service/stylist-offered? #{offered?}}))
           (when-not (= #{"SV2"} (:service/world item)) ;; GROT(SRV)
             (when-let [addons (->> (select ?addons items)
                                    (filter #(= (:item/line-item-group item)
                                                (:item/line-item-group %)))
                                    not-empty)]
               {:item.service/addons addons})))))

(defn extend-free-servicing
  "
  Discountable services, 'Free Mayvenn Services'

  Criteria:
  - A service item in cart of a sku that is discountable
  - Other physical items in the cart associated with the service item
  - A servicing stylist (that can provide those services)
  "
  [items {:catalog/keys [sku-id] :as item}]
  (merge item
         (when (select ?discountable [item])
           (let [rules-for-service (or
                                    (get rules sku-id)
                                    (get SV2-rules (-> item :item/service-attrs :product/sku-part)))
                 physical-items    (select ?physical items)

                 failed-rules
                 (keep (fn [[word essentials rule-quantity]]
                         (let [cart-quantity    (->> (select essentials physical-items)
                                                     (map :item/quantity)
                                                     (apply +))
                               missing-quantity (- rule-quantity cart-quantity)]
                           (when (pos? missing-quantity)
                             {:word             word
                              :cart-quantity    cart-quantity
                              :missing-quantity missing-quantity
                              :essentials       essentials})))
                       rules-for-service)]
             #:promo.mayvenn-install
             {:failed-criteria-count (->> [item ; 1.
                                           (empty? failed-rules) ; 2.
                                           (seq (:item.service/stylist item))] ; 3
                                          (remove boolean)
                                          count)
              :hair-missing          (seq failed-rules)
              :hair-missing-quantity (apply + (map :missing-quantity failed-rules))
              :hair-success-quantity (apply + (map last rules-for-service))}))))

(defn extend-hacky [products-db sku]
  ;; NOTE: HACK: The cart uses :copy/title from the product that the sku belongs
  ;; to This seems to be the long term name. We should canonicalize this name
  ;; (perhaps under :sku/cart-title) into cellar to prevent the need to enrich from the product
  (let [{product-title             :copy/title
         product-requirements-copy :promo.mayvenn-install/requirement-copy}
        (get products-db (-> sku :selector/from-products first))]
    (cond-> sku
      product-title
      (assoc :hacky/cart-title product-title)
      product-requirements-copy
      (assoc :hacky/promo-mayvenn-install-requirement-copy product-requirements-copy))))

(defn items<-
  "
  Model of items as skus + placement attrs (quantity + servicing-stylist)

  This is *not* waiter's line items!
  "
  [waiter-order recents products-db skus-db images-db facets-db servicing-stylist]
  ;; verify selected servicing stylist matches waiter-order
  ;; (= (:stylist-id cached-stylist) servicing-stylist-id)
  (let [items (->> waiter-order
                   :shipments
                   first
                   :storefront/all-line-items
                   (remove (comp neg? :id)) ; shipping
                   (keep (partial ns! "item"))
                   (keep (partial as-sku skus-db images-db facets-db))
                   (keep (partial extend-hacky products-db))
                   (keep (partial extend-recency recents)))]
    (->> items
         (keep (partial extend-servicing servicing-stylist items))
         (keep (partial extend-free-servicing items)))))
