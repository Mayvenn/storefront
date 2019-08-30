(ns stylist-matching.ui.spinner
  (:require [storefront.component :as component]))

(defn spinner-molecule [{:spinner/keys [id]}]
  (when id
    (component/html
     [:div.img-spinner.bg-no-repeat.bg-center.bg-contain.col-12.mt10
      {:style {:height "1.5em"}}])))

(defn organism
  [data]
  (component/create
   (spinner-molecule data)))
