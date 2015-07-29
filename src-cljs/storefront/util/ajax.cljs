(ns storefront.util.ajax
  (:require [ajax.core :as ajax]))

(defrecord PlaceholderRequest []
  ajax/AjaxRequest
  (-abort [_]))
