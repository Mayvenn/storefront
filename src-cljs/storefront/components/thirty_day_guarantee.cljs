(ns storefront.components.thirty-day-guarantee
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn thirty-day-guarantee-component [data owner]
  (om/component
   (html
    [:div.sans-serif.dark-gray
     [:div.bg-white.p2
      [:div.img-guarantee-icon.bg-no-repeat.bg-center {:style {:height "155px"}}]]
     [:div.center.px2.dark-black
      [:div.border-bottom.border-dark-white.border-width-2.py2
       "We guarantee that you'll love Mayvenn hair!"]
      [:div.border-bottom.border-dark-white.border-width-2.py2
       "Buy " [:div.green.inline "RISK FREE"] " with easy returns and exchanges."]]
     [:div.line-height-3.px2
      [:div.border-bottom.border-dark-white.border-width-2.py2.px2
       [:.medium.navy "EXCHANGES"]
       [:div "Wear it, dye it, even flat iron it. "
        "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase. "
        "Just call us:"
        [:a.medium.navy.block {:href "tel:1-888-562-7952"} "1-888-562-7952"]]]
      [:div.border-bottom.border-dark-white.border-width-2.py2.px2
       [:.medium.navy "RETURNS"]
       "If you are not completely happy with your Mayvenn hair before it is installed, "
       "we will refund your purchase if the bundle is unopened and the hair is in its original condition. "
       "Just call us:"
        [:a.medium.navy.block {:href "tel:1-888-562-7952"} "1-888-562-7952"]]]])))
