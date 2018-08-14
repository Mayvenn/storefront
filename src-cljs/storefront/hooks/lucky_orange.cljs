(ns storefront.hooks.lucky-orange
  (:require [storefront.browser.tags :as tags]
            [storefront.config :as config]))

(defn track-store-experience [store-experience]
  (tags/insert-tag-with-text
   (str
    " window._loq = window._loq || [];"
    " window._loq.push(['store_experience', '" store-experience "']);")
   "lucky-orange-store-experience"))

(defn remove-tracking []
  (tags/remove-tags-by-class "lucky-orange-store-experience"))
