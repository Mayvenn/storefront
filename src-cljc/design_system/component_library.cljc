(ns design-system.component-library
  (:require homepage.ui.promises
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
  [{:title           "Accordion: One open at a time"
    :id              "accordion"
    :query-ns        "example-accordion"
    :component-class accordion/component
    :opts            {:accordion.drawer.open/face-component   accordion/simple-face-open
                      :accordion.drawer.closed/face-component accordion/simple-face-closed
                      :accordion.drawer/contents-component    accordion/simple-contents}}
   {:title           "Accordion: Fully closable/openable"
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
   {:title           "Image Carousel"
    :id              "image-carousel"
    :query-ns        "example-image-carousel"
    :component-class carousel/component
    :opts            {:carousel/exhibit-component carousel/carousel-image-component}}
   {:title           "Promises"
    :id              "promises"
    :query-ns        "example-promises"
    :component-class homepage.ui.promises/organism
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
            (merge
             {:key (str id title)}
             (when (= id current-component-id)
               {:class "bold bg-checkerboard"}))
            [:a.block.inherit-color.p2
             (util/route-to e/navigate-design-system-component-library {:query-params {:id id}})
             title]]) components-list)]
   [:div.p4.bg-checkerboard
    {:style {:grid-row "1 / 3"}}
    (when-let [{:keys [title id query-ns component-class]} (component-id->component-entry current-component-id)]
      [:div
       [:div title]
       [:div.border-dotted.border-p-color.overflow-hidden
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
                                  {:id                "example-accordion2"
                                   :open-drawers      open-drawers
                                   :allow-all-closed? true
                                   :allow-multi-open? true
                                   :drawers           [{:id       "drawer-1"
                                                        :face     {:copy "foo"}
                                                        :contents {:copy "bar"}}
                                                       {:id       "drawer-2"
                                                        :face     {:copy "food"}
                                                        :contents {:copy "bard"}}]}))
     (within :example-carousel {:exhibits [{:class "bg-red"
                                            :index 0}
                                           {:class "bg-green"
                                            :index 1}
                                           {:class "bg-blue"
                                            :index 2}]})
     (within :example-image-carousel {:exhibits [{:src "http://placekitten.com/400/600?image=1"
                                                  :alt "image 0"}
                                                 {:src  "http://ucarecdn.com/89a0181c-cbbe-4a66-bed5-cc90e6a886e5/"
                                                  :type "video"
                                                  :alt  "video 1"}
                                                 {:src "http://placekitten.com/400/600?image=2"
                                                  :alt "image 2"}]})
     (within :example-promises
             {:list/icons
              [{:promises.icon/symbol :svg/hand-heart,
                :promises.icon/title  "Top-Notch Service"}
               {:promises.icon/symbol :svg/shield,
                :promises.icon/title  "30 Day Guarantee"}
               {:promises.icon/symbol :svg/check-cloud,
                :promises.icon/title  "100% Virgin Human Hair"}
               {:promises.icon/symbol :svg/ship-truck,
                :promises.icon/title  "Free Standard Shipping"}]}))))

(defn ^:export built-component
  [app-state _opts]
  (c/build component (query app-state) {:opts (-> app-state
                                                  (get-in k/navigation-query-params)
                                                  :id
                                                  component-id->component-entry
                                                  :opts)}))
