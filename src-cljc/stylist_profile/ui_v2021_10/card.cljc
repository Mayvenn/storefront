(ns stylist-profile.ui-v2021-10.card
  (:require clojure.pprint
            [mayvenn.visual.tools :refer [with]]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            stylist-matching.ui.stylist-cards
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [ui.molecules :as molecules]
            [storefront.events :as e]))



(defn ^:private circle-portrait
  [{:circle-portrait/keys [portrait]}]
  [:div
   [:div.hide-on-mb
    (ui/circle-picture {:width "160px" :alt ""}
                       (ui/square-image portrait 160))]
   [:div.hide-on-tb-dt
    (ui/circle-picture {:width "78px" :alt ""}
                       (ui/square-image portrait 72))]])

(defn ^:private stylist-title
  [{:title/keys [primary id]}]
  [:h1.title-2.proxima.shout.mt1 {:data-test id} primary])

(defn ^:private rating-count
  [{:keys [rating-count]}]
  [:div.title-3.proxima.p-color.pl1
   {:aria-label "6 ratings"}
   "(" rating-count ")"])

(defn ^:private salon
  [{:keys [id primary location]}]
  [:div.center {:data-test id}
   [:div.bold primary]
   [:div location]])

(defn ^:private hero
  [{:background/keys [ucare-id desktop-ucare-id] :as data}]
  [:div.relative
   [:div.absolute.z1.overlay.flex.flex-column.items-center.justify-center
    (circle-portrait data)
    (stylist-title data)
    [:a.block.flex
     (utils/fake-href e/control-scroll-to-selector {:selector "[data-ref=reviews]"}  )
     (molecules/stars-rating-molecule (with :star-bar data))
     (rating-count data)]
    (salon (with :salon data))]
   [:div
    [:div.hide-on-mb
     ^:inline (ui/img {:src    desktop-ucare-id
                       :alt    ""
                       :class  "block col-12"
                       :height 410})]
    [:div.hide-on-tb-dt
     ^:inline (ui/img {:src    ucare-id
                       :alt    ""
                       :class  "block col-12"
                       :height 196})]]])

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
  (when id
    [:div
     [:div.mx-auto.col-3.mx3.mt3.mb7.hide-on-mb
      {:id (str id "-desktop")}
      (ui/button-medium-primary (assoc (apply utils/fake-href target)
                                       :data-test (str id "-desktop")) primary)]
     [:div.mx3.mt3.mb7.hide-on-tb-dt
      {:id id}
      (ui/button-medium-primary (assoc (apply utils/fake-href target)
                                       :data-test id) primary)]]))

(defn top-stylist-information-points-grid-molecule
  [{:keys [points]} grid-class]
  (interleave
   (for [{:keys [id icon primary]} points]
     [:div.pb1.flex
      [:div.flex.items-center.justify-center {:style {:width "25px"}}
       (when icon
         (svg/symbolic->html icon))]
      [:div (merge (when grid-class
                     {:class grid-class})
                   (when id
                     {:data-test id}))
       primary]])
   (cycle [[:div] nil])))

;; Forked from top-stylist. They now deviate
(defn top-stylist-information-points-molecule
  [data]
  [:div
   [:div.pt3.mx-auto.grid.proxima.content-1.hide-on-mb
    {:style {:grid-template-columns "auto 10px auto"
             :max-width             "400px"}}
    (top-stylist-information-points-grid-molecule data "pl2")]
   [:div.pt3.mx-auto.grid.proxima.content-3.hide-on-tb-dt
    {:style {:grid-template-columns "auto 10px auto"
             :max-width             "305px"}}
    (top-stylist-information-points-grid-molecule data nil)]])

(c/defcomponent organism
  [data _ _]
  [:div
   (hero (with :stylist-profile.card.hero data))
   (star-rating (with :stylist-profile.card.star-rating data))
   [:div.mx-auto.col-10
    (top-stylist-information-points-molecule (with :stylist-profile.card.laurels data))]
   (cta (with :stylist-profile.card.cta data))])
