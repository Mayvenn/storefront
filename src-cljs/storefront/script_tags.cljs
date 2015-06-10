(ns storefront.script-tags)

(defn insert-tag-with-src [src class]
  (let [script-tag (.createElement js/document "script")
        first-script-tag (aget (.getElementsByTagName js/document "script") 0)]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-async script-tag) "true")
    (set! (.-src script-tag) src)
    (.add (.-classList script-tag) class)
    (.insertBefore (.-parentNode first-script-tag) script-tag first-script-tag)))

(defn remove-tag [class]
  (when-let [beacon-tag (aget (.getElementsByClassName js/document class) 0)]
    (.remove beacon-tag)))
