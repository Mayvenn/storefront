(ns homepage.ui.wig-customization
  (:require [storefront.component :as c]
            ;; TODO free from this namespace
            [adventure.components.layered :as layered]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]))

(defn ^:private wig-customization-cta-molecule
  [{:wig-customization.cta/keys [id value target]}]
  [:div.col-9.mx-auto.py6
   (ui/button-large-primary (-> (apply utils/route-to target)
                                (assoc :data-test id))
                            value)])

(defn ^:private wig-customization-list-molecule
  [{:wig-customization.list/keys [primary] :list/keys [bullets]}]
  [:div.col-10.col-6-on-dt.mx-auto.border.border-framed.flex.justify-center.mt8.pt3
   [:div.col-12.flex.flex-column.items-center.m5.py4
    {:style {:width "max-content"}} ;; TODO -> class for css rule
    [:div.proxima.title-2.shout.pt1.mb primary]
    [:ul.col-12.list-purple-diamond
     {:style {:padding-left "15px"}}
     (for [[idx bullet] (map-indexed vector bullets)
           :let [id (str "wig-customization.list" idx)]]
       [:li.py1 {:key id} bullet])]]])

;; TODO refactor layered/hero-image-component into system
(defn ^:private wig-customization-image-molecule
  [{:wig-customization.image/keys [ucare-id]}]
  [:div.col-12.col-6-on-dt.my5
   {:key ucare-id}
   (ui/screen-aware layered/hero-image-component
                    {:ucare?     true
                     :mob-uuid  ucare-id
                     :dsk-uuid  ucare-id
                     :file-name "who-shop-hair"}
                    nil)])

(defn ^:private wig-customization-title-molecule
  [{:wig-customization.title/keys [primary secondary]}]
  [:div.center.col-6-on-dt.mx-auto.my5.pt4
   [:div.mb3
    [:div.col-12.mx-auto.mb1
     (svg/purple-diamond {:height "9px" :width "9px"})
     [:span.proxima.title-3.shout.mx1 "New"]
     (svg/purple-diamond {:height "9px" :width "9px"})]
    [:div.title-1.canela primary]]
   [:div.col-9.mx-auto secondary]])

(c/defcomponent organism
  [data _ _]
  [:div.mb6
   (wig-customization-title-molecule data)
   (wig-customization-image-molecule data)
   (wig-customization-list-molecule data)
   (wig-customization-cta-molecule data)])
