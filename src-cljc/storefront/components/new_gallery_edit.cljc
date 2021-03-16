(ns storefront.components.new-gallery-edit
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(def pending-approval
  (component/html
   [:div.container-size.bg-gray.flex.items-center.center.p2.proxima.content-2
    "Your image has been successfully submitted and is pending approval. Check back here to be updated on its status."]))

(defcomponent component [{:keys [gallery]} owner opts]
  [:div.container
   (into [:div.clearfix.mxn1.flex.flex-wrap]
         (for [{:keys [status resizable-url]} gallery]
           [:div.col.col-4.pp1
            {:key resizable-url}
            (ui/aspect-ratio 1 1
                             (if (= "approved" status)
                               (ui/img {:class    "container-size"
                                        :style    {:object-position "50% 25%"
                                                   :object-fit      "cover"}
                                        :src      resizable-url
                                        :max-size 749})
                               pending-approval))]))])


(defn query [data]
  {:gallery (get-in data keypaths/user-stylist-gallery-images)})

(defn built-component [data opts]
  (component/build component (query data) nil))
