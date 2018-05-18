(ns storefront.components.content
  (:require [storefront.component :as component]
            [storefront.keypaths :as keypaths]))

(defn component [{:keys [content]} owner opts]
  (component/create
   [:div {:dangerouslySetInnerHTML {:__html content}}]))

(defn query [data]
  (let [sms-number (get-in data keypaths/sms-number)]
    {:sms-number sms-number
     :content    (get-in data keypaths/static-content)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
