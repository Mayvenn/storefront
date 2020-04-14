(ns homepage.ui.quality-stylists
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :refer [hero]]))

;; TODO all this hero business is real funny!
(c/defcomponent hero-image-component
  [{:screen/keys [seen?] :as data} _ _]
  [:div (c/build hero
                 (merge data
                        {:off-screen? (not seen?)})
                 nil)])

;; TODO refactor layered/hero-image-component into system
(defn quality-stylists-image-molecule
  [{:quality-stylists.image/keys [ucare-ids]}]
  [:div.col-12.my5
   (ui/screen-aware hero-image-component
                    {:ucare?      true
                     :mob-uuid    (:mobile ucare-ids)
                     :dsk-uuid    (:desktop ucare-ids)
                     :file-name   "quality-stylists"}
                    nil)])

(defn ^:private quality-stylists-cta-molecule
  [{:quality-stylists.cta/keys [target label id]}]
  [:div.pt3
   (ui/button-small-underline-primary
    (assoc (apply utils/route-to target)
           :data-test  id
           :data-ref id)
    label)])

(defn ^:private quality-stylists-body-atom
  [{:quality-stylists.body/keys [primary]}]
  [:div.col-9.mx-auto.title-2.canela primary])

(defn ^:private quality-stylists-title-molecule
  [{:quality-stylists.title/keys [primary secondary]}]
  [:div.title-1.canela
   [:div.py1.shout
    [:div.title-1.proxima {:style {:font-size "19px"}} primary]
    [:div.canela.mt2.mb4 {:style {:font-size "72px"}} secondary]]])

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    (list
     [:div.col-6-on-dt
      [:div.center.mx-auto.my5.pt4
       (quality-stylists-title-molecule data)
       (quality-stylists-body-atom data)
       (quality-stylists-cta-molecule data)]]
     [:div
      {:style {:order 3}}
      (quality-stylists-image-molecule data)])))
