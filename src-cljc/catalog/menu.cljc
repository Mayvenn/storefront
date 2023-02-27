(ns catalog.menu
  (:require #?@(:cljs [[storefront.browser.tags :as tags]])
            [api.catalog :refer [select]]
            [catalog.categories :as categories]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [ui.molecules :as ui-molecules]
            [storefront.accessors.experiments :as experiments]))

;;NOTE Used by slideout-nav

(defn major-menu-row [& content]
  [:div.py3
   (into [:a.block.inherit-color.flex.items-center.content-1.proxima] content)])

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
            [:span.p-color.shout "New "])
          copy])])]]])

(defn category-query [data]
  (let [{:keys [selector/essentials] :as nav-root} (accessors.categories/current-traverse-nav data)]
    {:return-link/event-message [events/menu-home]
     :return-link/copy          "Back"
     :return-link/id            "back-from-category"
     :menu/title                (:copy/title nav-root)
     :menu/options              (->> categories/menu-categories
                                     (select (select-keys nav-root essentials))
                                     (remove :menu/hide?)
                                     (map #(assoc %
                                                  :nav-message [events/navigate-category %]
                                                  :key (:page/slug %)
                                                  :new? (:category/new? %)
                                                  :copy (or (:menu/title %)
                                                            (:copy/title %)))))}))

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

(defn hair-shop-query [data]
  {:return-link/event-message [events/menu-home]
   :return-link/copy          "Back"
   :return-link/id            "back-from-hair-shop"
   :menu/title                "Hair Shop"
   :menu/options              (concat
                               [{:key "wigs"
                                 :nav-message [events/navigate-category {:page/slug "wigs" :catalog/category-id "13"}]
                                 :new? false
                                 :copy "Wigs"}
                                {:key "hair-bundles"
                                 :nav-message [events/navigate-category {:page/slug "human-hair-bundles" :catalog/category-id "27"}]
                                 :new? false
                                 :copy "Hair Bundles"}
                                {:key "closures"
                                 :nav-message [events/navigate-category {:page/slug "virgin-closures" :catalog/category-id "0"}]
                                 :new? false
                                 :copy "Closures"}
                                {:key "frontals"
                                 :nav-message [events/navigate-category {:page/slug "virgin-frontals" :catalog/category-id "1"}]
                                 :new? false
                                 :copy "Frontals"}
                                {:nav-message [events/navigate-category {:page/slug "seamless-clip-ins" :catalog/category-id "21"}]
                                 :key "clip-ins"
                                 :copy (str (when (experiments/clearance-clipins? data)
                                              "Clearance ")
                                            "Clip-ins")
                                 :new? false}])})

(defn wigs-query [data]
  {:return-link/event-message [events/menu-home]
   :return-link/copy          "Back"
   :return-link/id            "back-from-wigs"
   :menu/title                "Wigs"
   :menu/options              [{:key         "all-wigs"
                                :nav-message [events/navigate-category {:page/slug "wigs" :catalog/category-id "13"}]
                                :new?        false
                                :copy        "All Wigs"}
                               {:key         "wigs-101"
                                :nav-message [events/navigate-wigs-101-guide {}]
                                :new?        false
                                :copy        "Wigs 101"}
                               {:key         "ready-to-wear-wigs"
                                :nav-message [events/navigate-category {:page/slug           "ready-wear-wigs"
                                                                        :catalog/category-id "25"}]
                                :new?        false
                                :copy        "Ready to Wear Wigs"}]})

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
  #?(:cljs (tags/add-classname ".kustomer-app-icon" "hide"))
  (messages/handle-message events/menu-home))
