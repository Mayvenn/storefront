(ns storefront.components.new-gallery-edit
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))


(defcomponent component [{:keys [gallery]} owner opts]
  [:div.container
   (into [:div.clearfix.mxn1.flex.flex-wrap]
         (for [{:keys [status resizable-url]} gallery]
           [:div.col.col-4.pp1
            {:key resizable-url}
            (ui/aspect-ratio 1 1
                             (ui/img {:class    "container-size"
                                      :style    {:object-position "50% 25%"
                                                 :object-fit      "cover"}
                                      :src      resizable-url
                                      :max-size 749}))]))])


(defn query [data]
  {:gallery (get-in data keypaths/user-stylist-gallery-images)})

(defn built-component [data opts]
  (component/build component (query data) nil))
