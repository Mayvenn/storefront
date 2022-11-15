(ns design-system.component-library
  (:require homepage.ui.guarantees-v2022-10
            [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.components.carousel :as carousel]
            [storefront.platform.component-utils :as util]
            [storefront.events :as e]
            [mayvenn.visual.tools :refer [with within]]
            clojure.edn
            clojure.pprint))


(def components-list
  [{:title           "Accordion"
    :id              "accordion"
    :query-ns        "example-accordion"
    :component-class accordion/component
    :opts            {:accordion.drawer.open/face-component   accordion/simple-face-open
                      :accordion.drawer.closed/face-component accordion/simple-face-closed
                      :accordion.drawer/contents-component    accordion/simple-contents}}
   {:title           "Accordion 2"
    :id              "accordion2"
    :query-ns        "example-accordion2"
    :component-class accordion/component
    :opts            {:accordion.drawer.open/face-component   accordion/simple-face-open
                      :accordion.drawer.closed/face-component accordion/simple-face-closed
                      :accordion.drawer/contents-component    accordion/simple-contents}}
   {:title           "Carousel"
    :id              "carousel"
    :query-ns        "example-carousel"
    :component-class carousel/component
    :opts            {:carousel/exhibit-component carousel/example-exhibit-component}}
   {:title           "Guarantees"
    :id              "guarantees"
    :query-ns        "example-guarantees"
    :component-class homepage.ui.guarantees-v2022-10/organism
    :opts            {}}])

(defn ^:private component-id->component-entry [component-id]
  (->> components-list (filter #(= component-id (:id %))) first))

(c/defcomponent component
  [{:keys [current-component-id] :as props} _owner opts]
  [:div.grid
   {:style {:grid-template-columns "150px auto"
            :grid-template-rows    "1fr 1fr"
            :min-height            "90vh"}}
   [:ul.p0.list-style-none
    {:style {:grid-row "1 / 3"}}
    (map (fn [{:keys [id title]}]
           [:li
            (when (= id current-component-id)
              {:class "bold bg-checkerboard"})
            [:a.block.inherit-color.p2
             (util/route-to e/navigate-design-system-component-library {:query-params {:id id}})
             title]]) components-list)]
   [:div.p4.bg-checkerboard
    {:style {:grid-row "1 / 3"}}
    (when-let [{:keys [title id query-ns component-class]} (component-id->component-entry current-component-id)]
      [:div
       [:div title " (" id ")"]
       [:div.border-dotted.border-p-color
        (c/build component-class (with query-ns props) {:opts opts
                                                        :key  id})]])]])

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
                                                           :contents {:copy "bard"}}]}))
     (accordion/accordion-query (let [{:accordion/keys [open-drawers]} (accordion/<- app-state "example-accordion2")]
                                  {:id                   "example-accordion2"
                                   :open-drawers         open-drawers
                                   :allow-all-closed?    true
                                   :allow-multi-open?    true
                                   :drawers              [{:id       "drawer-1"
                                                           :face     {:copy "foo"}
                                                           :contents {:copy "bar"}}
                                                          {:id       "drawer-2"
                                                           :face     {:copy "food"}
                                                           :contents {:copy "bard"}}]}))
     (within :example-carousel {:exhibits [{:class "bg-red"}
                                           {:class "bg-blue"}
                                           {:class "bg-green"}
                                           {:class "bg-yellow"}
                                           {:class "bg-purple"}]})
     (within :example-guarantees
             {:list/icons
              [{:guarantees.icon/symbol [:svg/heart {:width "32px", :height "29px"}],
                :guarantees.icon/title "Top-Notch Customer Service"}
               {:guarantees.icon/symbol [:svg/calendar
                                         {:width "30px", :height "33px"}],
                :guarantees.icon/title "30 Day Guarantee"}
               {:guarantees.icon/symbol [:svg/worry-free
                                         {:width "35px", :height "36px"}],
                :guarantees.icon/title "100% Virgin Hair"}
               {:guarantees.icon/symbol [:svg/ship-truck
                                         {:width "30px", :height "34px"}],
                :guarantees.icon/title "Free Standard Shipping"}]
              })
     )))

(defn ^:export built-component
  [app-state _opts]
  (c/build component (query app-state) {:opts (-> app-state
                                                  (get-in k/navigation-query-params)
                                                  :id
                                                  component-id->component-entry
                                                  :opts)}))
