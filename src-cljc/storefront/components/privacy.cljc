(ns storefront.components.privacy
  (:require [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]))

(component/defdynamic-component wirewheel-upcp-iframe
  (did-mount
   [this]
   #?(:cljs
      (let [iframe (js/document.getElementById "wwiframe")]
        (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
         #js {:targetIframe iframe}) )))
  (render
   [this]
   (component/html
    [:iframe {:style {:height "900px"}
              :id    "wwiframe"
              :src   "https://ui.upcp.wirewheel.io/76/welcome"}])))

(component/defcomponent component [{:keys [content static-content-id wwupcp?]} owner opts]
  [:div.flex.flex-column.container
   [:div
    {:id                      (str "content-" static-content-id)
     :dangerouslySetInnerHTML {:__html content}}]
   (when wwupcp?
     (component/build wirewheel-upcp-iframe))])

(defn query [data]
  {:wwupcp?    (experiments/ww-upcp? data)
   :sms-number (get-in data keypaths/sms-number)
   :content    (get-in data keypaths/static-content)})

(defn page [data opts]
  (component/build component (query data) opts))
