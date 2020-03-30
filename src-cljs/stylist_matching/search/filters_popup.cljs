(ns stylist-matching.search.filters-popup
  (:require
   [storefront.api :as api]
   [storefront.component :as component]
   [storefront.components.header :as components.header]
   [storefront.components.popup :as popup]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.history :as history]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]
   [stylist-directory.keypaths]))

(defn specialty->filter [selected-filters [label specialty]]
  (let [checked? (some #{specialty} selected-filters)]
    {:stylist-search-filter/label    label
     :stylist-search-filter/id       (str "stylist-filter-" (name specialty))
     :stylist-search-filter/target   [events/control-stylist-search-toggle-filter
                                      {:previously-checked?      checked?
                                       :stylist-filter-selection specialty}]
     :stylist-search-filter/checked? checked?}))

(defn query [data]
  (let [selected-filters
        (get-in data stylist-directory.keypaths/stylist-search-selected-filters)]
    {:stylist-search-filters/title   "Free Mayvenn Services"
     :stylist-search-filters/primary (str
                                      "Get Mayvenn services (valued up to $200) for free when purchasing "
                                      "qualifying hair from Mayvenn. You buy the hair, we cover the service!")
     :stylist-search-filters/show? (get-in data stylist-directory.keypaths/stylist-search-show-filters?)
     :stylist-search-filters/filters
     (mapv (partial specialty->filter selected-filters)
           [["Leave out Install" :leave-out]
            ["Closure Install" :closure]
            ["Frontal Install" :frontal]
            ["360 Frontal Install" :360-frontal]
            ["Wig Customization" :wig-customization]])}))

(component/defcomponent component
 [{:stylist-search-filters/keys [filters title primary show?]} _ _]
 (when show?
   (ui/modal
    {:body-style  {:max-width "625px"}
     :close-attrs (utils/fake-href events/control-addon-service-menu-dismiss)
     :col-class   "col-12"}
    [:div.bg-white {:style {:min-height "100vh"}}
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
     [:div.flex.flex-column.p5.left-align
      [:div.shout.title-2.proxima title]
      [:div.content-3.mt2.mb3 primary]
      (for [{:stylist-search-filter/keys
             [id label target checked?]} filters]
        [:div.col-12.my1.flex.justify-between
         {:on-click (apply utils/send-event-callback target)}
         [:div.col-10 label]
         [:div.flex.justify-end
          {:style {:margin-right "-15px"}}
          (ui/check-box {:value     checked?
                         :id        id
                         :data-test id})]])]])))

(defmethod transitions/transition-state events/control-stylist-search-toggle-filter
  [_ event {:keys [previously-checked? stylist-filter-selection]} app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-selected-filters
             #(if previously-checked?
                (remove #{stylist-filter-selection} %)
                (conj % stylist-filter-selection))))

(defmethod effects/perform-effects events/control-stylist-search-toggle-filter
  [_ event _ _ app-state]
  (let [[nav-event nav-args] (get-in app-state storefront.keypaths/navigation-message) ; pre- or post- purchase
        service-filters      (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)
        selected-location    (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)]
    (history/enqueue-redirect nav-event
                              {:query-params
                               (merge (:query-params nav-args)
                                      {:lat                (:latitude selected-location)
                                       :long               (:longitude selected-location)
                                       :preferred-services service-filters})})))

(defmethod transitions/transition-state events/control-stylist-search-reset-filters
  [_ event _ app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-selected-filters #{}))

(defmethod transitions/transition-state events/control-show-stylist-search-filters
  [_ event args app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-show-filters? not))

(defmethod transitions/transition-state events/control-stylist-search-filters-dismiss
  [_ event args app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-show-filters? not))
