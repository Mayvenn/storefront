(ns catalog.menu
  (:require [catalog.categories :as categories]
            [spice.selector :as selector]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [ui.molecules :as ui-molecules]))

;;NOTE Used by slideout-nav


(defn major-menu-row [& content]
  [:div.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defcomponent component
  [{:menu/keys [title options] :as queried-data}
   owner
   opts]
  [:div
   [:div.px2.py3.bg-cool-gray (ui-molecules/return-link queried-data)]
   [:div.col-8.mx-auto.pt8
    (major-menu-row
     [:div.h2.flex-auto.title-2.proxima.shout.pb2 "Shop " title])
    [:ul.list-reset
     (for [{:keys [key nav-message new? copy]} options]
       [:li.mb2 {:key key}
        (major-menu-row
         (assoc (apply utils/route-to nav-message)
                :data-test (str "menu-step-" key))
         [:span.flex-auto.titleize
          (when new?
            [:span.p-color "NEW "])
          copy])])]]])

(defn category-query [data]
  (let [{:keys [selector/essentials] :as nav-root} (categories/current-traverse-nav data)]
    {:return-link/event-message [events/menu-home]
     :return-link/copy          "Back"
     :return-link/id            "back-from-category"
     :menu/title                (:copy/title nav-root)
     :menu/options              (->> categories/menu-categories
                                     (selector/match-all {:selector/strict? true}
                                                         (select-keys nav-root essentials))
                                     (map #(assoc %
                                                  :nav-message [events/navigate-category %]
                                                  :key (:page/slug %)
                                                  :new? (:category/new? %)
                                                  :copy (:copy/title %))))}))

(defn shop-looks-query [data]
  {:return-link/event-message [events/menu-home]
   :return-link/copy          "Back"
   :return-link/id            "back-from-shop-looks"
   :menu/title                "By Look"
   :menu/options              [{:key "all"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :look}]
                                :new? false
                                :copy "All Looks"}
                               {:key "straight"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :straight-looks}]
                                :new? false
                                :copy "Straight Looks"}
                               {:key "curly"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :wavy-curly-looks}]
                                :new? false
                                :copy "Wavy & Curly Looks"}]})

(defn shop-bundle-sets-query [data]
  {:return-link/event-message [events/menu-home]
   :return-link/copy          "Back"
   :return-link/id            "back-from-shop-bundle-sets"
   :menu/title                "By Bundle Sets"
   :menu/options              [{:key "all"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :all-bundle-sets}]
                                :new? false
                                :copy "All Bundle Sets"}
                               {:key "straight"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :straight-bundle-sets}]
                                :new? false
                                :copy "Straight Bundle Sets"}
                               {:key "curly"
                                :nav-message [events/navigate-shop-by-look {:album-keyword :wavy-curly-bundle-sets}]
                                :new? false
                                :copy "Wavy & Curly Bundle Sets"}]})

(defmethod transitions/transition-state events/menu-home
  [_ _ _ app-state]
  (update-in app-state keypaths/ui dissoc :current-traverse-nav))

(defmethod transitions/transition-state events/menu-list
  [_ _ {:keys [menu-type catalog/category-id]} app-state]
  (-> app-state
      (assoc-in keypaths/current-traverse-nav-menu-type (or menu-type :category))
      (assoc-in keypaths/current-traverse-nav-id category-id)))

(defmethod effects/perform-effects events/control-menu-expand-hamburger
  [_ _ _ _ _]
  (messages/handle-message events/menu-home))
