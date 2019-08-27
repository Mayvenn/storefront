(ns adventure.stylist-matching.organisms.match-success
  (:require [adventure.components.header :as header]
            adventure.keypaths
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [stylist-directory.stylists :as stylists]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn image-and-text-button-molecule
  [{:button/keys [image-id
                  copy
                  nav-message
                  data-test]}]
  (ui/white-button
   (merge {:style     {:border-radius "3px"}
           :class     "my1"
           :key       data-test
           :data-test data-test}
          (apply utils/route-to nav-message))
   [:div.flex.items-center.justify-start
    (ui/ucare-img {:width "60"} image-id)
    [:div.px4 copy]]))

(defn buttons-molecule
  [{:buttons/keys [buttons]}]
  [:div.mt1
   (for [button buttons]
     (image-and-text-button-molecule button))])

(defn organism
  [{:match-success/keys [prompt
                         mini-prompt] :as queried-data
    :keys [header-data]} _ _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    {:class "bg-adventure-basic-prompt"
     :style {:background-size     "100px"
             :background-position "right 22px bottom 21px"
             :background-image
             "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}}
    (header/built-component header-data nil)
    [:div.mx5.mb5
     {:style {:margin-top "95px"}}
     [:div.flex.flex-column.left-align
      [:div
       [:div.h1.light prompt]
       [:div.mt7.h5 mini-prompt]]]
     [:div.flex.flex-auto
      (buttons-molecule queried-data)]]]))

(defn query [app-state]
  (let [servicing-stylist    (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        stylist-display-name (stylists/->display-name servicing-stylist)]
    {:match-success/prompt      [:div "Congrats on matching with " [:span.bold stylist-display-name] "!"]
     :match-success/mini-prompt [:div
                                 [:div "Now for the fun part!"]
                                 [:div "How would you like to shop your hair?"]]
     :buttons/buttons           [{:button/image-id   "a9009728-efd3-4917-9541-b4514b8e4776"
                                  :button/copy        "Shop by look"
                                  :button/nav-message [events/navigate-shop-by-look {:album-keyword :look}]
                                  :button/data-test   "button-looks"}
                                 {:button/image-id   "6c39cd72-6fde-4ec2-823c-5e39412a6d54"
                                  :button/copy        "Choose individual bundles"
                                  :button/nav-message [events/navigate-category
                                                       {:page/slug           "mayvenn-install"
                                                        :catalog/category-id "23"}]
                                  :button/data-test   "button-a-la-carte"}
                                 {:button/image-id   "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"
                                  :button/copy        "Pre-made bundle sets"
                                  :button/nav-message [events/navigate-shop-by-look {:album-keyword :deals}]
                                  :button/data-test   "button-bundle-sets"}]
     :header-data               {:back-navigation-message [events/navigate-adventure-stylist-results-pre-purchase]}}))

(defn built-organism [app-state]
  (component/build organism (query app-state)))
