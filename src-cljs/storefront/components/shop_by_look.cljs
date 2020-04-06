(ns storefront.components.shop-by-look
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.accessors.contentful :as contentful]
            [storefront.components.ugc :as component-ugc]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [spice.maps :as maps]
            [storefront.ugc :as ugc]))

(defcomponent component [{:keys [looks copy spinning?]} owner opts]
  (if spinning?
    (ui/large-spinner {:style {:height "4em"}})
    [:div.bg-warm-gray
     [:div.center.py6
      [:h1.title-1.canela.py3 (:title copy)]
      [:p.col-10.col-6-on-tb-dt.mx-auto.proxima.content-2 (:description copy)]]
     [:div.flex.flex-wrap.mbn2.justify-center.justify-start-on-tb-dt.bg-cool-gray.py2-on-tb-dt.px1-on-tb-dt
      (for [look looks]
        (component/build component-ugc/social-image-card-component look {:opts {:copy copy}
                                                                         :key  (str (:id look))}))]]))

(defn query [data]
  (let [navigation-event  (get-in data keypaths/navigation-event)
        selected-album-kw (get-in data keypaths/selected-album-keyword)
        actual-album-kw   (ugc/determine-look-album data selected-album-kw)
        looks             (-> data (get-in keypaths/cms-ugc-collection) actual-album-kw :looks)
        color-details     (->> (get-in data keypaths/v2-facets)
                               (filter #(= :hair/color (:facet/slug %)))
                               first
                               :facet/options
                               (maps/index-by :option/slug))]
    {:looks     (mapv (partial contentful/look->social-card
                               navigation-event
                               selected-album-kw
                               color-details)
                      looks)
     :copy      (actual-album-kw ugc/album-copy)
     :spinning? (empty? looks)}))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
