(ns adventure.shopbybundles.product-list
  (:require #?@(:cljs [[storefront.platform.messages :refer [handle-message]]])
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.components.product-card :as product-card]
            [adventure.components.header :as header]
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]
            [storefront.accessors.skus :as skus]))

(defn ^:private query [data]
  (let [skus                (get-in data adventure-keypaths/adventure-matching-skus)
        products            (get-in data adventure-keypaths/adventure-matching-products)
        adventure-choices   (get-in data adventure-keypaths/adventure-choices)
        stylist-selected?   (some-> adventure-choices :flow #{"match-stylist"})
        current-step        (if stylist-selected? 3 2)
        facets              (get-in data keypaths/v2-facets)
        #_#_color-order-map (facets/color-order-map facets)]
    {:prompt-image           "//ucarecdn.com/4d53dac6-a7ce-4c10-bd5d-644821c5af4b/-/format/auto/bg.png"
     :data-test              "product-list"
     :current-step           current-step
     :footer                 [:div.h6.center.pb8
                              [:div.dark-gray "Not ready to shop hair?"]
                              [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
                               "Find a stylist"]]
     :header-data            {:title                   "The New You"
                              :progress                progress/shopbybundles-product-list
                              :back-navigation-message [events/navigate-adventure-shopbybundles-hair-color]
                              :subtitle                (str "Step " current-step " of 3")}
     :stylist-selected?      stylist-selected?
     :product-cards          (map (partial product-card/query data) products)}))

(defn ^:private component
  [{:as   data
    :keys [prompt-image header-data data-test stylist-selected? product-cards]} _ _]
  (component/create
   [:div.bg-too-light-teal.white.center.flex-auto.self-stretch
    (when header-data
      (header/built-component header-data nil))
    [:div.flex.items-center.bold.bg-light-lavender
     {:style {:height              "246px"
              :padding-top         "46px"
              :background-size     "cover"
              :background-position "center"
              :background-image    (str "url('" prompt-image "')")}}
     [:div.col.col-12
      [:div.h3 "Add 3 more items to get a free install from a Mayvenn Certified Stylist"]]]
    [:div.clearfix.px5
     (for [product-card product-cards]
       [:div.my2.black {:key (:slug product-card)}
        (component/build product-card/component product-card nil)])]
    (when-not stylist-selected?
      [:div.h6.center.pb8
       [:div.dark-gray "Not ready to shop hair?"]
       [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
        "Find a stylist"]])]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-shopbybundles-product-list
  [_ _ args _ app-state]
  #?(:cljs (handle-message events/adventure-fetch-matched-products {:criteria [:hair/texture :hair/family :hair/color]})))
