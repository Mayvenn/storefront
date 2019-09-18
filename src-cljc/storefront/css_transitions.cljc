(ns storefront.css-transitions
  (:require [sablono.core :refer [html]]
            #?@(:cljs [[react]
                       [react-transition-group]])))

(defn first-child [props]
  #?(:cljs (first (js/React.Children.toArray (.-children props)))
     :clj  props))

(defn transition-group [options & children]
  #?(:cljs (apply react/createElement js/ReactTransitionGroup.CSSTransition (clj->js options)  children)
     :clj  children))

(defn transition-element [options element]
  (transition-group (assoc options :component first-child) element))

(defn transition-background-color [run-transition? & content]
  (transition-element
   {:in         run-transition?
    :classNames "line-item-fade"
    :timeout    1300}
   content))
