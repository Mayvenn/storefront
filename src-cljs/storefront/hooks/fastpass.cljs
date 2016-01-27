(ns storefront.hooks.fastpass
  (:require [storefront.browser.tags :refer [replace-tag-with-src]]))

(defn insert-fastpass [url]
  (set! js/GSFN (clj->js {:rewrite_urls true}))
  (replace-tag-with-src "https://community.mayvenn.com/javascripts/fastpass.js" "fastpass")
  (replace-tag-with-src url "fastpass-trampoline"))
