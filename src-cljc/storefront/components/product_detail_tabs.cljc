(ns storefront.components.product-detail-tabs
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]
            [storefront.component :as c]))

(defn ^:private tab-element
  [active-tab-name tab-id tab-content]
  (let [active-tab? (= (name active-tab-name) tab-id)]
    (when (or active-tab? #?(:clj true))  ;; always show for server-side rendered html
      [:div.my3
       {:react-key (str tab-id "-tab")}
       (first (:description tab-content))])))

(defcomponent component [{:keys [product active-tab-name tabs] :as stuff} owner _]
  [:div.mx4
   [:div.flex.mx-auto.justify-between
    (for [{:keys [title id icon]} tabs]
      (let [tab-is-active-tab? (= (name active-tab-name) id)
            fill-color-class   (if tab-is-active-tab? "fill-black" "fill-gray")
            tab-classes  (if tab-is-active-tab?
                           "black border-width-4 border-black"
                           "dark-gray border-width-2 border-cool-gray")]
        [:div.canela.title-3.col-4.border-bottom
         {:class     tab-classes
          :react-key id}
         [:div.flex.justify-center
          (case id
            "description"
            (svg/description {:height           "18px"
                              :width            "18px"
                              :fill-color-class fill-color-class})
            "hair-info"
            (svg/info-black-circle {:height           "20px"
                                    :width            "20px"
                                    :fill-color-class fill-color-class})
            "care"
            (svg/heart {:height           "19px"
                        :width            "21px"
                        :fill-color-class fill-color-class}))]
         [:div.center title]]))]

   (for [{:keys [id tab-content] :as tab} tabs]
     (tab-element active-tab-name id tab-content))])
