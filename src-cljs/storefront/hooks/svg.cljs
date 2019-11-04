(ns storefront.hooks.svg
  (:require [storefront.browser.tags :as tags]
            [storefront.assets :as assets]
            [ajax.core :refer [GET raw-response-format]]))

(defn insert-sprite []
  (let [handler (fn [resp]
                  (let [div-tag (.createElement js/document "div")]
                    (set! (.-innerHTML div-tag) resp)
                    (.setAttribute div-tag "class" "hide")
                    (tags/insert-body-bottom div-tag)))]
    (GET (assets/path "/images/sprites.svg")
      {:format :raw
       :handler handler
       :response-format (raw-response-format)})))
