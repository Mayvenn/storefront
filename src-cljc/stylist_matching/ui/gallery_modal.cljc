(ns stylist-matching.ui.gallery-modal
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]))

(defn ^:private gallery-slide [index ucare-img-url]
  [:div {:key (str "gallery-slide" index)}
   (ui/aspect-ratio 1 1
                    (ui/ucare-img {:class "col-12"} ucare-img-url))])

(defcomponent organism
  [{:gallery-modal/keys [target ucare-image-urls initial-index]} _ _]
  [:div
   (when (seq ucare-image-urls)
     (ui/modal
      {:close-attrs (apply utils/fake-href target)
       :col-class   "col-12"}
      [:div.relative.mx-auto
       {:style {:max-width "750px"}}
       (component/build carousel/component
                        {:slides   (map-indexed gallery-slide ucare-image-urls)
                         :settings {:startIndex  (or initial-index 0)
                                    :nav         false
                                    :items       1
                                    :edgePadding 0}})
       [:div.absolute
        {:style {:top "1.5rem" :right "1.5rem"}}
        (ui/modal-close {:class       "stroke-black fill-gray"
                         :close-attrs (apply utils/fake-href target)})]]))])
