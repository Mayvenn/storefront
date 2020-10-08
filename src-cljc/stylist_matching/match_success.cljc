(ns stylist-matching.match-success
  (:require #?@(:cljs [[storefront.history :as history]
                       [storefront.accessors.orders :as orders]])
            adventure.keypaths
            api.orders
            [clojure.string :as string]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.header :as header]
            [storefront.effects :as effects]
            [storefront.events :as events]
            storefront.keypaths
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.matched-stylist :as matched-stylist]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist-pre-purchase
  [_ _ {:keys [order]} app-state]
  (assoc-in app-state storefront.keypaths/order order))

(defmethod transitions/transition-state events/api-success-assign-servicing-stylist
  [_ _ {:keys [servicing-stylist]} app-state]
  (assoc-in app-state adventure.keypaths/adventure-servicing-stylist servicing-stylist))

(defmethod effects/perform-effects events/api-success-assign-servicing-stylist-pre-purchase [_ _ _ _ app-state]
  #?(:cljs
     (let [current-order            (api.orders/current app-state)
           {services       "service"
            physical-items "spree"} (group-by :source (orders/product-and-service-items (:waiter/order current-order)))]
       (history/enqueue-navigate
        (cond (and (seq services) (seq physical-items))
              events/navigate-cart

              (seq services)
              events/navigate-adventure-match-success-pre-purchase

              :else
              events/navigate-adventure-match-success-pre-purchase-pick-service)))))

(defmethod effects/perform-effects events/navigate-adventure-match-success-pre-purchase
  [_ _ _ _ app-state]
  #?(:cljs
     (when (nil? (get-in app-state adventure.keypaths/adventure-servicing-stylist))
       (history/enqueue-redirect events/navigate-adventure-find-your-stylist))))

(defn header-query
  [{:order.items/keys [quantity]}
   browser-history]
  (cond-> {:header.title/id               "adventure-title"
           :header.title/primary          "Meet Your Stylist"
           :header.back-navigation/id     "adventure-back"
           :header.back-navigation/target [events/navigate-adventure-find-your-stylist]
           :header.cart/id                "mobile-cart"
           :header.cart/value             quantity
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
     :stylist-card.title/primary        (stylists/->display-name stylist)
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

(defn matched-stylist-query
  [servicing-stylist {:order/keys [submitted?] :order.shipping/keys [phone]}]
  (when submitted?
    (merge {:matched-stylist.title/id            "matched-with-stylist"
            :matched-stylist.title/primary       "Chat with your Stylist"
            :matched-stylist.title/secondary     [:div.h5.line-height-3.center
                                                  "A group text message will be sent to "
                                                  (if phone
                                                    [:span.bold.nowrap (formatters/phone-number phone)]
                                                    "you")
                                                  " and your stylist, "
                                                  [:span.nowrap {:data-test "servicing-stylist-name"}
                                                   (-> servicing-stylist :address :firstname)]
                                                  "."]
            :matched-stylist.cta-title/id        "matched-stylist-cta"
            :matched-stylist.cta-title/label     "View #MayvennFreeInstall"
            :matched-stylist.cta-title/primary   "In the meantime…"
            :matched-stylist.cta-title/secondary "Get inspired for your appointment"
            :matched-stylist.cta-title/target    ["https://www.instagram.com/explore/tags/mayvennfreeinstall/"]}
           (stylist-card-query servicing-stylist))))

(defn shopping-method-choice-query
  [servicing-stylist {:order/keys [submitted?]}]
  (when-not submitted?
    {:shopping-method-choice.title/id        "stylist-matching-shopping-method-choice"
     :shopping-method-choice.title/primary   [:div "Congratulations on matching with "
                                              (stylists/->display-name servicing-stylist)
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
  [{:keys [header shopping-method-choice matched-stylist]} _ _]
  [:div.center.flex.flex-auto.flex-column
   stylist-matching.A/bottom-right-party-background
   (header/adventure-header header)
   (component/build shopping-method-choice/organism shopping-method-choice nil)
   (component/build matched-stylist/organism matched-stylist nil)])

(defn page
  [app-state]
  (let [servicing-stylist (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        order             (api.orders/current app-state)
        browser-history   (get-in app-state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:header                 (header-query order browser-history)
                      :shopping-method-choice (shopping-method-choice-query servicing-stylist
                                                                            order)
                      :matched-stylist        (matched-stylist-query servicing-stylist
                                                                     order)})))
