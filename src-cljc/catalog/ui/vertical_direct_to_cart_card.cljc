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
            [storefront.request-keys :as request-keys]
            [storefront.accessors.orders :as orders]))

(defn query
  [data product]
  (let [sku-id            (first (:selector/skus product))
        service-sku       (get-in data (conj keypaths/v2-skus sku-id))
        image             (->> service-sku
                               (images/for-skuer (get-in data keypaths/v2-images))
                               (filter (comp #{"catalog"} :use-case))
                               first)
        product-slug      (:page/slug product)
        servicing-stylist (get-in data adventure.keypaths/adventure-servicing-stylist)
        store-nickname    (:store-nickname servicing-stylist)
        cta-disabled?     (boolean (some #(= sku-id (:sku %))
                                         (orders/service-line-items (get-in data keypaths/order))))
        any-updates?      (utils/requesting-from-endpoint? data request-keys/add-to-bag)
        card-disabled?    (and (experiments/stylist-mismatch? data)
                               servicing-stylist
                               (not (:stylist-provides-service product)))]
    (cond-> {:card-image/src                             (str (:url image) "-/format/auto/" (:filename image))
             :card/type                                  :vertical-direct-to-cart-card
             :sort/value                                 [card-disabled?
                                                          (:sku/price service-sku)]
             :card-image/alt                             (:alt image)
             :react/key                                  (str "product-" product-slug)
             :vertical-direct-to-cart-card-title/id      (some->> product-slug (str "product-card-title-"))
             :vertical-direct-to-cart-card/primary       (:copy/title product)
             :vertical-direct-to-cart-card/secondary     (mf/as-money (:sku/price service-sku))
             :vertical-direct-to-cart-card/price         (:sku/price service-sku)
             :vertical-direct-to-cart-card/cta-ready?    (not any-updates?)
             :vertical-direct-to-cart-card/card-target   [events/navigate-product-details
                                                          {:catalog/product-id (:catalog/product-id product)
                                                           :page/slug          product-slug
                                                           :query-params       {:SKU sku-id}}]
             :vertical-direct-to-cart-card/cta-spinning? (utils/requesting? data (conj request-keys/add-to-bag sku-id))}
      (and (not card-disabled?)
           (not cta-disabled?))
      (merge {:vertical-direct-to-cart-card/cta-id        (str "add-to-cart-" sku-id)
              :vertical-direct-to-cart-card/cta-disabled? false
              :vertical-direct-to-cart-card/cta-max-width "111px"
              :vertical-direct-to-cart-card/cta-label     "Add To Cart"
              :vertical-direct-to-cart-card/cta-target    [events/control-add-sku-to-bag {:sku      service-sku
                                                                                          :quantity 1}]})

      (and (not card-disabled?)
           cta-disabled?)
      (merge {:vertical-direct-to-cart-card/cta-id        (str "add-to-cart-disabled-" sku-id)
              :vertical-direct-to-cart-card/cta-disabled? true
              :vertical-direct-to-cart-card/cta-ready?    false
              :vertical-direct-to-cart-card/cta-max-width "139px"
              :vertical-direct-to-cart-card/cta-label     "Already In Cart"})

      card-disabled?
      (merge {:vertical-direct-to-cart-card/disabled-background? true
              :vertical-direct-to-cart-card/cta-ready?           false
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
    :vertical-direct-to-cart-card/keys [primary secondary not-offered-id disabled-background?
                                        cta-id cta-target card-target cta-disabled? cta-ready?
                                        cta-spinning? cta-label]
    disabled-id                        :vertical-direct-to-cart-card.disabled/id
    disabled-primary                   :vertical-direct-to-cart-card.disabled/primary}]
  (c/html
   (let [non-cta-action (apply utils/route-to card-target)]
     [:div.inherit-color.col.col-6.col-4-on-tb-dt.p1
      {:key react-key}
      [:div.border.border-cool-gray.container-height.center.flex.flex-column.justify-between
       {:class (when disabled-background? "bg-cool-gray")}
       [:a.inherit-color.mb2 (assoc non-cta-action :data-test react-key)
        (ui/screen-aware card-image-molecule data)]
       [:a.inherit-color (apply utils/route-to card-target) primary]
       [:a.inherit-color non-cta-action secondary]
       (when disabled-id
         [:a.red.content-3.mt2.mb4
          (assoc non-cta-action :data-test not-offered-id)
          disabled-primary])
       (when cta-id
         [:div.my2.mx5
          (ui/button-small-secondary
           (merge (when (and cta-target cta-ready?)
                    (apply utils/fake-href cta-target))
                  {:data-test (str "add-to-cart-" react-key)
                   :disabled? cta-disabled?})
           (if cta-spinning?
             ui/spinner
             cta-label))])]])))
