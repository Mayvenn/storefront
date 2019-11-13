(ns storefront.components.video
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]))

(defcomponent component [{:keys [youtube-id]} owner {:keys [close-attrs]}]
  (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs close-attrs
             :bg-class  "bg-darken-4"}
            (ui/youtube-responsive
             (str
              "//www.youtube.com/embed/"
              youtube-id
              "?rel=0"
              "&color=white"
              "&showinfo=0"))))

(defn built-component
  [data opts]
  (component/build component data opts))
