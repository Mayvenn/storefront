(ns catalog.ui.horizontal-direct-to-cart-card
  (:require adventure.keypaths
            [storefront.accessors.images :as images]
            [storefront.component :as c]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]))

(defn query
  [data product]
  (let [service-sku          (-> (get-in data keypaths/v2-skus)
                                 (select-keys (:selector/skus product))
                                 vals
                                 first)
        image                (->> service-sku
                                  (images/for-skuer (get-in data keypaths/v2-images))
                                  (filter (comp #{"catalog"} :use-case))
                                  first)
        product-slug         (:page/slug product)
        cta-disabled?        (boolean (some #(= (:catalog/sku-id service-sku) (:sku %))
                                            (orders/service-line-items (get-in data keypaths/order))))
        servicing-stylist    (get-in data adventure.keypaths/adventure-servicing-stylist)
        store-nickname       (:store-nickname servicing-stylist)
        card-disabled?       (and (experiments/stylist-mismatch? data)
                                  servicing-stylist
                                  (not (:stylist-provides-service product)))]
    (cond-> {:card-image/src                                     (str (:url image) "-/format/auto/" (:filename image))
             :card/type                                          :horizontal-direct-to-cart-card
             :card-image/alt                                     (:alt image)
             :react/key                                          (str "product-" product-slug)
             :horizontal-direct-to-cart-card-title/id            (some->> product-slug (str "product-card-title-"))
             :horizontal-direct-to-cart-card/primary             (:copy/title product)
             :horizontal-direct-to-cart-card/secondary           (list
                                                                  ^:ignore-interpret-warning [:span.strike (mf/as-money (:sku/price service-sku))]
                                                                  ^:ignore-interpret-warning [:span.ml2.s-color "FREE"])
             :horizontal-direct-to-cart-card/tertiary            (:promo.mayvenn-install/requirement-copy product)
             :horizontal-direct-to-cart-card/card-target         [events/navigate-product-details
                                                                  {:catalog/product-id (:catalog/product-id product)
                                                                   :page/slug          product-slug
                                                                   :query-params       {:SKU (:catalog/sku-id service-sku)}}]
             :horizontal-direct-to-cart-card/disabled-background? false}

      (and (not card-disabled?)
           (not cta-disabled?))
      (merge {:horizontal-direct-to-cart-card/cta-id        (str "add-to-cart-" (:catalog/sku-id service-sku))
              :horizontal-direct-to-cart-card/cta-disabled? false
              :horizontal-direct-to-cart-card/cta-max-width "111px"
              :horizontal-direct-to-cart-card/cta-label     "Add To Cart"
              :horizontal-direct-to-cart-card/cta-target    [events/control-add-sku-to-bag {:sku      service-sku
                                                                                            :quantity 1}]})

      (and (not card-disabled?)
           cta-disabled?)
      (merge {:horizontal-direct-to-cart-card/cta-id        (str "add-to-cart-disabled-" (:catalog/sku-id service-sku))
              :horizontal-direct-to-cart-card/cta-disabled? true
              :horizontal-direct-to-cart-card/cta-max-width "139px"
              :horizontal-direct-to-cart-card/cta-label     "Already In Cart"})

      card-disabled?
      (merge {:horizontal-direct-to-cart-card/disabled-background? true
              :horizontal-direct-to-cart-card.disabled/id          (str "disabled-not-available")
              :horizontal-direct-to-cart-card.disabled/primary     (str "Not Available with " store-nickname)}))))

(c/defcomponent card-image-molecule
  [{:keys [card-image/src card-image/alt screen/seen?]}
   _ _]
  ;; TODO: when adding aspect ratio, also use srcset/sizes to scale these images.
  (cond
    (nil? seen?) [:noscript
                  (ui/ucare-img
                   {:class "block col-12"
                    :alt   alt}
                   (ui/square-image
                    {:resizable-url src} 280))]
    seen?        (ui/ucare-img
                  {:class "block col-12"
                   :alt   alt}
                  (ui/square-image
                   {:resizable-url src} 280))
    :else        [:div.col-12 {:style {:height "100%"}}]))

(defn organism
  [{:as                                  data
    react-key                            :react/key
    :horizontal-direct-to-cart-card/keys [primary secondary tertiary
                                          cta-id cta-target card-target cta-disabled? cta-label cta-max-width
                                          not-offered-id disabled-background?]
    disabled-id                          :horizontal-direct-to-cart-card.disabled/id
    disabled-primary                     :horizontal-direct-to-cart-card.disabled/primary}]
  (c/html
   (let [non-cta-action
         (merge (apply utils/route-to card-target)
                {:key       react-key
                 :data-test react-key})]
     [:div.col.col-12.col-6-on-tb-dt
      [:div.border.border-cool-gray.m1
       {:class (when disabled-background? "bg-cool-gray")}
       [:div.container-height.flex.justify-between
        [:a.col-5.inherit-color
         non-cta-action
         (ui/screen-aware card-image-molecule data)]
        [:div.px3.py2.col-7.flex.flex-column
         [:a.inherit-color.flex.flex-column.justify-between
          non-cta-action
          [:div.content-1 primary]
          [:div.content-3 {:style {:line-height "12px"}} tertiary]
          [:div.mt1 secondary]]
         (when disabled-id
           [:a.red.content-3
            (merge {:data-test not-offered-id}
                   non-cta-action)
            disabled-primary])
         (when cta-id
           [:div.mbn1
            {:style {:max-width cta-max-width}
             :key   cta-id}
            (ui/button-small-secondary
             (merge (when cta-target (apply utils/fake-href cta-target))
                    {:key       (str "add-to-cart-" react-key)
                     :class     "px0"
                     :data-test (str "add-to-cart-" react-key)
                     :disabled? cta-disabled?})
             cta-label)])]]]])))
