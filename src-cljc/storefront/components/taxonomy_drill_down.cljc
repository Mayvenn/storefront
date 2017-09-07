(ns storefront.components.taxonomy-drill-down
  (:require #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[datascript.core :as d]
                       [storefront.api :as api]
                       [storefront.utils.maps :as maps]
                       [storefront.component :as component]
                       [storefront.history :as history]])
            [catalog.categories :as categories]
            [catalog.selector :as selector]
            [clojure.set :as set]
            [storefront.accessors.category-filters :as category-filters]
            [storefront.components.svg :as svg]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]))

(def ^:private slug->jump-out
  {"360-frontals" {:id   "10"
                   :name "360 Frontals"
                   :slug "360-frontals"}
   "closures"     {:id   "0"
                   :name "Closures"
                   :slug "closures"}
   "frontals"     {:id   "1"
                   :name "Frontals"
                   :slug "frontals"}})

(def ^:private back-caret
  (component/html
   (svg/dropdown-arrow {:class  "stroke-gray"
                        :width  "12px"
                        :height "10px"
                        :style  {:transform "rotate(90deg)"}})))

;;NOTE Used by slideout-nav
(def ^:private forward-caret
  (component/html
   (svg/dropdown-arrow {:class  "stroke-black"
                        :width  "23px"
                        :height "20px"
                        :style  {:transform "rotate(-90deg)"}})))

;;NOTE Used by slideout-nav (but not here)
(defn- minor-menu-row [& content]
  [:div.border-bottom.border-gray
   {:style {:padding "3px 0 2px"}}
   (into [:a.block.py1.h5.inherit-color.flex.items-center] content)])

;;NOTE Used by slideout-nav
(defn- major-menu-row [& content]
  [:div.h4.border-bottom.border-gray.py3
   (into [:a.block.inherit-color.flex.items-center] content)])

(defn ^:private valid-branch? [selected-options option]
  (or (empty? selected-options)
      (->> selected-options
           (map (partial map :sku-set-ids))
           (map (partial reduce set/union))
           (reduce set/intersection (:sku-set-ids option))
           seq)))

(defn- jump-out-li [jump-out]
  [:li {:key (:slug jump-out)}
   (major-menu-row
    (utils/fake-href events/navigate-category (select-keys jump-out [:id :slug]))
    [:span.teal "All " (:name jump-out)])])

(defn- down-step-li [option down-step current-step]
  [:li {:key (:slug option)}
   (major-menu-row
    (utils/fake-href events/menu-traverse-descend
                     {:down-step       down-step
                      :current-step    current-step
                      :selected-option option})
    [:span.flex-auto (:label option)] forward-caret)])

(defn- terminal-li [criteria option current-step]
  [:li {:key (:slug option)}
   (major-menu-row
    (utils/fake-href events/menu-traverse-out {:criteria (assoc criteria (:slug current-step) #{(:slug option)})})
    [:span.flex-auto.titleize (:label option)])])

(defn component
  [{:keys [root-name facets criteria promo-data current-step up-step down-step up-step-option selected-options]}
   owner
   opts]
  (component/create
   [:div
    [:a.gray.block.py1.px3.h6
     (if up-step
       (utils/fake-href events/menu-traverse-ascend {:up-step up-step})
       (utils/fake-href events/menu-traverse-root))
     [:span.mr1 back-caret] "Back"]
    [:div.px6
     (major-menu-row
      [:div.h2.flex-auto.center "Shop " root-name])
     [:ul.list-reset
      (when-let [jump-out (get slug->jump-out (:slug up-step-option))]
        (jump-out-li jump-out))
      (for [option (:options current-step)
            :when  (valid-branch? selected-options option)]
        (if down-step
          (down-step-li option down-step current-step)
          (terminal-li criteria option current-step)))]]]))

(defn query [data]
  (let [{root-name :name}                     (categories/current-traverse-nav data)
        {:keys [facets criteria] :as filters} (get-in data keypaths/category-filters-for-nav)
        [selected-steps unselected-steps]     (split-with :selected? facets)
        up-step                               (last (drop-last selected-steps))]
    {:root-name        root-name
     :criteria         criteria
     :current-step     (last selected-steps)
     :up-step          up-step
     :down-step        (first unselected-steps)
     :up-step-option   (first (filterv :selected? (:options up-step)))
     :selected-options (->> selected-steps
                            (map :options)
                            (map #(filterv :selected? %))
                            (remove empty?))}))

(defn ^:private ascend [filters {facet-slug :slug :as up-step}]
  (let [option-slug (-> (:criteria filters)
                        (get facet-slug)
                        first)]
    (-> filters
        (category-filters/undo-criterion)
        (category-filters/step up-step))))

(defn ^:private descend [filters current-step selected-option down-step]
  (-> filters
      (category-filters/replace-criterion (:slug current-step)
                                          (:slug selected-option))
      (category-filters/step down-step)))

(defmethod transitions/transition-state events/menu-traverse-root
  [_ _ {:keys [id]} app-state]
  (-> app-state
      (assoc-in  keypaths/category-filters-for-nav {})
      (update-in keypaths/current-traverse-nav dissoc :id)))

(defmethod effects/perform-effects events/menu-traverse-root
  [_ _ _ _ app-state]
  #?(:cljs
     (do
       (api/search-sku-sets (get-in app-state keypaths/api-cache)
                            {:product/department #{"hair"} :hair/family #{"closures" "frontals" "360-frontals"}}
                            identity)
       (api/search-sku-sets (get-in app-state keypaths/api-cache)
                            {:product/department #{"hair"} :hair/family #{"bundles"}}
                            identity)
       (api/fetch-facets (get-in app-state keypaths/api-cache)))))

(defmethod transitions/transition-state events/menu-traverse-descend
  [_ _ {:keys [id current-step selected-option down-step]} app-state]
  (if id
    (assoc-in app-state keypaths/current-traverse-nav-id id)
    (update-in app-state keypaths/category-filters-for-nav
               descend current-step selected-option down-step)))

(defmethod effects/perform-effects events/menu-traverse-descend
  [_ _ {:keys [id slug] :as args} _ app-state]
  #?(:cljs
     (when id
       (let [category (categories/current-traverse-nav app-state)]
         (api/search-sku-sets (get-in app-state keypaths/api-cache)
                              (:criteria category)
                              #(messages/handle-message events/api-success-sku-sets-for-nav
                                                        (assoc %
                                                               :category-id (:id category)
                                                               :criteria {})))))))

(defmethod transitions/transition-state events/menu-traverse-ascend
  [_ _ {:keys [up-step]} app-state]
  (update-in app-state
             keypaths/category-filters-for-nav
             ascend up-step))

(defn first-and-only-category [col]
  #?(:cljs (when (zero? (count col))
             (js/console.error "More than possible category found during menu-traverse-out.  Guessing you meant the first one...")))
  (first col))

(defmethod effects/perform-effects events/menu-traverse-out
  [_ event {:keys [criteria]} _ app-state]
  #?(:cljs
     ;;TODO: create and use a sku-set-db from app-state (like keypaths/db-skus)
     (let [sku-sets-db       (-> (d/empty-db)
                                 (d/db-with (->> (get-in app-state keypaths/sku-sets)
                                                 vals
                                                 (map #(merge (maps/map-values first (:criteria/essential %)) %))
                                                 (mapv #(dissoc % :criteria/essential)))))
           possible-sku-sets (selector/query sku-sets-db criteria)
           sku-set           (first possible-sku-sets)]
       (if (> (count possible-sku-sets) 1)
         (history/enqueue-navigate events/navigate-category
                                   (-> (filter (fn [category] (= (:criteria category)
                                                                 criteria))
                                               categories/initial-categories)
                                       first-and-only-category
                                       (select-keys [:id :slug])))
         (history/enqueue-navigate events/navigate-product-details {:id   (:sku-set/id sku-set)
                                                                    :slug (:sku-set/slug sku-set)})))))

(defmethod transitions/transition-state events/api-success-sku-sets-for-nav
  [_ event response app-state]
  (let [filters (categories/make-category-filters app-state response)]
    (assoc-in app-state keypaths/category-filters-for-nav
              (category-filters/open filters (-> filters
                                                 :facets
                                                 first
                                                 :slug)))))

(defmethod effects/perform-effects events/control-menu-expand-hamburger
  [_ _ _ _ _]
  (messages/handle-message events/menu-traverse-root))
