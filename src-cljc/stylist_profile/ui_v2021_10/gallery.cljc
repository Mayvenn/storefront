(ns stylist-profile.ui-v2021-10.gallery
  (:require [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn instagram
  [{:keys [id target]} class]
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
       [:a.block.proxima.inherit-color.shout.flex.items-center.hide-on-mb-tb
        (merge (utils/fake-href evt (assoc evt-args :open-in-new-tab? true))
               {:data-test id
                :class class})
        logo
        ig-username]])))

(defn gallery
  [{:keys [items grid-attrs]}]
  (into [:div {:class grid-attrs}]
        (map (fn [{:keys [target-message key ucare-img-url filler-img]}]
               (c/html
                (if filler-img
                  [:div.block.bg-cool-gray.flex.justify-center.items-center
                   filler-img]
                  [:a.block
                   (merge
                    (when target-message
                      (apply utils/route-to target-message))
                    {:key key})
                   (when ucare-img-url
                     (ui/aspect-ratio
                      1 1
                      (ui/img {:src   ucare-img-url
                               :style {:object-position "50% 25%"
                                       :object-fit      "cover"}
                               :class "col-12 container-size"})))])))
             items)))

(c/defcomponent organism
  [{:gallery/keys [target] :as data} _ _]
  (c/html
   [:div.mx-auto.col-11.border-top.border-cool-gray
    [:div.hide-on-mb.col-11.mx-auto
     [:div.flex.justify-between.items-center.mb2.mt4
      (instagram (with :gallery.instagram data) "button-font-1")]
     (gallery (with :gallery.desktop data))]
    [:div.hide-on-tb-dt.mx-auto
     [:div.flex.justify-between.items-center.mb2.mt4
      (instagram (with :gallery.instagram data) "button-font-2")
      [:div]
      (ui/button-small-underline-primary (apply utils/route-to target) "See all")]
     (gallery (with :gallery.mobile data))]]))
