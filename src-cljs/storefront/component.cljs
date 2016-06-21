(ns storefront.component
  ;; create sugar to just require storefront.component instead of needing :include-macros
  (:require-macros [storefront.component])
  ;; ensure that cljs libraries are present at advanced/non-advanced compile time. Removing
  ;; these will likely cause bugs at compile time
  (:require [cljsjs.react]
            [om.core]
            [sablono.core]))
