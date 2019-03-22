(ns adventure.a-la-carte.product-list
  (:require [storefront.components.ui :as ui]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.components.product-card :as product-card]
            [adventure.components.header :as header]
            adventure.checkout.cart.items
            [storefront.effects :as effects]
            [adventure.progress]
            [adventure.keypaths]
            [storefront.request-keys :as request-keys]
            [adventure.keypaths :as adventure.keypaths]))

(defn ^:private query
  [data]
  (let [products          (vals (get-in data keypaths/v2-products))
        adventure-choices (get-in data adventure.keypaths/adventure-choices)
        stylist-selected? (some-> adventure-choices :flow #{"match-stylist"})
        current-step      (if stylist-selected? 3 2)]
    (merge
     {:prompt-image      "//ucarecdn.com/4d53dac6-a7ce-4c10-bd5d-644821c5af4b/-/format/auto/"
      :data-test         "product-list"
      :current-step      current-step
      :footer            [:div.h6.center.pb8
                          [:div.dark-gray "Not ready to shop hair?"]
                          [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
                           "Find a stylist"]]
      :header-data       {:title                   "The New You"
                          :progress                adventure.progress/a-la-carte-product-list
                          :back-navigation-message [events/navigate-adventure-a-la-carte-hair-color]
                          :subtitle                (str "Step " current-step " of 3")
                          :shopping-bag?           true}
      :spinning?         (utils/requesting-from-endpoint? data request-keys/search-v2-products)
      :stylist-selected? stylist-selected?
      :product-cards     (->> products
                              (map (partial product-card/query data))
                              (sort-by (comp :sku/price :cheapest-sku)))}
     (adventure.checkout.cart.items/freeinstall-line-item-query data))))

(def qualified-banner
  [:div.flex.items-center.bold
   [:div.col.col-12.center.white
    [:div.h5.light "This order qualifies for a"]
    [:div.h1.shout "free install"]
    [:div.h5.light "from a Mayvenn Stylist near you"]]])

(defn add-more-hair-banner
  [number-of-items-needed]
  [:div.py4.px2.my2.mx4
   {:data-test "adventure-add-more-hair-banner"}
   [:div.h5.medium.center.px2
    "Add " [:span.pyp1.px1.bold.white.bg-purple.center
            number-of-items-needed]
    " more " (ui/pluralize number-of-items-needed "item")
    " to get a free install from a Mayvenn Certified Stylist"]])

(defn ^:private component
  [{:keys [add-more-hair?
           data-test
           header-data
           number-of-items-needed
           product-cards
           prompt-image
           spinning?
           stylist-selected?]}
   _ _]
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
      (if add-more-hair?
        (add-more-hair-banner number-of-items-needed)
        qualified-banner)]]
    (if spinning?
      [:div.flex.items-center.justify-center.h0.mt3
       ui/spinner]
      [:div.flex.flex-wrap.px5.col-12
       (for [product-card product-cards]
         (component/build product-card/component product-card nil))])
    (when-not stylist-selected?
      [:div.h6.center.pb8.mx-auto
       [:div.dark-gray "Not ready to shop hair?"]
       [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
        "Find a stylist"]])]))

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-a-la-carte-product-list
  [_ _ args _ app-state]
  #?(:cljs (messages/handle-message events/adventure-fetch-matched-products {:criteria [:hair/texture :hair/family]})))
