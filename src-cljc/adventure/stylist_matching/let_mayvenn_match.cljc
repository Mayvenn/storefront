(ns adventure.stylist-matching.let-mayvenn-match
  (:require [clojure.string :as string]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]))

(defn copy [& sentences]
  (string/join " " sentences))

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/p-color-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                        :class "bold"}
                       "View #MayvennFreeInstall")]])

(defcomponent component
  [_ _ _]
  [:div.bg-lavender.white {:style {:min-height "95vh"}}
   (ui/narrow-container
    [:div.center
     [:div.col-11.mx-auto.py4
      [:div
       [:div.py4.h3.bold
        "Let's match you with a Certified Mayvenn Stylist!"]
       [:div.h5.line-height-3
        (copy "A Mayvenn representative will contact you soon to help select a"
              "Certified Mayvenn Stylist with the following criteria:")]
       [:div
        [:ul.col-10.h6.list-img-purple-checkmark.py4.left-align.mx6
         (mapv (fn [txt] [:li.pl1.mb1 txt])
               ["Licensed Salon Stylist"
                "Mayvenn Certified"
                "In your area"])]]
       get-inspired-cta]]])])

(defn built-component
  [data opts]
  (component/build component nil opts))
