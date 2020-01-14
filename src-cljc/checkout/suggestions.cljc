(ns checkout.suggestions
  (:require #?(:cljs [storefront.api :as api])
            [spice.selector :as selector]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.accessors.images :as images]
            [storefront.accessors.orders :as orders]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.request-keys :as request-keys]
            [storefront.css-transitions :as css-transitions]
            [catalog.images :as catalog-images]))

(defn suggest-bundles
  [data products skus items]
  (when (= 1 (orders/line-item-quantity items))
    (let [{:keys [variant-attrs] sku-id :sku} (first items)]
      (when (= "bundles" (variant-attrs :hair/family))
        (let [sku               (get skus sku-id)
              image             (images/cart-image sku)
              adjacent-skus     (->> sku
                                     :selector/from-products
                                     first
                                     (get products)
                                     :selector/sku-ids
                                     (map (partial get skus))
                                     (selector/match-all {} {:hair/color (:hair/color sku)})
                                     (sort-by (comp first :hair/length))
                                     (partition-by #(= (:catalog/sku-id sku) (:catalog/sku-id %))))
              shorter-skus      (first adjacent-skus)
              longer-skus       (last adjacent-skus)
              short-suggestions (if (< (count shorter-skus) 2)
                                  (repeat 2 sku)
                                  (take-last 2 shorter-skus))
              long-suggestions  (if (< (count longer-skus) 2)
                                  (repeat 2 sku)
                                  (take 2 longer-skus))]
          (->> {:shorter-lengths short-suggestions
                :longer-lengths  long-suggestions}
               (filterv (fn in-stock? [[_ skus]]
                          (every? :inventory/in-stock? skus)))
               (mapv (fn transform [[position skus]]
                       {:position               position
                        :image                  image
                        :skus                   skus
                        :initial-sku             sku
                        :any-adding-to-bag?     (utils/requesting? data (fn [req]
                                                                          (subvec (:request-key req []) 0 1))
                                                                   request-keys/add-to-bag)
                        :this-is-adding-to-bag? (utils/requesting? data
                                                                   (conj request-keys/add-to-bag
                                                                         (set (map :catalog/sku-id skus))))}))))))))

(defn suggested-bundles
  [{:keys [image position skus initial-sku this-is-adding-to-bag? any-adding-to-bag?]}]
  (let [[short-sku long-sku] skus
        sized-image          (update image :style merge {:height "36px" :width "40px"})]
    [:div.mx2.my4.col-11
     {:data-test (str "suggestion-" (name position))
      :key       (str "suggestion-" (map :catalog/sku-id skus) "-" (name position))}
     [:div.absolute (svg/discount-tag {:style {:height      "3em"
                                               :width       "3em"
                                               :margin-left "-23px"
                                               :margin-top  "-20px"}})]
     [:div.border.border-cool-gray.bg-cool-gray
      {:style {:height "68px"}}
      [:div.bg-white.h5.medium.center
       (first (:hair/length short-sku))
       "” & "
       (first (:hair/length long-sku))
       "”"]
      [:div.flex.justify-center
       [:img.m1 sized-image]
       [:img.m1 sized-image]]
      [:div.col-10.mx-auto
       (ui/button-small-primary {;; we don't want to draw attention to the disabling of the other 'Add' button,
                         ;; but we do want to prevent people from clicking both.
                         ;; :disabled? (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                                 :on-click           (if (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                                                       utils/noop-callback
                                                       (utils/send-event-callback events/control-suggested-add-to-bag
                                                                                  {:skus        skus
                                                                                   :initial-sku initial-sku}))
                                 :spinning?          this-is-adding-to-bag?
                                 :data-test          (str "add-" (name position))
                                 :data-test-disabled (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                                 :style              {:margin-top "-10px"}}
                                "Add")]]]))

(defcomponent component
  [{:keys [suggestions]} _ _]
  (when (seq suggestions)
    [:div.mb4.px1.col-12.mx-auto.bg-cool-gray
     {:style     {:height "135px"}
      :data-test "auto-complete"}
     [:div.flex.justify-center (map suggested-bundles suggestions)]]))

(defn query
  [data]
  (let [skus       (get-in data keypaths/v2-skus)
        products   (get-in data keypaths/v2-products)
        line-items (orders/product-items (get-in data keypaths/order))]
    {:suggestions (suggest-bundles data products skus line-items)}))

(defn image-with-sticker
  [{:cart-icon/keys [ucare-id sku-id sticker-label sticker-id sticker-size highlighted? image-width top-margin left-margin]}]
  #?(:clj
     [:div]
     :cljs
     [:div.pr1.pt1
      (when-not sticker-label
        {:class "pr3"})
      (when sticker-label
        ;; TODO: highlighting does not work as before
        [:div.right.z1.circle.stacking-context.border.border-gray.flex.items-center.justify-center.bg-white.proxima.title-3
         {:key       sticker-id
          :data-test sticker-id
          :style     {:margin-left left-margin
                      :margin-top  top-margin
                      :width       sticker-size
                      :height      sticker-size}}
         sticker-label])

      [:div.flex.items-center.justify-center.p1
       (css-transitions/background-fade
        highlighted?
        {:key       (str "thumbnail-" sku-id)
         :data-test (str "line-item-img-" sku-id)})
       (ui/ucare-img {:width image-width
                      :class "border border-cool-gray"} ucare-id)]]))

(defn ->consolidated-suggested-bundles
  "TODO Heat needs to be updated to use new dt"
  [{:suggested-bundles/keys [id images-with-stickers label target cta-id disabled? spinning?]}]
  [:div.bg-cool-gray.mt1
   {:data-test id :key id}
   [:div.flex.justify-between.items-center.px2
    (let [[first-image second-image] images-with-stickers]
      [:div.flex.items-center
       (image-with-sticker first-image)
       [:span.pr2.proxima.title-2 "+"]
       (image-with-sticker second-image)])
    [:div.my1.col-5
     (ui/button-small-secondary {;; we don't want to draw attention to the disabling of the other 'Add' button,
                                 ;; but we do want to prevent people from clicking both.
                                 ;; :disabled? (and (not this-is-adding-to-bag?) any-adding-to-bag?)
                                 :on-click     (if disabled?
                                                 utils/noop-callback
                                                 (apply utils/send-event-callback target))
                                 :spinning?    spinning?
                                 :data-test    cta-id}
                                label)]]])

(defcomponent consolidated-component
  [{:keys [suggestions]} _ _]
  (when (seq suggestions)
    [:div {:data-test "auto-complete"}
     [:div.flex.items-center
      ^:inline (svg/angle-arrow {:style {:width  "13px"
                                         :height "13px"}})
      [:div.proxima.content-2.pl1 "Bundles often bought together"]]
     (map ->consolidated-suggested-bundles suggestions)]))

(defn consolidated-query
  [data]
  (let [skus       (get-in data keypaths/v2-skus)
        products   (get-in data keypaths/v2-products)
        line-items (orders/product-items (get-in data keypaths/order))]
    {:suggestions
     (map (fn [{:keys [position skus initial-sku this-is-adding-to-bag? any-adding-to-bag?]}]
            (let [[short-sku long-sku] skus]
              {:suggested-bundles/id                   (str "suggestion-" (map :catalog/sku-id skus) "-" (name position))
               :suggested-bundles/disabled?            (and (not this-is-adding-to-bag?) any-adding-to-bag?)
               :suggested-bundles/spinning?            this-is-adding-to-bag?
               :suggested-bundles/label                "add to cart"
               :suggested-bundles/target               [events/control-suggested-add-to-bag
                                                        {:skus        skus
                                                         :initial-sku initial-sku}]
               :suggested-bundles/cta-id               (str "add-" (name position))
               :suggested-bundles/images-with-stickers [{:cart-icon/ucare-id      (->> short-sku (catalog-images/image "cart") :ucare/id)
                                                         :cart-icon/sku-id        (:catalog/sku-id short-sku)
                                                         :cart-icon/sticker-label (when-let [length-circle-value (-> short-sku :hair/length first)]
                                                                                    (str length-circle-value "”"))
                                                         :cart-icon/sticker-id    (str "line-item-length-" (:catalog/sku-id short-sku))
                                                         :cart-icon/sticker-size  "28px"
                                                         :cart-icon/image-width   39
                                                         :cart-icon/top-margin    "-8px"
                                                         :cart-icon/left-margin   "-17px"}
                                                        {:cart-icon/ucare-id      (->> long-sku (catalog-images/image "cart") :ucare/id)
                                                         :cart-icon/sku-id        (:catalog/sku-id long-sku)
                                                         :cart-icon/sticker-label (when-let [length-circle-value (-> long-sku :hair/length first)]
                                                                                    (str length-circle-value "”"))
                                                         :cart-icon/sticker-id    (str "line-item-length-" (:catalog/sku-id long-sku))
                                                         :cart-icon/sticker-size  "28px"
                                                         :cart-icon/image-width   39
                                                         :cart-icon/top-margin    "-8px"
                                                         :cart-icon/left-margin   "-17px"}]}))
          (suggest-bundles data products skus line-items))}))

(defmethod effects/perform-effects events/control-suggested-add-to-bag
  [_ _ {:keys [skus initial-sku]} _ app-state]
  #?(:cljs
     (api/add-skus-to-bag (get-in app-state keypaths/session-id)
                          {:number           (get-in app-state keypaths/order-number)
                           :token            (get-in app-state keypaths/order-token)
                           :sku-id->quantity (into {}
                                                   (map (fn [[sku-id skus]] [sku-id (count skus)])
                                                        (group-by :catalog/sku-id skus)))}
                          #(messages/handle-message events/api-success-suggested-add-to-bag
                                                    (assoc % :initial-sku initial-sku)))))

(defmethod effects/perform-effects events/api-success-suggested-add-to-bag
  [_ _ {:keys [order]} _ _]
  (messages/handle-message events/save-order
                           {:order order}))
