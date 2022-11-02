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
            [mayvenn.visual.tools :as vt]))

(c/defcomponent faq-accordion
  [{:keys [] :as data} _ _]
  [:div "faq"])

(c/defcomponent social-media-block
  [{:keys [] :as data} _ _]
  [:div "social"])

(defn- footer-link [opts label]
  (c/html [:a.block.inherit-color.my2 opts label]))

(c/defcomponent underfoot
  [{:keys [] :as data} owner opts]
  [:div.white.bg-black
   [:div.container.p6
    ^:inline (svg/mayvenn-text-logo {:height "29px"
                                     :width  "115px"
                                     :class  "fill-white"})
    [:div.flex.justify-between
     ^:inline (footer-link (assoc (utils/route-to e/navigate-content-privacy)
                                  :data-test "content-privacy") "Privacy")
     ^:inline (footer-link {:href (str (routes/path-for e/navigate-content-privacy) "#ca-privacy-rights")}
                           "CA Privacy Rights")
     ^:inline (footer-link (assoc (utils/route-to e/navigate-content-tos)
                                  :data-test "content-tos") "Terms")
     ;; use traditional page load so anchors work
     ^:inline (footer-link {:href (str (routes/path-for e/navigate-content-privacy) "#our-ads")} "Our Ads")
     ]
    [:div.px3.container.py2.flex.items-center.white {:key "minimal"}
     "©" (date/year (date/now)) " " "Mayvenn"]]])

(defn query
  [app-state]
  (merge
   #:footer-links{:minimal-footer?                (nav/show-minimal-footer? (get-in app-state k/navigation-event))
                  :footer-email-input-value       (get-in app-state k/footer-email-value)
                  :footer-email-submitted?        (get-in app-state k/footer-email-submitted)
                  :footer-ready-for-email-signup? (get-in app-state k/footer-email-ready)}))

(c/defcomponent component
  [{:keys [] :as data} owner opts]
  [:div
   (c/build layered/lp-divider-purple-pink)
   (c/build faq-accordion)
   (c/build social-media-block)
   (c/build underfoot (vt/with :footer-links data) opts)])
