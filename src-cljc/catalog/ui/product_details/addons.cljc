(ns catalog.ui.product-details.addons
  (:require [catalog.keypaths :as c-k]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.trackings :as trackings]
            #?@(:cljs
                [[storefront.hooks.stringer :as stringer]])))

(defn addon-card [{:addon-line/keys [id target primary secondary tertiary checked? spinning? disabled? disabled-reason]}]
  [:div.mx3.my1.bg-white.p2
   (when disabled? {:class "dark-gray"})
   [:div.flex
    (merge (when-not disabled? (apply utils/fake-href target))
           {:key id})
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

(c/defdynamic-component organism
  (did-mount [this]
             (let [{:keys [related-addons] :as data} (c/get-props this)]
               (let [disabled (group-by :addon-line/disabled? related-addons)]
                 (handle-message e/visual-add-on-services-displayed
                                 {:addons/available-sku-ids   (mapv :addon-line/id (get disabled false))
                                  :addons/unavailable-sku-ids (mapv :addon-line/id (get disabled true))}))))
  (render [this]
          (let [{:keys [related-addons] :as data} (c/get-props this)]
            (let [{:cta-related-addon/keys [label target id]} data]
              (c/html
               [:div.bg-cool-gray
                [:div.px3.pt2.flex.justify-between.items-baseline
                 [:div.title-3.proxima.bold.shout "Pair with add-ons"]
                 (let [{:offshoot-action/keys [label target id]} data]
                   (when id
                     (ui/button-small-underline-primary (merge {:key id}
                                                               (apply utils/fake-href target))
                                                        label)))]
                (mapv addon-card related-addons)
                [:div.pt2.pb3.flex.justify-center
                 (ui/button-small-underline-primary (merge (apply utils/route-to target)
                                                           {:id        id
                                                            :data-test id})
                                                    label)]])))))

(defmethod trackings/perform-track e/visual-add-on-services-displayed
  [_ event {:addons/keys [available-sku-ids unavailable-sku-ids]} app-state]
  #?(:cljs
     (let [sku-db                (get-in app-state k/v2-skus)
           convert-to-variant-id (comp :legacy/variant-id (partial get sku-db))]
       (stringer/track-event "add_on_services_displayed"
                             {:available-add-on-variant-ids   (mapv convert-to-variant-id available-sku-ids)
                              :unavailable-add-on-variant-ids (mapv convert-to-variant-id unavailable-sku-ids)}))))

(defmethod trackings/perform-track e/control-product-detail-toggle-related-addon-list
  [_ event _ app-state]
  #?(:cljs
     (if (get-in app-state c-k/detailed-product-addon-list-open?)
       (stringer/track-event "expand_addon_on_services")
       (stringer/track-event "collapse_add_on_services"))))
