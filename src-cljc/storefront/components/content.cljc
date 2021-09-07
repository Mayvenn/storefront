(ns storefront.components.content
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.keypaths :as keypaths]))

(defcomponent component [{:keys [content static-content-id]} owner opts]
  [:div.mx-960.mx-auto
   {:id                      (str "content-" static-content-id)
    :dangerouslySetInnerHTML {:__html content}}])

(defn query [data]
  (let [sms-number (get-in data keypaths/sms-number)]
    {:sms-number sms-number
     :content    (get-in data keypaths/static-content)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
