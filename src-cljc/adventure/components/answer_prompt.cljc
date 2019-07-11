(ns adventure.components.answer-prompt
  (:require [adventure.components.header :as header]
            [spice.maps :as maps]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [spice.core :as spice]))

(def rect-button-attrs
  {:height-class "py6"
   :style {:border-radius "3px"}})

(defn teal-rect-button [attrs & content]
  (ui/teal-button (maps/deep-merge rect-button-attrs attrs) content))

(defn white-rect-button [attrs & content]
  (ui/white-button (maps/deep-merge rect-button-attrs attrs) content))

(defn ^:private content-component [{:keys [input-data header-data prompt-image prompt mini-prompt on-submit title-image-uuid title-image-alt]} owner opts]
  (component/create
   [:div
    (when header-data
      [:div.mx-auto {:style {:height "100px"}}
       (header/built-component header-data nil)])
    [:div.h1.medium.mb2.col-8.col-9-on-tb-dt.mx-auto
     (when-let [font-size (:text-style opts)]
       {:style {:font-size font-size}
        :class "line-height-2"})
     (when title-image-uuid
       (ui/ucare-img {:alt title-image-alt :class "mx-auto mb4"} title-image-uuid))
     prompt]
    [:div.h5.light.mb2.col-8.mx-auto mini-prompt]
    (let [{:keys [value id label type on-change-keypath disabled?]} input-data]
      [:form.col-12.block.mx-auto.mt4.pt2
       {:on-submit (apply utils/send-event-callback on-submit)}
       [:div.col-9.mx-auto
        (ui/text-input {:id        id
                        :label     label
                        :keypath   on-change-keypath
                        :value     value
                        :type      "email"
                        :required  true
                        :autoFocus true})]
       [:div.pt3.col-9.col-7-on-tb-dt.mx-auto
        (ui/submit-button "Get Started"
                          {:data-test (str id "-answer-submit")})]])]))

(defn component
  [{:keys [input-data header-data prompt-image prompt-desktop-image prompt mini-prompt on-submit] :as data} owner _]
  (component/create
   [:div
    [:div.z5.bg-lavender.white.center.fixed.overlay.bg-contain.bg-no-repeat.max-580.mx-auto.bg-contain.hide-on-mb
     {:style {:background-position "123px bottom"
              :background-size     "80%"
              :background-image    (str "url('" prompt-desktop-image  ")")}}
     (component/build content-component data {:opts {:text-style "48px"}})]

    [:div.z5.bg-lavender.white.center.fixed.overlay.bg-contain.bg-no-repeat.max-580.mx-auto.bg-contain.hide-on-tb-dt
     {:style {:background-position "right top 34px"
              :background-size     "90vw auto"
              :background-image    (str "url('" prompt-image "')")}}
     (component/build content-component data nil)]]))

