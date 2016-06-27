(ns storefront.browser.scroll
  (:require [goog.object :as object]
            [goog.dom.classlist :as classlist]))

(defn animate [el end-event start-fn end-fn]
  (let [body (.-body js/document)]
    (letfn [(listener [e]
              (end-fn e)
              (classlist/remove body "animating")
              (.removeEventListener el end-event listener))]
      (classlist/add body "animating")
      (start-fn)
      (.addEventListener el end-event listener))))

(defn set-scroll-top
  ([y] (set-scroll-top (.-body js/document) y))
  ([elem y] (set! (.. elem -scrollTop) y)))

(defn scroll-to-top []
  (set-scroll-top 0))

(defn scroll-to [y]
  (let [body (.-body js/document)
        scroll-top (.. js/document -body -scrollTop)
        dy (- y scroll-top)]
    (animate
     body
     "transitionend"
     #(do
        (set! (.. body -style -marginTop) (str dy "px"))
        (set-scroll-top y)
        (set! (.. body -style -transition) "margin-top 1s ease")
        (set! (.. body -style -marginTop) 0))
     #(when (= (.-target %) (.-currentTarget %))
        (set! (.. body -style -transition) "none")))))

(def scroll-padding 35.0)
(defn scroll-to-elem [el]
  (let [scroll-top (.. js/document -body -scrollTop)
        el-bottom (object/get (.getBoundingClientRect el) "bottom")
        window-height js/window.innerHeight]
    (when (> el-bottom window-height)
      (scroll-to (- (+ scroll-top el-bottom scroll-padding)
                    window-height)))))
