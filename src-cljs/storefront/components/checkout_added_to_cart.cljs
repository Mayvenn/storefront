(ns storefront.components.checkout-added-to-cart
  (:require api.orders
            [catalog.images :as catalog-images]
            [checkout.shop.cart-v202004 :as cart]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.images :as images]
            [storefront.accessors.sites :as sites]
            [storefront.accessors.line-items :as line-items]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.checkout-credit-card :as checkout-credit-card]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as ui-molecules]))

(defcomponent component
  [{:as   queried-data
    :keys [site
           order
           payment
           servicing-stylist
           freeinstall-cart-item
           service-line-items
           cart-items
           title]}
   owner _]
  [:div.container.p2
   [:div.clearfix.mxn3
    [:div
     [:div.p2 (ui-molecules/return-link queried-data)]
     [:div.bg-refresh-gray.p3.col-on-tb-dt.col-6-on-tb-dt.bg-white-on-tb-dt
      [:div.canela.title-2.center.my4 title]
      (for [service-line-item service-line-items]
        [:div.mt2-on-mb
         (component/build cart-item-v202004/organism {:cart-item service-line-item}
                          (component/component-id (:react/key service-line-item)))])

      (when (seq cart-items)
        [:div.mt3
         [:div
          {:data-test "confirmation-line-items"}

          (for [[index cart-item] (map-indexed vector cart-items)
                :let [react-key (:react/key cart-item)]
                :when react-key]
            [:div
             {:key (str index "-cart-item-" react-key)}
             (when-not (zero? index)
               [:div.flex.bg-white
                [:div.ml2 {:style {:width "75px"}}]
                [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto.mr2]])
             (component/build cart-item-v202004/organism {:cart-item  cart-item}
                              (component/component-id (str index "-cart-item-" react-key)))])]])]]

    [:div.col-on-tb-dt.col-6-on-tb-dt

     [:div.mx3
      [:div.col-12.mx-auto.mt4
       (ui/button-medium-primary (assoc (utils/route-to events/navigate-cart)
                                        :data-test "navigate-cart")
                                 "Go to Cart")]]]]])

(defn item-card-query
  [data]
  (let [order               (get-in data keypaths/order)
        skus                (get-in data keypaths/v2-skus)
        images              (get-in data keypaths/v2-images)
        facets              (maps/index-by :facet/slug (get-in data keypaths/v2-facets))
        color-options->name (->> facets
                                 :hair/color
                                 :facet/options
                                 (maps/index-by :option/slug)
                                 (maps/map-values :option/name))]
    {:items (mapv (fn [{sku-id :sku :as line-item}]
                    (let [sku   (get skus sku-id)
                          price (:unit-price line-item)]
                      {:react/key                 (str (:id line-item)
                                                       "-"
                                                       (:catalog/sku-id sku)
                                                       "-"
                                                       (:quantity line-item))
                       :circle/id                 (str "line-item-length-" sku-id)
                       :circle/value              (-> sku :hair/length first (str "”"))
                       :image/id                  (str "line-item-img-" (:catalog/sku-id sku))
                       :image/value               (->> sku (catalog-images/image images "cart") :ucare/id)
                       :title/id                  (str "line-item-title-" sku-id)
                       :title/value               (or (:product-title line-item)
                                                      (:product-name line-item))
                       :detail-top-left/id        (str "line-item-color-" sku-id)
                       :detail-top-left/value     (-> sku :hair/color first color-options->name str)
                       :detail-bottom-right/id    (str "line-item-price-ea-" sku-id)
                       :detail-bottom-right/value (str (mf/as-money price) " ea")
                       :detail-bottom-left/id     (str "line-item-quantity-" sku-id)
                       :detail-bottom-left/value  (str "Qty " (:quantity line-item))}))
                  (orders/product-items order))}))

(defn ^:private text->data-test-name [name]
  (-> name
      (string/replace #"[0-9]" (comp spice/number->word int))
      string/lower-case
      (string/replace #"[^a-z]+" "-")))

(defn free-service-line-items-query
  [data
   {:free-mayvenn-service/keys [service-item]}
   addon-skus]
  (let [sku-catalog        (get-in data storefront.keypaths/v2-skus)
        sku-id             (:sku service-item)
        service-sku        (get sku-catalog sku-id)
        wig-customization? (= "SRV-WGC-000" sku-id)
        images-catalog     (get-in data storefront.keypaths/v2-images)]
    (when (:id service-item)
      [(merge {:react/key                             "freeinstall-line-item-freeinstall"
               :cart-item-title/id                    "linr-item-title-upsell-free-service"
               :cart-item-floating-box/id             "line-item-freeinstall-price"
               :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                                        :value (str "You're all set! " (:copy/whats-included service-sku))}
                                                       {:id    (str "line-item-quantity-" sku-id)
                                                        :value (str "qty. " (:quantity service-item))}]
               :cart-item-floating-box/value          (some-> service-item
                                                              line-items/service-line-item-price
                                                              mf/as-money)
               :cart-item-service-thumbnail/id        "freeinstall"
               :cart-item-service-thumbnail/image-url (->> service-sku
                                                           (images/skuer->image images-catalog "cart")
                                                           :url)}
              (if wig-customization?
                {:cart-item-title/id      "line-item-title-applied-wig-customization"
                 :cart-item-title/primary "Wig Customization"}
                {:cart-item-title/id      "line-item-title-applied-mayvenn-install"
                 :cart-item-title/primary (:variant-name service-item)})
              (when (seq addon-skus)
                {:cart-item-sub-items/id    "addon-services"
                 :cart-item-sub-items/title "Add-On Services"
                 ;; think about sharing this function
                 :cart-item-sub-items/items (map (fn [addon-sku]
                                                   {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                    :cart-item-sub-item/price  (some-> addon-sku :sku/price mf/as-money)
                                                    :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                                 addon-skus)}))])))

(defn ^:private standalone-service-line-items-query
  [app-state]
  (let [skus                          (get-in app-state keypaths/v2-skus)
        images                        (get-in app-state keypaths/v2-images)
        recently-added-skus->qtys                       {"BNS24"        2
                                                         "PNS360FLC14"  1
                                                         "SRV-3BI-000"  1
                                                         "SRV-TKDU-000" 1
                                                         "SRV-WGM-000"  1}
        #_(get-in data storefront.keypaths/cart-recently-added-skus-qtys)
        recent-standalone-service-line-items (->> keypaths/order
                                                  (get-in app-state)
                                                  orders/service-line-items
                                                  (filter line-items/standalone-service?)
                                                  (filter (fn [{:keys [sku]}] (contains? recently-added-skus->qtys sku))))]
    (for [{sku-id :sku :as service-line-item} recent-standalone-service-line-items
          :let
          [sku   (get skus sku-id)
           price (or (:sku/price service-line-item)
                     (:unit-price service-line-item))]]
      {:react/key                             sku-id
       :cart-item-title/primary               (or (:product-title service-line-item)
                                                  (:product-name service-line-item))
       :cart-item-title/id                    (str "line-item-" sku-id)
       :cart-item-copy/lines                  [{:id    (str "line-item-whats-included-" sku-id)
                                                :value (:copy/whats-included sku)}
                                               {:id    (str "line-item-quantity-" sku-id)
                                                :value (str "qty. " (:quantity service-line-item))}]
       :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
       :cart-item-floating-box/value          (some-> price mf/as-money)
       :cart-item-service-thumbnail/id        sku-id
       :cart-item-service-thumbnail/image-url (->> sku (catalog-images/image images "cart") :ucare/id)})))

(defn cart-items-query
  [app-state line-items skus]
  (let [images (get-in app-state keypaths/v2-images)
        cart-items
        (for [{sku-id :sku :as line-item} line-items
              :let
              [sku                  (get skus sku-id)
               price                (or (:sku/price line-item)
                                        (:unit-price line-item))]]
          {:react/key                                (str sku-id "-" (:quantity line-item))
           :cart-item-title/id                       (str "line-item-title-" sku-id)
           :cart-item-title/primary                  (or (:product-title line-item)
                                                         (:product-name line-item))
           :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                       :value (str "qty. " (:quantity line-item))}]
           :cart-item-title/secondary                (:color-name line-item)
           :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
           :cart-item-floating-box/value             ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                                 (mf/as-money price)
                                                                                 [:div.proxima.content-4 " each"]]
           :cart-item-square-thumbnail/id            sku-id
           :cart-item-square-thumbnail/sku-id        sku-id
           :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> sku :hair/length first)]
                                                       (str length-circle-value "”"))
           :cart-item-square-thumbnail/ucare-id      (->> sku (catalog-images/image images "cart") :ucare/id)})]
    cart-items))

(defn query
  [data]
  (let [order                                           (get-in data keypaths/order)
        {servicing-stylist :services/stylist}           (api.orders/services data order)
        {:keys                    [service-item]
         free-service-discounted? :free-mayvenn-service/discounted?
         :as                      free-mayvenn-service} (api.orders/free-mayvenn-service servicing-stylist order)
        wig-customization?                              (orders/wig-customization? (get-in data keypaths/order))
        skus                                            (get-in data keypaths/v2-skus)
        recently-added-skus->qtys                       {"BNS24"        2
                                                         "PNS360FLC14"  1
                                                         "SRV-3BI-000"  1
                                                         "SRV-TKDU-000" 1
                                                         "SRV-WGM-000"  1}
        #_                                              (get-in data storefront.keypaths/cart-recently-added-skus-qtys)
        images-catalog                                  (get-in data storefront.keypaths/v2-images)
        products                                        (get-in data keypaths/v2-products)
        facets                                          (get-in data keypaths/v2-facets)
        recent-line-items                               (->> order
                                                             orders/product-and-service-items
                                                             (filter (fn [{:keys [sku]}] (contains? recently-added-skus->qtys sku)))
                                                             (map (fn [line-item] (assoc line-item :quantity (recently-added-skus->qtys (:sku line-item))))))
        ;; Start here on Monday

        ;; Idea is to put the addons on the base line items and use that to render the base services with addons.

        ;; base-line-items (->> recent-line-items
        ;;                      (filter line-items/base?)
        ;;                      (mapv (partial api.orders/->base-service order)))
        physical-line-items                             (->> recent-line-items
                                                             (filter line-items/product?)
                                                             (map (partial cart/add-product-title-and-color-to-line-item products facets)))

        ;; Then we'll use the addons on each base service instead of this
        addon-service-line-items                        (->> recent-line-items
                                                             (filter line-items/addon-service?)
                                                             (filter (comp boolean #{"addon"} :service/type :variant-attrs)))
        addon-service-skus                              (->> addon-service-line-items
                                                             (map (fn [addon-service] (get skus (:sku addon-service)))))]
    (cond->
        {:order                        order
         :title                        (str (ui/pluralize-with-amount (->> recent-line-items
                                                                           (mapv :quantity)
                                                                           (apply +))
                                                                      "Item")
                                            " Added")
         :store-slug                   (get-in data keypaths/store-slug)
         :site                         (sites/determine-site data)
         :products                     (get-in data keypaths/v2-products)
         :payment                      (checkout-credit-card/query data)
         :cart-items                   (cart-items-query data physical-line-items skus)
         :service-line-items           (concat
                                        (when (contains? recently-added-skus->qtys (:sku (:free-mayvenn-service/service-item free-mayvenn-service)))
                                          (free-service-line-items-query data free-mayvenn-service addon-service-skus))
                                        (standalone-service-line-items-query data))
         :return-link/back          (first (get-in data keypaths/navigation-undo-stack))
         :return-link/copy          "Continue Shopping"
         :return-link/event-message [events/navigate-category {:catalog/category-id "23", 
                                                               :page/slug "mayvenn-install"}]
         :return-link/id "continue-shopping-link"})))

(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))
