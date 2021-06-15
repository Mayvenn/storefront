(ns stylist-matching.top-stylist
  (:require adventure.keypaths
            [stylist-matching.stylist-results :as stylist-results]
            api.orders
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as components.header]
            [storefront.events :as e]
            [stylist-matching.ui.top-stylist-cards :as top-stylist-cards]
            [stylist-matching.ui.gallery-modal :as gallery-modal]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            storefront.keypaths
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.tools :refer [with within]]))

(defcomponent top-stylist-template
  [data _ _]
  (let [top-stylist-data (with :top-stylist data)]
    [:div.bg-pale-purple.black.center.flex.flex-auto.flex-column
     (component/build gallery-modal/organism (with :gallery-modal data) nil)
     (components.header/adventure-header (with :header data))
     [:div.m6
      (titles/canela-huge (with :title top-stylist-data))]
     (ui/screen-aware top-stylist-cards/organism
                      top-stylist-data
                      (component/component-id (:react/key data)))
     (let [{:keys [target primary]} (with :footer top-stylist-data)]
       [:div.mt4.mb8
        (ui/button-medium-underline-primary
         (apply utils/route-to target)
         primary)])]))

(defn ^:export page
  [app-state _]
  (let [;; Models
        current-order (api.orders/current app-state)
        matching      (stylist-matching.core/stylist-matching<- app-state)

        ;; Experiments
        just-added-only?       (experiments/just-added-only? app-state)
        just-added-experience? (experiments/just-added-experience? app-state)
        just-added-control?    (experiments/just-added-control? app-state)
        stylist-results-test?  (experiments/stylist-results-test? app-state)
        stylist-search-results        (:results/stylists matching)
        preferences                   (:param/services matching)
        top-stylist            (->> stylist-search-results
                                    (filter (partial stylist-results/matches-preferences? preferences))
                                    first)        stylist-data                  {:just-added-only?       just-added-only?
                                                                                 :just-added-experience? just-added-experience?
                                                                                 :stylist-results-test?  stylist-results-test?}]
    (component/build
     top-stylist-template
     (merge #_(within stylist.analytics
                      {:cards [(->> top-stylist
                                    (conj '())
                                    (assoc stylist-data :stylists)
                                    stylist-data->stylist-cards
                                    first)]})

            (within :gallery-modal
                    (stylist-results/gallery-modal-query app-state))

            (within :template
                    {:spinning?
                     (or (empty? (:status matching))
                         (utils/requesting-from-endpoint? app-state request-keys/fetch-matched-stylists)
                         (utils/requesting-from-endpoint? app-state request-keys/fetch-stylists-matching-filters)
                         (utils/requesting-from-endpoint? app-state request-keys/get-products)
                         (and (not (get-in app-state storefront.keypaths/loaded-convert))
                              stylist-results-test?
                              (or (not just-added-control?)
                                  (not just-added-only?)
                                  (not just-added-experience?))))})
            (within :top-stylist
                    (->> top-stylist
                         (conj '())
                         (assoc stylist-data :stylists)
                         stylist-results/stylist-data->stylist-cards
                         first))
            (within :top-stylist.crown
                    {:primary [:div.p-color.line-height-4 "Top Stylist"]
                     :icon    [:svg/crown {:style {:height "1.2em"
                                                   :width  "1.7em"}
                                           :class "fill-p-color mr1"}]})
            (within :top-stylist.laurels
                    {:points [{:icon    [:svg/calendar {:style {:height "1.2em"
                                                                :width  "1.7em"}
                                                        :class "fill-s-color mr1"}]
                               :primary [:div.content-4 "Booked 145 times"]}
                              {:icon    [:svg/chat-bubble-diamonds {:style {:height "1.2em"
                                                                            :width  "1.7em"}
                                                                    :class "fill-s-color mr1"}]
                               :primary [:div.content-4 "100% response rate"]}
                              {:icon    [:svg/experience-badge {:style {:height "1.2em"
                                                                        :width  "1.7em"}
                                                                :class "fill-s-color mr1"}]
                               :primary [:div.content-4 "Progessional salon"]}
                              {:icon    [:svg/certified {:style {:height "1.2em"
                                                                 :width  "1.7em"}
                                                         :class "fill-s-color mr1"}]
                               :primary [:div.content-4 "State licensed stylist"]}]})

            (within :header
                    (stylist-results/header<- current-order))
            (within :top-stylist.footer
                    {:primary "View All Stylists"
                     :target  [e/flow|stylist-matching|diversion-skipped]})
            (within :top-stylist.title
                    {:id        "top-match-copy-header"
                     :primary   "You are in luck!"
                     :secondary (str "Top Stylist alert! "
                                     (-> top-stylist :address :firstname) " " (-> top-stylist :address :lastname)
                                     " is an experienced and licensed "
                                     "stylist who is rated highly for their skill, professionalism, and "
                                     "cleanliness.")})))))
