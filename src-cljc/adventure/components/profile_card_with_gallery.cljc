(ns adventure.components.profile-card-with-gallery
  (:require [adventure.components.profile-card :as profile-card]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            [storefront.events :as events]
            [storefront.platform.messages :as messages]))


(defn component
  [{:keys [card-data gallery-data button]} _ _]
  (component/create
   [:div.bg-white.h6.my2.col-12.col-8-on-tb-dt
    (component/build profile-card/component card-data nil)
    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt
     [:div.h7.dark-gray.bold.left-align.mb1 (:title gallery-data)]
     (component/build carousel/component
                      {:slides   (map (fn [gallery-item]
                                        [:div
                                         {:on-click #(apply messages/handle-message (:target-message gallery-item))
                                          :key (:key gallery-item)}
                                         (ui/aspect-ratio
                                          1 1
                                          [:img {:src   (str (:ucare-img-url gallery-item) "-/scale_crop/204x204/-/format/auto/")
                                                 :class "rounded"
                                                 :width "102"}])])
                                      (:items gallery-data))
                       :settings {:swipe        true
                                  :initialSlide 0
                                  :arrows       true
                                  :dots         false
                                  :slidesToShow 3
                                  :infinite     true}}
                      {})]
    (ui/teal-button
     (merge {:data-test (:data-test button)}
            (apply utils/fake-href (:target-message button)))
     [:div.flex.items-center.justify-center.inherit-color
      (:text button)])]))

(defn stylist-profile-card-data [index {:keys [gallery-images stylist-id] :as stylist}]
  (let [ucare-img-urls (map :resizable-url gallery-images)]
    {:card-data    (profile-card/stylist-profile-card-data stylist)
     :index        index
     :key          (str "stylist-card-" stylist-id)
     :gallery-data {:title "Recent Work"
                    :items (map-indexed (fn [j ucare-img-url]
                                          {:key            (str "gallery-img-" stylist-id "-" j)
                                           :ucare-img-url  ucare-img-url
                                           :target-message [events/control-adventure-stylist-gallery-open
                                                            {:ucare-img-urls                 ucare-img-urls
                                                             :initially-selected-image-index j}]})
                                        ucare-img-urls)}
     :button       {:text           "Select"
                    :data-test      "select-stylist"
                    :target-message [events/control-adventure-select-stylist {:stylist-id        stylist-id
                                                                              :servicing-stylist stylist
                                                                              :card-index        index}]}}))
