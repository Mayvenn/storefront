(ns adventure.components.multi-prompt
  (:require [adventure.components.header :as header]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [spice.maps :as maps]))

(def rect-button-attrs
  {:height-class "py6"
   :class        "border-light-gray"
   :style        {:border-radius "3px"}})

(defn teal-rect-button [attrs & content]
  (ui/teal-button (maps/deep-merge rect-button-attrs attrs) content))

(defn white-rect-button [attrs & content]
  (ui/white-button (maps/deep-merge rect-button-attrs attrs) content))

(defn component
  [{:keys [prompt prompt-image header-data data-test buttons footer]} _ _]
  (component/create
   [:div.bg-too-light-teal.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold
     {:style {:height              "246px"
              :background-size     "cover"
              :background-position "center"
              :background-image    (str "url('"prompt-image "')")}}
     [:div.col-12.p5 prompt]]
    [:div.px5.py1
     {:data-test data-test}
     (for [{:as button :keys [text data-test-suffix]} buttons]
       (let [button-component (if (= :teal (:color button)) teal-rect-button
                                  white-rect-button)
             button-data-test (str data-test "-" data-test-suffix)]
         [:div.p1 {:key button-data-test}
          (button-component
           (merge
            {:data-test button-data-test}
            (utils/fake-href events/control-adventure-choice
                             {:prompt       prompt
                              :buttons      buttons
                              :choice       button
                              :current-step (:current-step header-data)}))
            text)]))]
    footer]))
