(ns catalog.ui.horizontal-direct-to-cart-card
  (:require api.current
            api.orders
            api.products
            [storefront.accessors.images :as images]
            [storefront.component :as c]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.orders :as orders]))

(defn query
  [data product]
  (let [{:service/keys [world]} (api.orders/current data)
        {:as service-sku :catalog/keys [sku-id]}
        ;; Find the base sku with no add-ons
        (if (= "SV2" world)
          (->> product
               (api.products/product<- data)
               :product?essential-service/result
               first)
          ;; GROT(SRV)
          (->> product
               :selector/skus
               first
               (conj keypaths/v2-skus)
               (get-in data)))

        image         (->> service-sku
                           (images/for-skuer (get-in data keypaths/v2-images))
                           (filter (comp #{"catalog"} :use-case))
                           first)
        product-slug  (:page/slug product)
        cta-disabled? (boolean (some (comp #{sku-id} :sku)
                                     (orders/service-line-items (get-in data keypaths/order))))
        any-updates?  (utils/requesting-from-endpoint? data request-keys/add-to-bag)]
    (cond-> {:card-image/src                                      (str (:url image) "-/format/auto/" (:filename image))
             :card/type                                           :horizontal-direct-to-cart-card
             :sort/value                                          [(case (first (:service/category service-sku))
                                                                     "install" 1
                                                                     "construction" 2
                                                                     3)
                                                                   (:sku/price service-sku)]
             :card-image/alt                                      (:alt image)
             :react/key                                           (str "product-" product-slug)
             :horizontal-direct-to-cart-card-title/id             (some->> product-slug (str "product-card-title-"))
             :horizontal-direct-to-cart-card/primary              (:copy/title product)
             :horizontal-direct-to-cart-card/secondary-struck     (mf/as-money (:sku/price service-sku))
             :horizontal-direct-to-cart-card/secondary            "FREE"
             :horizontal-direct-to-cart-card/tertiary             (:promo.mayvenn-install/requirement-copy product)
             :horizontal-direct-to-cart-card/card-target          [events/navigate-product-details
                                                                   {:catalog/product-id (:catalog/product-id product)
                                                                    :page/slug          product-slug
                                                                    :query-params       {:SKU sku-id}}]
             :horizontal-direct-to-cart-card/cta-ready?           (not any-updates?)
             :horizontal-direct-to-cart-card/cta-spinning?        (utils/requesting? data (conj request-keys/add-to-bag sku-id))}

      (not cta-disabled?)
      (merge {:horizontal-direct-to-cart-card/cta-id        (str "add-to-cart-" sku-id)
              :horizontal-direct-to-cart-card/cta-disabled? false
              :horizontal-direct-to-cart-card/cta-max-width "111px"
              :horizontal-direct-to-cart-card/cta-label     "Add To Bag"
              :horizontal-direct-to-cart-card/cta-target    [events/control-add-sku-to-bag {:sku      service-sku
                                                                                            :quantity 1}]})

      cta-disabled?
      (merge {:horizontal-direct-to-cart-card/cta-id        (str "add-to-cart-disabled-" sku-id)
              :horizontal-direct-to-cart-card/cta-disabled? true
              :horizontal-direct-to-cart-card/cta-ready?    false
              :horizontal-direct-to-cart-card/cta-max-width "139px"
              :horizontal-direct-to-cart-card/cta-label     "Already In Bag"}))))

(c/defcomponent card-image-molecule
  [{:keys [card-image/src card-image/alt screen/seen?]}
   _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (cond
    (nil? seen?) [:noscript
                  (ui/ucare-img
                   {:class "block col-12"
                    :alt   alt
                    :square? true
                    :size    280}
                   (ui/square-image
                    {:resizable-url src} 280))]
    seen?        (ui/ucare-img
                  {:class   "block col-12"
                   :alt     alt
                   :square? true
                   :size    280}
                  (ui/square-image
                   {:resizable-url src} 280))
    :else        [:div.col-12 {:style {:height "100%"}}]))

(defn organism
  [{:as                                  data
    react-key                            :react/key
    :horizontal-direct-to-cart-card/keys
    [card-target
     cta-disabled?
     cta-id
     cta-label
     cta-max-width
     cta-ready?
     cta-spinning?
     cta-target
     primary
     secondary
     secondary-struck
     tertiary]}]
  (c/html
   (let [non-cta-action
         (merge (apply utils/route-to card-target)
                {:data-test react-key})]
     [:div.col.col-12.col-6-on-tb-dt
      {:key react-key}
      [:div.border.border-cool-gray.m1
       [:div.container-height.flex.justify-between
        [:a.col-5.inherit-color
         non-cta-action
         (ui/screen-aware card-image-molecule data)]
        [:div.px3.py2.col-7.flex.flex-column
         [:a.inherit-color.flex.flex-column.justify-between
          non-cta-action
          [:div.content-2.proxima primary]
          [:div.content-3.py1 {:style {:line-height "1.125"}} tertiary]
          [:div.mb1
           [:span.strike secondary-struck]
           [:span.ml2.s-color secondary]]]
         (when cta-id
           [:div.mbn1
            {:style {:max-width cta-max-width}
             :key   cta-id}
            (ui/button-small-secondary
             (merge (when (and cta-target cta-ready?)
                      (apply utils/fake-href cta-target))
                    {:key       cta-id
                     :class     "px0"
                     :data-test cta-id
                     :disabled? cta-disabled?})
             (if cta-spinning?
               ui/spinner
               cta-label))])]]]])))
