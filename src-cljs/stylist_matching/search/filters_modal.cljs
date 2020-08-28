(ns stylist-matching.search.filters-modal
  (:require
   [spice.selector :as selector]
   clojure.string
   [storefront.component :as component]
   [storefront.components.header :as components.header]
   [storefront.components.svg :as svg]
   [storefront.components.money-formatters :as mf]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.history :as history]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.transitions :as transitions]
   [catalog.services :as services]
   [storefront.platform.messages :as messages]
   [stylist-directory.keypaths]
   [storefront.css-transitions :as css-transitions]))

(defn specialty->filter [selected-filters [primary specialty price]]
  (let [checked? (some #{specialty} selected-filters)]
    {:stylist-search-filter/primary   primary
     :stylist-search-filter/secondary (str "(" (mf/as-money-or-free price) ")")
     :stylist-search-filter/id        (str "stylist-filter-" specialty)
     :stylist-search-filter/target    [events/control-stylist-search-toggle-filter
                                       {:previously-checked?      checked?
                                        :stylist-filter-selection specialty}]
     :stylist-search-filter/checked?  checked?}))

(defn select-sorted
  [essentials sort-fn db]
  (->> db
       (selector/match-all {:selector/strict? true}
                           essentials)
       (sort-by sort-fn)))

(defn query
  [data]
  (let [selected-filters         (get-in data stylist-directory.keypaths/stylist-search-selected-filters)
        all-skus                 (vals (get-in data storefront.keypaths/v2-skus))
        expanded-filter-sections (get-in data stylist-directory.keypaths/stylist-search-expanded-filter-sections)]
    {:stylist-search-filters/show? (get-in data stylist-directory.keypaths/stylist-search-show-filters?)
     :stylist-search-filters/sections
     [(let [section-id "free-mayvenn-services"
            open?      (contains? expanded-filter-sections section-id)]
        {:stylist-search-filter-section/id           (str section-id "-" (if open? "open" "closed"))
         :stylist-search-filter-section/title        "Free Mayvenn Services"
         :stylist-search-filter-section/title-action [events/control-toggle-stylist-search-filter-section
                                                      {:previously-opened? open?
                                                       :section-id         section-id}]
         :stylist-search-filter-section/open?        open?
         :stylist-search-filter-section/primary      (str
                                                      "Get Mayvenn services (valued up to $200) for free when purchasing "
                                                      "qualifying hair from Mayvenn. You buy the hair, we cover the service!")
         :stylist-search-filter-section/filters      (->> all-skus
                                                          (select-sorted services/discountable :legacy/variant-id)
                                                          (mapv (juxt :sku/name :catalog/sku-id (constantly 0)))
                                                          (mapv (partial specialty->filter selected-filters)))})
      (let [section-id "a-la-carte-services"
            open?      (contains? expanded-filter-sections section-id)]
        {:stylist-search-filter-section/id           (str section-id "-" (if open? "open" "closed"))
         :stylist-search-filter-section/title        "À la carte Services"
         :stylist-search-filter-section/title-action [events/control-toggle-stylist-search-filter-section
                                                      {:previously-opened? open?
                                                       :section-id         section-id}]
         :stylist-search-filter-section/open?        open?
         :stylist-search-filter-section/filters      (->> all-skus
                                                          (select-sorted services/a-la-carte :legacy/variant-id)
                                                          (mapv (juxt :sku/name :catalog/sku-id :sku/price))
                                                          (mapv (partial specialty->filter selected-filters)))})
      (let [section-id "add-on-services"
            open?      (contains? expanded-filter-sections section-id)]
        {:stylist-search-filter-section/id           (str section-id "-" (if open? "open" "closed"))
         :stylist-search-filter-section/title        "Add-on Services"
         :stylist-search-filter-section/title-action [events/control-toggle-stylist-search-filter-section
                                                      {:previously-opened? open?
                                                       :section-id         section-id}]
         :stylist-search-filter-section/open?        open?
         :stylist-search-filter-section/filters      (->> all-skus
                                                          (select-sorted services/addons :legacy/variant-id)
                                                          (mapv (juxt :sku/name :catalog/sku-id :sku/price))
                                                          (mapv (partial specialty->filter selected-filters)))})]}))

(component/defcomponent filter-section
  [{:stylist-search-filter-section/keys [id filters title primary open? title-action]} _ _]
  [:div.flex.flex-column.px5.ptj1.left-align
   {:key id}
   [:a.block.flex.justify-between.inherit-color.items-center
    (assoc (apply utils/fake-href title-action)
           :data-test id)
    [:div.shout.title-2.proxima title]
    [:div.flex.items-center
     (when open? {:class "rotate-180 mrp2"})
     ^:inline (svg/dropdown-arrow {:class  "fill-black"
                                   :height "20px"
                                   :width  "20px"})]]
   (css-transitions/slide-down
    (when open?
      (component/html
       [:span
        [:div.content-3.mt2.mb3 primary]
        (for [{:stylist-search-filter/keys
               [id primary secondary target checked?]} filters]
          [:div.col-12.my1.flex
           {:on-click (apply utils/send-event-callback target)
            :key (str "preference-" id)}
           [:div.flex.justify-end
            (ui/check-box {:value     checked?
                           :id        id
                           :data-test id})]

           [:div primary [:span.dark-gray.ml1.content-3 secondary]]])])))])

(component/defcomponent component
  [{:stylist-search-filters/keys [sections]} _ _]
  [:div.col-12.bg-white {:style {:min-height "100vh"}}
   (components.header/mobile-nav-header
    {:class "border-bottom border-gray"}
    (component/html [:div (ui/button-medium-underline-black
                           (merge {:data-test "stylist-search-filters-reset"}
                                  (utils/fake-href events/control-stylist-search-reset-filters))
                           "RESET")])
    (component/html [:div.center.proxima.content-1 "Filters"])
    (component/html [:div (ui/button-medium-underline-primary
                           (merge {:data-test "stylist-search-filters-dismiss"}
                                  (utils/fake-href events/control-stylist-search-filters-dismiss))
                           "DONE")]))
   [:div.mb5 (map #(component/build filter-section % {}) sections)]])

(defmethod transitions/transition-state events/control-stylist-search-toggle-filter
  [_ event {:keys [previously-checked? stylist-filter-selection]} app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-selected-filters
             #(set (if previously-checked?
                     (disj % stylist-filter-selection)
                     (conj % stylist-filter-selection)))))

(defmethod effects/perform-effects events/control-stylist-search-toggle-filter
  [_ event _ _ app-state]
  (let [[nav-event nav-args] (get-in app-state storefront.keypaths/navigation-message) ; pre- or post- purchase
        service-filters      (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)
        selected-location    (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)]
    (history/enqueue-redirect nav-event
                              {:query-params
                               (merge (dissoc (:query-params nav-args) :preferred-services)
                                      {:lat  (:latitude selected-location)
                                       :long (:longitude selected-location)}
                                      (when (seq service-filters)
                                        {:preferred-services (clojure.string/join "~" service-filters)}))})))

(defmethod transitions/transition-state events/control-stylist-search-reset-filters
  [_ event _ app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search dissoc :selected-filters))

(defmethod effects/perform-effects events/control-stylist-search-reset-filters
  [_ event _ _ app-state]
  (let [[nav-event nav-args] (get-in app-state storefront.keypaths/navigation-message) ; pre- or post- purchase
        selected-location    (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)]
    (history/enqueue-redirect nav-event
                              {:query-params
                               (-> (merge (:query-params nav-args)
                                          {:lat  (:latitude selected-location)
                                           :long (:longitude selected-location)})
                                   (dissoc :preferred-services))})))

(defmethod transitions/transition-state events/control-show-stylist-search-filters
  [_ event args app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-show-filters? true))

(defmethod effects/perform-effects events/control-stylist-search-filters-dismiss
  [_ event args previous-app-state app-state]
  (messages/handle-message events/stylist-search-filter-menu-close))

(defmethod transitions/transition-state events/stylist-search-filter-menu-close
  [_ event args app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-show-filters? false))

(defmethod transitions/transition-state events/initialize-stylist-search-filters
  [_ event args app-state]
  (-> app-state
      (assoc-in stylist-directory.keypaths/stylist-search-show-filters? false)
      (assoc-in stylist-directory.keypaths/stylist-search-expanded-filter-sections #{})))

(defmethod transitions/transition-state events/control-toggle-stylist-search-filter-section
  [_ event {:keys [previously-opened? section-id]} app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-expanded-filter-sections
             #(set (if previously-opened?
                     (disj % section-id)
                     (conj % section-id)))))
