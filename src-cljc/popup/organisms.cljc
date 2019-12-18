(ns popup.organisms
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

;; MOLECULES


(defn close-x
  ([close-event]
   (close-x close-event nil))
  ([close-event opts]
   [:div.flex.justify-end.mt2.mr1
    (svg/x-sharp (merge (utils/fake-href close-event)
                        {:style {:width  "20px"
                                 :height "20px"}}
                        opts))]))

(defn monstrous-title
  [{:monstrous-title/keys [copy]}]
  [:div.pt3.title-1.canela
   {:style {:font-size   "60px"
            :line-height "62px"}}
   (for [line (cond-> copy string? vec)]
     [:div line])])

(defn subtitle
  [{:subtitle/keys [copy]}]
  [:div.flex.mt4.mb2.h2
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
     [:div.mb3
      (ui/text-field-large (merge {:required true}
                                  field-data))]
     (ui/submit-button (:title button-data) button-data)]]])

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
    [:div.flex.flex-column.bg-cover.bg-top.bg-white.p2.col-12
     {:style {:max-width "345px"}}
     (close-x modal-close-event
              {:data-test "dismiss-email-capture"})
     [:div.bold.px3
      pre-title-content
      (monstrous-title query)
      (subtitle query)
      description-copy]
     (single-field-form query)])])
