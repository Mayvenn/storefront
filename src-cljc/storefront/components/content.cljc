(ns storefront.components.content
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.keypaths :as keypaths]))

(defcomponent component [{:keys [content]} owner opts]
  [:div {:dangerouslySetInnerHTML {:__html content}}])

(defn query [data]
  (let [sms-number (get-in data keypaths/sms-number)]
    {:sms-number sms-number
     :content    (get-in data keypaths/static-content)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
