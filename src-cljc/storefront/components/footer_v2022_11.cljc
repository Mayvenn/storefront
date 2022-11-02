(ns storefront.components.footer-v2022-11
  (:require [spice.date :as date]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as c]
            [storefront.components.footer-links :as footer-links]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [mayvenn.visual.tools :as vt]
            [homepage.ui.email-capture :as email-capture]))

(c/defcomponent faq-accordion
  [{:keys [] :as data} _ _]
  [:div "faq"])

(defn- social-link
  ([uri icon] (social-link {:height "20px" :width "20px"} uri icon))
  ([{:keys [height width]} uri icon]
   (c/html
    ;; https://web.dev/external-anchors-use-rel-noopener/
    [:a.block.mr4 {:href uri :rel "noopener" :target "_blank"}
     [:div {:style {:width width :height height}}
      ^:inline icon]])))

(c/defcomponent social-media-block
  [_ _ _]
  [:div.px4.py3.flex.items-center
   ^:inline (social-link {:height "28px" :width "28px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter {:class "fill-p-color"}))
   ^:inline (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram {:class "fill-p-color"}))
   ^:inline (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook {:class "fill-p-color"}))
   ^:inline (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest {:class "fill-p-color"}))])

(c/defcomponent essence-block
  [{:keys [copy]} owner opts]
  [:div.px4.pb3.proxima.content-4.dark-gray copy])

(defn- underfoot-link [opts label]
  (c/html [:a.block.inherit-color.my2 opts label]))

(c/defcomponent underfoot
  [{:keys [] :as data} owner opts]
  [:div.white.bg-black.px4.py5.proxima.content-4
   ^:inline (svg/mayvenn-text-logo {:height "29px"
                                    :width  "115px"
                                    :class  "fill-white"})
   [:div.flex.justify-between.my3
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-privacy)
                                    :data-test "content-privacy") "Privacy")
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#ca-privacy-rights")}
                             "CA Privacy Rights")
    ^:inline (underfoot-link (assoc (utils/route-to e/navigate-content-tos)
                                    :data-test "content-tos") "Terms")
    ;; use traditional page load so anchors work
    ^:inline (underfoot-link {:href (str (routes/path-for e/navigate-content-privacy) "#our-ads")} "Our Ads")]
   [:div.flex.items-center {:key "minimal"}
    "©" (date/year (date/now)) " " "Mayvenn"]])

(defn query
  [app-state]
  (merge
   (let [textfield-keypath k/footer-email-value
         email             (get-in app-state textfield-keypath)
         submitted?        (get-in app-state k/footer-email-submitted)]
     (vt/within :email-capture
                {:submit/target             [e/control-footer-email-submit {:email email}]
                 :text-field/id             "footer-email-capture-input"
                 :text-field/placeholder    "Enter your Email"
                 :text-field/focused        (get-in app-state k/ui-focus)
                 :text-field/keypath        textfield-keypath
                 :text-field/errors         (get-in app-state (conj k/field-errors ["email"]))
                 :text-field/email          email
                 :text-field/submitted-text (when submitted? "Thank you for subscribing.")}))
   {:essence-block/copy "Included is a one year subscription to ESSENCE Magazine - a $10 value! Offer and refund details will be included with your confirmation."}
   #:underfoot{})) ; GROT?

(c/defcomponent component
  [{:keys [] :as data} owner opts]
  [:div
   (c/build email-capture/organism data) ; TODO: use with/within to avoid rerenders?
   (c/build layered/lp-divider-purple-pink)
   (c/build faq-accordion)
   (c/build social-media-block)
   (c/build essence-block (vt/with :essence-block data))
   (c/build underfoot (vt/with :underfoot data) opts)])
