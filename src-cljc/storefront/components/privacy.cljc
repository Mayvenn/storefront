(ns storefront.components.privacy
  (:require #?@(:cljs [[storefront.hooks.wirewheel-upcp :as wirewheel-upcp]
                       storefront.config])
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.platform.messages :as messages]))

(component/defcomponent wirewheel-upcp-iframe
  [{:keys [src]} owner opts]
  (component/html
   [:iframe #?(:cljs {:style   {:height "900px"}
                      :id      "wwiframe"
                      :src     src
                      :on-load (fn init-iframe []
                                 (when (clojure.core/exists? js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent)
                                   (js/window.cmpJavascriptSdk.WireWheelSDK.initEmbeddedParent
                                    #js {:targetIframe (js/document.getElementById "wwiframe")}))
                                 (messages/handle-message events/initialized-wirewheel-upcp))})]))

(component/defcomponent component [{:keys [content static-content-id ww-iframe-src]} owner opts]
  [:div.flex.flex-column.container
   [:div
    {:id                      (str "content-" static-content-id)
     :dangerouslySetInnerHTML {:__html content}}]
   (when ww-iframe-src
     (component/build wirewheel-upcp-iframe {:src ww-iframe-src}))])

(defn query [data]
  {:sms-number    (get-in data keypaths/sms-number)
   :content       (get-in data keypaths/static-content)
   :ww-iframe-src (and (experiments/ww-upcp? data)
                       (get-in data keypaths/loaded-wirewheel-upcp)
                       #?(:cljs storefront.config/wirewheel-upcp-url))})

(defn page [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-content-privacy
  [_ _ _ _ _]
  #?(:cljs
     (wirewheel-upcp/insert)))
