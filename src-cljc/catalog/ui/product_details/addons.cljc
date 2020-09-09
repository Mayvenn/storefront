(ns catalog.ui.product-details.addons
  (:require api.orders
            [catalog.keypaths :as c-k]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.trackings :as trackings]
            #?@(:cljs
                [[storefront.hooks.stringer :as stringer]])
            [adventure.keypaths :as adventure.keypaths]
            [stylist-matching.search.accessors.filters :as stylist-filters]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [catalog.services :as services]))

(defn addon-card
  [{:addon-line/keys [id target primary secondary tertiary checked? spinning? disabled? disabled-reason]}]
  [:div.mx3.my1.bg-white.p2
   (cond-> {:key id}
     disabled?
     (assoc :class "dark-gray"))
   [:div.flex
    (when-not disabled? (apply utils/fake-href target))
    (if spinning?
      [:div.mt1
       [:div.pr2 {:style {:width "41px"}}
        ui/spinner]]
      [:div.mt1.pl1
       (ui/check-box {:value     checked?
                      :data-test id
                      :disabled  disabled?})])
    [:div.flex-grow-1.mr2
     [:div.proxima.content-2 primary]
     [:div.proxima.content-3 secondary]
     [:div.content-3.red disabled-reason]]
    [:div tertiary]]])

(defn pair-with-addons-title-molecule
  [_]
  (c/html
   [:div.title-3.proxima.bold.shout "Pair with add-ons"]))

(defn offshoot-action-molecule
  [{:offshoot-action/keys [label target id]}]
  (c/html
   (when id
     (ui/button-small-underline-primary (merge {:key id}
                                               (apply utils/fake-href target))
                                        label))))

(defn cta-related-addon-molecule
  [{:cta-related-addon/keys [label target id]}]
  (c/html
   (when id
     (ui/button-small-underline-primary (merge (apply utils/route-to target)
                                               {:id        id
                                                :data-test id})
                                        label))))

(c/defdynamic-component organism
  (did-mount [this] (handle-message e/visual-add-on-services-displayed))
  (render [this]
          (let [{:keys [related-addons] :as data} (c/get-props this)]
            (c/html
             [:div.bg-cool-gray
              [:div.px3.pt2.flex.justify-between.items-baseline
               (pair-with-addons-title-molecule data)
               (offshoot-action-molecule data)]
              (mapv addon-card related-addons)
              [:div.pt2.pb3.flex.justify-center
               (cta-related-addon-molecule data)]]))))

(defmethod trackings/perform-track e/visual-add-on-services-displayed
  [_ event _ app-state]
  #?(:cljs
     (let [{services     :order.items/services
            waiter-order :waiter/order}                        (api.orders/current app-state)
           {:services/keys [stylist offered-services-sku-ids]} (api.orders/services app-state waiter-order)

           addons-sku-ids-on-order                             (->> services (mapcat :addons) (mapv :sku-id) set)
           {available-addon-skus   true
            unavailable-addon-skus false}                      (->> (get-in app-state catalog.keypaths/detailed-product-related-addons)
                                                                    (mapv #(assoc % :stylist-provides?
                                                                                  (or (nil? stylist) (contains? offered-services-sku-ids (:catalog/sku-id %)))))
                                                                    (sort-by (juxt (comp not :stylist-provides?) :order.view/addon-sort))
                                                               (group-by (fn [s]
                                                                           (boolean
                                                                            (and (:stylist-provides? s)
                                                                                 (not (contains? addons-sku-ids-on-order (:catalog/sku-id s))))))))]
       (stringer/track-event "add_on_services_displayed"
                             {:available-add-on-variant-ids   (mapv :legacy/variant-id available-addon-skus)
                              :unavailable-add-on-variant-ids (mapv :legacy/variant-id unavailable-addon-skus)}))))

(defmethod trackings/perform-track e/control-product-detail-toggle-related-addon-list
  [_ event _ app-state]
  #?(:cljs
     (if (get-in app-state c-k/detailed-product-addon-list-open?)
       (stringer/track-event "expand_add_on_services")
       (stringer/track-event "collapse_add_on_services"))))
