(ns storefront.css-transitions
  (:require [sablono.core :refer [html]]
            #?(:cljs [react-transition-group])))

(defn first-child [props]
  #?(:cljs (first (js/React.Children.toArray (.-children props)))
     :clj  props))

(defn transition-group [options & children]
  #?(:cljs (apply js/React.createElement js/ReactTransitionGroup.CSSTransitionGroup (clj->js options) (html children))
     :clj  children))

(defn transition-element [options element]
  (transition-group (assoc options :component first-child) element))

(defn transition-background-color [run-transition? & content]
  (if run-transition?
    (transition-element
     {:transitionName          "line-item-fade"
      :transitionAppearTimeout 1300
      :transitionAppear        true
      :transitionEnter         true
      :transitionEnterTimeout  1300
      :transitionLeaveTimeout  1300}
     content)
    content))
