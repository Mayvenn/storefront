(ns stylist-matching.ui.spinner
  (:require [storefront.component :as component :refer [defcomponent]]))

(defn spinner-molecule [{:spinner/keys [id]}]
  (when id
    (component/html
     [:div.img-spinner.bg-no-repeat.bg-center.bg-contain.col-12.mt10
      {:style {:height "1.5em"}}])))

(defcomponent organism
  [data _ _]
  (spinner-molecule data))
