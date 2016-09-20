(ns storefront.components.svg
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.assets :as assets]))

;; OPTIMIZATION TOOLS:
;; hiccup -> xml:           Let the browser do it... then delete the data-reactid's
;; svg -> optimized svg:    https://github.com/svg/svgo
;; manual cleanup           remove title, if warranted
;; svg -> sprited svg:      move svg to a `symbol` in sprites.svg
;;                          set id
;;                          use the same viewBox
;;                          remove width and height
;;                          remove xmlns
;;                          use .stroke-x and .fill-x color classes (internally,
;;                            not in the external sprite), so SVGs change with
;;                            palette. If an SVG uses more than one stroke or
;;                            fill, the colors will have be inlined in the
;;                            sprite.
;;                          use it, referencing id
;;                            here, in svg.cljc, as svg/svg-xlink
;;                            in static content as xlink:href

(defn svg-xlink [opts id]
  (component/html
   [:svg opts [:use {:xlinkHref (str (assets/path "/images/sprites.svg") "#" id)}]]))

;; Stylist Dashboard

(def micro-dollar-sign
  (svg-xlink {:class "stroke-light-gray" :style {:width "14px" :height "14px"}}
             "micro-dollar-sign"))

(def large-dollar
  ;; TODO: is there a way to use vector-effect: non-scaling-stroke; to unify
  ;; micro-dollar-sign and large-dollar?
  (svg-xlink {:class "stroke-pure-white" :style {:width "72px" :height "72px"}}
             "large-dollar"))

(def large-percent
  (svg-xlink {:class "stroke-pure-white" :style {:width "72px" :height "72px"}}
             "large-percent"))

(def large-payout
  (svg-xlink {:class "stroke-pure-white" :style {:width "62px" :height "60px"}}
             "large-payout"))

(def large-mail
  (svg-xlink {:style {:width "44px" :height "44px"}}
             "large-mail"))

;; Help

(def phone-ringing
  (svg-xlink {:class "stroke-dark-black" :style {:width "30px" :height "30px"}}
             "phone-ringing"))

(def mail-envelope
  (svg-xlink {:class "stroke-dark-black" :style {:width "30px" :height "30px"}}
             "closed-mail-envelope"))

(def message
  (svg-xlink {:class "stroke-dark-black" :style {:width "30px" :height "30px"}}
             "message-bubble"))

;;

(defn circled-check [svg-options]
  (svg-xlink svg-options "check"))

(defn bag [opts]
  (svg-xlink (merge {:style {:width "25px" :height "28px"}}
                    opts)
             "bag"))

(def counter-inc
  (svg-xlink {:class "stroke-pure-white" :style {:width "1.2em" :height "1.2em"}}
             "counter-inc"))

(def counter-dec
  (svg-xlink {:class "stroke-pure-white" :style {:width "1.2em" :height "1.2em"}}
             "counter-dec"))

;; Stylist Account Page
(def instagram
  (svg-xlink {:style {:width "100%" :height "100%"}}
             "instagram"))

(def styleseat
  (svg-xlink {:style {:width "100%" :height "100%"}}
             "styleseat"))

;; Footer

(def facebook
  (svg-xlink {:class "fill-dark-black" :style {:width "100%" :height "100%"}}
             "facebook"))

(def twitter
  (svg-xlink {:class "fill-dark-black" :style {:width "100%" :height "100%"}}
             "twitter"))

(def pinterest
  (svg-xlink {:class "fill-dark-black" :style {:width "100%" :height "100%"}}
             "pinterest"))

;;

(defn missing-profile-picture [svg-options]
  (svg-xlink svg-options "missing-profile-picture"))

(def play-video
  (svg-xlink {:style {:width "64px" :height "64px"}} "play-video"))

(def guarantee
  (svg-xlink {:class "fill-green" :height "5em" :width "100%"}
             "guarantee"))
