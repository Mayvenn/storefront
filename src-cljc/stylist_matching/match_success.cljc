(ns stylist-matching.match-success
  (:require #?@(:cljs [[storefront.history :as history]])
            api.current
            api.orders
            api.stylist
            [clojure.string :as string]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]))

(defmethod effects/perform-effects events/navigate-adventure-match-success
  [_ _ _ _ state]
  (when-not (api.current/stylist state)
    #?(:cljs
       (history/enqueue-redirect events/navigate-adventure-find-your-stylist))))

(defn header-query
  [{:order.items/keys [quantity]}
   browser-history]
  (cond-> {:header.title/id               "adventure-title"
           :header.title/primary          "Meet Your Stylist"
           :header.back-navigation/id     "adventure-back"
           :header.back-navigation/target [events/navigate-adventure-find-your-stylist]
           :header.cart/id                "mobile-cart"
           :header.cart/value             (or quantity 0)
           :header.cart/color             "white"}

    (seq browser-history)
    (merge {:header.back-navigation/back (first browser-history)})))

(defn stylist-card-query
  [{:keys [salon service-menu store-slug stylist-id rating] :as stylist}]
  (let [{salon-name :name
         :keys      [address-1 address-2 city state zipcode]} salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal]}                    service-menu]
    {:react/key                         (str "stylist-card-" store-slug)
     :stylist-card.header/target        [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                    :store-slug store-slug}]
     :stylist-card.header/id            (str "stylist-card-header" store-slug)
     :stylist-card.thumbnail/id         (str "stylist-card-thumbnail-" store-slug)
     :stylist-card.thumbnail/ucare-id   (-> stylist :portrait :resizable-url)
     :stylist-card.title/id             "stylist-name"
     :stylist-card.title/primary        (api.stylist/->display-name stylist)
     :rating/value                      rating
     :stylist-card.services-list/id     (str "stylist-card-services-" store-slug)
     :stylist-card.services-list/items  [{:id    (str "stylist-service-leave-out-" store-slug)
                                          :label "Leave Out"
                                          :value (boolean specialty-sew-in-leave-out)}
                                         {:id    (str "stylist-service-closure-" store-slug)
                                          :label "Closure"
                                          :value (boolean specialty-sew-in-closure)}
                                         {:id    (str "stylist-service-frontal-" store-slug)
                                          :label "Frontal"
                                          :value (boolean specialty-sew-in-frontal)}
                                         {:id    (str "stylist-service-360-" store-slug)
                                          :label "360° Frontal"
                                          :value (boolean specialty-sew-in-360-frontal)}]
     :element/type                      :stylist-card
     :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
     :stylist-card.address-marker/value (string/join " "
                                                     [(string/join ", "
                                                                   (remove string/blank? [address-1 address-2 city state]))
                                                      zipcode])}))

(defn shopping-method-choice-query
  [servicing-stylist {:order/keys [submitted?]}]
  (when-not submitted?
    {:shopping-method-choice.title/id        "stylist-matching-shopping-method-choice"
     :shopping-method-choice.title/primary   [:div "Congratulations on matching with "
                                              (api.stylist/->display-name servicing-stylist)
                                              "!"]
     :shopping-method-choice.title/secondary [:div
                                              [:div "Now for the fun part!"]
                                              [:div "How would you like to shop your hair?"]]
     :list/buttons
     (cond-> [{:shopping-method-choice.button/id       "button-looks"
               :shopping-method-choice.button/label    "Shop by look"
               :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                        {:album-keyword :look}]
               :shopping-method-choice.button/ucare-id "a9009728-efd3-4917-9541-b4514b8e4776"}
              {:shopping-method-choice.button/id       "button-bundle-sets"
               :shopping-method-choice.button/label    "Pre-made bundle sets"
               :shopping-method-choice.button/target   [events/navigate-shop-by-look
                                                        {:album-keyword :all-bundle-sets}]
               :shopping-method-choice.button/ucare-id "87b46db7-4c70-4d3a-8fd0-6e99e78d3c96"}
              {:shopping-method-choice.button/id       "button-a-la-carte"
               :shopping-method-choice.button/label    "Choose individual bundles"
               :shopping-method-choice.button/target   [events/navigate-category
                                                        {:page/slug           "mayvenn-install"
                                                         :catalog/category-id "23"}]
               :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}]

       (-> servicing-stylist
           :service-menu
           :specialty-wig-customization)
       (concat [{:shopping-method-choice.button/id       "button-shop-wigs"
                 :shopping-method-choice.button/label    "Shop Virgin Wigs"
                 :shopping-method-choice.button/target   [events/navigate-category
                                                          {:page/slug           "wigs"
                                                           :catalog/category-id "13"
                                                           :query-params        {:family (str "lace-front-wigs" accessors.categories/query-param-separator "360-wigs")}}]
                 :shopping-method-choice.button/ucare-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"}]))}))

(defcomponent template
  [{:keys [header shopping-method-choice]} _ _]
  [:div.center.flex.flex-auto.flex-column
   stylist-matching.A/bottom-right-party-background
   (header/adventure-header header)
   (component/build shopping-method-choice/organism shopping-method-choice nil)])

(defn ^:export page
  [app-state _]
  (let [servicing-stylist (:diva/stylist (api.current/stylist app-state))
        order             (api.orders/current app-state)
        browser-history   (get-in app-state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:header                 (header-query order browser-history)
                      :shopping-method-choice (shopping-method-choice-query servicing-stylist
                                                                            order)})))
