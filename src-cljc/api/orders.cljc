(ns api.orders
  (:require [adventure.keypaths :as adventure-keypaths]
            [spice.selector :refer [match-all]]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            storefront.keypaths))

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

(defn ^:private product-line-items->service-type
  [product-items]
  (->> product-items
       (map (comp :hair/family :variant-attrs))
       (sort-by (partial family->ordering))
       first
       hair-family->service-type))

(defn line-item->service-type
     [line-item]
     (get
      {"SRV-LBI-000" :leave-out
       "SRV-CBI-000" :closure
       "SRV-FBI-000" :frontal
       "SRV-3BI-000" :three-sixty
       "SRV-WGC-000" :wig-customization}
          (:sku line-item)))

(defn services->service-type
     [base-services]
     (->> base-services
          (keep line-item->service-type)
          first))

(defn locked-content-for [singular-addition-hair-family additional-hair-family]
  (fn [items-needed satisfied?]
    (if (not satisfied?)
      (let [bundles-count                 (items-needed "bundles")
            bundles-required?             (pos? bundles-count)
            additional-hair-family-count  (items-needed additional-hair-family)
            needs-additional-hair-family? (and additional-hair-family
                                               (pos? additional-hair-family-count))]
        (str "Add "
            (when bundles-required?
              (str bundles-count (ui/pluralize bundles-count " bundle" " bundles")))
            (when (and bundles-required?
                       needs-additional-hair-family?)
              " and ")
            (when needs-additional-hair-family?
              (str additional-hair-family-count
                   (ui/pluralize additional-hair-family-count
                                 singular-addition-hair-family
                                 additional-hair-family)))))
      "You’re all set! Shampoo, braiding and basic styling included.")))

(defn- satisfies-all-needs [items-needed]
  (not (some pos? (vals items-needed))))

(defn- remaining-count
  [items-needed]
  (reduce + (filter pos? (vals items-needed))))

(defn service-locked-content
  [service-type product-quantities]
  (let [all-requirements {:leave-out         {:satisfies?           satisfies-all-needs
                                              :requirements         {"bundles" 3}
                                              :steps-required       3
                                              :steps-remaining      remaining-count
                                              :promo-helper-copy-fn (locked-content-for nil nil)}
                          :closure           {:satisfies?           satisfies-all-needs
                                              :requirements         {"bundles"  2
                                                                     "closures" 1}
                                              :steps-required       3
                                              :steps-remaining      remaining-count
                                              :promo-helper-copy-fn (locked-content-for "closure" "closures")}
                          :frontal           {:satisfies?           satisfies-all-needs
                                              :requirements         {"bundles"  2
                                                                     "frontals" 1}
                                              :steps-required       3
                                              :steps-remaining      remaining-count
                                              :promo-helper-copy-fn (locked-content-for "frontal" "frontals")}
                          :three-sixty       {:requirements         {"bundles"      2
                                                                     "360-frontals" 1}
                                              :steps-required       3
                                              :steps-remaining      remaining-count
                                              :satisfies?           satisfies-all-needs
                                              :promo-helper-copy-fn (locked-content-for "360 frontal" "360-frontals")}
                          :wig-customization {:requirements         {"360-wigs"        1
                                                                     "lace-front-wigs" 1}
                                              :steps-required       1
                                              :steps-remaining      remaining-count
                                              :satisfies?           (fn [items-needed]
                                                                      (or (<= (items-needed "360-wigs") 0)
                                                                          (<= (items-needed "lace-front-wigs") 0)))
                                              :promo-helper-copy-fn (fn [items-needed satisfied?]
                                                                      (if satisfied?
                                                                        "You're all set! Bleaching knots, tinting & cutting lace and hairline customization included."
                                                                        "Add a Virgin Lace Front or a Virgin 360 Wig"))}
                          }
        requirements     (get all-requirements service-type)
        items-needed     (->> (select-keys product-quantities (keys (:requirements requirements)))
                              (merge-with (fnil - 0 0) (:requirements requirements)))
        satisfied?       ((:satisfies? requirements) items-needed)
        steps-required   (:steps-required requirements)
        steps-remaining  ((:steps-remaining requirements) items-needed)]
    {:promo-helper-copy ((:promo-helper-copy-fn requirements) items-needed satisfied?)
     :disable-checkout? (not satisfied?)
     :steps-required    steps-required
     :current-step      (- steps-required steps-remaining)
     :steps-remaining   steps-remaining}))


(defn product-line-items->hair-family-counts [line-items]
  (reduce (fn [m line-item]
            (update m (-> line-item
                          :variant-attrs
                          :hair/family)
                    (fnil + 0 0)
                    (:quantity line-item)))
          {}
          line-items))

(defn mayvenn-install
  "This is the 'Mayvenn Install' model that is used to build queries for views.
  Service type is inferred from the cart content in order to display appropriate
  service related copy.

  This is deprecated. There isn't a directly equivalent model to replace it.
  It has been conceptually superseded by 'free-mayvenn-service'"
  [order servicing-stylist sku-catalog add-free-service?]
  (let [shipment                (-> order :shipments first)
        any-wig?                (->> shipment
                                     :line-items
                                     (filter line-items/any-wig?)
                                     count
                                     pos?)
        {base-services  :base
         addon-services :addon} (->> order
                                     orders/service-line-items
                                     (group-by (comp keyword :service/type :variant-attrs)))

        product-line-items      (orders/product-items-for-shipment shipment)
        updated-service-type    (services->service-type base-services)
        service-type            (if add-free-service?
                                  updated-service-type
                                  (or (product-line-items->service-type product-line-items)
                                      updated-service-type))
        {:keys [disable-checkout?
                promo-helper-copy
                steps-required
                current-step
                steps-remaining]} (when (and add-free-service? service-type)
                                    (->> (product-line-items->hair-family-counts product-line-items)
                                         (service-locked-content service-type)))
        mayvenn-install-line-item (->> base-services
                                       (filter #(:promo.mayvenn-install/discountable (:variant-attrs %)))
                                       first)

        mayvenn-install-sku         (get sku-catalog (:sku mayvenn-install-line-item))
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
        sku-title                   (:sku/title mayvenn-install-sku)
        locked?                     (and mayvenn-install-line-item
                                         (not (line-items/fully-discounted? mayvenn-install-line-item)))]
    {:mayvenn-install/entered?           freeinstall-entered?
     :mayvenn-install/locked?            (if add-free-service? disable-checkout? locked?)
     :mayvenn-install/promo-helper-copy  promo-helper-copy
     :mayvenn-install/action-label       (if add-free-service? "add" "add items")
     :mayvenn-install/applied?           (orders/service-line-item-promotion-applied? order)
     :mayvenn-install/quantity-required  (if add-free-service? steps-required install-items-required)
     :mayvenn-install/quantity-remaining (if add-free-service? steps-remaining items-remaining-for-install)
     :mayvenn-install/quantity-added     (if add-free-service? current-step items-added-for-install)
     :mayvenn-install/stylist            servicing-stylist
     :mayvenn-install/service-title      sku-title
     :mayvenn-install/service-discount   (- (line-items/service-line-item-price mayvenn-install-line-item))
     :mayvenn-install/any-wig?           any-wig?
     :mayvenn-install/service-image-url  (->> mayvenn-install-sku (images/skuer->image "cart") :url)
     :mayvenn-install/addon-services     addon-services-skus
     :mayvenn-install/service-type       service-type}))

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
        "You’re all set! Shampoo, braiding and basic styling included."
        install-navigation-message [e/navigate-category {:catalog/category-id "23"
                                                         :page/slug           "mayvenn-install"}]
        wig-navigation-message     [e/navigate-category {:catalog/category-id "13"
                                                         :page/slug           "wigs-install"
                                                         :query-params        {:family (str "360-wigs" categories/query-param-separator "lace-front-wigs")}}]]
    {"SRV-LBI-000"
     {:rules                    [["bundle" ?bundles 3]] ; TODO {:word :essentials :cart-quantity}
      :failure-navigation-event install-navigation-message

      :success sew-in-success}
     "SRV-CBI-000"
     {:rules                    [["bundle" ?bundles 2] ["closure" ?closures 1]]
      :failure-navigation-event install-navigation-message
      :success                  sew-in-success}
     "SRV-FBI-000"
     {:rules                    [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
      :failure-navigation-event install-navigation-message
      :success                  sew-in-success}
     "SRV-3BI-000"
     {:rules                    [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
      :failure-navigation-event install-navigation-message
      :success                  sew-in-success}
     "SRV-WGC-000"
     {:rules                    [["Virgin Lace Front or a Virgin 360 Wig" ?wigs 1]]
      :failure-navigation-event wig-navigation-message
      :success                  wig-success}}))

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
                                                                            :rule-quantity    rule-quantity
                                                                            :cart-quantity    cart-quantity
                                                                            :missing-quantity missing-quantity
                                                                            :essentials       essentials})))
                                                                     rules-for-service)]
    (when (seq service-line-item)
      #:free-mayvenn-service
      {:failed-criteria-count    (->> [(seq service-line-item)
                                       (empty? failed-rules)
                                       (seq servicing-stylist)]
                                      (remove boolean)
                                      count)
       :failure-navigation-event failure-navigation-event
       ;; criterion 1
       :service-item             service-line-item
       ;; criterion 2
       :hair-success             (:success (get rules (:sku service-line-item)))
       :hair-missing             (seq failed-rules)
       :hair-missing-quantity    (apply + (map :missing-quantity failed-rules))
       :hair-success-quantity    (apply + (map :rule-quantity failed-rules))
       ;; criterion 3
       :stylist                  (not-empty servicing-stylist)})))

(defn ->order
  [app-state order]
  (let [waiter-order      order
        servicing-stylist (if (= "aladdin" (get-in app-state storefront.keypaths/store-experience))
                            (get-in app-state storefront.keypaths/store)
                            (get-in app-state adventure.keypaths/adventure-servicing-stylist))
        sku-catalog       (get-in app-state storefront.keypaths/v2-skus)
        store-slug        (get-in app-state storefront.keypaths/store-slug)
        add-free-service? (experiments/add-free-service? app-state)
        mayvenn-install   (mayvenn-install waiter-order servicing-stylist sku-catalog add-free-service?)]
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
