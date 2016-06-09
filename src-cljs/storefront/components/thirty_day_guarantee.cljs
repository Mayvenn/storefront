(ns storefront.components.thirty-day-guarantee
  (:require [om.core :as om]
            [sablono.core :refer-macros [html]]
            [storefront.components.ui :as ui]))

(def section :.border-bottom.border-light-silver.py2)

(defn action [title & description]
  [:.px2
   [:.medium.navy.shout title]
   (into [:div]
         (conj (vec description)
               "Just call us:"
               [:a.medium.navy.block {:href "tel:1-888-562-7952"} "1-888-562-7952"]))])

(defn thirty-day-guarantee-component [data owner]
  (om/component
   (html
    [:.sans-serif.dark-gray
     [:.bg-white.p2
      [:.img-guarantee-icon.bg-no-repeat.bg-center {:style {:height "155px"}}]]
     [:.mx-auto.px2 {:style {:max-width "768px"}}
      [:.center.dark-black
       [section "We guarantee that you'll love Mayvenn hair!"]
       [section "Buy " [:span.green.shout "RISK FREE"] " with easy returns and exchanges."]]
      [:.line-height-3
       [section
        (action "EXCHANGES"
                "Wear it, dye it, even flat iron it. "
                "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase. ")]
       [:.py2
        (action "RETURNS"
                "If you are not completely happy with your Mayvenn hair before it is installed, "
                "we will refund your purchase if the bundle is unopened and the hair is in its original condition. ")]]]])))
