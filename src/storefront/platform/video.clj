(ns storefront.platform.video
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [video-id]} owner opts]
  (component/create [:div]))
