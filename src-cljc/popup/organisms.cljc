(ns popup.organisms
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            
            ))

;; MOLECULES

(defn close-x
  ([close-event]
   (close-x close-event nil))
  ([close-event opts]
   [:div.flex.justify-end.mt2.mr1
    (svg/simple-x (merge (utils/fake-href close-event)
                         {:style {:width  "18px"
                                  :height "18px"}}
                         opts))]))

(defn monstrous-title
  [{:monstrous-title/keys [copy]}]
  [:div.h0.line-height-1.pt3
   (for [line (cond-> copy string? vec)]
     [:div line])])

(defn subtitle
  [{:subtitle/keys [copy]}]
  [:div.flex.my4.h2
   (for [span (cond-> copy string? vec)]
     span)])

(defn single-field-form
  [{:single-field-form/keys [callback
                             field-data
                             button-data]}]
  [:div.px3.my4
   [:form.col-12.flex.flex-column.items-center
    {:on-submit (utils/send-event-callback callback)}
    [:div.col-12.mx-auto
     (ui/text-field (merge {:required  true
                            :class     "dark-gray h6 bg-light-silver"}
                           field-data))
     (ui/submit-button (:title button-data) (merge {:class "h3 bold mt1"} button-data))]]])

;; ORGANISM

(defcomponent organism
  [{:as               query
    pre-title-content :pre-title/content
    modal-close-event :modal-close/event
    description-copy  :description/copy} _ _]
  [:div
   (ui/modal
    {:close-attrs (utils/fake-href modal-close-event)
     :col-class   "col-11 col-5-on-tb col-4-on-dt flex justify-center"}
    [:div.flex.flex-column.bg-cover.bg-top.bg-white.p2.rounded.col-12
     {:style {:max-width "345px"}}
     (close-x modal-close-event
              {:class     "dark-gray"
               :data-test "dismiss-email-capture"})
     [:div.bold.px3
      pre-title-content
      (monstrous-title query)
      (subtitle query)
      description-copy]
     [:div {:style {:height "30px"}}]
     (single-field-form query)])])
