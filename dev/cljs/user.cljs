(ns cljs.user
  (:require [storefront.core :as core]))

(defn debug-app-state []
  (js/console.log (clj->js @core/app-state)))
