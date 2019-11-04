(ns catalog.product-details-ugc
  (:require #?@(:cljs [[goog.string]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.ugc :as ugc]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as util]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [clojure.string :as str]
            
            
            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))

(defn ^:private carousel-slide
  [destination-event product-id page-slug sku-id dt-prefix idx
   {:keys [image-url]}]
  [:div.p1
   (when dt-prefix {:data-test (str dt-prefix idx)})
   [:a (if destination-event
         (util/fake-href destination-event {:offset idx})
         (util/route-to events/navigate-product-details
                        {:catalog/product-id product-id
                         :page/slug          page-slug
                         :query-params       {:SKU    sku-id
                                              :offset idx}}))
    (ui/aspect-ratio
     1 1
     {:class "flex items-center"}
     [:img.col-12 {:src image-url}])]])

(defn ->title-case [s]
  #?(:clj (str/capitalize s)
     :cljs (goog.string/toTitleCase s)))

(defn parse-int [v]
  #?(:clj (Integer/parseInt v)
     :cljs (js/parseInt v 10)))

(defn ^:private popup-slide [long-name social-card]
  ;; NOTE: desktop-aware? should always be false because we want the slide to take up the full width
  (component/build ugc/social-image-card-component
                   (assoc social-card :desktop-aware? false)
                   {:opts {:copy {:back-copy (str "back to " (->title-case long-name))
                                  :button-copy "View this look"}}}))

(defcomponent component
  [{{:keys [social-cards product-id page-slug sku-id destination-event]} :carousel-data} owner opts]
  (when (seq social-cards)
     [:div.center.mt4
      [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
      (component/build
       carousel/component
       {:slides   (map-indexed
                   (partial carousel-slide destination-event product-id page-slug sku-id "mayvenn-made-slide-")
                   social-cards)
        :settings {:nav        false
                   ;; The breakpoints are mobile-first. That is, the
                   ;; default values apply to the smallest screens, and
                   ;; 1000 means 1000 and above.
                   :items      2
                   :responsive {1000 {:items  3
                                      :center true}}}}
       opts)
      [:p.center.dark-gray.m2
       "Want to show up on our homepage? "
       "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]]))

(defcomponent popup-component [{:keys [carousel-data offset close-message]} owner opts]
  (let [close-attrs (apply util/route-to close-message)]
    ;; navigation event of the PDP page (freeinstall and classic have different events)
     (ui/modal
      {:close-attrs close-attrs}
      [:div.relative
       (component/build carousel/component
                        {:slides   (map (partial popup-slide (:product-name carousel-data))
                                        (:social-cards carousel-data))
                         :settings {:items       1
                                    :edgePadding 0
                                    :nav         false
                                    :startIndex  (parse-int offset)}}
                        {})
       [:div.absolute
        {:style {:top "1.5rem" :right "1.5rem"}}
        (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                         :close-attrs close-attrs})]])))
