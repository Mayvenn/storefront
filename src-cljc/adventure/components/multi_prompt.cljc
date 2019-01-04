(ns adventure.components.multi-prompt
  (:require [adventure.components.header :as header]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn component
  [{:keys [prompt prompt-image header-data data-test buttons]} _ _]
  (component/create
   [:div.bg-aqua.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold
     {:style {:height           "246px"
              :background-size  "cover"
              :background-image (str "url('"prompt-image "')")}}
     [:div.col-12.p5 prompt]]
    [:div.p5
     {:data-test data-test}
     (for [{:as button :keys [text value target data-test-suffix]} buttons]
       [:div (ui/aqua-button
              (merge
               {:data-test (str data-test "-" data-test-suffix)}
               (utils/fake-href events/control-adventure {:destination target
                                                          :choice      value}))
              text)])]]))
