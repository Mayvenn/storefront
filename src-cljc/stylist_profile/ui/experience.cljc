(ns stylist-profile.ui.experience
  (:require [storefront.component :as c]))

(defn experience-title-molecule
  [{:experience.title/keys [id primary]}]
  [:div
   {:key id}
   [:div.title-3.proxima.shout primary]])

(defn experience-body-molecule
  [{:experience.body/keys [primary secondary]}]
  [:div.content-2
   [:div primary]
   (when secondary
     [:div secondary])])

(c/defcomponent organism
  [data _ _]
  [:div.my3
   (experience-title-molecule data)
   (experience-body-molecule data)])
