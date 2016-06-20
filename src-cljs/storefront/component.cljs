(ns storefront.component
  ;; create sugar to just require storefront.component instead of needing :include-macros
  (:require-macros [storefront.component])
  ;; ensure that react is present as the macros require it
  (:require [cljsjs.react]))
