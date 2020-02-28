(ns checkout.ui.cart-item
  (:require [checkout.suggestions :as suggestions]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]
            [storefront.platform.component-utils :as utils]
            ui.molecules
            [storefront.platform.messages :as messages]))

(defn cart-item-floating-box-molecule
  [{:cart-item-floating-box/keys [id value]}]
  (when id
    [:div.right.right-align.proxima.content-2
     {:key       id
      :data-test id
      :style     {:height "100%"}}
     value]))

(defn cart-item-copy-molecule
  [{:cart-item-copy/keys [id value]}]
  (when value
    [:div.content-3.proxima {:data-test id} value]))

(defn cart-item-title-molecule
  [{:cart-item-title/keys [id primary secondary]}]
  (when (and id primary)
    [:div
     [:div.proxima.content-2
      {:data-test id}
      primary]
     [:div.dark-gray secondary]]))

(defn completed-progress-circle-atom
  [i _]
  [:div.bg-s-color.flex.justify-center.items-center
   {:key (str "complete-" i)
    :style {:height 21
            :width  21
            :border-radius "50%"}}
   (svg/check-mark {:class "fill-white"
                    :style {:width "11px"
                            :height "11px"}})])

(defn incomplete-progress-circle-atom
  [i content]
  [:div.bg-cool-gray.flex.justify-center.items-center.proxima.button-font-2
   {:key (str "incomplete-" i)
    :style {:height 21
            :width  21
            :border-radius "50%"}}
   content])

(def steps-hyphen-seperator-atom
  [:div.border-top.border-gray])

(defn cart-item-steps-to-complete-molecule
  [{:cart-item-steps-to-complete/keys
    [steps current-step action-label action-target id]}]
  (when id
    [:div.items-center.mr2.mt2.flex.flex-wrap
     (when (pos? (count steps))
       (let [[completed uncompleted] (split-at current-step steps)]
         [:div.flex.items-center.mr3.mb2 ; margin bottom for 320px screens
          (interpose (conj steps-hyphen-seperator-atom
                           {:class "mx1"
                            :style {:width "11px"}})
                     (concat
                      (map-indexed completed-progress-circle-atom
                                   completed)
                      (map-indexed incomplete-progress-circle-atom
                                   uncompleted)))]))
     (when (and action-target action-label)
       [:div.mb2 ; margin bottom for 320px screens
        (ui/button-small-primary (assoc (apply utils/route-to action-target)
                                        :data-test id)
                                 action-label)])]))

(defn cart-item-square-thumbnail-molecule
  [{:cart-item-square-thumbnail/keys
    [id ucare-id sku-id sticker-label highlighted?]}]
  (when id
    (let [sticker-id (str "line-item-length-" sku-id)]
      [:div.relative
       {:style {:height "45px"
                :width  "48px"}}
       (when sticker-label
         [:div.absolute.z1.circle.border.border-gray.bg-white.proxima.title-3.flex.items-center.justify-center
          (css-transitions/background-fade
           highlighted?
           {:key       sticker-id
            :data-test sticker-id
            :style     {:height "26px"
                        :width "26px"
                        :right -10
                        :top   -10}})
          sticker-label])
       [:div.flex.items-center.justify-center
        (css-transitions/background-fade
         highlighted?
         {:style     {:height "45px"
                      :width  "48px"}
          :key       (str "cart-item-square-thumbnail-" sku-id)
          :data-test (str "line-item-img-" sku-id)})
        (ui/ucare-img {:width 48
                       :class "block border border-cool-gray"}
                      ucare-id)]])))

(defn confetti-handler
  [mode]
  (when (= mode "ready")
    (messages/handle-message events/set-confetti-mode {:mode "firing"})))

(defn cart-item-service-thumbnail-molecule
  [{confetti-mode :confetti-mode
    :cart-item-service-thumbnail/keys [id highlighted? image-url locked?]}]
  (when id
    [:div.flex.justify-center.mtn2
     (css-transitions/background-fade
      highlighted?
      {:style {:border-radius "50%"
               :width         "56px"
               :height        "56px"}
       ;; QUESTION(jeff): is this an appropriate place for click handler inside css-transition?
       :on-click #(confetti-handler confetti-mode)})

     [:div.relative
      (if locked?
        [:div
         [:div.absolute.z1.col-12.flex.items-center.justify-center
          {:style {:height "100%"}}
          [:div.absolute.z2.col-12.flex.items-center.justify-center
           {:style {:height "100%"}}
           (svg/lock {:style {:width   "17px"
                              :height  "23px"
                              :opacity ".75"}
                      :class "mtn2"})]]
         (ui/ucare-img {:width "50px"
                        :class "mtp3"
                        :style {:filter "contrast(0.1) brightness(1.75)"}} image-url)]
        (ui/ucare-img {:width "50px"
                       :class "mtp3"} image-url))]]))

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

(defn cart-item-modify-button
  [{:cart-item-modify-button/keys [id target content]}]
  (when id
    [:div
     (ui/button-small-secondary (merge {:class        "p-color bold mt1"
                                        :data-test    id}
                                       (apply utils/route-to target))
                                content)]))

(defcomponent organism
  [{:keys [cart-item suggestions]} _ {:keys [id]}]
  [:div.pt1.pb2.m1.flex
   {:id id :data-test id}
   ;; image group
   [:div.relative.pt3
    {:style {:min-width "70px"}}
    (cart-item-square-thumbnail-molecule cart-item)
    (cart-item-service-thumbnail-molecule cart-item)]

   ;; info group
   [:div.flex-grow-1
    [:div.flex
     [:div.flex-grow-1
      (cart-item-title-molecule cart-item)

      [:div
       (cart-item-copy-molecule cart-item)
       (ui.molecules/stars-rating-molecule cart-item)
       (cart-item-adjustable-quantity-molecule cart-item)
       (cart-item-modify-button cart-item)]]

     ;; price group
     [:div.right.right-align.pt1.flex.flex-column.items-end
      {:style {:min-width "67px"}}
      (cart-item-remove-action-molecule cart-item)
      (cart-item-swap-action-molecule cart-item)
      (cart-item-floating-box-molecule cart-item)]]

    (cart-item-steps-to-complete-molecule cart-item)

    (component/build suggestions/consolidated-component suggestions nil)]])
