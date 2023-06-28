(ns catalog.ui.category-hero
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private category-hero-banner-molecule
  [{:category-hero.banner/keys [img-dsk-id img-mob-id img-alt]}]
  (when (and (seq img-dsk-id) (seq img-mob-id))
    [:div
     [:div.hide-on-mb
      (ui/img {:class    "col-12"
               :style    {:vertical-align "bottom"}
               :src      img-dsk-id
               ;; no alt for decorative image
               :alt      (str img-alt)})]
     [:div.hide-on-tb-dt
      (ui/img {:max-size 500
               :class    "col-12"
               :style    {:vertical-align "bottom"}
               :src      img-mob-id
               ;; no alt for decorative image
               :alt      (str img-alt)})]]))

(defn ^:private category-hero-icon-molecule
  [{:category-hero.icon/keys [image-src]}]
  (when image-src
    [:div.mt4 [:img {:src   image-src
                     :alt   ""
                     :style {:width "54px"}}]]))

(c/defcomponent organism
  [data _ _]
  [:div
   (if-let [banner (category-hero-banner-molecule data)]
     banner
     [:div.px2.pt10 
      [:h1.proxima.text-3xl (:category-hero.title/primary data)
       (when-let [tooltip (spice.core/spy (:category-hero.title/tooltip data))]
         [:div.tooltip.ml2 (svg/info-circle {:height "15px"
                                             :width  "15px"})
          [:a.tooltip-bottom
           [:p.text-xs tooltip]
           [:i]]] )]
      (category-hero-icon-molecule data) ; Is this still used?
      (let [{:category-hero.action/keys [label target aria]} data] 
        (when (and label target)
          (ui/button-medium-underline-primary
           (merge (apply utils/route-to target)
                  {:aria-label aria
                   :class      "block mt3"})
           label)))])])
