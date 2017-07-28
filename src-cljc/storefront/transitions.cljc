(ns storefront.transitions
  #?(:cljs
     (:require
      [storefront.frontend-transitions])))

(defmulti transition-state identity)

(defmethod transition-state :default [dispatch event args app-state]
  ;; (js/console.log "IGNORED transition" (clj->js event) (clj->js args)) ;; enable to see ignored transitions
  app-state)
