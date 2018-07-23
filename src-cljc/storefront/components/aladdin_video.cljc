(ns storefront.components.aladdin-video
  (:require [sablono.core :refer [html]]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]))

;; TODO: DRYUP- copied from free_install_video.cljc
(defn component [{:keys [youtube-id]} owner {:keys [close-attrs]}]
  (component/create
   (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
              :close-attrs close-attrs
              :bg-class  "bg-darken-4"}
             (ui/youtube-responsive
              (str
               "//www.youtube.com/embed/"
               youtube-id
               "?rel=0"
               "&color=white"
               "&showinfo=0")))))

(defn query [data]
  (get-in data keypaths/aladdin-video))

(defn built-component
  [data opts]
  (component/build component data opts))
