(ns storefront.css-transitions
  (:require
   #?@(:cljs [[react]
              [cljsjs.react-transition-group]])
   [storefront.component :as component]))

(defn transition-group [options & children]
  #?(:cljs (apply react/createElement js/ReactTransitionGroup.CSSTransition (clj->js options) children)
     :clj  children))

(defn background-fade
  [run-transition? style]
  (merge style
         (when run-transition?
           {:class "background-fade"})))

(defn slide-down [content]
  (transition-group
   {:classNames "slide-down"
    :in         (boolean content)
    :timeout    300}
   (if content
     ^:inline content
     (component/html [:div]))))
