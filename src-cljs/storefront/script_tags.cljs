(ns storefront.script-tags)

(defn insert-tag [tag]
  (let [first-script-tag (aget (.getElementsByTagName js/document "script") 0)]
    (.insertBefore (.-parentNode first-script-tag) tag first-script-tag)))

(defn src-tag
  [src class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-async script-tag) "true")
    (set! (.-src script-tag) src)
    (.add (.-classList script-tag) class)
    script-tag))

(defn text-tag [text class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-innerText script-tag) text)
    (.add (.-classList script-tag) class)
    script-tag))

(defn insert-tag-with-src [src class]
  (insert-tag (src-tag src class)))

(defn insert-tag-with-text [text class]
  (insert-tag (text-tag text class)))

(defn insert-tag-with-callback [tag callback]
  (set! (.-onload tag) callback)
  (insert-tag tag))

(defn insert-tag-pair [src text class]
  (let [tag (src-tag src (str class "-src"))
        callback #(insert-tag (text-tag text class))]
    (insert-tag-with-callback tag callback)))

(defn remove-tag [class]
  (when-let [beacon-tag (aget (.getElementsByClassName js/document class) 0)]
    (.remove beacon-tag)))

(defn remove-tag-by-src [src]
  (when-let [tag (.querySelector js/document (str "[src=\"" src "\"]"))]
    (.remove tag)))

(defn remove-tag-pair [class]
  (remove-tag (str class "-src"))
  (remove-tag class))
