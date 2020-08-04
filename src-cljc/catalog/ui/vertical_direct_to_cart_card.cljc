(ns catalog.ui.vertical-direct-to-cart-card
  (:require adventure.keypaths
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.accessors.images :as images]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.orders :as orders]))

(defn query
  [data product]
  (let [service-sku       (-> (get-in data keypaths/v2-skus)
                              (select-keys (:selector/skus product))
                              vals
                              first)
        image             (->> service-sku
                               (images/for-skuer (get-in data keypaths/v2-images))
                               (filter (comp #{"catalog"} :use-case))
                               first)
        product-slug      (:page/slug product)
        servicing-stylist (get-in data adventure.keypaths/adventure-servicing-stylist)
        store-nickname    (:store-nickname servicing-stylist)
        cta-disabled?     (boolean (some #(= (:catalog/sku-id service-sku) (:sku %))
                                         (orders/service-line-items (get-in data keypaths/order))))
        card-disabled?    (and (experiments/stylist-mismatch? data)
                               servicing-stylist
                               (not (:stylist-provides-service product)))]
    (cond-> {:card-image/src                             (str (:url image) "-/format/auto/" (:filename image))
             :card/type                                  :vertical-direct-to-cart-card
             :card-image/alt                             (:alt image)
             :react/key                                  (str "product-" product-slug)
             :vertical-direct-to-cart-card-title/id      (some->> product-slug (str "product-card-title-"))
             :vertical-direct-to-cart-card/primary       (:copy/title product)
             :vertical-direct-to-cart-card/secondary     (mf/as-money (:sku/price service-sku))
             :vertical-direct-to-cart-card/card-target   [events/navigate-product-details
                                                          {:catalog/product-id (:catalog/product-id product)
                                                           :page/slug          product-slug
                                                           :query-params       {:SKU (:catalog/sku-id service-sku)}}]}
      (and (not card-disabled?)
           (not cta-disabled?))
      (merge {:vertical-direct-to-cart-card/cta-id        (str "add-to-cart-" (:catalog/sku-id service-sku))
              :vertical-direct-to-cart-card/cta-disabled? false
              :vertical-direct-to-cart-card/cta-max-width "111px"
              :vertical-direct-to-cart-card/cta-label     "Add To Cart"
              :vertical-direct-to-cart-card/cta-target    [events/control-add-sku-to-bag {:sku      service-sku
                                                                                          :quantity 1}]})

      (and (not card-disabled?)
           cta-disabled?)
      (merge {:vertical-direct-to-cart-card/cta-id        (str "add-to-cart-disabled-" (:catalog/sku-id service-sku))
              :vertical-direct-to-cart-card/cta-disabled? true
              :vertical-direct-to-cart-card/cta-max-width "139px"
              :vertical-direct-to-cart-card/cta-label     "Already In Cart"})

      card-disabled?
      (merge {:vertical-direct-to-cart-card/disabled-background? true
              :vertical-direct-to-cart-card.disabled/id          (str "disabled-not-available")
              :vertical-direct-to-cart-card.disabled/primary     (str "Not Available with " store-nickname)}))))

(c/defcomponent card-image-molecule
  [{:keys [card-image/src card-image/alt screen/seen?]}
   _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (cond
    (nil? seen?) [:noscript
                  (ui/aspect-ratio
                   640 580
                   (ui/ucare-img
                    {:class "block col-12 container-height"
                     :alt   alt}
                    src))]
    seen?        (ui/aspect-ratio
                  640 580
                  (ui/ucare-img
                   {:class "block col-12 container-height"
                    :alt   alt}
                   src))
    :else        (ui/aspect-ratio
                  640 580
                  [:div.col-12.container-height
                   {:style {:height "100%"}}])))

(defn organism
  [{:as                                data
    react-key                          :react/key
    :vertical-direct-to-cart-card/keys [id primary secondary not-offered-id disabled-background?
                                        cta-id cta-target card-target cta-disabled? cta-label]
    disabled-id                        :vertical-direct-to-cart-card.disabled/id
    disabled-primary                   :vertical-direct-to-cart-card.disabled/primary}]
  (c/html
   (let [non-cta-action (merge (apply utils/route-to card-target)
                               {:data-test id})]
     [:div.inherit-color.col.col-6.col-4-on-tb-dt.p1
      {:key react-key}
      [:div.border.border-cool-gray.container-height.center.flex.flex-column.justify-between
       {:class (when disabled-background? "bg-cool-gray")}
       [:a.inherit-color.mb2 (merge
                              {:data-test react-key}
                              non-cta-action )
        (ui/screen-aware card-image-molecule data)]
       [:a.inherit-color non-cta-action primary]
       [:a.inherit-color non-cta-action secondary]
       (when disabled-id
         [:a.red.content-3.mt2.mb4
          (merge {:data-test not-offered-id}
                 non-cta-action)
          disabled-primary])
       (when cta-id
         [:div.my2.mx5
          (ui/button-small-secondary
           (merge (when cta-target (apply utils/fake-href cta-target))
                  {:data-test (str "add-to-cart-" react-key)
                   :disabled? cta-disabled?})
           cta-label)])]])))
