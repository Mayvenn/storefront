(ns storefront.components.free-install-video
  (:require [sablono.core :refer [html]]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

(defn component [{:keys []} owner {:keys [close-attrs]}]
  (component/create
   (html
    (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
               :close-attrs close-attrs
               :bg-class  "bg-darken-4"}
              (ui/youtube-responsive
               (str
                "//www.youtube.com/embed/cWkSO_2nnD4"
                "?rel=0"
                "&color=white"
                "&showinfo=0"))))))

(defn query
  [data]
  {})

(defn built-component
  [data opts]
  (component/build component data opts))
