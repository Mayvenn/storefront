(ns adventure.stylist-matching.organisms.match-success
  (:require [adventure.components.header :as header]
            adventure.keypaths
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [stylist-directory.stylists :as stylists]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn image-and-text-button-molecule
  [{:button/keys [image-url
                  copy
                  nav-message
                  data-test]}]
  (ui/white-button
   (merge {:style     {:border-radius "3px"}
           :class     "my1"
           :key       data-test
           :data-test data-test}
          (apply utils/fake-href nav-message))
   [:div.flex.items-center.justify-start
    [:img {:src   image-url
           :style {:height "60px"}}]
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
     :buttons/buttons           [{:button/image-url   "http://place-puppy.com/120x120"
                                  :button/copy        "Shop by look"
                                  :button/nav-message [:bar {}]
                                  :button/data-test   "button-looks"}
                                 {:button/image-url   "http://place-puppy.com/121x121"
                                  :button/copy        "Choose individual bundles"
                                  :button/nav-message [:bar {}]
                                  :button/data-test   "button-a-la-carte"}
                                 {:button/image-url   "http://place-puppy.com/122x122"
                                  :button/copy        "Pre-made bundle sets"
                                  :button/nav-message [:bar {}]
                                  :button/data-test   "button-bundle-sets"}]
     :header-data               {:back-navigation-message [events/navigate-adventure-stylist-results-pre-purchase]}}))

(defn built-organism [app-state]
  (component/build organism (query app-state)))
