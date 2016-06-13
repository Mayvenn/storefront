(ns storefront.components.counter
  (:require [storefront.events :as events]
            [storefront.components.utils :as utils]
            [storefront.messages :refer [handle-message]]
            [storefront.keypaths :as keypaths]
            [storefront.request-keys :as request-keys]
            [sablono.core :refer-macros [html]]
            [om.core :as om]))

;; TODO: delete this file
