(ns storefront.platform.reviews
  (:require [api.catalog]
            [catalog.products :as products]
            [storefront.accessors.products :as access-product]
            [storefront.accessors.shared-cart :as shared-cart]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.routes :as routes]))

(defcomponent reviews-component
  "The fully expanded reviews component using yotpo"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  [:div {:key (str "reviews-" (:data-product-id yotpo-data-attributes))}
   [:div
    [:.mx-auto
     [:.yotpo.yotpo-main-widget yotpo-data-attributes]]]])

(defcomponent reviews-summary-component
  "Yotpo summary reviews component"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  [:div {:key (str "reviews-summary-" (:data-product-id yotpo-data-attributes))}
   [:div.clearfix
    [:div.yotpo.bottomLine.mr2 yotpo-data-attributes]
    [:div.yotpo.QABottomLine yotpo-data-attributes]]])

(defcomponent reviews-summary-dropdown-experiment-component
  "Yotpo summary reviews component"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  [:div {:key (str "reviews-summary-" (:data-product-id yotpo-data-attributes))}
   [:div
    [:div.clearfix.flex.justify-start.flex-wrap.my1
     [:.yotpo.bottomLine.mr2 yotpo-data-attributes]
     [:.yotpo.QABottomLine yotpo-data-attributes]]]])

(defn- yotpo-data-attributes
  "Uses the first Sku from a Product to determine Yotpo data- attributes"
  [product all-skus]
  (when-let [{:keys [legacy/variant-id]}
             (some->> product :selector/skus first (get all-skus))]
    {:data-name       (:copy/title product)
     :data-product-id variant-id
     :data-url        (routes/path-for events/navigate-product-details product)}))

(defn query [data]
  (when-let [product (products/current-product data)]
    (when (products/eligible-for-reviews? product)
      (let [all-skus (get-in data keypaths/v2-skus)]
        {:yotpo-data-attributes (yotpo-data-attributes product all-skus)}))))

(defn query-look-detail [shared-cart-with-skus data]
  (let [all-skus (get-in data keypaths/v2-skus)
        sku-id   (->> shared-cart-with-skus
                     :line-items
                     (shared-cart/enrich-line-items-with-sku-data all-skus)
                     (api.catalog/select api.catalog/?hair)
                     first
                     :catalog/sku-id)
        products (get-in data keypaths/v2-products)
        product  (access-product/find-product-by-sku-id products sku-id)]
    (when (products/eligible-for-reviews? product)
      {:yotpo-data-attributes (yotpo-data-attributes product all-skus)})))
