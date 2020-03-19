(ns stylist-matching.search.filters-popup
  (:require
   [storefront.api :as api]
   [storefront.component :as component]
   [storefront.components.header :as components.header]
   [storefront.components.popup :as popup]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as messages]
   [storefront.request-keys :as request-keys]
   [storefront.transitions :as transitions]))

(defn specialty->filter [selected-filters [label specialty]]
  (let [checked? (contains? selected-filters specialty)]
    {:stylist-search-filter/label    label
     :stylist-search-filter/id       (str "stylist-filter-" (name specialty))
     :stylist-search-filter/target   [events/control-stylist-search-toggle-filter
                                      {:previously-checked? checked?
                                       :filter              specialty}]
     :stylist-search-filter/checked? (contains? selected-filters specialty)}))

(defmethod popup/query :stylist-search-filters
  [data]
  (let [selected-filters
        (get-in data stylist-directory.keypaths/stylist-search-selected-filters)]
    {:stylist-search-filters/title   "Free Mayvenn Services"
     :stylist-search-filters/primary (str
                                      "Get Mayvenn services (valued up to $200) for free when purchasing "
                                      "qualifying hair from Mayvenn. You buy the hair, we cover the service!")
     :stylist-search-filters/filters
     (mapv (partial specialty->filter selected-filters)
           [["Leave out Install" :leave-out]
            ["Closure Install" :closure]
            ["Frontal Install" :frontal]
            ["360 Frontal Install" :360-frontal]
            ["Wig Customization" :wig-customization]])}))

(defmethod popup/component :stylist-search-filters
  [{:stylist-search-filters/keys [filters title primary]} _ _]
  (component/html
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

(defmethod effects/perform-effects events/control-stylist-search-toggle-filter
  [_ event _ _ app-state]
  (let [service-filters   (get-in app-state stylist-directory.keypaths/stylist-search-selected-filters)
        selected-location (get-in app-state stylist-directory.keypaths/stylist-search-selected-location)
        params            {:latitude           (:latitude selected-location)
                           :longitude          (:longitude selected-location)
                           :radius             "100mi"
                           :preferred-services service-filters}]
    (api/fetch-stylists-matching-filters params)))

(defmethod transitions/transition-state events/control-stylist-search-toggle-filter
  [_ event {:keys [previously-checked? filter]} app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-selected-filters
             (if previously-checked? disj conj) filter))

(defmethod transitions/transition-state events/control-stylist-search-reset-filters
  [_ event _ app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-selected-filters #{}))

(defmethod transitions/transition-state events/control-show-stylist-search-filters [_ event args app-state]
  (assoc-in app-state keypaths/popup :stylist-search-filters))

(defmethod transitions/transition-state events/control-stylist-search-filters-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))
