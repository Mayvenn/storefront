(ns storefront.scroll
  (:require [goog.object :as object]))

(defn set-scroll-top [y]
  (set! (.. js/document -body -scrollTop) y))

(def scroll-to-top (partial set-scroll-top 0))

(defn scroll-to [y]
  (let [body (.-body js/document)
        scroll-top (.. js/document -body -scrollTop)
        dy (- y scroll-top)]
    (set! (.. body -style -marginTop) (str dy "px"))
    (set-scroll-top y)
    (set! (.. body -style -transition) "margin-top 1s ease")
    (set! (.. body -style -marginTop) 0)
    (.setTimeout js/window #(set! (.. body -style -transition) "none") 1000)))

(def scroll-padding 35.0)
(defn scroll-to-elem [el]
  (let [scroll-top (.. js/document -body -scrollTop)
        el-bottom (object/get (.getBoundingClientRect el) "bottom")
        window-height js/window.innerHeight]
    (when (> el-bottom window-height)
      (scroll-to (- (+ scroll-top el-bottom scroll-padding)
                    window-height)))))
