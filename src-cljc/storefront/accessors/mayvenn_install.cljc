(ns storefront.accessors.mayvenn-install
  (:require [adventure.keypaths :as adventure-keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            storefront.keypaths
            [storefront.accessors.images :as images]
            [storefront.components.money-formatters :as mf]
            [clojure.string :as str]))

(defn ^:private family->ordering
  [family]
  (let [families {"bundles"         5
                  "closures"        4
                  "frontals"        3
                  "360-frontals"    2
                  "360-wigs"        1
                  "lace-front-wigs" 1}]
    (get families family 10)))

(defn hair-family->service-type
  [family]
  (get
   {"bundles"         :leave-out
    "closures"        :closure
    "frontals"        :frontal
    "360-frontals"    :three-sixty
    "360-wigs"        :wig-customization
    "lace-front-wigs" :wig-customization}
   family))

(defn product-line-items->service-type
  [product-items]
  (->> product-items
       (map (comp :hair/family :variant-attrs))
       (sort-by (partial family->ordering))
       first
       hair-family->service-type))

;; TODO: consider unifying this with api.orders/mayvenn-install
(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views.
  Service type is inferred from the cart content in order to display appropriate
  service related copy"
  [app-state]
  (let [order                   (get-in app-state storefront.keypaths/order)
        shipment                (-> order :shipments first)
        any-wig?                (->> shipment
                                     :line-items
                                     (filter line-items/any-wig?)
                                     count
                                     pos?)
        service-type            (product-line-items->service-type (orders/product-items-for-shipment shipment))
        {base-services  :base
         addon-services :addon} (->> order
                                     orders/service-line-items
                                     (group-by (comp keyword :service/type :variant-attrs)))
        base-service-line-item  (first base-services)

        sku-catalog                 (get-in app-state storefront.keypaths/v2-skus)
        base-service-sku            (get sku-catalog (:sku base-service-line-item))
        addon-services-skus         (->> addon-services
                                         (map (fn [addon-service] (get sku-catalog (:sku addon-service))))
                                         (map (fn [addon-sku] {:addon-service/title  (:sku/title addon-sku)
                                                               :addon-service/price  (some-> addon-sku :sku/price mf/as-money)
                                                               :addon-service/sku-id (:catalog/sku-id addon-sku)})))
        wig-customization-service?  (= :wig-customization service-type)
        install-items-required      (if wig-customization-service? 1 3)
        item-eligibility-fn         (if wig-customization-service?
                                      line-items/customizable-wig?
                                      (partial line-items/sew-in-eligible? sku-catalog))
        items-added-for-install     (->> shipment
                                         :line-items
                                         (filter item-eligibility-fn)
                                         (map :quantity)
                                         (apply +)
                                         (min install-items-required))
        items-remaining-for-install (- install-items-required items-added-for-install)
        freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        servicing-stylist           (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                                      (get-in app-state storefront.keypaths/store)
                                      (get-in app-state adventure-keypaths/adventure-servicing-stylist))
        sku-title                   (:sku/title base-service-sku)]
    {:mayvenn-install/entered?           freeinstall-entered?
     :mayvenn-install/locked?            (and base-service-line-item
                                              (not (line-items/fully-discounted? base-service-line-item)))
     :mayvenn-install/applied?           (orders/service-line-item-promotion-applied? order)
     :mayvenn-install/quantity-required  install-items-required
     :mayvenn-install/quantity-remaining items-remaining-for-install
     :mayvenn-install/quantity-added     items-added-for-install
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-title      sku-title
     :mayvenn-install/add-service-ttitle (str/join " " (butlast (str/split sku-title " ")))
     :mayvenn-install/service-discount   (- (line-items/service-line-item-price base-service-line-item))
     :mayvenn-install/any-wig?           any-wig?
     :mayvenn-install/service-image-url  (->> base-service-sku (images/skuer->image "cart") :url)
     :mayvenn-install/addon-services     addon-services-skus
     :mayvenn-install/service-type       service-type}))
