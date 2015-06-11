(ns storefront.script-tags)

(defn insert-tag [tag]
  (let [first-script-tag (aget (.getElementsByTagName js/document "script") 0)]
    (.insertBefore (.-parentNode first-script-tag) tag first-script-tag)))

(defn insert-tag-with-src [src class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-async script-tag) "true")
    (set! (.-src script-tag) src)
    (.add (.-classList script-tag) class)
    (insert-tag script-tag)))

(defn insert-tag-with-text [text class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-innerText script-tag) text)
    (.add (.-classList script-tag) class)
    (insert-tag script-tag)))

(defn remove-tag [class]
  (when-let [beacon-tag (aget (.getElementsByClassName js/document class) 0)]
    (.remove beacon-tag)))

(defn remove-tag-by-src [src]
  (when-let [tag (.querySelector js/document (str "[src=\"" src "\"]"))]
    (.remove tag)))
