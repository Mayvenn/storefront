(ns catalog.menu
  (:require [catalog.categories :as categories]
            [spice.selector :as selector]
            [storefront.component :as component]
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
  [:div.h4.border-bottom.border-gray.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defn component
  [{:menu/keys [title options] :as queried-data}
   owner
   opts]
  (component/create
   [:div
    [:div.ml2.pl4.pt3 (ui-molecules/return-link queried-data)]
    [:div.px6
     (major-menu-row
      [:div.h2.flex-auto.center "Shop " title])
     [:ul.list-reset
      (for [{:keys [key nav-message new? copy]} options]
        [:li {:key key}
         (major-menu-row
          (assoc (apply utils/route-to nav-message)
                 :data-test (str "menu-step-" key))
          [:span.flex-auto.titleize
           (when new?
             [:span.teal "NEW "])
           copy])])]]]))

(defn query [data]
  (let [{:keys [selector/essentials] :as nav-root} (categories/current-traverse-nav data)]
    {:return-link/event-message [events/menu-home]
     :return-link/copy          "Back"
     :menu/title                (:copy/title nav-root)
     :menu/options              (->> categories/menu-categories
                                     (selector/match-all {:selector/strict? true}
                                                         (select-keys nav-root essentials))
                                     (map #(assoc %
                                                  :nav-message [events/navigate-category %]
                                                  :key (:page/slug %)
                                                  :new? (:category/new? %)
                                                  :copy (:copy/title %))))}))

(defmethod transitions/transition-state events/menu-home
  [_ _ _ app-state]
  (update-in app-state keypaths/current-traverse-nav dissoc :id))

(defmethod transitions/transition-state events/menu-list
  [_ _ {:keys [catalog/category-id]} app-state]
  (assoc-in app-state keypaths/current-traverse-nav-id category-id))

(defmethod effects/perform-effects events/control-menu-expand-hamburger
  [_ _ _ _ _]
  (messages/handle-message events/menu-home))
