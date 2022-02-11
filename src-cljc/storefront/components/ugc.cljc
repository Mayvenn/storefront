(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             [storefront.component :as component :refer [defcomponent]]))

(defcomponent ugc-image [{:keys [image-url overlay alt]} _ _]
  [:div.relative
   (ui/aspect-ratio
    1 1
    {:class "flex items-center"}
    (ui/img {:class    "col-12"
             :src      image-url
             :max-size 749
             :alt      alt}))
   (when overlay
     [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
      [:div {:style {:width       "0"
                     :height      "0"
                     :border-top  "28px solid #dedbe5"
                     :border-left "21px solid transparent"}}]
      [:div.flex.items-center.px3.bold.h6.bg-pale-purple.whisper.white
       overlay]])])

(defcomponent social-image-card-component
  [{:keys                [desktop-aware?
                          id description title
                          price discounted-price]
    :screen/keys         [seen?]
    [nav-event nav-args] :cta/navigation-message
    button-type          :cta/button-type
    :as                  card}
   _
   {:keys [copy screen/ref]}]
  [:div.pb2.px1-on-tb-dt.col-12
   (merge {:key (str "small-" id)
           :ref ref}
          (when desktop-aware?
            {:class "col-6-on-tb col-4-on-dt"}))
   (if (or seen?
           (:hack/above-the-fold? card))
     (let [cta-button-fn (case button-type
                           :p-color-button   ui/button-large-primary
                           :underline-button ui/button-medium-secondary)]
       [:div.bg-white
        [:div
         (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                            :short-name (:short-name copy)})
         (component/build ugc-image (select-keys card [:overlay :image-url :alt]))]
        [:div.p1.px3.pb3
         [:div.h5.medium.mt1.mb2
          [:div.flex.items-center.justify-between.mb2
           [:div.flex.items-center
            [:div.flex.flex-column
             [:h2.h3 title ", " description]
             [:div.flex.flex-row
              (when discounted-price [:div discounted-price])
              (cond
                (and price discounted-price) [:div.regular.strike.ml1.content-3 price]
                price                        [:div.regular price])]]]
           [:div.m1.self-start {:style {:width  "21px"
                                        :height "21px"}}
            ^:inline (svg/instagram)]]]
         (when nav-event
           (cta-button-fn
            (merge
             (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                                :short-name (:short-name copy)})
             {:data-test (str "look-" id)})
            [:span.bold (:button-copy copy)]))]])
     [:div.bg-white {:style {:min-height "520px"}}])])
