(ns ui.product-list-header
  (:require [storefront.component :as component]))

(defn organism
  [{:product-list-header/keys [slug copy-primary copy-secondary image-url copy-position]}]
  (component/create
   [:div
    {:key (str "header-" slug)
     :style {:background-image (str "url('" image-url  ")")
             :background-size "cover"
             :height "200px"}}
    [:div.col-6.flex.items-center
     {:class (cond
               (= :right copy-position)
               "right"

               (= :left copy-position)
               "left")
      :style {:height "100%"}}
     [:div
      [:h2.h3.bold copy-primary]
      [:p.h7 copy-secondary]]]]))
