(ns storefront.hooks.fastpass
  (:require [storefront.browser.tags :refer [replace-tag-with-src
                                             insert-tag-with-callback
                                             remove-tags-by-class
                                             src-tag]]
            [storefront.messages :refer [send]]
            [storefront.events :as events]))

(defn insert-fastpass [data url]
  (replace-tag-with-src "https://community.mayvenn.com/javascripts/fastpass.js" "fastpass")
  (remove-tags-by-class "fastpass-trampoline")
  (insert-tag-with-callback (src-tag url "fastpass-trampoline")
                            (fn [_] (send data events/inserted-fastpass))))
