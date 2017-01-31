(ns storefront.platform.video
  (:require [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.accessors.videos :as videos]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]
            [om.core :as om]
            [sablono.core :refer-macros [html]]))

(defn component [{:keys [video-id]} owner {:keys [on-close]}]
  (reify
    om/IRender
    (render [this]
      (html
       (ui/modal
        {:on-close on-close :bg-class "bg-darken-4" :col-class "col-10"}
        [:div
         [:div.wistia_responsive_padding
          (ui/aspect-ratio
           16 9
           {:class "wistia_responsive_wrapper"}
           ;; Wistia async embed bug causes the right-click menu to always appear. Using the iframe embed doesn't have this issue.
           [:iframe.wistia_embed {:src (str "//fast.wistia.com/embed/iframe/" video-id "?videoFoam=true&autoPlay=true&volume=0.33")
                                  :allowtransparency true
                                  :frameborder 0
                                  :scrolling "no"
                                  :name "wistia_embed"
                                  :allowfullscreen true
                                  :mozallowfullscreen true
                                  :webkitallowfullscreen true
                                  :oallowfullscreen true
                                  :msallowfullscreen true
                                  :width "100%"
                                  :height "100%"
                                  :id (str "center_" (videos/id->name video-id))}
            ui/nbsp])]
         [:div.light-gray.p3.col-12.center
          {:on-click       on-close
           :on-touch-start on-close}
          "close video"]])))))

(defn query [data]
  {:video-id (videos/name->id (get-in data keypaths/video))})

(defn built-component [data opts]
  (om/build component (query data) opts))
