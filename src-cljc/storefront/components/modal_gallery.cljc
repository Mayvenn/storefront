(ns storefront.components.modal-gallery
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.component :as component]))


(defn ucare-img-slide [ucare-id]
  [:div (ui/aspect-ratio 1 1
                         (ui/ucare-img {:class "col-12"} ucare-id))])

(defn simple
  [{:keys [slides open? close-event]}]
  (when open?
    (let [close-attrs (utils/fake-href close-event)]
      (ui/modal
       {:close-attrs close-attrs
        :col-class   "col-12"}
       [:div.relative.mx-auto
        {:style {:max-width "750px"}}
        (component/build carousel/component
                         {:slides   slides
                          :settings {:slidesToShow 1}}
                         {})
        [:div.absolute
         {:style {:top "1.5rem" :right "1.5rem"}}
         (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                          :close-attrs close-attrs})]]))))
