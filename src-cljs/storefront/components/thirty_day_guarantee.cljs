(ns storefront.components.thirty-day-guarantee
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn thirty-day-guarantee-component [data owner]
  (om/component
   (html
    [:div
     [:div.guarantee-header
      [:img.guarantee-header-image {:src "images/30_day_header.png"}]]
     [:div.guarantee-info-banner
      "Buy "
      [:em "risk free"]
      " with easy returns and exchanges.\n"]
     [:div.guarantee-content
      [:div.guarantee-item
       [:h3 "Exchanges"]
       [:p
        "Wear it, dye it, even flat iron it.  If you do not love your Mayvenn hair we will exchange it within 30 days of purchase.  Just call us: "
        [:a {:href "tel:1-888-562-7952"} "1-888-562-7952"]]]
      [:div.guarantee-item
       [:h3 "Returns"]
       [:p
        "If you are not completely happy with your Mayvenn hair before it is installed, we will refund your purchase if the bundle is unopened and the hair is in its original condition.  Just call us: "
        [:a {:href "tel:1-888-562-7952"} "1-888-562-7952"]]]]])))
