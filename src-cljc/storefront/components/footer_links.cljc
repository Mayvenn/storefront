(ns storefront.components.footer-links
  (:require [spice.date :as date]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

(defn- footer-link [opts label]
  (component/html [:a.block.inherit-color.my2 opts label]))

(defn- social-link
  ([uri icon] (social-link {:height "18px" :width "18px"} uri icon))
  ([{:keys [height width]} uri icon]
   (component/html
    [:a.block.px1.mx1.flex.items-center {:href uri :target "_blank"}
     [:div {:style {:width width :height height}}
      ^:inline icon]])))

(defn- minimal-footer-link [opts label]
  (component/html [:a.inherit-color ^:attrs opts ^:inline label]))

(defcomponent component [{:keys [minimal?]} owner opts]
  (if minimal?
    [:div.content-3.proxima.center
     ^:inline (minimal-footer-link (assoc (utils/route-to events/navigate-content-privacy)
                                          :data-test "content-privacy") "Privacy")
     " - "
     ^:inline (minimal-footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                                   "CA Privacy Rights")
     " - "
     ^:inline (minimal-footer-link (assoc (utils/route-to events/navigate-content-tos)
                                          :data-test "content-tos") "Terms")
     " - "
     ;; use traditional page load so anchors work
     ^:inline (minimal-footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
    [:div.flex-column.content-3.white.bg-black.pt6
     [:div.px3.container
      [:div.flex.justify-between
       ^:inline (svg/mayvenn-text-logo {:height "29px"
                                        :width  "115px"
                                        :class  "fill-white"})
       [:div.flex.items-center
        ^:inline (social-link {:height "24px" :width "24px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter))
        ^:inline (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram))
        ^:inline (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook))
        ^:inline (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest))]]
      [:div.flex.mt4.mb3.col-5-on-dt
       [:div.col-4 {:key "full"}
        ^:inline (footer-link (utils/route-to events/navigate-content-about-us) "About")
        ^:inline (footer-link {:href "https://jobs.mayvenn.com"} "Careers")
        ^:inline (footer-link (utils/route-to events/navigate-content-help) "Contact")]
       [:div.col-4 {:key "standard"}
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-privacy)
                                     :data-test "content-privacy") "Privacy")
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-tos)
                                     :data-test "content-tos") "Terms")

        ;; use traditional page load so anchors work
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
       [:div.col-4
        ;; use traditional page load so anchors work
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                              "CA Privacy Rights")]]]
     [:div.bg-gray-mask
      [:div.px3.container
       [:span.py2.flex.items-center.gray {:key "minimal"}
        "©" (date/year (date/now)) " " "Mayvenn"]]]]))

(defn query
  [data]
  {:minimal? (nav/show-minimal-footer? (get-in data keypaths/navigation-event))})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
