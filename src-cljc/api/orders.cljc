(ns api.orders
  (:require adventure.keypaths
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            storefront.keypaths))

(defn ^:private mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views

  This is deprecated. There isn't a directly equivalent model to replace it.
  It has been conceptually superseded by 'free-mayvenn-service'

  Please don't use models because they conveniently have some of the information
  you need. Create a new model and discuss with the team to arrive at the most
  economical collection of models."
  [order servicing-stylist sku-catalog]
  (let [freeinstall-entered?        (boolean (orders/freeinstall-entered? order))
        service-line-item           (first (orders/service-line-items order))
        wig-customization?          (= "SRV-WGC-000" (:sku service-line-item))
        install-items-required      (if wig-customization? 1 3)
        item-eligibility-fn         (if wig-customization?
                                      line-items/customizable-wig?
                                      (partial line-items/sew-in-eligible? sku-catalog))
        items-added-for-install     (->> order
                                         :shipments
                                         first
                                         :line-items
                                         (filter item-eligibility-fn)
                                         (map :quantity)
                                         (apply +)
                                         (min install-items-required))
        items-remaining-for-install (- install-items-required items-added-for-install)]
    (when freeinstall-entered?
      {:mayvenn-install/entered?           freeinstall-entered?
       :mayvenn-install/locked?            (and freeinstall-entered?
                                                (pos? items-remaining-for-install))
       :mayvenn-install/applied?           (orders/service-line-item-promotion-applied? order)
       :mayvenn-install/quantity-required  install-items-required
       :mayvenn-install/quantity-remaining (- install-items-required items-added-for-install)
       :mayvenn-install/quantity-added     items-added-for-install
       :mayvenn-install/stylist            servicing-stylist
       :mayvenn-install/service-discount   (- (line-items/service-line-item-price service-line-item))})))

(def rules
  (let [?bundles
        {:catalog/department #{"hair"} :hair/family #{"bundles"}}
        ?closures
        {:catalog/department #{"hair"} :hair/family #{"closures"}}
        ?frontals
        {:catalog/department #{"hair"} :hair/family #{"frontals"}}
        ?360-frontals
        {:catalog/department #{"hair"} :hair/family #{"360-frontals"}}
        ?wigs
        {:catalog/department #{"hair"} :hair/family #{"lace-front-wigs" "360-wigs"}}
        wig-success
        "You're all set! Bleaching knots, tinting & cutting lace and hairline customization included."
        sew-in-success
        "You’re all set! Shampoo, braiding and basic styling included."]
    {"SRV-LBI-000"
     {:rules   [["bundle" ?bundles 3]]
      :success sew-in-success}
     "SRV-CBI-000"
     {:rules   [["bundle" ?bundles 2] ["closure" ?closures 1]]
      :success sew-in-success}
     "SRV-FBI-000"
     {:rules   [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
      :success sew-in-success}
     "SRV-3BI-000"
     {:rules   [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
      :success sew-in-success}
     "SRV-WGC-000"
     {:rules [["wig" ?wigs 1]]
      :success wig-success}}))

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
  [servicing-stylist order sku-catalog]
  (let [freeinstall-entered?     (boolean (orders/freeinstall-entered? order))
        service-line-item        (first (filter
                                     (comp :promo.mayvenn-install/discountable :variant-attrs)
                                     (orders/service-line-items order)))
        rules-for-service        (:rules (get rules (:sku service-line-item)))
        physical-items           (->> order :shipments (mapcat :line-items)
                                      (filter (fn [item]
                                                (= "spree" (:source item))))
                                      (map (fn [item]
                                             (merge
                                              (dissoc item :variant-attrs)
                                              (:variant-attrs item)))))
        failed-rules             (reduce (fn [acc [word essentials rule-quantity]]
                                           (let [cart-quantity    (->> physical-items
                                                                       (spice.selector/match-all
                                                                        {:selector/strict? true}
                                                                        essentials)
                                                                       (map :quantity)
                                                                       (apply +))
                                                 missing-quantity (- rule-quantity cart-quantity)]
                                             (cond-> acc
                                               (pos? missing-quantity)
                                               (conj [word essentials missing-quantity cart-quantity]))))
                                         []
                                         rules-for-service)
        physical-items-remaining (apply + (map (comp last butlast) failed-rules))]
    (when (seq service-line-item)
      #:free-mayvenn-service
      {:failed-criteria-count (->> [(seq service-line-item)
                                    (empty? failed-rules)
                                    (seq servicing-stylist)]
                                   (remove boolean)
                                   count)
       ;; criterion 1
       :service-item          service-line-item
       ;; criterion 2
       :hair-success          (:success (get rules (:sku service-line-item)))
       :hair-missing          (seq failed-rules)
       :hair-missing-quantity physical-items-remaining
       ;; criterion 3
       :stylist               (not-empty servicing-stylist)})))

(defn ->order
  [app-state order]
  (let [waiter-order      order
        servicing-stylist (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                            (get-in app-state storefront.keypaths/store)
                            (get-in app-state adventure.keypaths/adventure-servicing-stylist))
        sku-catalog       (get-in app-state storefront.keypaths/v2-skus)
        store-slug        (get-in app-state storefront.keypaths/store-slug)
        mayvenn-install   (mayvenn-install waiter-order servicing-stylist sku-catalog)]
    (merge
     mayvenn-install
     {:waiter/order         waiter-order
      :order/dtc?           (= "shop" store-slug)
      :order/submitted?     (= "submitted" (:state order))
      :order.shipping/phone (get-in waiter-order [:shipping-address :phone])
      :order.items/quantity (orders/product-quantity waiter-order)})))

(defn completed
  [app-state]
  (->order app-state (get-in app-state storefront.keypaths/completed-order)))

(defn current
  [app-state]
  (->order app-state (get-in app-state storefront.keypaths/order)))

(defn services
  "Model for services in the cart

  Scope:
  - services in cart
  - servicing stylist for those services
  "
  [app-state order]
  (let [experience (get-in app-state storefront.keypaths/store-experience)]
    (when-not (= "classic" experience)
      (let [stylist (if (= "aladdin" experience)
                      (get-in app-state storefront.keypaths/store)
                      (get-in app-state adventure.keypaths/adventure-servicing-stylist))]
        #:services{:stylist stylist
                   :items   (orders/service-line-items order)}))))
