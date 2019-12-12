(ns adventure.components.answer-prompt
  (:require [adventure.components.header :as header]
            [spice.maps :as maps]
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [spice.core :as spice]))

(defcomponent ^:private content-component
  [{:keys [input-data header-data prompt on-submit]}
   owner {:keys [desktop-header-dt]}]
  [:div
   (when header-data
     [:div.mx-auto {:style {:height "100px"}}
      (cond-> header-data
        desktop-header-dt
        (update-in [:right-corner :id] str desktop-header-dt)

        :always
        (header/built-component nil))])
   [:div.p5
    (svg/mayvenn-logo {:width "44px" :height "26px"})]
   [:div.mb4.col-9-on-tb-dt.mx-auto.canela.title-1.col-11
    prompt]
   (let [{:keys [value id label type on-change-keypath disabled?]} input-data]
     [:form.block.mx-auto.p4.col-7-on-tb-dt
      {:on-submit (apply utils/send-event-callback on-submit)}
      [:div.mx-auto
       (ui/text-field-large {:id        (str id desktop-header-dt)
                             :label     label
                             :keypath   on-change-keypath
                             :value     value
                             :type      type
                             :required  true
                             :autoFocus true})]
      [:div.pt1.mx-auto
       (ui/submit-button "Next"
                         {:data-test          (str id "-input-submit" desktop-header-dt)
                          :navigation-message on-submit
                          :disabled?          disabled?})]])])

(defcomponent component
  [data owner _]
  [:div
   [:div.z5.bg-pale-purple.center.fixed.overlay.bg-contain.bg-no-repeat.max-580.mx-auto.hide-on-mb
    (component/build content-component
                     data
                     {:opts {:text-style "48px"
                             :desktop-header-dt "-desktop"}})]

   [:div.z5.bg-pale-purple.center.absolute.overflow-scroll.overflow-auto.overlay.bg-contain.bg-no-repeat.max-580.mx-auto.hide-on-tb-dt.pb3
    (component/build content-component data nil)]])
