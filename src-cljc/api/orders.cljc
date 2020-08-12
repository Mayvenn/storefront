(ns api.orders
  (:require [adventure.keypaths :as adventure-keypaths]
            [clojure.string :as string]
            [spice.selector :refer [match-all]]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.components.money-formatters :as mf]
            [storefront.events :as e]
            storefront.keypaths))

(defn hair-family->service-sku-ids [hair-family]
  (get {"bundles"         #{"SRV-LBI-000" "SRV-UPCW-000"}
        "closures"        #{"SRV-CBI-000" "SRV-CLCW-000"}
        "frontals"        #{"SRV-FBI-000" "SRV-LFCW-000"}
        "360-frontals"    #{"SRV-3BI-000" "SRV-3CW-000"}
        "360-wigs"        #{"SRV-WGC-000"}
        "lace-front-wigs" #{"SRV-WGC-000"}}
       hair-family))

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
        install-navigation-message [e/navigate-category {:catalog/category-id "23"
                                                         :page/slug           "mayvenn-install"}]
        wig-navigation-message     [e/navigate-category {:catalog/category-id "13"
                                                         :page/slug           "wigs-install"
                                                         :query-params        {:family (str "360-wigs" categories/query-param-separator "lace-front-wigs")}}]

                                                                   ; [word essentials cart-quantity]
        leave-out-service-rules           {:rules                    [["bundle" ?bundles 3]]
                                           :failure-navigation-event install-navigation-message
                                           :add-more?                true}
        closure-service-rules             {:rules                    [["bundle" ?bundles 2] ["closure" ?closures 1]]
                                           :failure-navigation-event install-navigation-message
                                           :add-more?                true}
        frontal-service-rules             {:rules                    [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
                                           :failure-navigation-event install-navigation-message
                                           :add-more?                true}
        three-sixty-frontal-service-rules {:rules                    [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
                                           :failure-navigation-event install-navigation-message
                                           :add-more?                true}]
    {"SRV-LBI-000"
     leave-out-service-rules
     "SRV-UPCW-000"
     leave-out-service-rules
     "SRV-CBI-000"
     closure-service-rules
     "SRV-CLCW-000"
     closure-service-rules
     "SRV-FBI-000"
     frontal-service-rules
     "SRV-LFCW-000"
     frontal-service-rules
     "SRV-3BI-000"
     three-sixty-frontal-service-rules
     "SRV-3CW-000"
     three-sixty-frontal-service-rules
     "SRV-WGC-000"
     {:rules                    [["Virgin Lace Front or a Virgin 360 Wig" ?wigs 1]]
      :failure-navigation-event wig-navigation-message
      :add-more?                false}}))

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
  [servicing-stylist order]
  (let [service-line-item                                    (first (filter
                                                                     (comp :promo.mayvenn-install/discountable :variant-attrs)
                                                                     (orders/service-line-items order)))
        {rules-for-service        :rules
         add-more?                :add-more?
         failure-navigation-event :failure-navigation-event} (get rules (:sku service-line-item))
        physical-items                                       (->> order :shipments (mapcat :line-items)
                                                                  (filter (fn [item]
                                                                            (= "spree" (:source item))))
                                                                  (map (fn [item]
                                                                         (merge
                                                                          (dissoc item :variant-attrs)
                                                                          (:variant-attrs item)))))
        failed-rules                                         (keep (fn [[word essentials rule-quantity]]
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
       :add-more?                add-more?
       :failed-criteria-count    (->> [(seq service-line-item)
                                       (empty? failed-rules)
                                       (seq servicing-stylist)]
                                      (remove boolean)
                                      count)
       :failure-navigation-event failure-navigation-event
       ;; criterion 1
       :service-item             service-line-item
       ;; criterion 2
       :hair-missing             (seq failed-rules)
       :hair-missing-quantity    (apply + (map :missing-quantity failed-rules))
       :hair-success-quantity    (apply + (map last rules-for-service))
       ;; criterion 3
       :stylist                  (not-empty servicing-stylist)})))

(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views.
  Service type is inferred from the cart content in order to display appropriate
  service related copy.

  This is deprecated. There isn't a directly equivalent model to replace it.
  It has been conceptually superseded by 'free-mayvenn-service'"
  [order servicing-stylist sku-catalog images-catalog promotion-helper?]
  (let [shipment                        (-> order :shipments first)
        {addon-services :addon}         (->> order
                                             orders/service-line-items
                                             (group-by (comp keyword :service/type :variant-attrs)))
        {:free-mayvenn-service/keys
         [hair-missing
          hair-missing-quantity
          hair-success-quantity
          service-item]
         :as free-mayvenn-service-data} (free-mayvenn-service servicing-stylist order)

        discountable-services-on-order? (boolean (orders/discountable-services-on-order? order))]
    (merge
     {:mayvenn-install/discountable-services-on-order? discountable-services-on-order?
      :mayvenn-install/action-label       "add"
      :mayvenn-install/applied?           (orders/service-line-item-promotion-applied? order)
      :mayvenn-install/stylist            servicing-stylist
      :mayvenn-install/wig-customization? (= "SRV-WGC-000" (:sku service-item))
      :mayvenn-install/any-wig?           (->> shipment
                                               :line-items
                                               (filter line-items/any-wig?)
                                               count
                                               pos?)}
     (when (seq free-mayvenn-service-data)
       (let [satisfied?          (zero? hair-missing-quantity)
             steps-remaining     hair-missing-quantity
             mayvenn-install-sku (get sku-catalog (:sku service-item))]
         {:mayvenn-install/locked?                            (if promotion-helper?
                                                                false
                                                                (not satisfied?))
          :mayvenn-install/needs-more-items-for-free-service? (< 0 steps-remaining)
          :mayvenn-install/cart-helper-copy                   (if satisfied?
                                                                (str "You're all set! " (:copy/whats-included mayvenn-install-sku))
                                                                (str "Add " (string/join " and " (->> hair-missing
                                                                                                      (map (fn [{:keys [word missing-quantity]}]
                                                                                                             (apply str (if (= 1 missing-quantity)
                                                                                                                          ["a " word]
                                                                                                                          [missing-quantity " " word "s"]))))))))
          :mayvenn-install/quantity-required                  hair-success-quantity
          :mayvenn-install/quantity-remaining                 hair-missing-quantity
          :mayvenn-install/quantity-added                     (- hair-success-quantity hair-missing-quantity)
          :mayvenn-install/service-sku                        mayvenn-install-sku
          :mayvenn-install/service-title                      (:sku/title mayvenn-install-sku)
          :mayvenn-install/service-discount                   (- (line-items/service-line-item-price service-item))
          :mayvenn-install/service-image-url                  (->> mayvenn-install-sku (images/skuer->image images-catalog "cart") :url)
          :mayvenn-install/addon-services                     (->> addon-services
                                                                   (map (fn [addon-service] (get sku-catalog (:sku addon-service))))
                                                                   (map (fn [addon-sku] {:addon-service/title  (:sku/title addon-sku)
                                                                                         :addon-service/price  (some-> addon-sku :sku/price mf/as-money)
                                                                                         :addon-service/sku-id (:catalog/sku-id addon-sku)})))})))))

(defn ->order
  [app-state order]
  (let [waiter-order      order
        images-catalog    (get-in app-state storefront.keypaths/v2-images)
        servicing-stylist (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                            (get-in app-state storefront.keypaths/store)
                            (get-in app-state adventure.keypaths/adventure-servicing-stylist))
        sku-catalog       (get-in app-state storefront.keypaths/v2-skus)
        store-slug        (get-in app-state storefront.keypaths/store-slug)
        promotion-helper? (experiments/promotion-helper? app-state)
        mayvenn-install   (mayvenn-install waiter-order servicing-stylist sku-catalog images-catalog promotion-helper?)]
    (merge
     mayvenn-install
     {:waiter/order         waiter-order
      :order/dtc?           (= "shop" store-slug)
      :order/submitted?     (= "submitted" (:state order))
      :order.shipping/phone (get-in waiter-order [:shipping-address :phone])
      :order.items/quantity (orders/displayed-cart-count waiter-order)})))

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
