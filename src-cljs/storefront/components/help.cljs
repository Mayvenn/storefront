(ns storefront.components.help
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]))

(defn display-sms [number]
  (->> number
       (re-find #"(\d{3})(\d{3})(\d{4})")
       rest
       (string/join "-")))

(defn help-component [data owner]
  (om/component
   (html
    [:div.padded-container
     [:h2.header-bar-heading.left "Customer Service"]
     [:div#help-content
      [:div#help-summary
       [:div.hours-icon-container [:figure.hours-icon]]
       [:p.spaced-help.hours "HOURS: Monday - Saturday, 9-5 PST"]
       [:p.spaced-help.shipment-schedule
        "Orders placed before 11am PST ship that day"]]
      [:h4.dashboard-details-header.no-top-space "Get In Touch"]
      [:div.solid-line-divider]
      [:div#help-methods
       [:a.help-link {:href "tel://+18885627952"}
        [:div.help-method-row
         [:div.help-method-icon.call]
         [:div.help-method-details-container
          [:div.help-method-details
           [:p.help-method "Call"]
           [:p.help-method-means "1-888-562-7952"]]]]]
       [:div.solid-line-divider]
       (let [number (get-in data keypaths/sms-number)]
         [:a.help-link (when number {:href (str "sms://+1" number)})
          [:div.help-method-row
           [:div.help-method-icon.sms]
           [:div.help-method-details-container
            [:div.help-method-details
             [:p.help-method "Text"]
             [:p.help-method-means
              (if number
                (display-sms number)
                "Loading...")]]]]])
       [:div.solid-line-divider]
       [:a.help-link {:href "mailto:help@mayvenn.com"}
        [:div.help-method-row
         [:div.help-method-icon.email]
         [:div.help-method-details-container
          [:div.help-method-details
           [:p.help-method "Email"]
           [:p.help-method-means "help@mayvenn.com"]]]]]
       [:div.solid-line-divider]]]])))
