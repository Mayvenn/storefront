(ns catalog.product-details-ugc
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.pixlee :as pixlee]
            [storefront.accessors.experiments :as experiments]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as util]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [catalog.products :as products]
            #?@(:clj [[storefront.component-shim :as component]]
                :cljs [[storefront.component :as component]
                       [goog.string]])
            [spice.core :as spice]
            [clojure.string :as str]))

(defn ^:private carousel-slide [product-id page-slug sku-id idx {:keys [imgs content-type]}]
  [:div.p1
   [:a (util/route-to events/navigate-product-details
                      {:catalog/product-id product-id
                       :page/slug          page-slug
                       :query-params       {:SKU    sku-id
                                            :offset idx}})
    (ui/aspect-ratio
     1 1
     {:class "flex items-center"}
     [:img.col-12 (:medium imgs)]
     (when (= content-type "video")
       [:div.absolute.overlay.flex.items-center.justify-center
        svg/play-video-muted]))]])

(defn ^:private content-view [{:keys [imgs content-type source-url] :as item}]
  (ui/aspect-ratio
   1 1
   {:class "bg-black"}
   (if (= content-type "video")
     [:video.container-size.block {:controls true}
      [:source {:src source-url}]]
     [:div.container-size.bg-cover.bg-no-repeat.bg-center
      {:style {:background-image (str "url(" (-> imgs :large :src) ")")}}])))

(defn ^:private view-look-button [{{:keys [view-look view-other]} :links} nav-stack-item]
  (let [[nav-event nav-args] (or view-look view-other)]
    (ui/teal-button
     (util/route-to nav-event nav-args nav-stack-item)
     "View this look")))

(defn ^:private user-attribution [{:keys [user-handle social-service]}]
  [:div.flex.items-center
   [:div.flex-auto.dark-gray.medium {:style {:word-break "break-all"}} "@" user-handle]
   [:div.ml1.line-height-1 {:style {:width "1em" :height "1em"}}
    (svg/social-icon social-service)]])

(defn ->title-case [s]
  #?(:clj (str/capitalize s)
     :cljs (goog.string/toTitleCase s)))

(defn parse-int [v]
  #?(:clj (Integer/parseInt v)
     :cljs (js/parseInt v 10)))

(defn ^:private popup-slide [long-name {:keys [links] :as item}]
  [:div.m1.rounded-bottom
   (content-view item)
   [:div.bg-white.rounded-bottom.p2
    [:div.h5.px4 (user-attribution item)]
    (when (-> links :view-look boolean)
      [:div.mt2 (view-look-button item {:back-copy (str "back to " (->title-case long-name))})])]])

(defn component [{{:keys [album product-id page-slug sku-id]} :carousel-data} owner opts]
  (component/create
   (when (seq album)
     [:div.center.mt4
      [:div.h2.medium.dark-gray.crush.m2 "#MayvennMade"]
      (component/build carousel/component
                       {:slides   (map-indexed (partial carousel-slide product-id page-slug sku-id)
                                               album)
                        :settings {:centerMode    true
                            ;; must be in px, because it gets parseInt'd for
                            ;; the slide width calculation
                                   :centerPadding "36px"
                            ;; The breakpoints are mobile-last. That is, the
                            ;; default values apply to the largest screens, and
                            ;; 1000 means 1000 and below.
                                   :slidesToShow  3
                                   :responsive    [{:breakpoint 1000
                                                    :settings   {:slidesToShow 2}}]}}
                       opts)
      [:p.center.dark-gray.m2
       "Want to show up on our homepage? "
       "Tag your best pictures wearing Mayvenn with " [:span.bold "#MayvennMade"]]])))

(defn popup-component [{:keys [carousel-data offset back] :as data} owner opts]
  (component/create
   (let [close-attrs (util/route-to events/navigate-product-details
                                    {:catalog/product-id (:product-id carousel-data)
                                     :page/slug          (:page-slug carousel-data)
                                     :query-params       {:SKU (:sku-id carousel-data)}})]
     (ui/modal
      {:close-attrs close-attrs}
      [:div.relative
       (component/build carousel/component
                        {:slides   (map (partial popup-slide (:product-name carousel-data))
                                        (:album carousel-data))
                         :settings {:slidesToShow 1
                                    :initialSlide (parse-int offset)}}
                        {})
       [:div.absolute
        {:style {:top "1.5rem" :right "1.5rem"}}
        (ui/modal-close {:class       "stroke-dark-gray fill-gray"
                         :close-attrs close-attrs})]]))))
