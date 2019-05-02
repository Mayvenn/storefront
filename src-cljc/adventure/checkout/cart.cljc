(ns adventure.checkout.cart
  (:require
   #?@(:cljs [[storefront.hooks.quadpay :as quadpay]])
   [adventure.checkout.cart.items :as adventure-cart-items]
            [adventure.checkout.cart.summary :as adventure-cart-summary]
            [adventure.keypaths :as adventure.keypaths]
            [catalog.facets :as facets]
            [catalog.images :as catalog-images]
            [checkout.suggestions :as suggestions]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.products :as products]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as storefront.footer]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn display-adjustable-line-items
  [recently-added-skus line-items skus update-line-item-requests delete-line-item-requests]
  (for [{sku-id :sku variant-id :id :as line-item} line-items
        :let [sku                  (get skus sku-id)
              price                (or (:sku/price line-item) (:unit-price line-item))
              removing?            (get delete-line-item-requests variant-id)
              updating?            (get update-line-item-requests sku-id)
              just-added-to-order? (contains? recently-added-skus sku-id)
              length-circle-value  (-> sku :hair/length first)]]
    [:div.pt1.pb2 {:key (str sku-id "-" (:quantity line-item))}
     [:div.left.pr1
      (when-not length-circle-value
        {:class "pr3"})
      (when length-circle-value
        (css-transitions/transition-background-color
         just-added-to-order?
         [:div.right.z1.circle.stacking-context.border.border-light-gray.flex.items-center.justify-center.medium.h5.bg-too-light-teal
          {:key       (str "length-circle-" sku-id)
           :data-test (str "line-item-length-" sku-id)
           :style     {:margin-left "-21px"
                       :margin-top  "-10px"
                       :width       "32px"
                       :height      "32px"}} (str length-circle-value "”")]))

      (css-transitions/transition-background-color
       just-added-to-order?
       [:div.flex.items-center.justify-center.ml1
        {:key       (str "thumbnail-" sku-id)
         :data-test (str "line-item-img-" (:catalog/sku-id sku))
         :style     {:width "79px" :height "74px"}}
        (ui/ucare-img
         {:width 75}
         (->> sku (catalog-images/image "cart") :ucare/id))])]

     [:div {:style {:margin-top "-14px"}}
      [:a.medium.titleize.h5
       {:data-test (str "line-item-title-" sku-id)}
       (or (:product-title line-item)
           (:product-name line-item))]
      [:div.h6
       [:div.flex.justify-between.mt1
        [:div
         {:data-test (str "line-item-color-" sku-id)}
         (:color-name line-item)]
        [:div.flex.items-center.justify-between
         (if removing?
           [:div.h3 {:style {:width "1.2em"}} ui/spinner]
           [:a.gray.medium
            (merge {:data-test (str "line-item-remove-" sku-id)}
                   (utils/fake-href events/control-cart-remove (:id line-item)))
            (svg/trash-can {:height "1.1em"
                            :width  "1.1em"
                            :class  "stroke-dark-gray"})])]]
       [:div.flex.justify-between.mt1
        [:div.h3
         {:data-test (str "line-item-quantity-" sku-id)}
         (ui/auto-complete-counter {:spinning? updating?
                                    :data-test sku-id}
                                   (:quantity line-item)
                                   (utils/send-event-callback events/control-cart-line-item-dec
                                                              {:variant line-item})
                                   (utils/send-event-callback events/control-cart-line-item-inc
                                                              {:variant line-item}))]
        [:div.h5 {:data-test (str "line-item-price-ea-" sku-id)} (mf/as-money-without-cents price) " ea"]]]]]))

(defn ^:private freeinstall-line-item
  [freeinstall-just-added? {:keys [id title detail price thumbnail-image-fn]}]
  [:div.pt1.pb2.clearfix
   [:div.left.ml1.pr3.mtp4
    (css-transitions/transition-background-color
     freeinstall-just-added?
     [:div.flex.justify-center.items-center
      {:key "freeinstall-line-item"
       :style {:height "79px"
               :width  "79px"}}
      (thumbnail-image-fn 75)])]
   [:div
    [:div.medium.titleize.h5
     {:data-test (str "line-item-title-" id)}
     title]
    [:div.h6.flex.items-center.justify-between
     [:div.flex.justify-between.mt1
      [:div {:data-test (str "line-item-detail-" id)}
       detail]]
     [:div.h5.right {:data-test (str "line-item-price-ea-" id)} (some-> price mf/as-money)]]]])

(def qualified-banner
  [:div.flex.items-center.bold
   {:data-test "adventure-qualified-banner"
    :style {:height              "246px"
            :padding-top         "43px"
            :background-size     "cover"
            :background-position "center"
            :background-image    "url('//ucarecdn.com/97d80a16-1f48-467a-b8e2-fb16b532b75e/-/format/auto/-/quality/normal/aladdinMatchingCelebratoryOverlayImagePurpleR203Lm3x.png')"}}
   [:div.col.col-12.center.white
    [:div.h5.light "This order qualifies for a"]
    [:div.h1.shout "free install"]
    [:div.h5.light "from a Mayvenn Stylist near you"]]])

(defn add-more-hair-button
  [navigation-event]
  (ui/teal-button
   (merge {:data-test "adventure-add-more-hair"}
          (utils/route-to navigation-event))
   "Add more hair"))

(defn add-more-hair-banner [how-shop-choice number-of-items-needed navigation-event]
  [:div.bg-too-light-teal.py4.px2.my2 {:data-test "adventure-add-more-hair-banner"}
   [:div.h5.medium.center.px2
    "Add " [:span.pyp1.px1.bold.white.bg-purple.center
            number-of-items-needed]
    " more " (ui/pluralize number-of-items-needed "item")
    " to get a free install from a Mayvenn Certified Stylist"]

   [:div.mt2 (add-more-hair-button navigation-event)]])

(defn full-component [{:keys [skus
                              order
                              updating?
                              redirecting-to-paypal?
                              suggestions
                              line-items
                              update-line-item-requests
                              recently-added-skus
                              delete-line-item-requests
                              freeinstall-line-item-data
                              freeinstall-just-added?
                              servicing-stylist
                              how-shop-choice
                              add-more-hair-navigation-event
                              loaded-quadpay?
                              cart-summary]} owner _]
  (component/create
   (let [{:keys [number-of-items-needed add-more-hair?]} freeinstall-line-item-data]
     [:div.container
      (if add-more-hair?
        (add-more-hair-banner how-shop-choice number-of-items-needed add-more-hair-navigation-event)
        qualified-banner)
      [:div.p2
       [:div.clearfix.mxn3
        [:div.px3.pt2
         {:data-test "cart-line-items"}
         (display-adjustable-line-items recently-added-skus
                                        line-items
                                        skus
                                        update-line-item-requests
                                        delete-line-item-requests)
         [:div.px2
          (component/build suggestions/component suggestions nil)]

         (freeinstall-line-item freeinstall-just-added? freeinstall-line-item-data)]

        [:div.px3
         (component/build adventure-cart-summary/component cart-summary nil)

         #?@(:cljs
             [(component/build quadpay/component
                               {:show?       loaded-quadpay?
                                :order-total (:total order)
                                :directive   [:div.flex.items-center.justify-center
                                              "Just select"
                                              [:div.mx1 {:style {:width "70px" :height "14px"}}
                                               svg/quadpay-logo]
                                              "at check out."]}
                               nil)])

         (if add-more-hair?
           (add-more-hair-button add-more-hair-navigation-event)
           [:div.bg-too-light-teal.py4.px2
            [:div.h5.medium.center
             (if-let [servicing-stylist-name (stylists/->display-name servicing-stylist)]
               (str "You’ll be connected with " servicing-stylist-name " after checkout.")
               "You’ll be able to select your Certified Mayvenn Stylist after checkout.")]
            [:div.mt2
             (ui/teal-button {:spinning? false
                              :disabled? updating?
                              :on-click  (utils/send-event-callback events/control-checkout-cart-submit)
                              :data-test "start-checkout-button"}
                             [:div "Check out"])]

            [:div.h5.black.center.py1.flex.justify-around.items-center
             [:div.flex-grow-1.border-bottom.border-light-gray]
             [:div.mx2 "or"]
             [:div.flex-grow-1.border-bottom.border-light-gray]]

            [:div.pb2
             (ui/aqua-button {:on-click  (utils/send-event-callback events/control-checkout-cart-paypal-setup)
                              :spinning? redirecting-to-paypal?
                              :disabled? updating?
                              :data-test "paypal-checkout"}
                             [:div
                              "Check out with "
                              [:span.medium.italic "PayPal™"]])]])]]]])))

(defn ^:private variants-requests [data request-key variant-ids]
  (->> variant-ids
       (map (juxt identity
                  #(utils/requesting? data (conj request-key %))))
       (into {})))

(defn ^:private update-pending? [data]
  (let [request-key-prefix (comp vector first :request-key)]
    (some #(apply utils/requesting? data %)
          [request-keys/add-promotion-code
           [request-key-prefix request-keys/update-line-item]
           [request-key-prefix request-keys/delete-line-item]])))

(defn add-product-title-and-color-to-line-item [products facets line-item]
  (merge line-item {:product-title (->> line-item
                                        :sku
                                        (products/find-product-by-sku-id products)
                                        :copy/title)
                    :color-name    (-> line-item
                                       :variant-attrs
                                       :color
                                       (facets/get-color facets)
                                       :option/name)}))

(defn ^:private full-cart-query [data]
  (let [order           (get-in data keypaths/order)
        products        (get-in data keypaths/v2-products)
        facets          (get-in data keypaths/v2-facets)
        line-items      (map (partial add-product-title-and-color-to-line-item products facets)
                             (orders/product-items order))
        variant-ids     (map :id line-items)
        how-shop-choice (get-in data adventure.keypaths/adventure-choices-how-shop)]
    {:suggestions                    (suggestions/query data)
     :order                          order
     :servicing-stylist              (get-in data adventure.keypaths/adventure-servicing-stylist)
     :line-items                     line-items
     :skus                           (get-in data keypaths/v2-skus)
     :updating?                      (update-pending? data)
     :redirecting-to-paypal?         (get-in data keypaths/cart-paypal-redirect)
     :how-shop-choice                how-shop-choice
     :add-more-hair-navigation-event (if (= "individual-bundles" how-shop-choice)
                                       events/navigate-adventure-a-la-carte-product-list
                                       events/navigate-adventure-how-shop-hair)
     :update-line-item-requests      (merge-with
                                      #(or %1 %2)
                                      (variants-requests data request-keys/add-to-bag (map :sku line-items))
                                      (variants-requests data request-keys/update-line-item (map :sku line-items)))
     :cart-summary                   (adventure-cart-summary/query data)
     :delete-line-item-requests      (variants-requests data request-keys/delete-line-item variant-ids)
     :recently-added-skus            (get-in data keypaths/cart-recently-added-skus)
     :freeinstall-just-added?        (get-in data keypaths/cart-freeinstall-just-added?)
     :freeinstall-line-item-data     (adventure-cart-items/freeinstall-line-item-query data)
     :loaded-quadpay?                (get-in data keypaths/loaded-quadpay)}))

(defn component
  [{:keys [fetching-order? full-cart]} owner opts]
  (component/create
   (if fetching-order?
     [:div.py3.h2 ui/spinner]
     (component/build full-component full-cart opts))))

(defn query [data]
  {:fetching-order? (utils/requesting? data request-keys/get-order)
   :full-cart       (full-cart-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn header [{:keys [return-route item-count]} owner opts]
  (component/create
   [:div.center.bg-light-lavender.white.flex.flex-wrap.items-center
    {:style {:height "75px"}}
    [:a.block.p3.col-3
     (merge {:data-test "adventure-back"} return-route)
     [:div.flex.items-center.justify-center
      {:style {:height "24px" :width "20px"}}
      (ui/back-arrow {:width "14"})]]
    [:div.col-6
     [:div.h5.medium "Your Bag"]
     [:div.h6 (ui/pluralize-with-amount item-count "item")]]
    [:div.col-3]]))

(defn header-query [data]
  {:return-route (utils/route-back-or-to (first (get-in data keypaths/navigation-undo-stack)) events/navigate-adventure-home)
   :item-count   (orders/product-quantity (get-in data keypaths/order))} )

(defn layout [data nav-event]
  [:div.flex.flex-column.max-580.mx-auto
   {:style {:min-height    "100vh"
            :margin-bottom "-1px"}}
   (component/build header (header-query data) nil)
   [:div.relative.flex.flex-column.flex-auto
    [:div.bg-light-lavender
     (flash/built-component data nil)]

    [:main.bg-white.flex-auto
     {:data-test (keypaths/->component-str nav-event)}
     (built-component data nil)]

    [:footer
     (storefront.footer/built-component data nil)]
    ui/adventure-chat-icon]])
