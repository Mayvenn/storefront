(ns storefront.components.help
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]
            [storefront.components.ui :as ui]))

(defn display-sms [number]
  (->> number
       (re-find #"(\d{3})(\d{3})(\d{4})")
       rest
       (string/join "-")
       (str "1-")))

(defn cell-border [& body]
  (into [:.border.border-silver.rounded.px1.py2.flex.flex-column.justify-between.items-center
         {:style {:height "134px"}}]
        body))
(defn cell-icon [icon-class width height]
  [:.bg-center.bg-contain.bg-no-repeat.mb1
   {:style {:height height :width width}
    :class icon-class}])
(def cell-text :.h1.light.dark-gray)
(def cell-description :.h4.navy.medium)

(defn help-component [{:keys [sms-number]} owner]
  (om/component
   (html
    [:div.sans-serif
     [:div.py4.bg-white.center
      [:.h1.navy.mb2 "Get in touch"]
      [:.h3.dark-gray.light "Have a problem?"]
      [:.h3.dark-gray.light "Need advice on a style or product?"]
      [:.h3.dark-gray.light "Here are a few ways to get a hold of us."]]

     [:.bg-pure-white.center
      [:.mx1
       [:.border-bottom.border-width-2.border-dark-white.py2.line-height-4.mxp3
        [:.h4.dark-black "Monday to Friday from 9am to 5pm PST"]
        [:.h4.dark-black "Orders placed before 10am PST ship that day"]]]
      [:.px1.py2
       [:.col.col-6.pp3
        [:a.navy {:href "http://help.mayvenn.com" :target "_blank"}
         (cell-border
          (cell-icon "img-faqs-icon" "44px" "52px")
          [cell-text "FAQs"]
          [cell-description "Visit our help center"])]]
       [:.col.col-6.pp3
        [:a.navy {:href "tel://+18885627952"}
         (cell-border
          (cell-icon "img-phone-icon" "45px" "52px")
          [cell-text "Call"]
          [cell-description "1-888-562-7952"])]]
       [:.col.col-6.pp3
        [:a.navy (when sms-number {:href (str "sms://+1" sms-number)})
         (cell-border
          (cell-icon "img-text-icon" "54px" "52px")
          [cell-text "Text"]
          [cell-description
           (if sms-number
             (display-sms sms-number)
             "Loading...")])]]
       [:.col.col-6.pp3
        [:a.navy {:href "mailto:help@mayvenn.com"}
         (cell-border
          (cell-icon "img-email-icon" "49px" "52px")
          [cell-text "Email"]
          [cell-description "help@mayvenn.com"])]]]
      [:.clearfix.mb4]]])))
