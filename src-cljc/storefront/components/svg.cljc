(ns storefront.components.svg
  (:require [spice.maps :as maps]
            [storefront.component :as component]))

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

(defn question-circle [opts]
  (component/html
   [:svg opts
    (svg-xlink "question-circle")]))

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

(def no-expenses
  (component/html
   [:svg {:class "stroke-teal" :style {:width "70px" :height "70px" :stroke-width "2"}}
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

(defn bag-flyout [opts]
  (component/html
   [:svg (merge {:style {:width "35px" :height "33px"}} opts)
    (svg-xlink "bag-flyout")]))

(defn counter-inc
  ([] (counter-inc {}))
  ([opts]
   [:svg (maps/deep-merge {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}} opts)
    (svg-xlink "counter-inc")]))

(defn counter-dec
  ([] (counter-dec {}))
  ([opts]
   [:svg (maps/deep-merge {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}} opts)
    (svg-xlink "counter-dec")]))

(defn close-x [{:keys [class]}]
  (component/html
   [:svg.rotate-45 {:class class :style {:width "1.2em" :height "1.2em"}}
    (svg-xlink "counter-inc")]))

(defn simple-x [opts]
  [:svg opts (svg-xlink "simple-x")])

(def open-hamburger-menu
  (component/html
   [:svg {:class "fill-dark-gray" :style {:width "30px" :height "30px"}}
    (svg-xlink "open-hamburger-menu")]))

(def close-hamburger-menu
  (component/html
   [:svg {:class "fill-dark-gray" :style {:width "30px" :height "30px"}}
    (svg-xlink "close-hamburger-menu")]))

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

(defn guarantee [opts]
  (component/html
   [:svg opts
    (svg-xlink "guarantee")]))

(defn number-circle [number-kw]
  (component/html
   [:svg {:class "fill-teal bold"
          :style {:width     "74px"
                  :height    "74px"
                  :font-size "35px"}}
    (svg-xlink
     (case number-kw
       :1 "number-circle-1"
       :2 "number-circle-2"
       :3 "number-circle-3"))]))

(defn number-circle-with-white-border [number-kw]
  (component/html
   [:div.relative {:style {:left "-37px"}}
    [:svg.absolute {:class "fill-white bold"
                    :style {:width     "80px"
                            :height    "80px"
                            :font-size "35px"}}
     (svg-xlink "number-circle-1")]
    [:svg.absolute {:class "fill-teal bold"
                    :style {:width       "74px"
                            :height      "74px"
                            :font-size   "35px"
                            :margin-left "3px"
                            :margin-top  "3px"}}
     (svg-xlink
      (case number-kw
        :1 "number-circle-1"
        :2 "number-circle-2"
        :3 "number-circle-3"))]]))

(defn minus-sign [opts]
  (component/html
   [:svg opts
    (svg-xlink "minus-sign")]))

(defn plus-sign [opts]
  (component/html
   [:svg opts
    (svg-xlink "plus-sign")]))

(defn trash-can [opts]
  (component/html
   [:svg opts
    (svg-xlink "trash-can")]))

(defn discount-tag [opts]
  (component/html
   [:svg opts
    (svg-xlink "discount-tag")]))

(defn share-arrow [opts]
  (component/html
   [:svg opts
    (svg-xlink "share-arrow")]))

(defn celebration-horn [opts]
  (component/html
   [:svg opts
    (svg-xlink "celebration-horn")]))

(defn coin-stack [opts]
  (component/html
   [:svg opts
    (svg-xlink "coin-stack")]))

(defn certified-ribbon [opts]
  (component/html
   [:svg opts
    (svg-xlink "certified-ribbon")]))

(defn icon-sms [opts]
  (component/html
   [:svg opts
    (svg-xlink "icon-sms")]))

(defn icon-call [opts]
  (component/html
   [:svg opts
    (svg-xlink "icon-call")]))

(defn icon-email [opts]
  (component/html
   [:svg opts
    (svg-xlink "icon-email")]))
