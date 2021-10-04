(ns stylist-profile.ui-v2021-10.gallery
  (:require [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn instagram
  [{:keys [id target]}]
  (when id
    [:a.block.proxima.button-font-2.inherit-color.shout.flex.items-center
     (merge (apply utils/fake-href target) {:data-test id})
     [:img
      {:src    "//ucarecdn.com/df7cc161-057f-46f4-94a7-d5638c91755c/-/format/auto/-/resize/25x25/"
       :width  "25"
       :height "25"
       :alt    "instagram logo"
       :class "mr2"}]
     (:ig-username (second target))]))

(c/defcomponent organism
  [{:gallery/keys [items target] :as data} _ _]
  (c/html
   [:div.mx2.border-top.border-gray
    [:div.flex.justify-between.items-center.mb2.mt4
     (instagram (with :gallery.instagram data))
     [:div]
     (ui/button-small-underline-primary (apply utils/route-to target) "See all")]
    (into [:div {:style {:display               "grid"
                         :grid-template-columns "repeat(3, 1fr)"}}]
          (map (fn [{:keys [target-message key ucare-img-url]}]
                 (c/html
                  [:a.block
                   (merge (apply utils/route-to target-message) {:key key})
                   (ui/aspect-ratio
                    1 1
                    (ui/img {:src   ucare-img-url
                             :style {:object-position "50% 25%"
                                     :object-fit      "cover"}
                             :class "col-12 container-size"}))]))
               ;; Only show 3, 6, 9, or 12 photos.
               (-> items count (quot 3) (* 3) (min 12) (take items))))]))
