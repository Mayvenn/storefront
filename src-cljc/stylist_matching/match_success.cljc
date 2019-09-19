(ns stylist-matching.match-success
  (:require [storefront.component :as component]
            [storefront.events :as events]
            adventure.keypaths
            api.orders
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.formatters :as formatters]
            [stylist-directory.stylists :as stylists]
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.matched-stylist :as matched-stylist]
            [adventure.keypaths :as adventure.keypaths]
            [storefront.keypaths :as storefront.keypaths]))

(defn header-query
  [{:order.items/keys [quantity]
    :order/keys       [submitted?]}]
  (cond-> {:header.title/id           "adventure-title"
           :header.title/primary      "Meet Your Certified Stylist"
           :header.back-navigation/id "adventure-back"}

    submitted?
    (merge {:header.back-navigation/target [events/navigate-adventure-stylist-results-post-purchase]})

    (not submitted?)
    (merge {:header.back-navigation/target [events/navigate-adventure-stylist-results-pre-purchase]
            :header.cart/id                "mobile-cart"
            :header.cart/value             quantity
            :header.cart/color             "white"})))

(defn stylist-card-query
  [stylist-profiles? {:keys [salon service-menu store-slug stylist-id] :as stylist}]
  (let [{salon-name :name
         :keys      [address-1 address-2 city state zipcode]} salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal]}                    service-menu]
    (cond-> {:react/key                        (str "stylist-card-" store-slug)
             :stylist-card/target              [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                           :store-slug store-slug}]
             :stylist-card/id                  (str "stylist-card-" store-slug)
             :stylist-card.thumbnail/id        (str "stylist-card-thumbnail-" store-slug)
             :stylist-card.thumbnail/ucare-id  (-> stylist :portrait :resizable-url)
             :stylist-card.title/id            "stylist-name"
             :stylist-card.title/primary       (stylists/->display-name stylist)
             :rating/value                     (:rating stylist)
             :stylist-card.services-list/id    (str "stylist-card-services-" store-slug)
             :stylist-card.services-list/value [(stylist-cards/checks-or-x-atom "Leave Out"
                                                                                (boolean specialty-sew-in-leave-out))
                                                (stylist-cards/checks-or-x-atom "Closure"
                                                                                (boolean specialty-sew-in-closure))
                                                (stylist-cards/checks-or-x-atom "360° Frontal"
                                                                                (boolean specialty-sew-in-360-frontal))
                                                (stylist-cards/checks-or-x-atom "Frontal" (boolean specialty-sew-in-frontal))]}

      (not stylist-profiles?) ;; Control
      (merge
       (let [phone-number             (some-> stylist :address :phone formatters/phone-number)
             google-maps-redirect-url (str "https://www.google.com/maps/place/"
                                           (string/join "+" (list address-1 address-2 city state zipcode)))
             detail-attributes        [(when (:licensed stylist)
                                         "Licensed")
                                       (case (-> stylist :salon :salon-type)
                                         "salon"   "In-Salon"
                                         "in-home" "In-Home"
                                         nil)
                                       (when (:stylist-since stylist)
                                         (str (ui/pluralize-with-amount
                                               (- (date/year (date/now)) (:stylist-since stylist))
                                               "yr")
                                              " Experience"))]]
         {:element/type                      :control-stylist-card
          :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
          :stylist-card.address-marker/value [:div
                                              [:div.bold.line-height-4.py1
                                               [:div.dark-gray salon-name]
                                               [:a.navy
                                                (merge
                                                 {:data-test "stylist-salon-address"}
                                                 (utils/route-to events/control-adventure-stylist-salon-address-clicked
                                                                 {:stylist-id               (:stylist-id stylist)
                                                                  :google-maps-redirect-url google-maps-redirect-url}))
                                                [:div (string/join ", " [address-1 address-2])]
                                                [:div
                                                 (string/join ", " [city state])
                                                 " "
                                                 zipcode]]]
                                              (ui/link :link/phone
                                                       :a.navy.light.my3
                                                       {:data-test "stylist-phone"
                                                        :on-click
                                                        (utils/send-event-callback events/control-adventure-stylist-phone-clicked
                                                                                   {:stylist-id   (:stylist-id stylist)
                                                                                    :phone-number phone-number})}
                                                       phone-number)
                                              [:div.dark-gray
                                               (into [:div.flex.flex-wrap]
                                                     (comp
                                                      (remove nil?)
                                                      (map (fn [x] [:div x]))
                                                      (interpose [:div.mxp3 "·"]))
                                                     detail-attributes)]]}))

      stylist-profiles? ;; Experiment Variation
      (merge {:element/type                      :experiment-stylist-card
              :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
              :stylist-card.address-marker/value (string/join " "
                                                              [(string/join ", "
                                                                            [address-1 address-2 city state])
                                                               zipcode ])}))))

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
           (stylist-card-query false servicing-stylist))))

(defn shopping-method-choice-query
  [servicing-stylist {:order/keys [submitted?]}]
  (when-not submitted?
    {:shopping-method-choice.title/id        "stylist-matching-shopping-method-choice"
     :shopping-method-choice.title/primary   [:div "Congrats on matching with "
                                              [:span.bold (stylists/->display-name servicing-stylist)]
                                              "!"]
     :shopping-method-choice.title/secondary [:div
                                              [:div "Now for the fun part!"]
                                              [:div "How would you like to shop your hair?"]]
     :list/buttons
     [{:shopping-method-choice.button/id       "button-looks"
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
       :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}]}))

(def pre-purchase? #{events/navigate-adventure-stylist-results-pre-purchase
                     events/navigate-adventure-match-success-pre-purchase})

(defn template
  [{:keys [header shopping-method-choice matched-stylist]} _ _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    stylist-matching.A/bottom-right-party-background
    (component/build header/organism header nil)
    (component/build shopping-method-choice/organism shopping-method-choice nil)
    (component/build matched-stylist/organism matched-stylist nil)]))

(defn page
  [app-state]
  (let [servicing-stylist (get-in app-state adventure.keypaths/adventure-servicing-stylist)
        order             (if (pre-purchase? (get-in app-state storefront.keypaths/navigation-event))
                            (api.orders/current app-state)
                            (api.orders/completed app-state))]
    (component/build template
                     {:header                 (header-query order)
                      :shopping-method-choice (shopping-method-choice-query servicing-stylist
                                                                            order)
                      :matched-stylist        (matched-stylist-query servicing-stylist
                                                                     order)})))
