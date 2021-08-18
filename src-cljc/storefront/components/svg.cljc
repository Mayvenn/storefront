(ns storefront.components.svg
  (:require [spice.maps :as maps]
            [storefront.component :as component :refer [defcomponent]]
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

(defn x-sharp [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "x-sharp")]))

(defn close-x [{:keys [class height width]}]
  (component/html
   [:svg.rotate-45 {:class class :style {:width (or width "1.2em") :height (or height "1.2em")}}
    ^:inline (svg-xlink "counter-inc")]))

;; NOTE(jeff): was an SVG, but upgrading to zip logo is a PNG.
;;             longer term, we should move away from SVG sprites and into individual images (because of http/2)
(defn quadpay-logo
  ([] (quadpay-logo nil))
  ([opts]
   (component/html
    [:img
     (merge
     {:src    "//ucarecdn.com/6ed38929-a47b-4e45-bd03-1e955dc831de/-/format/auto/-/resize/76x28/zip"
      :width  "38"
      :height "14"
      :alt    "zip logo"} 
      opts)])))

;; Social
(defn instagram
  ([] (instagram {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "instagram")])))

(defn facebook-f
  ([] (facebook-f {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "facebook-f")])))

(defn pinterest
  ([] (pinterest {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "pinterest")])))

(defn twitter
  ([] (twitter {}))
  ([opts]
   (component/html
    [:svg.container-size (maps/deep-merge {:class "fill-black"} opts)
     ^:inline (svg-xlink "twitter")])))

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
     [:svg.container-size {:class "fill-black" :role "img" :aria-labelledby title-id}
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

(defn line-item-delete [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "line-item-delete")]))

(defn discount-tag [opts]
  (component/html
   [:svg.fill-s-color opts
    ^:inline (svg-xlink "discount-tag")]))

(defn share-arrow [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "share-arrow")]))

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
    ^:inline (svg-xlink "info-outlined")]))

(defn info-filled [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "info-filled")]))

(defn info-color-circle [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "info-color-circle")]))

(defn lock [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "lock")]))

(defn stylist-lock [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "stylist-lock")]))

(defn swap-person [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "swap-person")]))

(defn swap-arrows [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "swap-arrows")]))

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

(defn purple-diamond [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "purple-diamond")]))

(defn white-diamond [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "white-diamond")]))

(defn pink-bang [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "pink-bang")]))

(defn shipping [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "shipping")]))

(defn experience-badge [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "experience-badge")]))

(defn description [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "description")]))

(defn shaded-shipping-package [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "shaded-shipping-package")]))

(defn customer-service-representative [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "customer-service-representative")]))

(defn exclamation-circle [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "exclamation-circle")]))

;; TODO: Find a way to override particular portions of the svg's fill currently
;; we are only able to override the larget background area and not the inner
;; diamonds
(defn chat-bubble-diamonds [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "chat-bubble-diamonds")]))
(defn chat-bubble-diamonds-p-color [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "chat-bubble-diamonds-p-color")]))

(defn crown [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "crown")]))

(defn certified [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "certified")]))

(defn edit
  "A pencil denoting an edit action"
  [opts]
  (component/html
   [:svg opts
    ^:inline (svg-xlink "edit")]))

(defn symbolic->html
  "Converts a data from query that describes an svg to the appropriate html.

  Query data is in the form: [:type options]
  "
  [[kind attrs]]
  (component/html
   (if kind
     (case kind
       :svg/play-video                      ^:inline (play-video attrs)
       :svg/calendar                        ^:inline (calendar attrs)
       :svg/check-mark                      ^:inline (check-mark attrs)
       :svg/close-x                         ^:inline (close-x attrs)
       :svg/customer-service-representative ^:inline (customer-service-representative attrs)
       :svg/discount-tag                    ^:inline (discount-tag attrs)
       :svg/heart                           ^:inline (heart attrs)
       :svg/icon-call                       ^:inline (icon-call attrs)
       :svg/icon-email                      ^:inline (icon-email attrs)
       :svg/icon-sms                        ^:inline (icon-sms attrs)
       :svg/mirror                          ^:inline (mirror attrs)
       :svg/shaded-shipping-package         ^:inline (shaded-shipping-package attrs)
       :svg/worry-free                      ^:inline (worry-free attrs)
       :svg/chat-bubble-diamonds            ^:inline (chat-bubble-diamonds attrs)
       :svg/chat-bubble-diamonds-p-color    ^:inline (chat-bubble-diamonds-p-color attrs)
       :svg/crown                           ^:inline (crown attrs)
       :svg/experience-badge                ^:inline (experience-badge attrs)
       :svg/certified                       ^:inline (certified attrs)
       :svg/mayvenn-logo                    ^:inline (mayvenn-logo attrs)
       :svg/edit                            ^:inline (edit attrs)
       [:div])
     [:div])))
