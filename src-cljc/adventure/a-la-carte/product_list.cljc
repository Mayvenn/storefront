(ns adventure.a-la-carte.product-list
  (:require #?@(:cljs [[storefront.platform.messages :refer [handle-message]]])
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [adventure.components.product-card :as product-card]
            [adventure.components.header :as header]
            adventure.checkout.cart.items
            [storefront.effects :as effects]
            [storefront.transitions :as transitions]
            [adventure.progress]
            [adventure.keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]
            [storefront.accessors.skus :as skus]
            [adventure.keypaths :as adventure.keypaths]))

(defn ^:private query
  [data]
  (let [skus              (get-in data adventure.keypaths/adventure-matching-skus)
        products          (get-in data adventure.keypaths/adventure-matching-products)
        adventure-choices (get-in data adventure.keypaths/adventure-choices)
        stylist-selected? (some-> adventure-choices :flow #{"match-stylist"})
        current-step      (if stylist-selected? 3 2)
        facets            (get-in data keypaths/v2-facets)]
    (merge
     {:prompt-image      "//ucarecdn.com/4d53dac6-a7ce-4c10-bd5d-644821c5af4b/-/format/auto/bg.png"
      :data-test         "product-list"
      :current-step      current-step
      :footer            [:div.h6.center.pb8
                          [:div.dark-gray "Not ready to shop hair?"]
                          [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
                           "Find a stylist"]]
      :header-data       {:title                   "The New You"
                          :progress                adventure.progress/a-la-carte-product-list
                          :back-navigation-message [events/navigate-adventure-shopbybundles-hair-color]
                          :subtitle                (str "Step " current-step " of 3")
                          :shopping-bag?           true}
      :stylist-selected? stylist-selected?
      :product-cards     (map (partial product-card/query data) products)}
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
