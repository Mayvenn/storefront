(ns homepage.ui.mayvenn-install
  (:require [homepage.ui.atoms :as A]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :refer [hero]]))

(defn ^:private mayvenn-install-cta-molecule
  [{:mayvenn-install.cta/keys [id value target]}]
  [:div.col-9.mx-auto.py6
   (ui/button-large-primary (-> (apply utils/route-to target)
                                (assoc :data-test id))
                            value)])

(defn ^:private mayvenn-install-list-molecule
  [{:mayvenn-install.list/keys [primary] :list/keys [bullets]}]
  [:div.col-10.col-6-on-dt.mx-auto.border.border-framed.flex.justify-center.mt8.pt3
   [:div.col-12.flex.flex-column.items-center.m5.py4
    {:style {:width "max-content"}} ;; TODO -> class for css rule
    [:div.proxima.title-2.shout.pt1.mb primary]
    [:ul.col-12.list-purple-diamond
     {:style {:padding-left "15px"}}
     (for [[idx bullet] (map-indexed vector bullets)
           :let [id (str "mayvenn-install.list" idx)]]
       [:li.py1 {:key id} bullet])]]])

;; TODO all this hero business is real funny!
(c/defcomponent hero-image-component
  [{:screen/keys [seen?] :as data} owner opts]
  [:div (c/build hero
                 (merge data
                        {:off-screen? (not seen?)})
                 nil)])

;; TODO refactor layered/hero-image-component into system
(defn ^:private mayvenn-install-image-molecule
  [{:mayvenn-install.image/keys [ucare-id]}]
  [:div.col-12.col-6-on-dt.my5
   {:key ucare-id}
   (ui/screen-aware hero-image-component
                    {:ucare?     true
                     :mob-uuid  ucare-id
                     :dsk-uuid  ucare-id
                     :file-name "who-shop-hair"}
                    nil)])

(defn ^:private mayvenn-install-title-molecule
  [{:mayvenn-install.title/keys [primary secondary]}]
  [:div.center.col-6-on-dt.mx-auto.my5.pt4
   [:div.mb3
    [:div.title-1.canela primary]]
   [:div.col-9.mx-auto secondary]])

(c/defcomponent organism
  [data _ _]
  (when (seq data)
    [:div
     [:div.mb6
      (mayvenn-install-title-molecule data)
      (mayvenn-install-image-molecule data)
      (mayvenn-install-list-molecule data)
      (mayvenn-install-cta-molecule data)]
     A/horizontal-rule-atom]))
