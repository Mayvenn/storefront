(ns stylist-matching.stylist-results
  (:require adventure.keypaths
            api.orders
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component]
            [storefront.events :as events]
            [stylist-matching.ui.header :as header]
            [stylist-matching.ui.stylist-cards :as stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            [adventure.organisms.call-out-center :as call-out-center]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.formatters :as formatters]
            [storefront.components.ui :as ui]
            [spice.date :as date]))

(defn header-query
  [{:order.items/keys [quantity]}
   back]
  {:header.cart/id                "adventure-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"
   :header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Certified Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/back   back
   :header.back-navigation/target [events/navigate-home]})

(defn stylist-card-query
  [stylist-profiles?
   navigation-event
   idx
   {:keys [salon service-menu gallery-images store-slug stylist-id] :as stylist}]
  (let [{salon-name :name :keys [address-1 address-2 city state zipcode]} salon

        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal]} service-menu
        cta-event                          (if (= events/navigate-adventure-stylist-results-pre-purchase navigation-event)
                                             events/control-adventure-select-stylist-pre-purchase
                                             events/control-adventure-select-stylist-post-purchase)]
    (cond-> {:react/key                       (str "stylist-card-" store-slug)

             :stylist-card/target             [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                          :store-slug store-slug}]
             :stylist-card/id                 (str "stylist-card-" store-slug)
             :stylist-card.thumbnail/id       (str "stylist-card-thumbnail-" store-slug)
             :stylist-card.thumbnail/ucare-id (-> stylist :portrait :resizable-url)

             :stylist-card.title/id             "stylist-name"
             :stylist-card.title/primary        (stylists/->display-name stylist)
             :rating/value                      (:rating stylist)
             :stylist-card.services-list/id     (str "stylist-card-services-" store-slug)
             :stylist-card.services-list/value  [(stylist-cards/checks-or-x-atom "Leave Out"
                                                                                 (boolean specialty-sew-in-leave-out))
                                                 (stylist-cards/checks-or-x-atom "Closure"
                                                                                 (boolean specialty-sew-in-closure))
                                                 (stylist-cards/checks-or-x-atom "360° Frontal"
                                                                                 (boolean specialty-sew-in-360-frontal))
                                                 (stylist-cards/checks-or-x-atom "Frontal" (boolean specialty-sew-in-frontal))]
             :stylist-card.cta/id               (str "select-stylist-" store-slug)
             :stylist-card.cta/label            "Select"
             :stylist-card.cta/target           [cta-event
                                                 {:servicing-stylist stylist
                                                  :card-index        idx}]

             :stylist-card.gallery/id           (str "stylist-card-gallery-" store-slug)}

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
          :stylist-card.gallery/items
          (let [ucare-img-urls (map :resizable-url gallery-images)]
            (map-indexed
             (fn [j ucare-img-url]
               {:stylist-card.gallery-item/id       (str "gallery-img-" stylist-id "-" j)
                :stylist-card.gallery-item/target   [events/control-adventure-stylist-gallery-open
                                                     {:ucare-img-urls                 ucare-img-urls
                                                      :initially-selected-image-index j}]
                :stylist-card.gallery-item/ucare-id ucare-img-url})
             ucare-img-urls))
          :stylist-card.gallery/title        "Recent Work"
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
                                                        :on-click  (utils/send-event-callback events/control-adventure-stylist-phone-clicked {:stylist-id   (:stylist-id stylist)
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
              :stylist-card.gallery/items
              (let [ucare-img-urls (map :resizable-url gallery-images)]
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
              :stylist-card.address-marker/value (string/join " "
                                                              [(string/join ", "
                                                                            [address-1 address-2 city state])
                                                               zipcode ])}))))

(defn stylist-cards-query
  [stylist-profiles? navigation-event stylists]
  (map-indexed (partial stylist-card-query stylist-profiles? navigation-event) stylists))

(def call-out-query
  {:call-out-center/bg-class    "bg-lavender"
   :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
   :call-out-center/title       "Want to book with your own stylist?"
   :call-out-center/subtitle    "Recommend them to become Mayvenn Certified"
   :cta/id                      "recommend-stylist"
   :cta/target                  [events/external-redirect-typeform-recommend-stylist]
   :cta/label                   "Submit Your Stylist"
   :element/type                :call-out
   :react/key                   :recommend-stylist})

(defn ^:private insert-at-pos
  "TODO this needs to be refine"
  [position i coll]
  (let [[h & r] (partition-all position coll)]
    (flatten (into [h] (concat [i] r)))))

(defn ^:private display-list
  "TODO this needs to be refined"
  [dispatches items & fall-back]
  (for [{:keys [element/type] :as item} items]
    (when-let [component (get dispatches type fall-back)]
      (component/build component item nil))))

(defn gallery-modal-query
  [app-state]
  (let [gallery-images (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-urls)
        index          (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-index)]
    {:gallery-modal/target           [events/control-adventure-stylist-gallery-close]
     :gallery-modal/ucare-image-urls gallery-images
     :gallery-modal/initial-index    index }))

(defn template
  [{:keys [gallery-modal header list/results]} _ _]
  (component/create
   [:div.bg-fate-white.black.center.flex.flex-auto.flex-column
    (component/build gallery-modal/organism gallery-modal nil)
    (component/build header/organism header nil)
    [:div
     (display-list {:call-out                call-out-center/organism
                    :control-stylist-card    stylist-cards/control-organism
                    :experiment-stylist-card stylist-cards/experiment-organism}
                   results)]]))

(defn page
  [app-state]
  (let [current-order          (api.orders/current app-state)
        stylist-search-results (get-in app-state adventure.keypaths/adventure-matched-stylists)
        navigation-event       (get-in app-state storefront.keypaths/navigation-event)]
    (component/build template
                     {:gallery-modal (gallery-modal-query app-state)
                      :header        (header-query current-order (first (get-in app-state storefront.keypaths/navigation-undo-stack)))
                      :list/results  (insert-at-pos 3
                                                    call-out-query
                                                    (stylist-cards-query (experiments/stylist-profiles? app-state)
                                                                         navigation-event
                                                                         stylist-search-results))})))
