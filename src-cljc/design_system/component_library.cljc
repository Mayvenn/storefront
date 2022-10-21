(ns design-system.component-library
  (:require [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.platform.component-utils :as util]
            [storefront.events :as e]
            [mayvenn.visual.tools :refer [with within]]
            clojure.edn
            clojure.pprint))


(def components-list
  [{:title           "Accordion"
    :id              "accordion"
    :component-class accordion/component
    :opts            {:accordion.drawer.open/face-component   accordion/simple-face-open
                      :accordion.drawer.closed/face-component accordion/simple-face-closed
                      :accordion.drawer/contents-component    accordion/simple-contents}}])

(defn ^:private component-id->component-entry [component-id]
  (->> components-list (filter #(= component-id (:id %))) first))

(c/defcomponent component
  [{:keys [current-component-id] :as props} _owner opts]
  [:div.grid
   {:style {:grid-template-columns "150px auto"
            :grid-template-rows    "1fr 1fr"
            :min-height            "90vh"}}
   [:ul.p0
    {:style {:grid-row "1 / 3"}}
    (map (fn [{:keys [id title]}]
           [:li
            (merge {:key id}
                   (when (= id current-component-id)
                     {:class "bold bg-checkerboard"}))
            [:a.block.inherit-color.p2
             (util/route-to e/navigate-design-system-component-library {:query-params {:id id}})
             title]]) components-list)]
   [:div.p4.bg-checkerboard
    {:style {:grid-row "1 / 3"}}
    [:div.border-dotted.border-p-color

     (when-let [{:keys [title id component-class]} (component-id->component-entry current-component-id)]
       [:div
        [:div title " (" id ")"]
        ;; TODO: don't hard code namespace ("example-accordion")
        (c/build component-class (with "example-accordion" props) {:opts opts})])]]])

(defn query [app-state]
  (when-let [component-id (-> app-state (get-in k/navigation-query-params) :id component-id->component-entry :id)]
    (merge
     {:current-component-id component-id}
     (accordion/accordion-query (let [{:accordion/keys [open-drawers]} (accordion/<- app-state "example-accordion")]
                                  {:id                   "example-accordion"
                                   :open-drawers         open-drawers
                                   :allow-all-closed?    false
                                   :allow-multi-open?    false
                                   :initial-open-drawers #{"drawer-1"}
                                   :drawers              [{:id       "drawer-1"
                                                           :face     {:copy "foo"}
                                                           :contents {:copy "bar"}}
                                                          {:id       "drawer-2"
                                                           :face     {:copy "food"}
                                                           :contents {:copy "bard"}}]})))))

(defn ^:export built-component
  [app-state _opts]
  (c/build component (query app-state) {:opts (-> app-state
                                                  (get-in k/navigation-query-params)
                                                  :id
                                                  component-id->component-entry
                                                  :opts)}))
