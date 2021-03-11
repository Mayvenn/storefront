(ns stylist-profile.ui.carousel
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]))

(defn organism
  [{:carousel/keys [items]}]
  (when (seq items)
    (c/build
     carousel/component
     {:data     items
      :settings {:controls true
                 :nav      false
                 :items    3
                 ;; setting this to true causes some of our event listeners to
                 ;; get dropped by tiny-slider.
                 :loop     false}}
     {:opts {:mode     :multi
             :settings {:nav false}
             :slides   (map (fn [{:keys [target-message
                                         key
                                         ucare-img-url]}]
                            (c/html
                             [:a.px1.block
                              (merge (apply utils/route-to target-message)
                                     {:key key})
                              (ui/aspect-ratio
                               1 1
                               [:img {:src   (str ucare-img-url "-/scale_crop/204x204/-/format/auto/")
                                      :class "col-12"}])]))
                          items)}})))
