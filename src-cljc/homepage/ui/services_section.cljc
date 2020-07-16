(ns homepage.ui.services-section
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :refer [hero]]))

(defn ^:private services-section-secondary-cta-molecule
  [{:services-section.secondary-cta/keys [id value target]}]
  [:div.col-10.col-6-on-dt.mx-auto.mb6.flex.justify-center
   (ui/button-small-underline-primary
    (-> (apply utils/route-to target)
        (assoc :data-test id))
    value)])

(defn ^:private services-section-cta-molecule
  [{:services-section.cta/keys [id value target]}]
  [:div.col-10.col-6-on-dt.mx-auto.my6
   (ui/button-medium-primary (-> (apply utils/route-to target)
                                 (assoc :data-test id))
                             value)])

;; TODO all this hero business is real funny!
(c/defcomponent hero-image-component
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (c/build hero
                 (merge data
                        {:off-screen? (not seen?)})
                 nil)])

;; TODO refactor layered/hero-image-component into system
(defn ^:private services-section-image-molecule
  [{:services-section.image/keys [ucare-id]}]
  [:div.col-12
   {:key ucare-id}
   (ui/screen-aware hero-image-component
                    {:ucare?    true
                     :mob-uuid  ucare-id
                     :dsk-uuid  ucare-id
                     :file-name "who-shop-hair"}
                    nil)])

(defn ^:private services-section-title-molecule
  [{:services-section.title/keys [primary secondary]}]
  [:div.center.mx-auto
   [:div.title-1.canela.px1.my3 primary]
   [:div.col-9.mx-auto secondary]])

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.flex-on-dt
     (when (= "image-last" (:services-section/orientation data))
       {:style {:flex-direction "row-reverse"}})
     [:div.col-6-on-dt
      (services-section-image-molecule data)]
     [:div.flex.flex-column.items-center.justify-center.col-6-on-dt.p4
      (services-section-title-molecule data)
      (services-section-cta-molecule data)
      (services-section-secondary-cta-molecule data)]]))
