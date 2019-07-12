(ns storefront.platform.reviews
  (:require [catalog.products :as products]
            [om.core :as om]
            [sablono.core :refer [html]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.routes :as routes]))

(defn ^:private reviews-component-inner
  [{:keys [loaded? yotpo-data-attributes]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        [:.mx-auto.mb3
         [:.yotpo.yotpo-main-widget yotpo-data-attributes]]]))))

(defn reviews-component
  "The fully expanded reviews component using yotpo"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-" (:data-product-id yotpo-data-attributes))}
     (om/build reviews-component-inner queried-data opts)])))

(defn ^:private reviews-summary-component-inner
  [{:keys [loaded? yotpo-data-attributes]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        [:.px3.clearfix.pyp3
         [:.yotpo.bottomLine.mr2 yotpo-data-attributes]
         [:.yotpo.QABottomLine yotpo-data-attributes]]]))))

(defn reviews-summary-component
  "Yotpo summary reviews component"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-summary-" (:data-product-id yotpo-data-attributes))}
     (om/build reviews-summary-component-inner queried-data opts)])))


(defn ^:private reviews-summary-component-dropdown-experiment-inner
  [{:keys [loaded? yotpo-data-attributes]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        [:div.clearfix.flex.justify-start.flex-wrap.my1
         [:.yotpo.bottomLine.mr2 yotpo-data-attributes]
         [:.yotpo.QABottomLine yotpo-data-attributes]]]))))

(defn reviews-summary-dropdown-experiment-component
  "Yotpo summary reviews component"
  [{:keys [yotpo-data-attributes] :as queried-data} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-summary-" (:data-product-id yotpo-data-attributes))}
     (om/build reviews-summary-component-dropdown-experiment-inner queried-data opts)])))

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
  (let [line-item (first (:line-items shared-cart-with-skus))
        product (get (get-in data keypaths/v2-products) (first (:selector/from-products line-item)))]
    (when (products/eligible-for-reviews? product)
      (let [all-skus (get-in data keypaths/v2-skus)]
        {:yotpo-data-attributes (yotpo-data-attributes product all-skus)}))))
