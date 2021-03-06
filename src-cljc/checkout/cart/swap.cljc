(ns checkout.cart.swap
  "Carts can only contain one stylist and discountable base service.

  The model 'cart-swap', when given stylist and service intended contains:
  - whether a swap is necessary
  - originals and intended for both stylist and discountable service
  - because of product debt, associated addons for the intended service base

  A popup can be invoked that uses the model to prompt the user and
  follows up with confirm or dismiss events "
  (:require #?@(:cljs [[storefront.components.popup :as popup]
                       [storefront.hooks.stringer :as stringer]
                       storefront.frontend-trackings])
            [api.catalog :refer [select ?discountable]]
            api.current
            api.orders
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as t]
            [storefront.trackings :as trackings]))

(def k-cart-swap [:models :cart-swap])

(defn cart-swap<-
  [state {intended-service :service/intended
          intended-addons  :addons/intended
          intended-stylist :stylist/intended}]
  (let [original-service (when intended-service
                           (->> (api.orders/current state)
                                :order/items
                                (select ?discountable)
                                first))
        original-stylist (when intended-stylist
                           (api.current/stylist state))]
    (cond-> {:service/swap? false
             :stylist/swap? false}
      intended-service
      (merge {:service/intended intended-service})
      (seq intended-addons)
      (merge {:addons/intended (seq intended-addons)})

      ;; Swapping is for discountable services, only 1 allowed
      (and intended-service original-service)
      (merge {:service/original original-service ; already selected for discountable
              :service/swap?    (and (select ?discountable
                                             [intended-service])
                                     (not= (:catalog/sku-id original-service)
                                           (:catalog/sku-id intended-service)))})

      intended-stylist
      (merge {:stylist/intended intended-stylist})

      ;; Swapping for stylists when a new one is intended
      (and intended-stylist original-stylist)
      (merge {:stylist/intended intended-stylist
              :stylist/original original-stylist
              :stylist/swap?    (not= (:stylist/id original-stylist)
                                      (:stylist/id intended-stylist))}))))

;; --------------- behavior

(defmethod t/transition-state e/cart-swap-popup-show
  [_ _ cart-swap state]
  (-> state
      (assoc-in k-cart-swap cart-swap)
      (assoc-in storefront.keypaths/popup :cart-swap)))

(defmethod trackings/perform-track e/cart-swap-popup-show
  [_ _ cart-swap app-state]
  (let [order          (get-in app-state storefront.keypaths/order)
        images-catalog (get-in app-state storefront.keypaths/v2-images)
        skus-db        (get-in app-state storefront.keypaths/v2-skus)]
    #?(:cljs
       (stringer/track-event "swap_modal_deployed"
                             (cond->
                                 {:cart_items                   (storefront.frontend-trackings/cart-items-model<- order
                                                                                                                  images-catalog
                                                                                                                  skus-db)
                                  :order_number                 (:number order)
                                  :current_servicing_stylist_id (some->> cart-swap
                                                                         :stylist/original
                                                                         :stylist/id)
                                  :current_service_variant_id   (some->> cart-swap
                                                                         :service/original
                                                                         :legacy/variant-id)}
                               (:stylist/swap? cart-swap)
                               (merge
                                {:switch_to_servicing_stylist_id (some->> cart-swap
                                                                          :stylist/intended
                                                                          :stylist/id)})
                               (:service/swap? cart-swap)
                               (merge
                                {:switch_to_service_variant_id (some->> cart-swap
                                                                        :service/intended
                                                                        :legacy/variant-id)}))))))

(defmethod fx/perform-effects e/control-cart-swap-popup-confirm
  [_ _ _ _ state]
  ;; NOTE:
  ;; quantity is always 1 because if it could be more, it wouldn't be a swap
  (let [cart-swap (get-in state k-cart-swap)]
    (if (:stylist/intended cart-swap)
      (messages/handle-message e/add-servicing-stylist-and-sku
                               {:sku               (:service/intended cart-swap)
                                :servicing-stylist (:stylist/intended cart-swap)
                                :quantity          1})
      (if-let [addons (:addons/intended cart-swap)]
        (messages/handle-message e/bulk-add-sku-to-bag
                                 {:items         (->>
                                                  (:service/intended cart-swap)
                                                  (conj addons)
                                                  (into [] (map (fn [x] {:sku x :quantity 1}))))
                                  :service-swap? true})
        (messages/handle-message e/add-sku-to-bag
                                 {:sku           (:service/intended cart-swap)
                                  :stay-on-page? (= e/navigate-category
                                                    (get-in state storefront.keypaths/navigation-event))
                                  :service-swap? true
                                  :quantity      1}))))
  (messages/handle-message e/popup-hide
                           {:clear/keypath k-cart-swap}))

(defmethod fx/perform-effects e/control-cart-swap-popup-dismiss
  [_ _ _ _ _]
  (messages/handle-message e/popup-hide
                           {:clear/keypath k-cart-swap}))

(defmethod trackings/perform-track e/control-cart-swap-popup-dismiss
  [_ _ _ _]
  #?(:cljs
     (stringer/track-event "swap_modal_closed" {})))

;; ------------- views

(defn cart-swap-popup-dismiss-molecule
  [{:cart-swap.popup.dismiss/keys [target]}]
  (c/html
   [:div.flex.justify-end.self-stretch
    [:a.p2
     (svg/x-sharp
      (merge (apply utils/fake-href target)
             {:data-test "cart-swap-popup-dismiss"
              :height    "20px"
              :width     "20px"}))]]))

(defn modal-attrs-atom
  [{:cart-swap.popup.dismiss/keys [target]}]
  {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
   :close-attrs (apply utils/fake-href target)
   :bg-class    "bg-darken-4"})

(defn cart-swap-popup-dismiss-button-molecule
  [{:cart-swap.popup.dismiss/keys [target label]}]
  (c/html
   [:div.flex.mb10.items-center.justify-center.col-12
    (ui/button-medium-underline-primary
     (merge (apply utils/fake-href target)
            {:class "col-5 center"})
     label)]))

(defn cart-swap-popup-confirm-button-molecule
  [{:cart-swap.popup.confirm/keys [target label]}]
  (c/html
   [:div.flex.mb5.items-center.justify-center.col-12
    (ui/button-medium-primary
     (merge (apply utils/fake-href target)
            {:class     "col-8"
             :data-test "cart-swap-popup-confirm"})
     label)]))

(defn ^:private cart-swap-popup-title-molecule
  [{:cart-swap.popup.title/keys [primary]}]
  (c/html
   [:div.flex.flex-column.items-center
    [:div.my5
     (svg/swap-arrows
      {:width  "30px"
       :height "36px"
       :class  "stroke-p-color fill-p-color"})]
    [:div.title-2.canela
     [:div primary]]]))

(c/defcomponent ^:private cart-swap-popup-body-notice-molecule
  [{:cart-swap.popup.body.notice/keys [primary secondary]} _ {:keys [id]}]
  [:li.py2
   {:key id}
   [:div primary]
   [:div secondary]])

(defn ^:private cart-swap-popup-body-molecule
  [{:cart-swap.popup.body/keys [id] :as data}]
  (c/html
   [:ul.mx1.my4.list-purple-diamond
    {:data-test id}
    (c/elements cart-swap-popup-body-notice-molecule data
                :cart-swap.popup.body/notices)]))

(defn cart-swap-popup-organism
  [data]
  (ui/modal
   (modal-attrs-atom data)
   [:div.bg-white.flex.flex-column.items-center.p2.mx2
    (cart-swap-popup-dismiss-molecule data)
    [:div.col-10
     (cart-swap-popup-title-molecule data)
     (cart-swap-popup-body-molecule data)]
    (cart-swap-popup-confirm-button-molecule data)
    (cart-swap-popup-dismiss-button-molecule data)]))

(defn ^:private swap-copy
  [original intended]
  (str "You are about to swap " original " with " intended "."))

(defn cart-swap-popup<-
  "We display a swap modal to inform the customer that the following
  conditions have occurred:

  - They can only have one discountable service (free mayvenn)
  - They are implicitly swapping out their chosen stylist"
  [state]
  (let [cart-swap (get-in state k-cart-swap)]
    {:cart-swap.popup.confirm/target [e/control-cart-swap-popup-confirm]
     :cart-swap.popup.confirm/label  "Confirm Swap"
     :cart-swap.popup.dismiss/target [e/control-cart-swap-popup-dismiss]
     :cart-swap.popup.dismiss/label  "Cancel"
     :cart-swap.popup.title/primary  "Before we move on..."
     :cart-swap.popup.body/id        "cart-swap-explanation"
     :cart-swap.popup.body/notices
     (cond-> []
       (:service/swap? cart-swap)
       (conj
        {:cart-swap.popup.body.notice/primary   "1 Free Mayvenn Service per order."
         :cart-swap.popup.body.notice/secondary (swap-copy (-> cart-swap
                                                               :service/original
                                                               :sku/title)
                                                           (-> cart-swap
                                                               :service/intended
                                                               :sku/title)) })
       (:stylist/swap? cart-swap)
       (conj
        {:cart-swap.popup.body.notice/primary   "1 Stylist per order."
         :cart-swap.popup.body.notice/secondary (swap-copy (-> cart-swap
                                                               :stylist/original
                                                               :stylist/name)
                                                           (-> cart-swap
                                                               :stylist/intended
                                                               :stylist/name))}))}))

#?(:cljs
   [(defmethod popup/query :cart-swap [state]
      (cart-swap-popup<- state))
    (defmethod popup/component :cart-swap [state _ _]
      (cart-swap-popup-organism state))])
