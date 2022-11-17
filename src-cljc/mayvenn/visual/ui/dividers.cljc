(ns mayvenn.visual.ui.dividers
  (:require [storefront.component :as c]))

(def green
  (c/html
   [:div
    {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))

(def purple
  (c/html
   [:div
    {:style {:background-image    "url('//ucarecdn.com/73db5b08-860e-4e6c-b052-31ed6d951f00/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))
