(ns storefront.components.svg
  (:require [spice.maps :as maps]
            [storefront.component :as component :refer [defcomponent]]))

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
  ([opts id] (component/html [:g [:use {:xlinkHref (str "#" id)}]])))

(defn error [opts]
  (component/html
   [:svg opts (svg-xlink "circled-exclamation")]))

(defn angle-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "angle-arrow")]))

(defn dropdown-arrow [opts]
  (component/html
   [:svg (maps/deep-merge {:style {:stroke-width "3"}} opts)
    ^:inline (svg-xlink "dropdown-arrow")]))

(defn left-caret [opts]
  (component/html
   [:svg.mtp1 opts
    ^:inline (svg-xlink "left-caret")]))

(defn thick-left-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "thick-left-arrow")]))

(defn left-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "left-arrow")]))

;; Stylist Dashboard

(defn box-package [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "box-package")]))

;; Help

(defn phone-ringing [opts]
  (component/html
   [:svg (merge {:class "stroke-black" :style {:width "30px" :height "30px"}} opts)
    ^:inline (svg-xlink "phone-ringing")]))

(defn mail-envelope [opts]
  (component/html
   [:svg (merge {:class "stroke-black" :style {:width "30px" :height "30px"}} opts)
    ^:inline (svg-xlink "closed-mail-envelope")]))

(defn message-bubble [opts]
  (component/html
   [:svg (merge {:class "stroke-black" :style {:width "30px" :height "30px"}} opts)
    ^:inline (svg-xlink "message-bubble")]))

;;

(defn circled-check [opts]
  (component/html
   [:svg
    (maps/deep-merge {:style {:stroke-width "0.5"}} opts)
    ^:inline (svg-xlink "circled-check")]))

(defn check [opts]
  (component/html
   [:svg
    (maps/deep-merge {:style {:stroke-width "0.5"}} opts)
    ^:inline (svg-xlink "check")]))

(defn bag [opts]
  (component/html
   [:svg (merge {:style {:width "25px" :height "28px"}} opts)
    ^:inline (svg-xlink "bag")]))

(defn counter-inc
  ([] (counter-inc {}))
  ([opts]
   (component/html
    [:svg (maps/deep-merge {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}} opts)
     ^:inline (svg-xlink "counter-inc")])))

(defn counter-dec
  ([] (counter-dec {}))
  ([opts]
   (component/html
    [:svg (maps/deep-merge {:class "stroke-white fill-gray" :style {:width "1.2em" :height "1.2em"}} opts)
     ^:inline (svg-xlink "counter-dec")])))

;; NOTE: This is our new "x" with sharp edges for the new branding. The "simple-x" is too round
(defn x-sharp [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "x-sharp")]))

(defn close-x [{:keys [class height width]}]
  (component/html
   [:svg.rotate-45 {:class class :style {:width (or width "1.2em") :height (or height "1.2em")}}
    ^:inline (svg-xlink "counter-inc")]))

;; NOTE (deprecated): Let's not use this rounded-cornered x. See x-sharp above ^^^.
(defn simple-x [opts]
  (component/html
   [:svg opts ^:inline (svg-xlink "simple-x")]))

(defn quadpay-logo
  ([] (quadpay-logo {}))
  ([opts]
   (component/html
    [:svg.container-size
     opts
     ^:inline (svg-xlink "quadpay-logo")])))

;; Social
(defn instagram
  ([] (instagram {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "instagram")])))

(defn facebook-f []
  (component/html
   [:svg.container-size
    ^:inline (svg-xlink "facebook-f")]))

(defn pinterest []
  (component/html
   [:svg.container-size
    ^:inline (svg-xlink "pinterest")]))

(defn twitter []
  (component/html
   [:svg.container-size
    ^:inline (svg-xlink "twitter")]))

(defn styleseat
  ([] (styleseat {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "styleseat")])))

(def social-icon
  {"instagram" instagram
   "desktop"   instagram
   "facebook"  facebook-f
   "pinterest" pinterest
   "twitter"   twitter
   "styleseat" styleseat})

;; Footer

(defn ^:private mayvenn-on-social [title xlink]
  (let [title-id (str "social-title-" xlink)]
    (component/html
     [:svg.container-size {:class "fill-white" :role "img" :aria-labelledby title-id}
      [:title {:id title-id} title]
      ^:inline (svg-xlink {:role "presentation"} xlink)])))

(defn mayvenn-on-facebook []
  (mayvenn-on-social "Follow Mayvenn on Facebook" "facebook-f"))

(defn mayvenn-on-instagram []
  (mayvenn-on-social "Follow Mayvenn on Instagram" "instagram"))

(defn mayvenn-on-twitter []
  (mayvenn-on-social "Follow Mayvenn on Twitter" "twitter"))

(defn mayvenn-on-pinterest []
  (mayvenn-on-social "Follow Mayvenn on Pinterest" "pinterest"))

(defn white-play-video [opts]
  (component/html
   [:svg opts ^:inline (svg-xlink "white-play-video")]))

(defn minus-sign [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "minus-sign")]))

(defn plus-sign [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "plus-sign")]))

(defn trash-can [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "trash-can")]))

(defn consolidated-trash-can [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "consolidated-trash-can")]))

(defn discount-tag [opts]
  (component/html
   [:svg.fill-s-color opts
    ^:inline (svg-xlink "discount-tag")]))

(defn share-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "share-arrow")]))

(defn celebration-horn [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "celebration-horn")]))

(defn coin-in-slot [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "coin-in-slot")]))

(defn stack-o-cash [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "stack-o-cash")]))

(defn icon-sms [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "icon-sms")]))

(defn icon-call [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "icon-call")]))

(defn icon-email [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "icon-email")]))

(defn phone [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "phone")]))

(defn position [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "position")]))

(defn info [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "info")]))

(defn lock [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "lock")]))

(defn swap-person [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "swap-person")]))

(defn forward-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "forward-arrow")]))

(defn empty-star [opts]
  (component/html
   [:svg (maps/deep-merge {:class "fill-s-color"} opts)
    ^:inline (svg-xlink "empty-star")]))

(defn half-star [opts]
  (component/html
   [:svg (maps/deep-merge {:class "fill-s-color"} opts)
    ^:inline (svg-xlink "half-star")]))

(defn three-quarter-star [opts]
  (component/html
   [:svg (maps/deep-merge {:class "fill-s-color"} opts)
    ^:inline (svg-xlink "three-quarter-star")]))

(defn whole-star [opts]
  (component/html
   [:svg (maps/deep-merge {:class "fill-s-color"} opts)
    ^:inline (svg-xlink "whole-star")]))

(defn chat-bubble [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "chat-bubble")]))

(defn share-icon [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "share-icon")]))

(defn straight-line [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "straight-line")]))

(defn quotation-mark [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "quotation-mark")]))

(defn shopping-bag [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "shopping-bag")]))

(defn alert-icon [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "alert-icon")]))

(defn heart [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "heart")]))

(defn calendar [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "calendar")]))

(defn worry-free [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "worry-free")]))

(defn mirror [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "mirror")]))

(defn check-mark [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "check-mark")]))

(defn mayvenn-logo [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "mayvenn-logo")]))

(defn mayvenn-text-logo [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "mayvenn-text-logo")]))

(defn play-video [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "play-video")]))

(defn vertical-squiggle [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "vertical-squiggle")]))

(defn map-pin [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "map-pin")]))

(defn qr-code-icon [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "qr-code-icon")]))

(defn diamond-check [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "diamond-check")]))

(defn button-facebook-f [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "button-facebook-f")]))

(defn magnifying-glass [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "magnifying-glass")]))

(defn funnel [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "funnel")]))

(defn symbolic->html
  "Converts a data from query that describes an svg to the appropriate html.

  Query data is in the form: [:type options]
  "
  [[kind attrs]]
  (component/html
   (if kind
     (case kind
       :svg/close-x      ^:inline (close-x attrs)
       :svg/discount-tag ^:inline (discount-tag attrs))
     [:div])))
