(ns looks.customization-modal
  (:require
   [storefront.component :as c]
   [storefront.accessors.contentful :as contentful]
   [storefront.accessors.shared-cart :as shared-cart]
   [storefront.components.popup :as popup]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.events :as e]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.transitions :as t]))

(defmethod popup/query :looks-customization
  [state]
  (let [skus                   (get-in state keypaths/v2-skus)
        shared-cart            (get-in state keypaths/shared-cart-current)
        {:keys [selector/from-products
                catalog/sku-id
                hair/origin
                hair/color
                hair/texture]} (some->> shared-cart
                                        :line-items
                                        (shared-cart/enrich-line-items-with-sku-data skus)
                                        (filter #(contains? (:catalog/department %) "hair"))
                                        shared-cart/sort-by-depart-and-price
                                        first)
        cheapest-product       (get-in state (conj keypaths/v2-products (first from-products)))
        pdp-target             [e/navigate-product-details {:page/slug          (:page/slug cheapest-product)
                                                            :catalog/product-id (:catalog/product-id cheapest-product)
                                                            :query-params       {:SKU sku-id}}]
        install-target         [e/navigate-category {:catalog/category-id "23"
                                                     :page/slug           "mayvenn-install"
                                                     :query-params        {:origin  (first origin)
                                                                           :color   (first color)
                                                                           :texture (first texture)}}]]
    (when sku-id
      {:image-id (->> state
                      contentful/selected-look
                      (contentful/look->look-detail-social-card
                       (get-in state keypaths/selected-album-keyword))
                      :image-url
                      ui/ucare-img-id)
       :title    "What do you want to change from this look?"
       :options  [{:title  "Color"
                   :target pdp-target
                   :id     "customize-color"}
                  {:title  "Length"
                   :target pdp-target
                   :id     "customize-length"}
                  {:title  "Install Type"
                   :target install-target
                   :id     "customize-install-type"}]})))

(c/defcomponent option [{:keys [title target id]} _ {react-id :id}]
  [:a.flex.justify-between.items-center.border.border-cool-gray.p5.mb2.inherit-color
   (merge {:key       react-id
           :data-test id}
          (apply utils/route-to target))
   [:span.medium.flex-auto title]
   ^:inline (ui/forward-caret {:width  17
                               :height 17})])

(defmethod popup/component :looks-customization
  [{:keys [title image-id] :as data} _ _]
  (c/html
   (ui/modal
    {:body-style  {:max-width "625px"}
     :close-attrs (utils/fake-href e/control-looks-customization-dismiss)
     :col-class   "col-12 p3"}
    [:div.bg-white.p5.flex.flex-column
     [:a.flex.self-end
      (svg/x-sharp
       (merge (apply utils/fake-href [e/control-looks-customization-dismiss])
              {:data-test "looks-customization-dismiss"
               :height    "18px"
               :width     "18px"}))]
     [:div.flex.justify-center.my4 (ui/circle-ucare-img {:width "120px"} image-id )]
     [:div.canela.title-2.center.mb6 title]

     (c/elements option data :options)])))

(defmethod t/transition-state e/control-show-looks-customization-modal
  [_ _ _ state]
  (assoc-in state keypaths/popup :looks-customization))

(defmethod t/transition-state e/control-looks-customization-dismiss
  [_ _ _ state]
  (assoc-in state keypaths/popup nil))
