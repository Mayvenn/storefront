(ns storefront.components.svg
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])))

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

(defn svg-xlink
  ([id] (svg-xlink {} id))
  ([opts id] [:g {:dangerouslySetInnerHTML {:__html (str "<use xlink:href=\"#" id "\"></use>")}}]))

(defn error [opts]
  [:svg opts (svg-xlink "circled-exclamation")])

(defn dropdown-arrow [opts]
  [:svg opts (svg-xlink "dropdown-arrow")])

(defn left-caret [opts]
  [:svg opts
   (svg-xlink "left-caret")])

;; Stylist Dashboard

(def micro-dollar-sign
  (component/html
   [:svg {:class "stroke-dark-gray" :style {:width "14px" :height "14px"}}
    (svg-xlink "micro-dollar-sign")]))

(def large-dollar
  ;; TODO: is there a way to use vector-effect: non-scaling-stroke; to unify
  ;; micro-dollar-sign and large-dollar?
  (component/html
   [:svg {:class "stroke-white" :style {:width "1em" :height "1em"}}
    (svg-xlink "large-dollar")]))

(def large-percent
  (component/html
   [:svg {:class "stroke-white" :style {:width "1em" :height "1em"}}
    (svg-xlink "large-percent")]))

(def large-payout
  (component/html
   [:svg {:class "stroke-white" :style {:width "1em" :height "1em"}}
    (svg-xlink "large-payout")]))

(def large-mail
  (component/html
   [:svg {:style {:width "44px" :height "44px"}}
    (svg-xlink "large-mail")]))

;; Vendor Logos

(defn affirm [opts]
  (component/html
   [:svg (merge {:style {:width "51.8px" :height "15px"}} opts)
    (svg-xlink "affirm")]))

;; Help

(defn phone-ringing [opts]
  [:svg (merge {:class "stroke-dark-gray" :style {:width "30px" :height "30px"}} opts)
   (svg-xlink "phone-ringing")])

(defn mail-envelope [opts]
  [:svg (merge {:class "stroke-dark-gray" :style {:width "30px" :height "30px"}} opts)
   (svg-xlink "closed-mail-envelope")])

(defn message-bubble [opts]
  [:svg (merge {:class "stroke-dark-gray" :style {:width "30px" :height "30px"}} opts)
   (svg-xlink "message-bubble")])

;;

(defn circled-check [svg-options]
  [:svg svg-options (svg-xlink "check")])

(defn bag [opts]
  [:svg (merge {:style {:width "25px" :height "28px"}} opts)
   (svg-xlink "bag")])

(def counter-inc
  (component/html
   [:svg {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}}
    (svg-xlink "counter-inc")]))

(defn close-x [{:keys [class]}]
  (component/html
   [:svg.rotate-45 {:class class :style {:width "1.2em" :height "1.2em"}}
    (svg-xlink "counter-inc")]))

(defn simple-x [opts]
  [:svg opts (svg-xlink "simple-x")])

(def counter-dec
  (component/html
   [:svg {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}}
    (svg-xlink "counter-dec")]))

;; Social
(def instagram
  (component/html
   [:svg.container-size
    (svg-xlink "instagram")]))

(def facebook-f
  (component/html
   [:svg.container-size
    (svg-xlink "facebook-f")]))

(def pinterest
  (component/html
   [:svg.container-size
    (svg-xlink "pinterest")]))

(def twitter
  (component/html
   [:svg.container-size
    (svg-xlink "twitter")]))

(def styleseat
  (component/html
   [:svg.container-size
    (svg-xlink "styleseat")]))

(def social-icon
  {"instagram" instagram
   "facebook"  facebook-f
   "pinterest" pinterest
   "twitter"   twitter
   "styleseat" styleseat})

;; Footer

(defn ^:private mayvenn-on-social [title xlink]
  (let [title-id (str "social-title-" xlink)]
    (component/html
     [:svg.container-size {:class "fill-dark-gray" :role "img" :aria-labelledby title-id}
      [:title {:id title-id} title]
      (svg-xlink {:role "presentation"} xlink)])))

(def mayvenn-on-facebook
  (mayvenn-on-social "Follow Mayvenn on Facebook" "facebook-f"))

(def mayvenn-on-instagram
  (mayvenn-on-social "Follow Mayvenn on Instagram" "instagram"))

(def mayvenn-on-twitter
  (mayvenn-on-social "Follow Mayvenn on Twitter" "twitter"))

(def mayvenn-on-pinterest
  (mayvenn-on-social "Follow Mayvenn on Pinterest" "pinterest"))

;;

(defn missing-portrait [svg-options]
  [:svg svg-options
   (svg-xlink "mayvenn-wave")])

(def play-video
  (component/html
   [:svg {:class "fill-teal" :style {:width "64px" :height "64px"}}
    (svg-xlink "play-video")]))

(def play-video-muted
  (component/html
   [:svg {:class "fill-dark-gray" :style {:width "64px" :height "64px" :fill-opacity "0.6"}}
    (svg-xlink "play-video")]))

(def guarantee
  (component/html
   [:svg {:class "fill-teal" :height "5em"}
    (svg-xlink "guarantee")]))
