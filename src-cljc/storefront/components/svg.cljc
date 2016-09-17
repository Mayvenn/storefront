(ns storefront.components.svg
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])))

;; OPTIMIZATION TOOLS:
;; hiccup -> xml:           Let the browser do it... then delete the data-reactid's
;; svg -> optimized svg:    https://github.com/svg/svgo
;; manual cleanup           remove title, if warranted
;;                          remove xmlns, assuming SVG will be rendered inline
;;                          remove width and height, if SVG will have adjustable size
;;                          use .stroke-x and .fill-x color classes, so SVGs
;;                            change with palette
;; svg -> sprited svg:      change svg to symbol in sprites.svg
;;                          use the same viewBox
;;                          set id
;;                          set width and height to 100%
;;                          use it
;;                            here, in svg.cljc, as svg/svg-xlink
;;                            in static content as xlink:href

(defn svg-xlink [opts id]
  (component/html
   [:svg opts [:use {:xlinkHref (str "#" id)}]]))

(def micro-dollar-sign
  (svg-xlink {:style {:width "14px" :height "14px"}}
             "micro-dollar-sign"))

(def large-dollar
  ;; TODO: is there a way to use vector-effect: non-scaling-stroke; to unify
  ;; micro-dollar-sign and large-dollar?
  (svg-xlink {:style {:width "72px" :height "72px"}}
             "large-dollar"))

(def large-percent
  (svg-xlink {:style {:width "72px" :height "72px"}}
             "large-percent"))

(def large-payout
  (svg-xlink {:style {:width "62px" :height "60px"}}
             "large-payout"))

(def large-mail
  (svg-xlink {:style {:width "44px" :height "44px"}}
             "large-mail"))

(def phone-ringing
  (svg-xlink {:style {:width "30px" :height "30px"}}
             "phone-ringing"))

(def mail-envelope
  (svg-xlink {:style {:width "30px" :height "30px"}}
             "closed-mail-envelope"))

(def message
  (svg-xlink {:style {:width "30px" :height "30px"}}
             "message-bubble"))

(defn circled-check [svg-options]
  (svg-xlink svg-options "check"))

(defn bag [opts]
  (svg-xlink (merge {:style {:width "25px" :height "28px"}}
                    opts)
             "bag"))

(def counter-inc
  (svg-xlink {:style {:width "1.2em" :height "1.2em"}}
             "counter-inc"))

(def counter-dec
  (svg-xlink {:style {:width "1.2em" :height "1.2em"}}
             "counter-dec"))

(def instagram (svg-xlink {:style {:width "100%" :height "100%"}} "instagram"))

(def styleseat (svg-xlink {:style {:width "100%" :height "100%"}} "styleseat"))

(def facebook (svg-xlink {:style {:width "100%" :height "100%"}} "facebook"))

(def twitter (svg-xlink {:style {:width "100%" :height "100%"}} "twitter"))

(def pinterest (svg-xlink {:style {:width "100%" :height "100%"}} "pinterest"))

(defn missing-profile-picture [svg-options]
  (svg-xlink svg-options "missing-profile-picture"))

(def play-video
  (svg-xlink {:style {:width "64px" :height "64px"}} "play-video"))

(defn guarantee [svg-options]
  (svg-xlink svg-options "guarantee"))
