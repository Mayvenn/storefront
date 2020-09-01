(ns checkout.ui.cart-item-v202004
  (:require [checkout.suggestions :as suggestions]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            ui.molecules
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]))

(defn cart-item-floating-box-molecule
  [{:cart-item-floating-box/keys [id value]}]
  (when id
    [:div.right.right-align.proxima.content-2
     {:key       id
      :data-test id
      :style     {:height "100%"}}
     value]))

(defn cart-item-copy-molecule
  [{:cart-item-copy/keys [lines]}]
  (when (seq lines)
    [:div
     (for [{:keys [id value]} lines]
       [:div.content-3.proxima {:key       id
                                :data-test id} value])]))

(defn cart-item-title-molecule
  [{:cart-item-title/keys [id primary secondary]}]
  (when (and id primary)
    [:div
     [:div.proxima.content-2
      {:data-test id}
      primary]
     [:div.dark-gray secondary]]))

(defn cart-item-square-thumbnail-molecule
  [{:cart-item-square-thumbnail/keys
    [id ucare-id sku-id sticker-label]}]
  (when id
    (let [sticker-id (str "line-item-length-" sku-id)]
      [:div.relative.pt1
       {:style {:height "45px"
                :width  "48px"}}
       (when sticker-label
         [:div.absolute.z1.circle.border.border-gray.bg-white.proxima.title-3.flex.items-center.justify-center
          {:key       sticker-id
           :data-test sticker-id
           :style     {:height "26px"
                       :width  "26px"
                       :right  -10
                       :top    -5}}
          sticker-label])
       [:div.flex.items-center.justify-center
        {:style     {:height "45px"
                     :width  "48px"}
         :key       (str "cart-item-square-thumbnail-" sku-id)
         :data-test (str "line-item-img-" sku-id)}
        (ui/ucare-img {:width 48
                       :class "block border border-cool-gray"}
                      ucare-id)]])))

(defn new-cart-item-service-thumbnail-molecule
  [{:cart-item-service-thumbnail/keys [id image-url]}]
  (when id
    [:div.flex.justify-center
     {:style {:border-radius "50%"
              :width         "60px"
              :height        "60px"}}

     [:div.relative
      (ui/ucare-img {:width "56px"
                     :class "mtp2"} image-url)]]))

(defn cart-item-remove-action-molecule
  [{:cart-item-remove-action/keys [id target spinning?]}]
  (when target
    (if spinning?
      [:div.h3
       {:style {:width "1.2em"}}
       ui/spinner]
      [:div
       [:a.gray.medium.m1
        (merge {:data-test id}
               (apply utils/fake-href target))
        (svg/consolidated-trash-can {:width  "14px"
                                     :height "16px"})]])))

(defn new-cart-item-remove-action-molecule
  [{:cart-item-remove-action/keys [id target spinning?]}]
  (when target
    (if spinning?
      [:div.h3
       {:style {:width "1.2em"}}
       ui/spinner]
      [:div
       [:a.gray.medium
        (merge {:data-test id}
               (apply utils/fake-href target))
        (svg/line-item-delete {:width  "16px"
                               :height "17px"})]])))

(defn cart-item-swap-action-molecule
  [{:cart-item-swap-action/keys [target]}]
  (when target
    [:div
     [:a.gray.medium.m1
      (merge {:data-test "stylist-swap"}
             (apply utils/route-to target))
      (svg/swap-person {:width  "16px"
                        :height "19px"})]]))

(defn cart-item-adjustable-quantity-molecule
  [{:cart-item-adjustable-quantity/keys
    [id spinning? value id-suffix decrement-target increment-target]}]
  (when id
    [:div {:data-test id}
     (ui/consolidated-cart-auto-complete-counter
      {:spinning? spinning?
       :data-test id-suffix}
      value
      (apply utils/send-event-callback decrement-target)
      (apply utils/send-event-callback increment-target))]))

(component/defdynamic-component cart-item-modify-button
  (did-mount
   [this]
   (let [{:cart-item-modify-button/keys [id tracking-target]}
         (component/get-props this)]
     (when (and id tracking-target)
       (apply messages/handle-message tracking-target))))

  (render
   [this]
   (component/html
    (let [{:cart-item-modify-button/keys [id target content]}
          (component/get-props this)]
      (when id
        [:div.flex
         (ui/button-small-secondary (merge {:class     "p-color bold mt1"
                                            :data-test id}
                                           (apply utils/route-to target))
                                    content)])))))



(defn cart-item-sub-items-molecule
  [{:cart-item-sub-items/keys [id title items]}]
  (when id
    [:div {:key id}
     [:div.shout.proxima.title-3 title]
     (mapv (fn [{:cart-item-sub-item/keys [sku-id title price]}]
             [:div.flex.justify-between
              {:key (str id "-" sku-id)}
              [:div.content-3.flex.items-center
               [:div.flex.justify-center.items-center.mr1.bg-s-color
                {:style {:height        11
                         :width         11
                         :border-radius "50%"}}
                (svg/check-mark {:class "fill-white"
                                 :style {:width  "7px"
                                         :height "7px"}})]
               title]
              [:div {:data-test (str "line-item-price-ea-" sku-id)} price]]) items)]))


(component/defcomponent stylist-organism
  [{:servicing-stylist-banner/keys [id title image-url rating action-id target]} _ _]
  (when id
    [:div.flex.bg-white.pl3 {:data-test id}
     [:div.py2
      {:style {:min-width "70px"}}
      (ui/circle-picture {:width 56} (ui/square-image {:resizable-url image-url} 50))]
     [:div.flex-auto
      [:div.flex.flex-auto.pr2.py2
       [:div.flex.flex-grow-1.items-center
        [:div
         [:div.content-2.proxima.flex.justify-between title]
         [:div.content-3.proxima "Your Certified Mayvenn Stylist"]
         [:div.mt1 (ui.molecules/stars-rating-molecule rating)]]]
       (when action-id
         [:a.block.gray.medium
          (merge {:data-test action-id
                  :href (routes/path-for events/navigate-adventure-find-your-stylist)
                  :on-click (apply utils/send-event-callback target)})
          (svg/swap-arrows {:width "16px"
                            :height "20px"
                            :class "fill-dark-gray stroke-dark-gray"})])]
      [:div.mt1.border-bottom.border-cool-gray.hide-on-mb]]]))

(component/defcomponent no-stylist-organism
  [{:stylist-organism/keys [id]} _ _]
  (when id
    [:div.bg-white
     [:div.pt3.pb4.px3.canela.title-2.dark-gray.items-center.flex.flex-column
      [:div.mb1 "No Stylist Selected"]
      [:div (ui/button-small-primary
             (merge {:data-test "pick-a-stylist"}
                    (utils/fake-href events/control-pick-stylist-button))
             "Pick Your Stylist")]]
     [:div.mb1.border-bottom.border-cool-gray.hide-on-mb]]))

(component/defcomponent no-items
  [_ _ _]
  [:div.bg-white.dark-gray.title-2.canela.center.py3
   "No Items in Your Cart"])

(component/defcomponent organism
  [{:keys [cart-item suggestions]} _ {:keys [id]}]
  [:div.p2.flex.bg-white.items-center
   {:key id :data-test id}
   ;; image group
   [:div.relative.self-start
    {:style {:min-width "70px"}}
    (cart-item-square-thumbnail-molecule cart-item)
    (new-cart-item-service-thumbnail-molecule cart-item)]

   ;; info group
   [:div.flex-grow-1
    [:div.flex.items-center
     [:div.flex-grow-1
      (cart-item-title-molecule cart-item)

      [:div
       (cart-item-copy-molecule cart-item)
       (ui.molecules/stars-rating-molecule cart-item)
       (cart-item-adjustable-quantity-molecule cart-item)]]

     ;; price group
     [:div.right-align.flex.flex-column.self-stretch
      {:style {:min-width "67px"}}
      (new-cart-item-remove-action-molecule cart-item)
      (cart-item-floating-box-molecule cart-item)]]

    (cart-item-sub-items-molecule cart-item)
    (component/build cart-item-modify-button cart-item nil)

    (component/build suggestions/consolidated-component suggestions nil)]])
