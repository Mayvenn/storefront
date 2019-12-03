(ns storefront.components.modal-gallery
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.carousel :as carousel]
            [storefront.component :as component :refer [defcomponent]]))

(defn ucare-img-slide [ucare-id]
  [:div (ui/aspect-ratio 1 1
                         (ui/ucare-img {:class "col-12"} ucare-id))])

(defn simple
  [{:keys [slides open? close-event]}]
  (when open?
    (let [close-attrs (merge (utils/fake-href close-event)
                             {:style {:margin-top "-60px"
                                      :margin-right "-15px"}})]
      (ui/modal
       {:close-attrs close-attrs
        :col-class   "col-12"}
       [:div.relative.mx-auto
        {:style {:max-width "750px"}}
        (component/build carousel/component
                         {:slides   slides
                          :settings {:controls    true
                                     :nav         false
                                     :edgePadding 0
                                     :items       1}}
                         {})
        [:div.absolute
         {:style {:top "1.5rem" :right "1.5rem"}}
         (ui/modal-close {:class       "stroke-black fill-gray"
                          :close-attrs close-attrs})]]))))
