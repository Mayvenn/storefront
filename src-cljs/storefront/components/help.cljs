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

(defn cell [link-attrs & body]
  [:a.dark-gray.btn.btn-outline.border-silver.pxp6.py2.sm-m2.m1
   (merge {:style {:height "134px" :width "142px"}}
          link-attrs)
   (into
    [:.flex.flex-column.justify-between.items-center]
    body)])

(defn cell-icon [icon-class width height]
  [:.bg-center.bg-contain.bg-no-repeat.mb1
   {:style {:height height :width width}
    :class icon-class}])
(def cell-text :.f1.light.dark-gray)
(def cell-description :.f4.navy.medium)

(defn help-component [{:keys [sms-number]} owner]
  (om/component
   (html
    [:div.sans-serif
     [:div.py4.bg-white.center
      [:.h1.navy.mb2 "Get in touch"]
      [:.h3.dark-gray.light
       [:div "Have a problem?"]
       [:div "Need advice on a style or product?"]
       [:div "Here are a few ways to get a hold of us."]]]

     [:.m2.center
      [:.border-bottom.border-light-silver.py2.line-height-4
       [:div "Monday to Friday from 9am to 5pm PST"]
       [:div "Orders placed before 10am PST ship that day"]]
      [:.my2.flex.flex-wrap.justify-center
       [:.flex
        (cell
         {:href "http://help.mayvenn.com"
          :target "_blank"}
         (cell-icon "img-faqs-icon" "44px" "52px")
         [cell-text "FAQs"]
         [cell-description "Visit our help center"])
        (cell
         {:href "tel://+18885627952"}
         (cell-icon "img-phone-icon" "45px" "52px")
         [cell-text "Call"]
         [cell-description "1-888-562-7952"])]
       [:.flex
        (cell
         (when sms-number {:href (str "sms://+1" sms-number)})
         (cell-icon "img-text-icon" "54px" "52px")
         [cell-text "Text"]
         [cell-description
          (if sms-number
            (display-sms sms-number)
            "Loading...")])
        (cell
         {:href "mailto:help@mayvenn.com"}
         (cell-icon "img-email-icon" "49px" "52px")
         [cell-text "Email"]
         [cell-description
          [:span {:style {:word-break "break-all"}} "help@mayvenn.com"]])]]]])))
