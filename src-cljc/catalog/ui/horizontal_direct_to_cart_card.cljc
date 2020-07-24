(ns catalog.ui.horizontal-direct-to-cart-card
  (:require adventure.keypaths
            [storefront.accessors.images :as images]
            [storefront.component :as c]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.orders :as orders]))

(defn query
  [data product]
  (let [service-sku    (-> (get-in data keypaths/v2-skus)
                           (select-keys (:selector/skus product))
                           vals
                           first)
        image          (->> service-sku
                            (images/for-skuer (get-in data keypaths/v2-images))
                            (filter (comp #{"catalog"} :use-case))
                            first)
        product-slug   (:page/slug product)
        disabled?      (boolean (some #(= (:catalog/sku-id service-sku) (:sku %))
                                      (orders/service-line-items (get-in data keypaths/order))))
        store-nickname (:store-nickname (get-in data adventure.keypaths/adventure-servicing-stylist))
        offered?       (:stylist-provides-service product)]
    {:card-image/src                                     (str (:url image) "-/format/auto/" (:filename image))
     :card/type                                          :horizontal-direct-to-cart-card
     :card-image/alt                                     (:alt image)
     :react/key                                          (str "product-" product-slug)
     :horizontal-direct-to-cart-card-title/id            (some->> product-slug (str "product-card-title-"))
     :horizontal-direct-to-cart-card/primary             (:copy/title product)
     :horizontal-direct-to-cart-card/secondary           (list
                                                          [:span.strike (mf/as-money (:sku/price service-sku))]
                                                          [:span.ml2.s-color "FREE"])
     :horizontal-direct-to-cart-card/tertiary            (:promo.mayvenn-install/requirement-copy product)
     :horizontal-direct-to-cart-card/card-target         [events/navigate-product-details
                                                          {:catalog/product-id (:catalog/product-id product)
                                                           :page/slug          product-slug
                                                           :query-params       {:SKU (:catalog/sku-id service-sku)}}]
     :horizontal-direct-to-cart-card/cta-disabled?       disabled?
     :horizontal-direct-to-cart-card/cta-max-width       (if disabled? "139px" "111px")
     :horizontal-direct-to-cart-card/cta-label           (if disabled? "Already In Cart" "Add To Cart")
     :horizontal-direct-to-cart-card/cta-target          [events/control-add-sku-to-bag {:sku      service-sku
                                                                                         :quantity 1}]
     :horizontal-direct-to-cart-card/not-offered-primary (str "Not Available with " store-nickname)
     :horizontal-direct-to-cart-card/not-offered-id      (if offered? false "not-offered-id")}))

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
  [{:as                data react-key :react/key
    :horizontal-direct-to-cart-card/keys [primary secondary tertiary cta-target card-target
                                          cta-disabled? cta-label cta-max-width
                                          not-offered-primary not-offered-id]}]
  (c/html
   (let [non-cta-action
         (merge (apply utils/route-to card-target)
                {:key       react-key
                 :data-test react-key})]
     [:div.col.col-12.col-6-on-tb-dt
      [:div.border.border-cool-gray.m1
       {:class (when not-offered-id "bg-cool-gray")}
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
         (if not-offered-id
           [:a.red.content-3 non-cta-action
            not-offered-primary]
           [:div.mbn1 {:style {:max-width cta-max-width}}
            (ui/button-small-secondary
             (merge (apply utils/fake-href cta-target)
                    {:key       (str "add-to-cart-" react-key)
                     :class     "px0"
                     :data-test (str "add-to-cart-" react-key)
                     :disabled? cta-disabled?})
             cta-label)])]]]])))
