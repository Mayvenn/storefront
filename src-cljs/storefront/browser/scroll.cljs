(ns storefront.browser.scroll
  (:require [goog.object :as object]
            [goog.dom.classlist :as classlist]
            [storefront.browser.events :as events]
            [clojure.string :as string]))

(defn disable-body-scrolling []
  (some-> js/document.body .-classList (.add "overflow-hidden")))

(defn enable-body-scrolling []
  (some-> js/document.body .-classList (.remove "overflow-hidden")))

(defn scroll-target []
  (or (.-scrollingElement js/document)
      (if (string/starts-with? (str (.-compatMode js/document)) "CSS1")
        (.-documentElement js/document)
        (.-body js/document))))

(defn animate [el end-event start-fn end-fn]
  (letfn [(listener [e]
            (end-fn e)
            (classlist/remove (scroll-target) "animating")
            (.removeEventListener el end-event listener))]
    (classlist/add (scroll-target) "animating")
    (start-fn)
    (.addEventListener el end-event listener)))

(defn scroll-to-y
  [elem y]
  (when elem
    (set! (.. elem -scrollTop) y)))

(defn snap-to [y]
  (scroll-to-y (scroll-target) y))

(defn snap-to-top []
  (snap-to 0))

(defn scroll-to [dest]
  (let [current-y (.. (scroll-target) -scrollTop)
        dy (- dest current-y)]
    (animate
     (scroll-target)
     "transitionend"
     #(do
        (set! (.. (scroll-target) -style -transition) "none")
        (set! (.. (scroll-target) -style -marginTop) (str dy "px"))
        (scroll-to-y (scroll-target) dest)
        (set! (.. (scroll-target) -style -transition) "margin-top 1s ease")
        (set! (.. (scroll-target) -style -marginTop) 0))
     #(when (= (.-target %) (.-currentTarget %))
        (set! (.. (scroll-target) -style -transition) "none")))))

(def scroll-padding 35.0)
;; TODO: rename

;; ATTN: Having trouble scrolling to an element right on page load -- probably on a
;; page with a lot of images? Use ui/aspect-ratio on image containers to let the
;; browser calculate the height of the page!

(defn scroll-to-elem [el]
  (let [scroll-top (.. (scroll-target) -scrollTop)
         el-bottom (object/get (.getBoundingClientRect el) "bottom")
         window-height js/window.innerHeight]
     (when (> el-bottom window-height)
       (scroll-to (- (+ scroll-top el-bottom scroll-padding)
                     window-height)))))
(defn scroll-elem-to-top [el]
  (let [el-top (object/get (.getBoundingClientRect el) "top")
        scroll-top     (.. (scroll-target) -scrollTop)]
    (scroll-to (+ scroll-top el-top (- scroll-padding)))))

(defn scroll-selector-to-top [selector]
  (when-let [el (.querySelector js/document selector)]
    (scroll-elem-to-top el)))

(defn scroll-to-selector
  [selector]
  (when-let [el (.querySelector js/document selector)]
   (scroll-to-elem el)))

;; END ATTN:
