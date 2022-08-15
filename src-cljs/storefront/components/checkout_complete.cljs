(ns storefront.components.checkout-complete
  (:require [storefront.components.svg :as svg]
            [storefront.accessors.categories :as categories]
            [storefront.api :as api]
            api.orders
            [storefront.accessors.experiments :as experiments]
            [api.catalog :refer [select]]
            [stylist-matching.core :as core
             :refer [stylist-matching<- service-delimiter]]
            [storefront.events :as events]
            [storefront.component :as component :refer [defcomponent
                                                        defdynamic-component]]
            [storefront.components.formatters :as formatters]
            [storefront.components.sign-up :as sign-up]
            [storefront.components.ui :as ui]
            [storefront.accessors.stylists :as stylists]
            [storefront.accessors.sites :as sites]
            [storefront.keypaths :as keypaths]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.shopping-method-choice :as shopping-method-choice]
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            ui.molecules
            [mayvenn.concept.booking :as booking]
            [storefront.platform.messages :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [goog.dom.dataset :as dataset]))

(def scrim-atom
  [:div.absolute.overlay.bg-darken-4])

(defn ^:private divider []
  (component/html
   [:hr.border-top.border-gray.col-12.m0
    {:style {:border-bottom 0
             :border-left 0
             :border-right 0}}]))

(defn matched-component-message-molecule
  [{:matched-component.message/keys [id title body]}]
  (when id
    [:div.center
     [:div.shout.proxima.title-2.flex.items-center.justify-center
      (svg/chat-bubble {:class "mr1 fill-p-color" :height "18px" :width "19px"})
      title]
     [:div.content-2.center.my2 body]]))

(defn matched-with-servicing-stylist-component [{:matched-component.message/keys [id] :as queried-data}]
  (when id
    [:div.bg-white.pt8.pb4.px3.bg-refresh-gray
     {:data-test id}
     (matched-component-message-molecule queried-data)
     (let [{:matched-component.cta/keys [id label target]} queried-data]
       (when id
         [:div.col-10.my2.mx-auto
          (ui/button-large-primary
           (merge (apply utils/route-to target)
                  {:data-test id}) label)]))]))

(defcomponent guest-sign-up
  [{:guest-sign-up/keys [id sign-up-data]
    :as data} _ _]
  (when id
    [:section.center.mt3.col-11.mx-auto
     [:h1.canela.title-2.mt4 "Sign Up"]
     [:p.col-10.col-9-on-tb-dt.proxima.content-3.my3.mx-auto
      "Create a Mayvenn.com account below and enjoy faster checkout, order history, and more."]
     (sign-up/form sign-up-data {:sign-up-text "Create my account"
                                 :hide-email?  true})]))

(defcomponent matching-count-organism
  [{:keys [stylist-count-content]} _ _]
  [:div.col-12.content-3.mt2.mb1.left-align.px3
   stylist-count-content])

(defcomponent non-matching-breaker
  [{:keys [breaker-results breaker-content]} _ _]
  [:div.my5.content-2
   (when breaker-results
     [:div
      breaker-results])
   breaker-content])

(defdynamic-component desktop-results-template
  (did-mount
   [this]
   (let [{:keys [stylist.analytics/cards stylist-results-returned?]}
         (component/get-props this)]
     (when stylist-results-returned?
       (publish events/adventure-stylist-search-results-displayed
                                {:cards cards}))))
  (render
   [this]
   (let [{:keys [stylist-results-present? shopping-method-choice] :as data}
         (component/get-props this)]
     (component/html
      (if stylist-results-present?
        [:div
         (when-let [count-id (:list.stylist-counter/key data)]
           [:div
            {:key count-id}
            (ui/screen-aware matching-count-organism
                             {:stylist-count-content (:list.stylist-counter/title data)}
                             (component/component-id count-id))])
         (when (:list.matching/key data)
           [:div
            (for [data (:list.matching/cards data)]
              [:div {:key (:react/key data)}
               (ui/screen-aware stylist-cards/desktop-organism
                                data
                                (component/component-id (:react/key data)))])])
         (when (:list.breaker/id data)
           [:div
            {:key       "non-matching-breaker"
             :data-test (:list.breaker/id data)}
            (component/build non-matching-breaker
                             {:breaker-results (:list.breaker/results-content data)
                              :breaker-content (:list.breaker/content data)})])
         (when (:list.non-matching/key data)
           [:div
            (for [card (:list.non-matching/cards data)]
              [:div {:key (:react/key card)}
               (ui/screen-aware stylist-cards/organism
                                card
                                (component/component-id (:react/key card)))])])]

        (component/build shopping-method-choice/organism
                         shopping-method-choice))))))

(defdynamic-component results-template
  (did-mount
   [this]
   (let [{:keys [stylist.analytics/cards stylist-results-returned?]}
         (component/get-props this)]
     (when stylist-results-returned?
       (publish events/adventure-stylist-search-results-displayed
                                {:cards cards}))))
  (render
   [this]
   (let [{:keys [stylist-results-present? shopping-method-choice] :as data}
         (component/get-props this)]
     (component/html
      (if stylist-results-present?
        [:div
         (when-let [count-id (:list.stylist-counter/key data)]
           [:div
            {:key count-id}
            (ui/screen-aware matching-count-organism
                             {:stylist-count-content (:list.stylist-counter/title data)}
                             (component/component-id count-id))])
         (when (:list.matching/key data)
           [:div
            (for [data (:list.matching/cards data)]
              [:div {:key (:react/key data)}
               (ui/screen-aware stylist-cards/organism
                                data
                                (component/component-id (:react/key data)))])])
         (when (:list.breaker/id data)
           [:div
            {:key       "non-matching-breaker"
             :data-test (:list.breaker/id data)}
            (component/build non-matching-breaker
                             {:breaker-results (:list.breaker/results-content data)
                              :breaker-content (:list.breaker/content data)})])
         (when (:list.non-matching/key data)
           [:div
            (for [card (:list.non-matching/cards data)]
              [:div {:key (:react/key card)}
               (ui/screen-aware stylist-cards/organism
                                card
                                (component/component-id (:react/key card)))])])]

        (component/build shopping-method-choice/organism
                         shopping-method-choice))))))

(defcomponent component
  [{:thank-you/keys [primary secondary]
    :keys [spinning?
           scrim?
           tertiary
           results] :as data} _ _]
  (ui/narrow-container
   [:div.p3 {:style {:min-height "95vh"}}
    [:div.center
     [:h1.mt5.mb2.canela.title-1 {:data-test "checkout-success-message"} "Thank You"]
     [:div.proxima.content-2 primary]
     (when secondary
       [:div.proxima.content-2.mt4.red secondary])
     (when tertiary
       [:div.proxima.content-2.mt4 tertiary])]

    [:div.hide-on-mb
     #_(component/build desktop-search-inputs-organism desktop-stylist-search-inputs)
     (if spinning?
       [:div.mt6 ui/spinner]
       [:div.relative.stretch
        (component/build desktop-results-template results)
        (when scrim?
          scrim-atom)])]
    [:div.max-580.col-12.mx-auto.hide-on-tb-dt
     #_(component/build search-inputs-organism stylist-search-inputs)
     (if spinning?
       [:div.mt6 ui/spinner]
       [:div.relative.stretch
        (component/build results-template results)
        (when scrim?
          scrim-atom)])]

    [:div.py2.mx-auto.white.border-bottom
     {:style {:border-width "0.5px"}}]
    (matched-with-servicing-stylist-component data)
    (component/build guest-sign-up data nil)]))

(defn shop-query [data]
  (let [{completed-waiter-order :waiter/order}                             (api.orders/completed data)
        {service-items :services/items}                                    (api.orders/services data completed-waiter-order)
        {:mayvenn.concept.booking/keys [selected-date selected-time-slot]} (booking/<- data)
        appointment-selected                                               (and selected-date selected-time-slot)
        easy-booking?                                                      (experiments/easy-booking? data)]
    (when (seq service-items)
      (merge
       {:thank-you/primary
        (str "We've received your order and a Mayvenn Concierge representative will contact you to "
             (if (and easy-booking? appointment-selected)
               "confirm your"
               "make an")
             " appointment within 3 business days.")}))))

(defn- address->display-string
  [{:keys [address-1 address-2 city state zipcode]}]
  (string/join " "
               [(string/join ", " (->> [address-1 address-2 city state]
                                       (remove string/blank?)))
                zipcode]))

(defn stylist-card<-
  [idx stylist]
  (let [{:keys [rating-star-counts
                salon
                service-menu
                gallery-images
                store-slug
                store-nickname
                stylist-id
                stylist-since
                top-stylist
                rating]}                      stylist
        rating-count                          (->> rating-star-counts vals (reduce +))
        newly-added-stylist                   (< rating-count 3)
        show-newly-added-stylist-ui?          newly-added-stylist
        years-of-experience                   (some->> stylist-since (- (date/year (date/now))))
        {:keys [latitude longitude]}     salon
        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal
                specialty-wig-customization]} service-menu]
    (merge {:react/key                       (str "stylist-card-" store-slug)
            :stylist-card.header/target      [events/flow|stylist-matching|selected-for-inspection {:stylist-id stylist-id
                                                                                               :store-slug store-slug}]
            :stylist-card.header/id          (str "stylist-card-header-" store-slug)
            :stylist-card.thumbnail/id       (str "stylist-card-thumbnail-" store-slug)
            :stylist-card.thumbnail/ucare-id (-> stylist :portrait :resizable-url)
            :stylist-card.thumbnail/alt      (str (stylists/->display-name stylist) "'s profile picture")

            :stylist-card.title/id      "stylist-name"
            :stylist-card.title/primary (stylists/->display-name stylist)
            :rating/value               rating
            :rating/count               rating-count
            :rating/id                  (when (not show-newly-added-stylist-ui?)
                                          (str "rating-count-" store-slug))
            :analytics/rating           (when (not show-newly-added-stylist-ui?)
                                          rating)
            :analytics/lat              latitude
            :analytics/long             longitude

            :analytics/just-added?             (when newly-added-stylist
                                                 show-newly-added-stylist-ui?)
            :analytics/years-of-experience     (when (and newly-added-stylist
                                                          years-of-experience)
                                                 years-of-experience)
            :analytics/stylist-id              stylist-id
            :analytics/top-stylist             top-stylist
            :stylist.just-added/id             (when show-newly-added-stylist-ui?
                                                 (str "just-added-" store-slug))
            :stylist.just-added/content        "Just Added"
            :stylist-ratings/id                (when (not show-newly-added-stylist-ui?)
                                                 (str "stylist-ratings-" store-slug))
            :stylist-ratings/content           (str "(" rating ")")
            :stylist-experience/id             (when newly-added-stylist
                                                 (str "stylist-experience-" store-slug))
            :stylist-experience/content        (str (ui/pluralize-with-amount years-of-experience "year") " of experience")
            :stylist-card.services-list/id     (str "stylist-card-services-" store-slug)
            :stylist-card.services-list/items  [{:id         (str "stylist-service-leave-out-" store-slug)
                                                 :label      "Leave Out"
                                                 :value      (boolean specialty-sew-in-leave-out)
                                                 :preference :leave-out}
                                                {:id         (str "stylist-service-closure-" store-slug)
                                                 :label      "Closure"
                                                 :value      (boolean specialty-sew-in-closure)
                                                 :preference :closure}
                                                {:id         (str "stylist-service-frontal-" store-slug)
                                                 :label      "Frontal"
                                                 :value      (boolean specialty-sew-in-frontal)
                                                 :preference :frontal}
                                                {:id         (str "stylist-service-360-" store-slug)
                                                 :label      "360° Frontal"
                                                 :value      (boolean specialty-sew-in-360-frontal)
                                                 :preference :360-frontal}
                                                {:id         (str "stylist-service-wig-customization-" store-slug)
                                                 :label      "Wig Customization"
                                                 :value      (boolean specialty-wig-customization)
                                                 :preference :wig-customization}]
            :stylist-card.gallery/id           (str "stylist-card-gallery-" store-slug)
            :stylist-card.gallery/items        (let [ucare-img-urls (map :resizable-url gallery-images)]
                                                 (map-indexed
                                                  (fn [j ucare-img-url]
                                                    {:stylist-card.gallery-item/id       (str "gallery-img-" stylist-id "-" j)
                                                     :stylist-card.gallery-item/target   [events/navigate-adventure-stylist-gallery
                                                                                          {:store-slug   store-slug
                                                                                           :stylist-id   stylist-id
                                                                                           :query-params {:offset j}}]
                                                     :stylist-card.gallery-item/ucare-id ucare-img-url})
                                                  ucare-img-urls))
            :stylist-card.address-marker/id    (str "stylist-card-address-" store-slug)
            :stylist-card.address-marker/value (address->display-string salon)})))

(defn stylist-cards-query
  [_ stylists]
  (map-indexed
   (partial stylist-card<-)
   stylists))

(defn stylist-data->stylist-cards
  [{:keys [stylists] :as data}]
  (when (seq stylists)
    (into [] (mapcat identity [(stylist-cards-query data stylists)]))))

(def shopping-method-choice-query
  {:shopping-method-choice.error-title/id        "stylist-matching-shopping-method-choice"
   :shopping-method-choice.error-title/primary   "We need some time to find you the perfect stylist!"
   :shopping-method-choice.error-title/secondary (str
                                                  "A Mayvenn representative will contact you soon "
                                                  "to help select a Certified Mayvenn Stylist. In the meantime…")

   :list/buttons [{:shopping-method-choice.button/id       "button-looks"
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
                   :shopping-method-choice.button/ucare-id "6c39cd72-6fde-4ec2-823c-5e39412a6d54"}
                  {:shopping-method-choice.button/id       "button-shop-wigs"
                   :shopping-method-choice.button/label    "Shop Virgin Wigs"
                   :shopping-method-choice.button/target   [events/navigate-category
                                                            {:page/slug           "wigs"
                                                             :catalog/category-id "13"
                                                             :query-params
                                                             {:family
                                                              (str "lace-front-wigs"
                                                                   categories/query-param-separator
                                                                   "360-wigs")}}]
                   :shopping-method-choice.button/ucare-id "71dcdd17-f9cc-456f-b763-2c1c047c30b4"}]})

(defn results<
  [{:param/keys   [services]
    :results/keys [stylists]
    :keys         [status]}]
  (let [{matching-stylists     true
         non-matching-stylists false}             (->> stylists
                                                       (group-by (partial core/matches-preferences?
                                                                      services)))
        top-stylist                               (some #(when (:top-stylist %) %) matching-stylists)
        matching-stylists-with-top-stylist-bumped (if top-stylist
                                                    (concat [top-stylist]
                                                            (remove #(= (:stylist-id %)
                                                                        (:stylist-id top-stylist)) matching-stylists))
                                                    matching-stylists)
        premium-stylist-ids                       [111023 111025 111026 96040 1663 111024 111029]
        premium-stylists                          (when-let [premium-stylists (filter #((set premium-stylist-ids) (:stylist-id %))
                                                                                      matching-stylists-with-top-stylist-bumped)]
                                                    (concat premium-stylists
                                                            (remove #((set premium-stylist-ids) (:stylist-id %))
                                                                    matching-stylists-with-top-stylist-bumped)))
        matching-stylist-cards                    (stylist-data->stylist-cards
                                                   {:stylists               premium-stylists})
        non-matching-stylist-cards                (stylist-data->stylist-cards
                                                   {:stylists               non-matching-stylists})]

    {:stylist-results-present? (seq (concat matching-stylists non-matching-stylists))

     :stylist-results-returned?    (contains? status :results/stylists)
     :list.stylist-counter/title   (str (count matching-stylists) " Stylists Found")
     :list.stylist-counter/key     (when (pos? (count matching-stylists))
                                     "stylist-count-content")
     :list.matching/key            (when (seq matching-stylists) "stylist-matching")
     :list.matching/cards          matching-stylist-cards
     :list.breaker/id              (when (seq non-matching-stylists) "non-matching-breaker")
     :list.breaker/results-content (when (and (seq non-matching-stylists)
                                              (empty? matching-stylists))
                                     "0 results found")
     :list.breaker/content         "Other stylists in your area"
     :list.non-matching/key        (when (seq non-matching-stylists) "non-matching-stylists")
     :list.non-matching/cards      non-matching-stylist-cards
     :stylist.analytics/cards      (into matching-stylist-cards non-matching-stylist-cards)
     :shopping-method-choice       shopping-method-choice-query}))

;;; Discountable promotions
(def ^:private ?bundles
  {:catalog/department #{"hair"}
   :hair/family        #{"bundles"}})

(def ^:private ?closures
  {:catalog/department #{"hair"}
   :hair/family        #{"closures"}})

(def ^:private ?frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"frontals"}})

(def ^:private ?360-frontals
  {:catalog/department #{"hair"}
   :hair/family        #{"360-frontals"}})

(def ^:private ?wigs
  {:catalog/department #{"hair"}
   :hair/family        #{"lace-front-wigs" "360-wigs"}})

(def SV2-rules
  {"LBI"  [["bundle" ?bundles 3]]
   "UPCW" [["bundle" ?bundles 3]]
   "CBI"  [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "CLCW" [["bundle" ?bundles 2] ["closure" ?closures 1]]
   "FBI"  [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "LFCW" [["bundle" ?bundles 2] ["frontal" ?frontals 1]]
   "3BI"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "3CW"  [["bundle" ?bundles 2] ["360 frontal" ?360-frontals 1]]
   "WGC"  [["Virgin Lace Front or a Virgin 360 Wig" ?wigs 1]]})

(defn ^:private failed-rules [waiter-order rule]
  (let [physical-items (->> waiter-order
                            :shipments
                            (mapcat :line-items)
                            (filter (fn [item]
                                      (= "spree" (:source item))))
                            (map (fn [item]
                                   (merge
                                    (dissoc item :variant-attrs)
                                    (:variant-attrs item)))))]
    (keep (fn [[word essentials rule-quantity]]
            (let [cart-quantity    (->> physical-items
                                        (select essentials)
                                        (map :quantity)
                                        (apply +))
                  missing-quantity (- rule-quantity cart-quantity)]
              (when (pos? missing-quantity)
                {:word             word
                 :cart-quantity    cart-quantity
                 :missing-quantity missing-quantity
                 :essentials       essentials})))
          rule)))

(defn query
  [data]
  (let [shop?        (= :shop (sites/determine-site data))
        guest?       (not (get-in data keypaths/user-id))
        matching     (stylist-matching.core/stylist-matching<- data)
        waiter-order (:waiter/order (api.orders/completed data))]

    (cond->
        {:thank-you/primary "We've received your order and will contact you as soon as your package is shipped."}

      guest?
      (merge
       (let [sign-up-data (sign-up/query data)]
         (merge
          {:guest-sign-up/id           "guest-sign-up"
           :guest-sign-up/sign-up-data (sign-up/query data)})))

      shop?
      (merge (shop-query data))

      (and (:remove-free-install (get-in data storefront.keypaths/features))
           (->> SV2-rules
                vals
                (some (comp empty? (partial failed-rules waiter-order))))
           (seq matching))
      (merge {:spinning (or
                         (utils/requesting? data request-keys/fetch-stylists)
                         (utils/requesting? data request-keys/fetch-stylists-matching-filters))
              :tertiary "Check out these stylists in your area that can help you install your hair."
              :results (results< matching)}))))

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
