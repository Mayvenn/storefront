(ns catalog.ui.category-hero
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private category-hero-tag-molecule
  [{:category-hero.tag/keys [primary]}]
  (when primary
    [:div.p-color.title-3.proxima.bold.shout
     primary]))

(defn ^:private category-hero-title-molecule
  [{:category-hero.title/keys [primary]}]
  [:h1.title-1.canela primary])

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

(defn ^:private category-hero-body-molecule
  [{:category-hero.body/keys [primary]}]
  [:div.content-2.proxima
   primary])

(defn ^:private category-hero-action-molecule
  [{:category-hero.action/keys [label target aria]}]
  (when (and label target)
    [:div.mt3
     (ui/button-small-underline-black
      {:on-click   (apply utils/send-event-callback target)
       :aria-label aria}
      label)]))

;; GROT
(c/defcomponent organism-old
  [data _ _]
  [:div.bg-warm-gray.center.mx-auto
   (if-let [banner (category-hero-banner-molecule data)]
     banner
     [:div.px2.py10
      (category-hero-tag-molecule data)
      (category-hero-title-molecule data)
      (category-hero-icon-molecule data)
      [:div.my3.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt
       (category-hero-body-molecule data)
       (category-hero-action-molecule data)]])])

(c/defcomponent organism
  [data _ _]
  [:div
   (if-let [banner (category-hero-banner-molecule data)]
     banner
     [:div.px2.py10 
      [:h1.proxima.text-3xl (:category-hero.title/primary data)]
      (category-hero-icon-molecule data) ; Is this still used?
      (let [{:category-hero.action/keys [label target aria]} data] 
        (when (and label target)
          [:div.my3.mx6-on-mb.col-8-on-tb-dt.mx-auto-on-tb-dt ; Is this still used?
           [:div.mt3
            (ui/button-small-underline-black
             {:on-click   (apply utils/send-event-callback target)
              :aria-label aria}
             label)]]))])])