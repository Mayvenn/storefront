(ns storefront.components.ugc
  (:require [storefront.platform.ugc :as ugc]))

(defn image-thumbnail [img]
  [:img.col-12.block img])

(defn image-attribution [look]
  [:div.bg-light-gray.p1
   [:div.h5.mt1.mb2.mx3-on-mb.mx1-on-tb-dt
    (ugc/user-attribution look)]
   (ugc/view-look-button look {:back-copy "back to shop by look"})])

