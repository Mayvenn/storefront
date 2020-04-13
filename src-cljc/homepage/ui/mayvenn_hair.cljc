(ns homepage.ui.mayvenn-hair
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent ^:private ugc-image
  [{:screen/keys [seen?] :keys [image-url]} owner opts]
  (ui/aspect-ratio
   1 1
   (cond
     seen?          [:img {:class "col-12"
                           :src   image-url}]
     :else          [:div.col-12 " "])))

(defn ^:private more-looks-cta
  [{:mayvenn-hair.cta/keys [target label id]}]
  (ui/button-small-underline-primary
   (assoc (apply utils/route-to target)
          :data-test  id
          :data-ref id)
   label))

(def ^:private mayvenn-hair-title-atom
  [:div.title-2.proxima.shout.bold "#MayvennHair"])

(defn ^:private looks-images-molecule
  [{:mayvenn-hair.looks/keys [images]}]
  (for [{:keys [image-url]} images]
    [:a.col-6.col-3-on-tb-dt.p1
     {:key (str image-url)}
     (ui/screen-aware ugc-image {:image-url image-url} nil)]))

(c/defcomponent organism
  [data _ _]
  [:div.py8.col-10.mx-auto.center
   mayvenn-hair-title-atom
   [:div.flex.flex-wrap.py3
    (looks-images-molecule data)]
   (more-looks-cta data)])
