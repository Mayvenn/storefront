(ns storefront.components.phone-consult
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as c :refer [defcomponent]]))

(defcomponent component
  [{:keys [message released] :as data} owner _]
  (when released
    [:a.center
     (merge {:href (str "tel:+")})
     message])
  )
