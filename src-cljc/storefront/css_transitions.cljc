(ns storefront.css-transitions
  #?(:cljs
     (:require [react]
               [cljsjs.react-transition-group])))

(defn transition-group [options & children]
  #?(:cljs (apply react/createElement js/ReactTransitionGroup.CSSTransition (clj->js options) children)
     :clj  children))

(defn background-fade
  [run-transition? style]
  (merge style
         (when run-transition?
           {:class "background-fade"})))
