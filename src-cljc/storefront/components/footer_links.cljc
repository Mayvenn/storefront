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
  [:a.block.inherit-color.my2 opts label])

(defn- social-link
  ([uri icon] (social-link {:height "18px" :width "18px"} uri icon))
  ([{:keys [height width]} uri icon]
   [:a.block.px1.mx1.flex.items-center {:href uri}
    [:div {:style {:width width :height height}}
     ^:inline icon]]))

(defcomponent component [{:keys [minimal?]} owner opts]
  [:div.flex-column.content-3.white.bg-black.pt6
   [:div.px3.container
    [:div.flex.justify-between
     (svg/mayvenn-text-logo {:height "29px"
                             :width  "115px"
                             :class  "fill-white"})
     [:div.flex.items-center
      (social-link {:height "24px" :width "24px"} "https://twitter.com/MayvennHair" (svg/mayvenn-on-twitter))
      (social-link "http://instagram.com/mayvennhair" (svg/mayvenn-on-instagram))
      (social-link "https://www.facebook.com/MayvennHair" (svg/mayvenn-on-facebook))
      (social-link "http://www.pinterest.com/mayvennhair/" (svg/mayvenn-on-pinterest))]]
    [:div.flex.mt4.mb3.col-5-on-dt
     (when-not minimal?
       [:div.col-4 {:key "full"}
        (footer-link (utils/route-to events/navigate-content-about-us) "About")
        (footer-link {:href "https://jobs.mayvenn.com"} "Careers")
        (footer-link (utils/route-to events/navigate-content-help) "Contact")])
     [:div.col-4 {:key "standard"}
      (footer-link (assoc (utils/route-to events/navigate-content-privacy)
                          :data-test "content-privacy") "Privacy")
      (footer-link (assoc (utils/route-to events/navigate-content-tos)
                          :data-test "content-tos") "Terms")

      ;; use traditional page load so anchors work
      (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
     [:div.col-4
      ;; use traditional page load so anchors work
      (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                   "CA Privacy Rights")]]]
   (when-not minimal?
     [:div.bg-gray-mask
      [:div.px3.container
       [:span.py2.flex.items-center.gray {:key "minimal"}
        "©" (date/year (date/now)) " " "Mayvenn"]]])])

(defn query
  [data]
  {:minimal? (nav/show-minimal-footer? (get-in data keypaths/navigation-event))})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
