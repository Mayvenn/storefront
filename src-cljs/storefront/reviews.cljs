(ns storefront.reviews
  (:require [storefront.script-tags :refer [insert-tag-with-text remove-tag]]))

(def ^:private tag-class "product-review-tag")

(defn insert-reviews []
  (when-not (aget (.getElementsByClassName js/document tag-class) 0)
    (insert-tag-with-text
     "(function e(){var e=document.createElement(\"script\");e.type=\"text/javascript\",e.async=true,e.src=\"//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js\";var t=document.getElementsByTagName(\"script\")[0];t.parentNode.insertBefore(e,t)})();"
     tag-class)))

(defn remove-reviews []
  (remove-tag tag-class))
