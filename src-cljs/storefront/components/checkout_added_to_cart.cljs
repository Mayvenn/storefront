(ns storefront.components.checkout-added-to-cart
  (:require api.orders
            catalog.images
            [checkout.shop.cart-v202004 :as cart]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            spice.selector
            [storefront.accessors.orders :as orders]
            [storefront.accessors.images :as images]
            [storefront.accessors.line-items :as line-items]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.money-formatters :as $]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as ui-molecules]))

(defcomponent component
  [{:as   queried-data
    :keys [title
           service-line-items
           cart-items]}
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

(defn free-service-line-item-query
  [sku-db images-db free-service-line-item addon-skus]
  (let [sku-id      (:sku free-service-line-item)
        service-sku (get sku-db sku-id)]
    (when service-sku
      [(merge {:react/key                             (str "free-service-" sku-id)
               :cart-item-title/id                    (str "free-service-title-" sku-id)
               :cart-item-floating-box/id             (str "free-service-price-" sku-id)
               :cart-item-copy/lines                  [{:id    (str "line-item-quantity-" sku-id)
                                                        :value (str "qty. " (:quantity free-service-line-item))}]
               :cart-item-floating-box/value          (some-> free-service-line-item line-items/service-line-item-price $/as-money)
               :cart-item-service-thumbnail/id        (str "free-service-thumbnail-" sku-id)
               :cart-item-service-thumbnail/image-url (->> service-sku (images/skuer->image images-db "cart") :url)
               :cart-item-title/primary (:variant-name free-service-line-item)}
              (when (seq addon-skus)
                {:cart-item-sub-items/id    (str "free-service-" sku-id "-addon-services")
                 :cart-item-sub-items/title "Add-On Services"
                 ;; think about sharing this function
                 :cart-item-sub-items/items (map (fn [addon-sku]
                                                   {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                    :cart-item-sub-item/price  (some-> addon-sku :sku/price $/as-money)
                                                    :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                                 addon-skus)}))])))

(defn cart-items-query
  [sku-db images-db line-items]
  (for [{sku-id :sku :as line-item} line-items
        :let
        [sku   (get sku-db sku-id)
         price (or (:sku/price line-item)
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
                                                                           ($/as-money price)
                                                                           [:div.proxima.content-4 " each"]]
     :cart-item-square-thumbnail/id            sku-id
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> sku :hair/length first)]
                                                 (str length-circle-value "”"))
     :cart-item-square-thumbnail/ucare-id      (->> sku (catalog.images/image images-db "cart") :ucare/id)}))

(defn ^:private a-la-carte-service-line-items-query
  [sku-db images-db line-items]
  (for [{sku-id :sku :as service-line-item} line-items
        :let
        [sku   (get sku-db sku-id)
         price (or (:sku/price service-line-item)
                   (:unit-price service-line-item))]]
    {:react/key                             sku-id
     :cart-item-title/primary               (or (:product-title service-line-item)
                                                (:product-name service-line-item))
     :cart-item-title/id                    (str "line-item-" sku-id)
     :cart-item-copy/lines                  [{:id    (str "line-item-quantity-" sku-id)
                                              :value (str "qty. " (:quantity service-line-item))}]
     :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
     :cart-item-floating-box/value          (some-> price $/as-money)
     :cart-item-service-thumbnail/id        sku-id
     :cart-item-service-thumbnail/image-url (->> sku (catalog.images/image images-db "cart") :ucare/id)}))

(defn query
  [data]
  (let [order                                 (get-in data keypaths/order)
        sku-db                                (get-in data keypaths/v2-skus)
        images-db                             (get-in data keypaths/v2-images)
        recently-added-sku-ids->quantities    (get-in data storefront.keypaths/cart-recently-added-skus)
        products                              (get-in data keypaths/v2-products)
        facets                                (get-in data keypaths/v2-facets)
        recent-line-items                     (->> order
                                                   orders/product-and-service-items
                                                   (filter (fn [{:keys [sku]}] (contains? recently-added-sku-ids->quantities sku)))
                                                   (map (fn [line-item] (assoc line-item :quantity (recently-added-sku-ids->quantities (:sku line-item)))))
                                                   ;; NOTE: enables selecting later on
                                                   (map #(merge (:variant-attrs %) %)))

        physical-line-items           (->> recent-line-items
                                           (filter line-items/product?)
                                           (map (partial cart/add-product-title-and-color-to-line-item products facets)))
        free-mayvenn-service-line-item (->> recent-line-items
                                            (spice.selector/match-all {:selector/strict? true}
                                                                      catalog.services/discountable)
                                            first)

        a-la-carte-service-line-items (->> recent-line-items
                                           (spice.selector/match-all {:selector/strict? true}
                                                                     catalog.services/a-la-carte))
        addon-service-skus            (->> recent-line-items
                                           (spice.selector/match-all {:selector/strict? true}
                                                                     catalog.services/addons)
                                           (map (fn [addon-service] (get sku-db (:sku addon-service)))))]
    {:title                     (str (ui/pluralize-with-amount (->> recent-line-items
                                                                    (mapv :quantity)
                                                                    (apply +))
                                                               "Item") " Added")
     :cart-items                (cart-items-query sku-db images-db physical-line-items)
     :service-line-items        (concat
                                 (free-service-line-item-query sku-db
                                                               images-db
                                                               free-mayvenn-service-line-item addon-service-skus)
                                 (a-la-carte-service-line-items-query sku-db
                                                                      images-db
                                                                      a-la-carte-service-line-items))
     :return-link/back          (first (get-in data keypaths/navigation-undo-stack))
     :return-link/copy          "Continue Shopping"
     :return-link/event-message [events/navigate-category {:catalog/category-id "23",
                                                           :page/slug           "mayvenn-install"}]
     :return-link/id            "continue-shopping-link"}))

(defn ^:export built-component
  [data opts]
  (component/build component (query data) opts))
