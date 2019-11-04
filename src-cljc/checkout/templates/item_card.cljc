(ns checkout.templates.item-card
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.css-transitions :as css-transitions]))

(defcomponent component
  [{:keys [items]} _ _]
  [:div
   (for [{:as       item
          react-key :react/key} items]
     [:div.pt1.pb2.flex.items-center.col-12 {:key react-key}

       ;; image group
      [:div.mt2.mr2.relative.self-start

        ;; circle over image
       (let [{:circle/keys [id value highlight?]} item]
         (when id
           [:div.medium.h5
            (css-transitions/background-fade
             highlight?
             {:key (str react-key "-image")})
            [:div.absolute.z1.circle.stacking-context.border.border-light-gray.bg-too-light-teal.flex.items-center.justify-center
             {:data-test id
              :style     {:right  "-5px"
                          :top    "-12px"
                          :width  "32px"
                          :height "32px"}}
             value]]))

        ;; actual image
       (let [{:image/keys [id value highlight?]} item]
         (when id
           [:div.flex.items-center.justify-center.ml1
            (css-transitions/background-fade
             highlight?
             {:key       (str react-key "-actual-image")
              :data-test id
              :style     {:width "79px" :height "74px"}})
            [:div.pp1
             (ui/ucare-img {:width 75} value)]]))]

       ;; info group
      [:div.h6.flex.flex-wrap.flex-auto.justify-between.mt1

        ;; title
       (let [{:title/keys [id value]} item]
         [:div.col-12
          (when id
            [:a.medium.titleize.h5 {:data-test id} value])])

        ;; detail top-left
       (let [{:detail-top-left/keys [id value opts]} item]
         [:div.col-10
          (when id (merge {:data-test id} opts))
          (when id value)])

        ;; action top-right
       (let [{:detail-top-right/keys [id value opts]} item]
         [:div.col-2.right-align
          (when id (merge {:data-test id} opts))
          (when id value)])

        ;; detail bottom-left
       (let [{:detail-bottom-left/keys [id value opts]} item]
         [:div.col-6
          (when id (merge {:data-test id} opts))
          (when id value)])

        ;; detail bottom-right
       (let [{:detail-bottom-right/keys [id value opts]} item]
         [:div.col-6.right-align
          (when id (merge {:data-test id} opts))
          (when id value)])]])])
