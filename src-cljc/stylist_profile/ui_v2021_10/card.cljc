(ns stylist-profile.ui-v2021-10.card
  (:require clojure.pprint
            [mayvenn.visual.tools :refer [with within]]
            [storefront.component :as c]
            [storefront.components.marquee :as marquee]
            [storefront.components.ui :as ui]
            stylist-matching.ui.stylist-cards
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.effects :as fx]
            [ui.molecules :as molecules]
            [storefront.events :as e]))



(defn ^:private circle-portrait
  [{:circle-portrait/keys [portrait]}]
  (ui/circle-picture {:width "78px"}
                     (ui/square-image portrait 72)))

(defn ^:private stylist-title
  [{:title/keys [primary id]}]
  [:div.title-2.proxima.shout.mt1 {:data-test id} primary])

(defn ^:private rating-count
  [{:keys [rating-count]}]
  [:div.title-3.proxima.p-color.pl1
   "(" rating-count ")"])

(defn ^:private salon
  [{:keys [id primary location]}]
  [:div.center {:data-test id}
   [:div.bold primary]
   [:div location]])

(defn ^:private hero
  [{:background/keys [ucare-id] :as data}]
  [:div.relative
   [:div.absolute.z1.overlay.flex.flex-column.items-center.justify-center
    (circle-portrait data)
    (stylist-title data)
    [:div.flex
     (molecules/stars-rating-molecule (with :star-bar data))
     (rating-count data)]
    (salon (with :salon data))]
   ^:inline (ui/img {:src   ucare-id
                     :class "block col-12"
                     :height 196})])

(defn ^:private star-rating
  [{:keys [id value]}]
  [:a.block.flex.mt4.mb2.flex-column
   (utils/fake-href e/control-scroll-to-selector {:selector "[data-ref=reviews]"}  )
   [:div.flex.justify-center.mr2
    [:div.mt1.mr1
     (svg/symbolic->html [:svg/whole-star {:class "fill-p-color"
                                           :style {:height "0.9em"
                                                   :width  "0.9em"}}])]
    [:div.title-1.proxima.p-color {:data-test id} value]]
   [:div.flex.justify-center.black "Rating"]])

(defn ^:private cta
  [{:keys [id primary target]}]
  [:div.mx3.mt3.mb7#nonsticky-select-stylist {:data-test id}
   (ui/button-medium-primary (apply utils/fake-href target) primary)])

(c/defcomponent organism
  [data _ _]
  [:div
   (hero (with :stylist-profile.card.hero data))
   (star-rating (with :stylist-profile.card.star-rating data))
   [:div.mx-auto.col-10
    (stylist-matching.ui.stylist-cards/top-stylist-information-points-molecule (with :stylist-profile.card.laurels data))]
   (cta (with :stylist-profile.card.cta data))])
