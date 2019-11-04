(ns storefront.components.footer-links
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [spice.date :as date]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]

            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defcomponent component [{:keys [minimal?]} owner opts]
  [:div.center
   (when-not minimal?
     [:span {:key "full"}
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
   [:span {:key "standard"}
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
      [:span {:key "minimal"}
       " - ©" (date/year (date/now)) " " "Mayvenn"])]])

(defn query
  [data]
  {:minimal? (nav/show-minimal-footer? (get-in data keypaths/navigation-event))})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
