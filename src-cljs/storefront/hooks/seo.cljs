(ns storefront.hooks.seo
  (:require [storefront.browser.tags :as tags]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.seo-tags :as seo-tags]))

(defn set-tags [data]
  (tags/remove-tags-by-class seo-tags/tag-class)
  (when-not (= events/navigate-content-about-us (get-in data keypaths/navigation-event))
    (doseq [[el props content] (seo-tags/tags-for-page data)]
     (let [tag (.createElement js/document (clj->js el))]
       (doseq [[k v] props]
         (.setAttribute tag (clj->js k) v))
       (when content
         (set! (.-innerHTML tag) content))
       (tags/insert-in-head tag)))))
