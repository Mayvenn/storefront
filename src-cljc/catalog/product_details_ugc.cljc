(ns catalog.product-details-ugc
  (:require #?@(:cljs [[goog.string]
                       [storefront.history]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ugc :as ugc]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as util]
            [storefront.platform.carousel :as carousel]
            [clojure.string :as string]))

(defn ^:private carousel-slide
  [destination-event product-id page-slug sku-id dt-prefix idx
   {:keys [image-url]}]
  (component/html
   [:a.block.p1
    (merge
     (when dt-prefix {:data-test (str dt-prefix "-slide-" idx)})
     (if destination-event
       (util/fake-href destination-event {:offset idx})
       (util/route-to events/navigate-product-details
                      {:catalog/product-id product-id
                       :page/slug          page-slug
                       :query-params       {:SKU    sku-id
                                            :offset idx}})))
    (ui/aspect-ratio
     1 1
     {:class "flex items-center"}
     (ui/basic-defer-img {:class "col-12"} image-url))]))

(defn ->title-case [s]
  #?(:clj (string/capitalize s)
     :cljs (goog.string/toTitleCase s)))

(defn parse-int [v]
  #?(:clj (Integer/parseInt v)
     :cljs (js/parseInt v 10)))

(defn ^:private popup-slide [long-name social-card]
  ;; NOTE: desktop-aware? should always be false because we want the slide to take up the full width
  (ui/screen-aware ugc/social-image-card-component
                   (assoc social-card :desktop-aware? false)
                   {:opts {:copy {:back-copy (str "back to " (->title-case long-name))
                                  :button-copy "View this look"}}}))

(defcomponent component
  [{{:keys [social-cards product-id page-slug sku-id destination-event]} :carousel-data
    id :id} owner opts]
  (when (seq social-cards)
    [:div.center.mt4
     {:data-test id}
     [:div.proxima.title-2.m2.shout "#MayvennMade"]
     (component/build
      carousel/component
      {:data social-cards}
      (update opts
              :opts
              merge {:settings {:nav         false
                                :startIndex  0
                                :edgePadding 50
                                ;; setting this to true causes some of our event listeners to
                                ;; get dropped by tiny-slider.
                                :loop        false
                                ;; The breakpoints are mobile-first. That is, the
                                ;; default values apply to the smallest screens, and
                                ;; 1000 means 1000 and above.
                                :items       2
                                :responsive  {1000 {:items 3}}}
                     :slides   (map-indexed
                                (partial carousel-slide
                                         destination-event
                                         product-id
                                         page-slug
                                         sku-id
                                         id)
                              social-cards)}))
     [:p.center.px6.mb2.mt4.content-2.proxima
      "Want to show up on our homepage? "
      "Tag your best pictures wearing Mayvenn with "]
     [:div.block.shout.proxima.title-3 "#MayvennMade"]]))

(defcomponent popup-component [{:keys [carousel-data offset close-message]} owner opts]
  (let [[nav-event nav-args] close-message
        close-attrs          (apply util/route-to close-message)]
    ;; navigation event of the PDP page (freeinstall and classic have different events)
    (ui/modal
     {:close-attrs close-attrs}
     (component/html
      [:div.relative
       (component/build carousel/component
                        {:data (:social-cards carousel-data)}
                        {:opts {:settings {:items       1
                                           :edgePadding 0
                                           :nav         false
                                           :startIndex  (parse-int offset)}
                                :slides (mapv (partial popup-slide (:product-name carousel-data))
                                              (:social-cards carousel-data))
                                :events [["indexChanged"
                                          (fn [info _]
                                            #?(:cljs
                                               (storefront.history/enqueue-redirect
                                                nav-event (-> nav-args
                                                              (update
                                                               :query-params
                                                               assoc :offset (some-> info
                                                                                     .-index
                                                                                     dec))))))]]}})
       [:div.absolute
        {:style {:top "1.5rem" :right "1.5rem"}}
        (ui/modal-close {:class       "stroke-black fill-gray"
                         :data-test   "pdp-ugc-modal-close"
                         :close-attrs close-attrs})]]))))
