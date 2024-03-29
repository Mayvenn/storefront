(ns design-system.component-library
  (:require homepage.ui.promises
            [storefront.component :as c]
            [storefront.keypaths :as k]
            [storefront.components.accordion-v2022-10 :as accordion]
            [storefront.components.carousel :as carousel]
            [storefront.platform.component-utils :as util]
            [storefront.events :as e]
            [mayvenn.visual.tools :refer [with within]]
            clojure.edn))


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
    :opts            {:carousel/exhibit-highlight-component carousel/example-exhibit-component
                      :carousel/id                          :example-carousel}}
   {:title           "Image Carousel"
    :id              "image-carousel"
    :query-ns        "example-image-and-video-carousel"
    :component-class carousel/component
    :opts            {:carousel/exhibit-thumbnail-component carousel/product-carousel-thumbnail
                      :carousel/exhibit-highlight-component carousel/product-carousel-highlight
                      :carousel/id                          :example-image-and-video-carousel}}
   {:title           "Slider Carousel"
    :id              "slider-image-carousel"
    :query-ns        "example-slider-image-carousel"
    :component-class carousel/component
    :opts            {:carousel/exhibit-highlight-component carousel/slider-image-exhibit
                      :carousel/id                          :example-slider-image-carousel
                      :carousel/desktop-layout              :slider}}
   {:title           "No Scroll Carousel"
    :id              "no-scroll-carousel"
    :query-ns        "example-no-scroll-carousel"
    :component-class carousel/component
    :opts            {:carousel/exhibit-highlight-component carousel/slider-image-exhibit
                      :carousel/id                          :example-slider-image-carousel
                      :carousel/desktop-layout              :no-scroll}}
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
     (within :example-carousel {:selected-exhibit-idx (:idx (carousel/<- app-state :example-carousel))
                                :exhibits             [{:class "bg-red"}
                                                       {:class "bg-green"}
                                                       {:class "bg-blue"}
                                                       {:class "bg-aqua"}
                                                       {:class "bg-purple"}
                                                       {:class "bg-orange"}
                                                       {:class "bg-lime"}
                                                       {:class "bg-maroon"}
                                                       {:class "bg-pink"}]})
     (within :example-image-and-video-carousel {:selected-exhibit-idx (:idx (carousel/<- app-state :example-image-and-video-carousel))
                                                :exhibits             [{:src "http://placekitten.com/400/600?image=1"
                                                                        :alt "image 1"}
                                                                       {:src  "http://ucarecdn.com/89a0181c-cbbe-4a66-bed5-cc90e6a886e5/"
                                                                        :type "video"
                                                                        :alt  "video 1"}
                                                                       {:src "http://placekitten.com/400/600?image=2"
                                                                        :alt "image 2"}
                                                                       {:src "http://placekitten.com/400/600?image=3"
                                                                        :alt "image 3"}
                                                                       {:src "http://placekitten.com/400/600?image=4"
                                                                        :alt "image 4"}
                                                                       {:src "http://placekitten.com/400/600?image=5"
                                                                        :alt "image 5"}
                                                                       {:src "http://placekitten.com/400/600?image=6"
                                                                        :alt "image 6"}
                                                                       {:src "http://placekitten.com/400/600?image=7"
                                                                        :alt "image 7"}
                                                                       {:src "http://placekitten.com/400/600?image=8"
                                                                        :alt "image 8"}]})
     (within :example-slider-image-carousel {:selected-exhibit-idx (:idx (carousel/<- app-state :example-slider-image-carousel))
                                             :exhibits             [{:src "http://placekitten.com/400/400?image=1"
                                                                     :alt "image 1"}
                                                                    {:src "http://placekitten.com/400/400?image=2"
                                                                     :alt "image 2"}
                                                                    {:src "http://placekitten.com/400/400?image=3"
                                                                     :alt "image 3"}
                                                                    {:src "http://placekitten.com/400/400?image=4"
                                                                     :alt "image 4"}
                                                                    {:src "http://placekitten.com/400/400?image=5"
                                                                     :alt "image 5"}
                                                                    {:src "http://placekitten.com/400/400?image=6"
                                                                     :alt "image 6"}
                                                                    {:src "http://placekitten.com/400/400?image=7"
                                                                     :alt "image 7"}
                                                                    {:src "http://placekitten.com/400/400?image=8"
                                                                     :alt "image 8"}]})
     (within :example-no-scroll-carousel {:selected-exhibit-idx (:idx (carousel/<- app-state :example-no-scroll-carousel))
                                          :exhibits             [{:src "http://placekitten.com/400/400?image=1"
                                                                  :alt "image 1"}
                                                                 {:src "http://placekitten.com/400/400?image=2"
                                                                  :alt "image 2"}
                                                                 {:src "http://placekitten.com/400/400?image=3"
                                                                  :alt "image 3"}
                                                                 {:src "http://placekitten.com/400/400?image=4"
                                                                  :alt "image 4"}
                                                                 {:src "http://placekitten.com/400/400?image=5"
                                                                  :alt "image 5"}
                                                                 {:src "http://placekitten.com/400/400?image=6"
                                                                  :alt "image 6"}
                                                                 {:src "http://placekitten.com/400/400?image=7"
                                                                  :alt "image 7"}]})

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
