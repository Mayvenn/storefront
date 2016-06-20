(ns storefront.hooks.fastpass
  (:require [storefront.browser.tags :refer [replace-tag-with-src
                                             insert-tag-with-callback
                                             remove-tags-by-class
                                             src-tag]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]))

(defn community-url []
  (when (.hasOwnProperty js/window "GSFN")
    (str "https://community.mayvenn.com?fastpass="
         (js/encodeURIComponent js/GSFN.fastpass_url))))

(defn insert-fastpass [url]
  (replace-tag-with-src "https://community.mayvenn.com/javascripts/fastpass.js" "fastpass")
  (remove-tags-by-class "fastpass-trampoline")
  (insert-tag-with-callback (src-tag url "fastpass-trampoline") identity))
