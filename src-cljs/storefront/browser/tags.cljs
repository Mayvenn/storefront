(ns storefront.browser.tags
  (:require [goog.dom.classlist :as classlist]
            [goog.dom.dataset :as dataset]
            [clojure.string :as string]))

(defn- insert-before-selector [selector tag]
  (let [first-tag (.querySelector js/document selector)]
    (.insertBefore (.-parentNode first-tag) tag first-tag)))

(defn insert-body-bottom [tag]
  (.appendChild js/document.body tag))

(def insert-in-head (partial insert-before-selector "head link"))

(defn src-tag
  [src class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-async script-tag) "true")
    (set! (.-src script-tag) src)
    (classlist/add script-tag class)
    script-tag))

(defn text-tag [text class]
  (let [script-tag (.createElement js/document "script")]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-innerText script-tag) (string/replace text #"\n" "")) ;; note: firefox puts <br/> in multiline strings?
    (classlist/add script-tag class)
    script-tag))

(defn insert-tag-with-src [src class]
  (insert-body-bottom (src-tag src class)))

(defn insert-tag-with-text [text class]
  (insert-body-bottom (text-tag text class)))

(defn insert-tag-with-callback [tag callback]
  (set! (.-onload tag) callback)
  (insert-body-bottom tag))

(defn insert-tag-with-dataset-and-callback
  [tag dataset-key dataset callback]
  (dataset/set tag dataset-key dataset)
  (insert-tag-with-callback tag callback))

(defn insert-tag-pair [src text class]
  (let [tag (src-tag src (str class "-src"))
        callback #(insert-body-bottom (text-tag text class))]
    (insert-tag-with-callback tag callback)))

(defn remove-tag [tag]
  (.removeChild (.-parentNode tag) tag))

(defn remove-tags-by-class [class]
  (doseq [tag (array-seq (.querySelectorAll js/document (str "." class)))]
    (remove-tag tag)))

(defn query-by-src [src]
  (.querySelector js/document (str "[src=\"" src "\"]")))

(defn insert-tag-with-src-once
  ([src class]
   (when-not (query-by-src src)
     (insert-tag-with-src src class)))
  ([src class callback]
   (let [existing-tag      (query-by-src src)
         existing-callback (some-> existing-tag .-onload)]
     (cond
       (not existing-tag)        (-> src
                                     (src-tag class)
                                     (insert-tag-with-callback callback))
       (some? existing-callback) (set! (.-onload existing-tag)
                                       (juxt existing-callback callback))
       :else                     (set! (.-onload existing-tag)
                                       callback)))))

(defn remove-tag-by-src [src]
  (when-let [tag (query-by-src src)]
    (remove-tag tag)))

(defn remove-tag-pair [class]
  (remove-tags-by-class (str class "-src"))
  (remove-tags-by-class class))

(defn replace-tag-with-src [src class]
  (remove-tags-by-class class)
  (insert-tag-with-src src class))

(defn insert-image-with-src [src]
  (let [img-tag (.createElement js/document "img")]
    (set! (.-src img-tag) src)
    (set! (.-width img-tag) "1")
    (set! (.-height img-tag) "1")
    (set! (.-border img-tag) "0")
    (insert-body-bottom img-tag)))

(defn add-classname
  [selector classname]
  (when-let [element (.querySelector js/document selector)]
    (classlist/add element classname)))

(defn remove-classname
  [selector classname]
  (when-let [element (.querySelector js/document selector)]
    (when (classlist/contains element classname)
      (classlist/remove element classname))))
