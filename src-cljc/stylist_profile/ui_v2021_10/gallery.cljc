(ns stylist-profile.ui-v2021-10.gallery
  (:require [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn instagram
  [{:keys [id target]}]
  (when id
    (let [logo
          [:img
           {:src    "//ucarecdn.com/df7cc161-057f-46f4-94a7-d5638c91755c/-/format/auto/-/resize/25x25/"
            :width  "25"
            :height "25"
            :alt    "instagram logo"
            :class  "mr2"}]

          [evt {:keys [ig-username] :as evt-args}]
          target]
      [:div
       [:a.block.proxima.button-font-2.inherit-color.shout.flex.items-center.hide-on-mb-tb
        (merge (utils/fake-href evt (assoc evt-args :open-in-new-tab? true)) {:data-test id})
        logo
        ig-username]
       [:a.block.proxima.button-font-2.inherit-color.shout.flex.items-center.hide-on-dt
        (merge (utils/fake-href evt (assoc evt-args :open-in-new-tab? false)) {:data-test id})
        logo
        ig-username]])))

(c/defcomponent organism
  [{:gallery/keys [items target] :as data} _ _]
  (c/html
   [:div.mx2.border-top.border-gray
    [:div.flex.justify-between.items-center.mb2.mt4
     (instagram (with :gallery.instagram data))
     [:div]
     (ui/button-small-underline-primary (apply utils/route-to target) "See all")]
    (into [:div.grid.gap-1px.cols-3]
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
