(ns storefront.components.footer-links
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [spice.date :as date]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

(defn template [{:keys [minimal?]} opts]
  (component/html
   [:div.center
    (when-not minimal?
      [:span
       [:a.inherit-color
        (utils/route-to events/navigate-content-about-us) "About"]
       " - "
       [:span
        [:a.inherit-color {:href "https://jobs.mayvenn.com"}
         "Careers"]
        " - "]
       [:a.inherit-color
        (utils/route-to events/navigate-content-help) "Contact"]
       " - "])
    [:a.inherit-color
     (assoc (utils/route-to events/navigate-content-privacy)
            :data-test "content-privacy") "Privacy"]
    " - "
    [:a.inherit-color
     ;; use traditional page load so anchors work
     {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
     "CA Privacy Rights"]
    " - "
    [:a.inherit-color (assoc (utils/route-to events/navigate-content-tos)
                             :data-test "content-tos") "Terms"]
    " - "
    [:a.inherit-color
     ;; use traditional page load so anchors work
     {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")}
     "Our Ads"]
    (when-not minimal?
      [:span
       " - "
       [:span
        {:item-prop "name"
         :content "Mayvenn Hair"}
        " ©" (date/year (date/now)) " " "Mayvenn"]])]))

(defn query
  [data]
  {:minimal? (nav/show-minimal-footer? (get-in data keypaths/navigation-event))})
