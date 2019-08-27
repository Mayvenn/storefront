(ns adventure.stylist-matching.organisms.match-success
  (:require [adventure.components.header :as header]
            adventure.keypaths
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [stylist-directory.stylists :as stylists]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(comment
  ;; TODO this should be domain.ui.organisms.name
  "stylist-matching.match-success"
  "sytlist-matching.ui.choose-shopping-method")

(defn shopping-method-choice-button-molecule
  [{:shopping-method-choice-button/keys [id target ucare-id label]}]
  (ui/white-button
   (merge {:style     {:border-radius "3px"}
           :class     "my1"
           :key       id
           :data-test id}
          (apply utils/route-to target))
   [:div.flex.items-center.justify-between
    (ui/ucare-img {:width 60} ucare-id)
    [:div.flex-auto.left-align.p3 label]
    [:div.p2 (ui/forward-caret {:width 16 :height 16 :color "gray"})]]))

(defn buttons-list-molecule
  [{:list/keys [buttons]}]
  [:div.mt1
   (for [button buttons]
     (shopping-method-choice-button-molecule button))])

(defn shopping-method-choice-title-molecule
  [{:shopping-method-choice-title/keys [id primary secondary]}]
  [:div.left-align
   [:div
    [:div.h1.light primary]
    [:div.h5.mt7 secondary]]])

(defn choose-shopping-method-organism
  [{:match-success/keys [prompt
                         mini-prompt] :as queried-data
    :keys [header-data]} _ _]
  (component/create
   [:div.mx5.mb5
    ;; TODO this weird margin-top should be part of the template/header????
    {:style {:margin-top "95px"}}
    (shopping-method-choice-title-molecule queried-data)
    (buttons-list-molecule queried-data)]))

(def bottom-right-party-background-atom
  {:style {:background-size     "100px"
           :background-position "right 22px bottom 21px"
           :background-repeat   "no-repeat"
           :background-image
           "url(//ucarecdn.com/8a87f86f-948f-48da-b59d-3ca4d8c6d5a0/-/format/png/-/quality/normal/)"}})

(defn component
  [{:match-success/keys [prompt
                         mini-prompt] :as queried-data
    :keys [header-data]} _ _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    bottom-right-party-background-atom
    (header/built-component header-data nil)
    (component/build choose-shopping-method-organism queried-data nil)]))

(defn query
  [app-state]
  ;; TODO servicing-stylist api
  (let [servicing-stylist    (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        stylist-display-name (stylists/->display-name servicing-stylist)]
    {:shopping-method-choice-title/id        "stylist-matching-shopping-method-choice"
     :shopping-method-choice-title/primary   [:div "Congrats on matching with " [:span.bold stylist-display-name] "!"]
     :shopping-method-choice-title/secondary [:div
                                              [:div "Now for the fun part!"]
                                              [:div "How would you like to shop your hair?"]]

     :list/buttons
     [{:shopping-method-choice-button/id       "button-looks"
       :shopping-method-choice-button/label    "Shop by look"
       :shopping-method-choice-button/target   [events/navigate-shop-by-look
                                                {:album-keyword :look}]
       :shopping-method-choice-button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
      {:shopping-method-choice-button/id       "button-a-la-carte"
       :shopping-method-choice-button/label    "Choose individual bundles"
       :shopping-method-choice-button/target   [events/navigate-category
                                                {:page/slug           "mayvenn-install"
                                                 :catalog/category-id "23"}]
       :shopping-method-choice-button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}
      {:shopping-method-choice-button/id       "button-bundle-sets"
       :shopping-method-choice-button/label    "Pre-made bundle sets"
       :shopping-method-choice-button/target   [events/navigate-shop-by-look
                                                {:album-keyword :deals}]
       :shopping-method-choice-button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}]
     :header-data {:back-navigation-message [events/navigate-adventure-stylist-results-pre-purchase]}}))

(defn built-organism
  [app-state]
  (->> app-state
       query
       (component/build component)))
