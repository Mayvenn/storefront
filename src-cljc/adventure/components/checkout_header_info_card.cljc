(ns adventure.components.checkout-header-info-card
  (:require [adventure.components.profile-card :as profile-card]
            [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn component [{:keys [title
                         subtitle
                         cta-title
                         cta-subtitle
                         card-data
                         button
                         data-test]}
                 _
                 opts]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    [:div.border-bottom.border-gray.flex.items-center
     [:div.bg-white.flex-auto.py3 (ui/clickable-logo
                                   {:data-test "header-logo"
                                    :height    "40px"})]]
    [:div.center
     [:div.col-10.mx-auto.py4
      [:div {:data-test data-test}
       [:div.py4.h3.bold title]
       [:div.h5.line-height-3.center subtitle]]
      [:div.my4
       (component/build profile-card/component card-data nil)]
      [:div.py2
       [:h3.bold cta-title]
       [:h4.py2 cta-subtitle]
       [:div.py2
        (ui/teal-button (merge {:class     "bold"
                                :data-test (:data-test button)
                                :key       (:data-test button)}
                               (cond
                                 (:target-message button) (apply utils/route-to (:target-message button))
                                 (:href button)           {:href (:href button)}
                                 :else                    {:href "#"}))
                        (:text button))]]]]]))
