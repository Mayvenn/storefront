(ns stylist-matching.search.filters-modal
  (:require
   [spice.selector :as selector]
   [storefront.browser.tags :as tags]
   [storefront.component :as component]
   [storefront.components.header :as components.header]
   [storefront.components.svg :as svg]
   [storefront.components.money-formatters :as mf]
   [storefront.components.ui :as ui]
   [storefront.effects :as effects]
   [storefront.events :as events]
   [storefront.keypaths :as keypaths]
   [storefront.platform.component-utils :as utils]
   [storefront.transitions :as transitions]
   [catalog.services :as services]
   [storefront.platform.messages :as messages]
   [stylist-directory.keypaths]
   [stylist-matching.core :refer [stylist-matching<-]]
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
  (let [selected-filters         (:param/services (stylist-matching<- data))
        all-skus                 (->> (vals (get-in data storefront.keypaths/v2-skus))
                                      (filter #(re-find #"SRV" (:catalog/sku-id %))))
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

(defmethod effects/perform-effects events/control-stylist-search-toggle-filter
  [_ _ {:keys [previously-checked? stylist-filter-selection]} _ state]
  (messages/handle-message events/flow|stylist-matching|param-services-constrained
                           {:services (-> (stylist-matching<- state)
                                          (update :param/services
                                                  (if previously-checked? disj conj)
                                                  stylist-filter-selection)
                                          :param/services)})
  (messages/handle-message events/flow|stylist-matching|prepared))

(defmethod effects/perform-effects events/control-stylist-search-reset-filters
  [_ _ _ _ _]
  (messages/handle-message events/flow|stylist-matching|param-services-constrained
                           {:services nil})
  (messages/handle-message events/flow|stylist-matching|prepared))

(def ^:private select (comp seq (partial selector/match-all {:selector/strict? true})))

(defmethod transitions/transition-state events/control-show-stylist-search-filters
  [_ _ _ app-state]
  (let [service-skus     (->> (get-in app-state storefront.keypaths/v2-skus)
                              vals
                              (select services/service))
        selected-filters {:catalog/sku-id (:param/services (stylist-matching<- app-state))}]
    (-> app-state
        (assoc-in stylist-directory.keypaths/stylist-search-show-filters? true)
        (assoc-in stylist-directory.keypaths/stylist-search-expanded-filter-sections
                  (set (cond-> nil
                         (select (merge services/discountable selected-filters) service-skus)
                         (conj "free-mayvenn-services")
                         (select (merge services/a-la-carte selected-filters) service-skus)
                         (conj "a-la-carte-services")
                         (select (merge services/addons selected-filters) service-skus)
                         (conj "add-on-services")
                         :default
                         (or #{"free-mayvenn-services"})))))))

(defmethod effects/perform-effects events/control-show-stylist-search-filters
  [_ event args previous-app-state app-state]
  (tags/add-classname ".kustomer-app-icon" "hide"))

(defmethod effects/perform-effects events/control-stylist-search-filters-dismiss
  [_ event args previous-app-state app-state]
  (tags/remove-classname ".kustomer-app-icon" "hide")
  (messages/handle-message events/stylist-search-filter-menu-close))

(defmethod transitions/transition-state events/stylist-search-filter-menu-close
  [_ event args app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-show-filters? false))

(defmethod transitions/transition-state events/initialize-stylist-search-filters
  [_ event args app-state]
  (assoc-in app-state stylist-directory.keypaths/stylist-search-show-filters? false))

(defmethod transitions/transition-state events/control-toggle-stylist-search-filter-section
  [_ event {:keys [previously-opened? section-id]} app-state]
  (update-in app-state stylist-directory.keypaths/stylist-search-expanded-filter-sections
             #(set (if previously-opened?
                     (disj % section-id)
                     (conj % section-id)))))
