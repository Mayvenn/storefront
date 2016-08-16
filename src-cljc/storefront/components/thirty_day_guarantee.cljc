(ns storefront.components.thirty-day-guarantee
  (:require [storefront.components.ui :as ui]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])))

(def section :div.border-bottom.border-light-silver.py2)

(defn action [title & description]
  [:div.px2
   [:div.medium.navy.shout title]
   (into [:div]
         (conj (vec description)
               "Just call us:"
               [:a.medium.navy.block {:href "tel:1-888-562-7952"} "1-888-562-7952"]))])

(def content
  (component/html
   [:div.dark-gray
    [:div.bg-white.p2
     [:div.img-guarantee-icon.bg-no-repeat.bg-center {:style {:height "155px"}}]]
    [:div.mx-auto.px2 {:style {:max-width "768px"}}
     [:div.center.dark-black
      [section "We guarantee that you'll love Mayvenn hair!"]
      [section "Buy " [:span.green.shout "RISK FREE"] " with easy returns and exchanges."]]
     [:div.line-height-3
      [section
       (action "EXCHANGES"
               "Wear it, dye it, even flat iron it. "
               "If you do not love your Mayvenn hair we will exchange it within 30 days of purchase. ")]
      [:div.py2
       (action "RETURNS"
               "If you are not completely happy with your Mayvenn hair before it is installed, "
               "we will refund your purchase if the bundle is unopened and the hair is in its original condition. ")]]]]))

(defn built-component [data opts]
  content)
