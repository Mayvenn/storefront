(ns checkout.ui.cart-item
  (:require [storefront.component :as component]
            [storefront.css-transitions :as css-transitions]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            ))

(defn cart-item-floating-box-molecule
  [{:cart-item-floating-box/keys [id value]}]
  (when id
    [:div.col-2.right.right-align
     {:data-test id}
     value]))

(defn cart-item-copy-molecule
  [{:cart-item-copy/keys [value]}]
  (when value
    [:div.h7.dark-gray value]))

(defn cart-item-title-molecule
  [{:cart-item-title/keys [id primary secondary]}]
  (when (and id primary)
    [:div
     [:div
      [:a.medium.titleize.h5
       {:data-test id}
       primary]]
     [:div
      [:a.titleize.h6
       secondary]]]))

(defn completed-progress-circle-atom
  [_]
  [:div.bg-purple.flex.justify-center.items-center.white.bold.h5
   {:style {:height 21
            :width  21
            :border-radius "50%"}}
   "✓"])

(defn incomplete-progress-circle-atom
  [content]
  [:div.bg-light-gray.flex.justify-center.items-center.dark-silver.bold
   {:style {:height 21
            :width  21
            :border-radius "50%"}}
   content])

(def steps-hyphen-seperator-atom
  [:div.border-top.border-light-gray])

(defn cart-item-steps-to-complete-molecule
  [{:cart-item-steps-to-complete/keys
    [steps current-step action-label action-target]}]
  (when (and steps current-step)
    (let [[completed uncompleted] (split-at current-step steps)]
      [:div.flex.items-center
       [:div.flex.items-center.mr2
        (interpose (conj steps-hyphen-seperator-atom
                         {:class "mx1"
                          :style {:width "11px"}})
                   (concat
                    (map completed-progress-circle-atom
                         completed)
                    (map incomplete-progress-circle-atom
                         uncompleted)))]
       (when (and action-target action-label)
         (ui/teal-button (assoc (apply utils/route-to action-target)
                                :height-class :small
                                :width-class :small)
                         action-label))])))


(defn cart-item-thumbnail-molecule
  [{:cart-item-thumbnail/keys [highlighted? value locked? ucare-id]}]
  (css-transitions/transition-background-color
   highlighted?
   [:div.flex.justify-center.items-center
    value
    (let [diameter              50
          inner-circle-diameter (- diameter 4)]
      [:div.relative
       (when locked?
         [:div.absolute.z1.col-12.flex.items-center.justify-center
          {:style {:height "100%"}}

          [:div {:class "absolute z1 block"
                 :style {:background    "#ffffffdd"
                         :width         (str inner-circle-diameter "px")
                         :height        (str inner-circle-diameter "px")
                         :border-radius "50%"}}]
          [:div.absolute.z2.col-12.flex.items-center.justify-center
           {:style {:height "100%"}}
           (svg/lock {:style {:width  "17px"
                              :height "23px"}})]])
       (ui/circle-ucare-img {:class "block"
                             :style {:width  diameter
                                     :height  diameter
                                     :margin "auto"}}
                            ucare-id)])]))

(defn cart-item-remove-action-molecule
  [{:cart-item-remove-action/keys [id target spinning?]}]
  (if spinning?
    [:div.h3
     {:style {:width "1.2em"}}
     ui/spinner]
    [:a.gray.medium.m1
     (merge {:data-test (str "line-item-remove-" id)}
            (apply utils/fake-href target))
     ^:inline (svg/consolidated-trash-can {:width  "16px"
                                           :height "17px"})]))

(defn organism
  [{:as cart-item react-key :react/key} _ _]
  (component/create
   [:div
    (when react-key
      [:div.pt1.pb2.flex.items-center.col-12
       {:key react-key}
       ;; image group
       [:div.mt2.mr2.relative.self-start.col-2
        (cart-item-thumbnail-molecule cart-item)]

       ;; info group
       [:div.h6.mt1.col-10

        [:div.right.col-2.right-align
         (cart-item-remove-action-molecule cart-item)]

        (cart-item-title-molecule cart-item)

        [:div
         (cart-item-floating-box-molecule cart-item)
         (cart-item-copy-molecule cart-item)]

        (cart-item-steps-to-complete-molecule cart-item)]])]))
