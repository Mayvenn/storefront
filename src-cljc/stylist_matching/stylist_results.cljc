(ns stylist-matching.stylist-results
  (:require [adventure.components.wait-spinner :as wait-spinner]
            adventure.keypaths
            api.orders
            [clojure.string :as string]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
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
   back post-purchase?]
  (cond-> {:header.title/id               "adventure-title"
           :header.title/primary          "Meet Your Certified Stylist"
           :header.back-navigation/id     "adventure-back"
           :header.back-navigation/back   back
           :header.back-navigation/target [events/navigate-adventure-find-your-stylist]}
    (not post-purchase?)
    (merge {:header.cart/id    "mobile-cart"
            :header.cart/value quantity
            :header.cart/color "white"})))

(defn stylist-card-query
  [post-purchase?
   mayvenn-rating?
   idx
   {:keys [salon service-menu gallery-images store-slug stylist-id external-rating mayvenn-rating] :as stylist}]
  (let [{salon-name :name :keys [address-1 address-2 city state zipcode]} salon

        {:keys [specialty-sew-in-leave-out
                specialty-sew-in-closure
                specialty-sew-in-360-frontal
                specialty-sew-in-frontal]} service-menu
        cta-event                          (if post-purchase?
                                             events/control-adventure-select-stylist-post-purchase
                                             events/control-adventure-select-stylist-pre-purchase)]
    (cond-> {:react/key                       (str "stylist-card-" store-slug)
             :stylist-card/target             (if post-purchase?
                                                [events/navigate-adventure-stylist-profile-post-purchase {:stylist-id stylist-id
                                                                                                          :store-slug store-slug}]
                                                [events/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                            :store-slug store-slug}])
             :stylist-card/id                 (str "stylist-card-" store-slug)
             :stylist-card.thumbnail/id       (str "stylist-card-thumbnail-" store-slug)
             :stylist-card.thumbnail/ucare-id (-> stylist :portrait :resizable-url)

             :stylist-card.title/id            "stylist-name"
             :stylist-card.title/primary       (stylists/->display-name stylist)
             :rating/value                     external-rating
             :stylist-card.services-list/id    (str "stylist-card-services-" store-slug)
             :stylist-card.services-list/value [(stylist-cards/checks-or-x-atom "Leave Out"
                                                                                (boolean specialty-sew-in-leave-out))
                                                (stylist-cards/checks-or-x-atom "Closure"
                                                                                (boolean specialty-sew-in-closure))
                                                (stylist-cards/checks-or-x-atom "360° Frontal"
                                                                                (boolean specialty-sew-in-360-frontal))
                                                (stylist-cards/checks-or-x-atom "Frontal" (boolean specialty-sew-in-frontal))]
             :stylist-card.cta/id              (str "select-stylist-" store-slug)
             :stylist-card.cta/label           "Select"
             :stylist-card.cta/target          [cta-event
                                                {:servicing-stylist stylist
                                                 :card-index        idx}]

             :stylist-card.gallery/id           (str "stylist-card-gallery-" store-slug)
             ;; TODO: Element Type
             :element/type                      :stylist-card
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
             :stylist-card.address-marker/value (string/join " "
                                                             [(string/join ", "
                                                                           [address-1 address-2 city state])
                                                              zipcode])}

      (and mayvenn-rating? mayvenn-rating)
      (merge {:rating/value mayvenn-rating}))))

(defn stylist-cards-query
  [post-purchase? mayvenn-rating? stylists]
  (map-indexed (partial stylist-card-query post-purchase? mayvenn-rating?) stylists))

(def call-out-query
  {:call-out-center/bg-class    "bg-pale-purple"
   :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
   :call-out-center/title       "Want to book with your own stylist?"
   :call-out-center/subtitle    "Recommend them to become Mayvenn Certified"
   :cta/id                      "recommend-stylist"
   :cta/target                  [events/external-redirect-typeform-recommend-stylist]
   :cta/label                   "Submit Your Stylist"
   :element/type                :call-out
   :react/key                   :recommend-stylist})

(defn ^:private insert-at-pos
  "TODO this needs to be refined"
  [position i coll]
  (let [[h & r] (partition-all position coll)]
    (flatten (into [h] (concat [i] r)))))

(defn ^:private display-list
  "TODO this needs to be refined"
  [dispatches items & fall-back]
  (for [item  items
        :let  [component (get dispatches (:element/type item) fall-back)]
        :when component]
    [:div {:key (:react/key item)}
     (ui/screen-aware component item)]))

(defn gallery-modal-query
  [app-state]
  (let [gallery-images (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-urls)
        index          (get-in app-state adventure.keypaths/adventure-stylist-gallery-image-index)]
    {:gallery-modal/target           [events/control-adventure-stylist-gallery-close]
     :gallery-modal/ucare-image-urls gallery-images
     :gallery-modal/initial-index    index}))

(defcomponent template
  [{:keys [gallery-modal header list/results]} _ _]
  [:div.bg-cool-gray.black.center.flex.flex-auto.flex-column
   (component/build gallery-modal/organism gallery-modal nil)
   (component/build header/organism header nil)
   [:div
    (display-list {:call-out     call-out-center/organism
                   :stylist-card stylist-cards/organism}
                  results)]])

(def post-purchase? #{events/navigate-adventure-stylist-results-post-purchase})

(defn page
  [app-state]
  (let [current-order          (api.orders/current app-state)
        stylist-search-results (get-in app-state adventure.keypaths/adventure-matched-stylists)
        nav-event              (get-in app-state storefront.keypaths/navigation-event)
        post-purchase?         (post-purchase? nav-event)
        spinning?              (or (get-in app-state adventure.keypaths/adventure-stylist-results-delaying?)
                                   (empty? stylist-search-results))]
    (if spinning?
      (component/build wait-spinner/component app-state)
      (component/build template
                       {:gallery-modal (gallery-modal-query app-state)
                        :header        (header-query current-order (first (get-in app-state storefront.keypaths/navigation-undo-stack)) post-purchase?)
                        :list/results  (insert-at-pos 3
                                                      call-out-query
                                                      (stylist-cards-query post-purchase?
                                                                           (experiments/mayvenn-rating? app-state)
                                                                           stylist-search-results))}))))
