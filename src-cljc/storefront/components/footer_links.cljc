(ns storefront.components.footer-links
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.date :as date]
            [storefront.routes :as routes]))

(defn component [{:keys [minimal?]} owner opts]
  (component/create
   (into [:div.center]
         (concat
          (when-not minimal?
            [[:a.inherit-color
              (utils/route-to events/navigate-content-about-us) "About"]
             " - "
             [:span
              [:a.inherit-color {:href "https://jobs.mayvenn.com"}
               "Careers"]
              " - "]
             [:a.inherit-color
              (utils/route-to events/navigate-content-help) "Contact"]
             " - "])
          [[:a.inherit-color
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
             " - "
             [:span
              {:item-prop "name"
               :content "Mayvenn Hair"}
              " ©" (date/full-year (date/current-date)) " " "Mayvenn"])]))) )

(defn query
  [data]
  {:minimal? (nav/show-minimal-footer? (get-in data keypaths/navigation-event))})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
