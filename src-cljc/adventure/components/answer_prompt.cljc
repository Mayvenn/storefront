(ns adventure.components.answer-prompt
  (:require [adventure.components.header :as header]
            [spice.maps :as maps]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]))

(def rect-button-attrs
  {:height-class "py6"
   :style {:border-radius "3px"}})

(defn teal-rect-button [attrs & content]
  (ui/teal-button (maps/deep-merge rect-button-attrs attrs) content))

(defn white-rect-button [attrs & content]
  (ui/white-button (maps/deep-merge rect-button-attrs attrs) content))

(defn ^:private handle-on-change
  #?(:clj
     ([keypath _e] nil)
     :cljs
     ([keypath ^js/Event e]
      (messages/handle-message events/control-change-state
                               {:keypath keypath
                                :value   (.. e -target -value)}))))

(defn ^:private content-component [{:keys [input-data header-data prompt-image prompt mini-prompt on-submit title-image-uuid title-image-alt]} owner _]
  (component/create
   [:div
    (when header-data
      [:div.mx-auto {:style {:height "100px"}}
       (header/built-component header-data nil)])
    [:div.h3.medium.mb2.col-8.mx-auto
     (when title-image-uuid
       (ui/ucare-img {:alt title-image-alt :class "mx-auto mb4"} title-image-uuid))
     prompt]
    [:div.h5.light.mb2.col-8.mx-auto mini-prompt]
    (let [{:keys [value id label type on-change-keypath disabled?]} input-data]
      [:form.col-12.block.mx-auto.mt4.pt2
       {:on-submit (apply utils/send-event-callback on-submit)}
       [:div.col-9.mx-auto
        ;; TODO: have a new textfield component
        ;;  [x] should placeholder text be more grayed out - yes
        [:input.h5.border-none.px2.bg-white.col-12.rounded.placeholder-dark-silver
         {:style                           {:height    "56px" ; style-guide is 18px
                                            :font-size "16px"} ; style-guide is 41px
          :key                             id
          :label                           label
          :data-test                       (str id "-input")
          :name                            id
          :id                              (str id "-input")
          :type                            (or type "text")
          :value                           (or value "")
          :autoFocus                       true
          :required                        true
          :placeholder                     label
          :on-change                       (partial handle-on-change on-change-keypath)}]]
       [:div.pt3.col-9.mx-auto
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
     (component/build content-component data nil)]

    [:div.z5.bg-lavender.white.center.fixed.overlay.bg-contain.bg-no-repeat.max-580.mx-auto.bg-contain.hide-on-tb-dt
     {:style {:background-position "right top 34px"
              :background-image    (str "url('" prompt-image "')")}}
     (component/build content-component data nil)]]))

