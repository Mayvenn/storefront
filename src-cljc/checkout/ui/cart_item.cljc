(ns checkout.ui.cart-item
  (:require [checkout.suggestions :as suggestions]
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.platform.component-utils :as utils]
            ui.molecules))

(defn cart-item-floating-box-molecule
  [{:cart-item-floating-box/keys [id value]}]
  (when id
    [:div.right.right-align
     {:key   id
      :style {:height "100%"}}
     value]))

(defn cart-item-copy-molecule
  [{:cart-item-copy/keys [id value]}]
  (when value
    [:div.h7.dark-gray {:data-test id} value]))

(defn cart-item-title-molecule
  [{:cart-item-title/keys [id primary secondary]}]
  (when (and id primary)
    [:div
     [:div
      [:a.medium.titleize.h5
       {:data-test id}
       primary]]
     [:div
      [:a.h6
       secondary]]]))

(defn completed-progress-circle-atom
  [i _]
  [:div.bg-purple.flex.justify-center.items-center.white.bold.h5
   {:key (str "complete-" i)
    :style {:height 21
            :width  21
            :border-radius "50%"}}
   "✓"])

(defn incomplete-progress-circle-atom
  [i content]
  [:div.bg-light-gray.flex.justify-center.items-center.dark-silver.bold.h6
   {:key (str "incomplete-" i)
    :style {:height 21
            :width  21
            :border-radius "50%"}}
   content])

(def steps-hyphen-seperator-atom
  [:div.border-top.border-light-gray])

(defn cart-item-steps-to-complete-molecule
  [{:cart-item-steps-to-complete/keys
    [steps current-step action-label action-target id]}]
  (when (and steps current-step)
    (let [[completed uncompleted] (split-at current-step steps)]
      [:div.items-center.mr2.mt2.flex.flex-wrap
       [:div.flex.items-center.mr3.mb2 ; margin bottom for 320px screens
        (interpose (conj steps-hyphen-seperator-atom
                         {:class "mx1"
                          :style {:width "11px"}})
                   (concat
                    (map-indexed completed-progress-circle-atom
                                 completed)
                    (map-indexed incomplete-progress-circle-atom
                                 uncompleted)))]
       (when (and action-target action-label)
         [:div.mb2 ; margin bottom for 320px screens
          (ui/teal-button (assoc (apply utils/route-to action-target)
                                 :height-class :small
                                 :width-class  :small
                                 :data-test id)
                          action-label)])])))

(defn cart-item-square-thumbnail-molecule
  [{:cart-item-square-thumbnail/keys
    [id ucare-id sku-id sticker-label highlighted?]}]
  (when id
    (let [sticker-id (str "line-item-length-" sku-id)]
      [:div.relative
       {:style {:height "49px"
                :width  "52px"}}
       (when sticker-label
         (css-transitions/transition-background-color
          highlighted?
          [:div.absolute.z1.circle.border.border-white.medium.h6.bg-too-light-teal
           {:key       sticker-id
            :data-test sticker-id
            :style     {:right -12
                        :top   -12}}
           [:div.flex.items-center.justify-center
            sticker-label]]))
       (css-transitions/transition-background-color
        highlighted?
        [:div.flex.items-center.justify-center.rounded
         {:style     {:height "50px"
                      :width  "54px"}
          :key       (str "cart-item-square-thumbnail-" sku-id)
          :data-test (str "line-item-img-" sku-id)}
         (ui/ucare-img {:width 48
                        :class "block rounded border border-light-gray"}
                       ucare-id)])])))

(defn cart-item-thumbnail-molecule
  [{:cart-item-thumbnail/keys [id highlighted? value locked? image-url]}]
  (when id
    (css-transitions/transition-background-color
     highlighted?
     (let [diameter                  50             ; Stylist portrait / Generic stylist image
           lock-circle-diameter      (- diameter 4) ; Overlay lock scrim
           highlight-circle-diameter (+ diameter 6)] ; highlight
       [:div.flex.justify-center.items-center
        {:style {:border-radius "50%"
                 :width         (str highlight-circle-diameter "px")
                 :height        (str highlight-circle-diameter "px")}}
        value
        [:div.relative
         (when locked?
           [:div.absolute.z1.col-12.flex.items-center.justify-center
            {:style {:height "100%"}}

            [:div {:class "absolute z1 block"
                   :style {:background    "#ffffffdd"
                           :width         (str lock-circle-diameter "px")
                           :height        (str lock-circle-diameter "px")
                           :border-radius "50%"}}]
            [:div.absolute.z2.col-12.flex.items-center.justify-center
             {:style {:height "100%"}}
             (svg/lock {:style {:width  "17px"
                                :height "23px"}})]])
         (ui/circle-picture
          {:width diameter}
          ;; Note: We are not using ucare-id because stylist portraits may have
          ;; ucarecdn crop parameters saved into the url
          (ui/square-image {:resizable-url image-url} diameter))]]))))

(defn cart-item-remove-action-molecule
  [{:cart-item-remove-action/keys [id target spinning?]}]
  (when target
    (if spinning?
      [:div.h3
       {:style {:width "1.2em"}}
       ui/spinner]
      [:div 
       [:a.gray.medium.m1
        (merge {:data-test (str "line-item-remove-" id)}
               (apply utils/fake-href target))
        (svg/consolidated-trash-can {:width  "16px"
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

(defn cart-item-pick-stylist-molecule
  [{:cart-item-pick-stylist/keys [id target content]}]
  (when id
    [:div
     (ui/teal-ghost-button (merge {:height-class :small
                                   :width-class  "col-6"
                                   :class        "teal bold mt1"
                                   :data-test    id}
                                  (apply utils/route-to target))
                           content)]))

(defn organism
  [{:keys [cart-item suggestions]} _ _]
  (component/create
   [:div
    (when-let [react-key (:react/key cart-item)]
      [:div.pt1.pb2.ml2.flex
       {:key react-key}
       ;; image group
       [:div.relative.justify-middle.pt3
        {:style {:min-width "78px"}}
        (cart-item-square-thumbnail-molecule cart-item)
        (cart-item-thumbnail-molecule cart-item)]

       ;; info group
       [:div.flex-grow-1
        [:div.flex
         [:div.flex-grow-1
          (cart-item-title-molecule cart-item)

          [:div
           (cart-item-copy-molecule cart-item)
           (ui.molecules/stars-rating-molecule cart-item)
           (cart-item-adjustable-quantity-molecule cart-item)
           (cart-item-pick-stylist-molecule cart-item)]]

         ;; price group
         [:div.right.right-align.h6.pt1
          {:style {:min-width "67px"}}
          (cart-item-remove-action-molecule cart-item)
          (cart-item-swap-action-molecule cart-item)
          (cart-item-floating-box-molecule cart-item)]]

        (cart-item-steps-to-complete-molecule cart-item)

        (component/build suggestions/consolidated-component suggestions nil)]])]))
