(ns storefront.components.content
  (:require [storefront.keypaths :as keypaths]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [clojure.string :as string]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(defn component [{:keys [content requesting?]} owner opts]
  (component/create
   [:div
    [:div {:dangerouslySetInnerHTML {:__html content}}]]))

(defn query [data]
  (let [sms-number (get-in data keypaths/sms-number)]
    {:sms-number  sms-number
     :requesting? (utils/requesting? data request-keys/get-static-content)
     :content     (get-in data keypaths/static-content)}))

(defn built-component [data opts]
  (component/build component (query data) opts))
