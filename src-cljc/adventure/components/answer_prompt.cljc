(ns adventure.components.answer-prompt
  (:require [clojure.string :as string]
            [adventure.components.header :as header]
            [storefront.events :as events]
            [spice.maps :as maps]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            ))

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

(defn component
  [{:keys [input-data errors display? header-data prompt-image prompt mini-prompt on-submit]} owner _]
  (component/create
   [:div.z5.bg-lavender.white.center.fixed.overlay.bg-contain.bg-no-repeat
    {:style {:background-position "bottom"
             :background-image    (str "url('" prompt-image "')")}}

    (when header-data
      [:div.mx-auto {:style {:height "100px"}}
       (header/built-component header-data nil)])
    [:div.h3.medium.mb2.col-8.mx-auto prompt]
    [:div.h5.light.mb2.col-8.mx-auto mini-prompt]
    (let [{:keys [value id label type on-change-keypath disabled?]} input-data]
      [:div.col-12.mx-auto
       [:form.block.flex.justify-center
        {:on-submit (apply utils/send-event-callback on-submit)}
        [:input.h5.border-none.px3.bg-white.col-9
         {:key         id
          :label       label
          :data-test   (str id "-input")
          :name        id
          :id          (str id "-input")
          :type        (or type "text")
          :value       (or value "")
          :autoFocus   true
          :required    true
          :placeholder label
          :on-change   (partial handle-on-change on-change-keypath)}]
        [:button
         {:type      "submit"
          :disabled  disabled?
          :style     {:width  "45px"
                      :height "43px"}
          :class     (ui/button-class
                      :color/teal
                      (merge {:class "flex items-center justify-center not-rounded x-group-item"}
                             (when disabled?
                               {:disabled?      disabled?
                                :disabled-class "bg-gray"})))
          :data-test (str id "-answer-submit")} ;; TODO: Update heat
         (ui/forward-arrow {:width     "14"
                            :disabled? disabled?})]]])
    ]))

;; Pages that use this component: Welcome/Home, find your stylist, shop-hair, match-stylist
