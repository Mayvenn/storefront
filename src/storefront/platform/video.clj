(ns storefront.platform.video
  (:require [storefront.component-shim :as component]))

(defn component [{:keys [video-id]} owner opts]
  (component/create [:div]))

;; This is used on the homepage, but in a popup, so technically doesn't need to
;; be in the CLJ version of this file
(defn built-home-component [_ opts]
  (component/build component {:video-id "66ysezzxwk"} opts))
