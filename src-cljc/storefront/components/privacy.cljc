(ns storefront.components.privacy
  (:require #?@(:cljs [[storefront.hooks.wirewheel-upcp :as wirewheel-upcp]
                       storefront.config])
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]))

(component/defdynamic-component wirewheel-upcp-iframe
  (did-mount
   [this]
   (prn "mounted")
   #?(:cljs (wirewheel-upcp/init-iframe)))
  (render
   [this]
   (component/html
    (spice.core/spy "rendered"
                    (let [src (:src (component/get-props this))]
                      [:iframe {:style {:height "900px"}
                                :id    "wwiframe"
                                :src src}])))))

(component/defcomponent component [{:keys [content static-content-id ww-iframe? ww-iframe-src]} owner opts]
  [:div.flex.flex-column.container
   [:div
    {:id                      (str "content-" static-content-id)
     :dangerouslySetInnerHTML {:__html content}}]
   (when ww-iframe?
     (component/build wirewheel-upcp-iframe {:src ww-iframe-src}))])

(defn query [data]
  (let [ww-ff?         (experiments/ww-upcp? data)
        ww-iframe-src  #?(:cljs (if (get-in data keypaths/inited-wirewheel-upcp)
                                  storefront.config/wirewheel-upcp-url
                                  nil)
                          :clj nil)]
    {:sms-number    (get-in data keypaths/sms-number)
     :content       (get-in data keypaths/static-content)
     :ww-iframe?    ww-ff?
     :ww-iframe-src ww-iframe-src}))

(defn page [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-content-privacy [_ event _ _ app-state]
  #?(:cljs
     (wirewheel-upcp/insert)))
