(ns storefront.platform.images
  (:require [storefront.css-transitions :as css-transitions]))

(defn platform-hq-image [attrs]
  (css-transitions/transition-group
   {:transitionName {:appear "transparent"
                     :appearActive "opaque"}
    :transitionAppear true
    :transitionAppearTimeout 500
    :transitionEnter false
    :transitionLeave false}
   [:img.col-12.absolute.overlay.transition-2.transition-ease attrs]))
