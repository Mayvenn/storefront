(ns homepage.ui.wig-customization
  (:require [homepage.ui.atoms :as A]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :refer [hero]]))

(defn ^:private wig-customization-cta-molecule
  [{:wig-customization.cta/keys [id value target]}]
  [:div.col-9.col-9-on-dt.mx-auto.py6
   (ui/button-large-primary (-> (apply utils/route-to target)
                                (assoc :data-test id))
                            value)])

(defn ^:private wig-customization-list-molecule
  [{:wig-customization.list/keys [primary] :list/keys [bullets]}]
  [:div.col-10.col-8-on-dt.mx-auto.border.border-framed.flex.justify-center.mt8.pt3
   [:div.col-12.flex.flex-column.items-center.m5.py4.py5-on-dt
    {:style {:width "max-content"}} ;; TODO -> class for css rule
    [:div.proxima.title-2.shout.pt1.mb3 primary]
    [:ul.col-12.list-purple-diamond
     {:style {:padding-left "15px"}}
     (for [[idx bullet] (map-indexed vector bullets)
           :let [id (str "wig-customization.list" idx)]]
       [:li.py1 {:key id} bullet])]]])

;; TODO all this hero business is real funny!
(c/defcomponent hero-image-component
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (c/build hero
                 (merge data
                        {:off-screen? (not seen?)})
                 nil)])

;; TODO refactor layered/hero-image-component into system
(defn ^:private wig-customization-image-molecule
  [{:wig-customization.image/keys [ucare-id]}]
  [:div.col-12.col-6-on-dt.mt5
   {:key ucare-id}
   (ui/screen-aware hero-image-component
                    {:ucare?     true
                     :mob-uuid  ucare-id
                     :dsk-uuid  ucare-id
                     :file-name "who-shop-hair"}
                    nil)])

(defn ^:private wig-customization-title-molecule
  [{:wig-customization.title/keys [primary secondary]}]
  [:div.center.col-6-on-dt.mx-auto
   [:div.col-12.mx-auto.mb1
    (svg/purple-diamond {:height "9px" :width "9px"})
    [:span.proxima.title-3.shout.mx1 "New"]
    (svg/purple-diamond {:height "9px" :width "9px"})]
   [:div.title-1.canela.mb3 primary]
   [:div.col-9.mx-auto secondary]])

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.mb4.mb0-on-dt
     A/horizontal-rule-atom
     [:div.my6.pt4
      (wig-customization-title-molecule data)]
     [:div.flex-on-dt
      {:style {:flex-direction "row-reverse"}}
      (wig-customization-image-molecule data)
      [:div.col-6-on-dt.col-12
       (wig-customization-list-molecule data)
       (wig-customization-cta-molecule data)]]]))
