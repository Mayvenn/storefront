(ns storefront.components.landing-page
  (:require [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [api.catalog :refer [select ?discountable ?physical]]
            [storefront.components.svg :as svg]
            [storefront.accessors.contentful :as contentful]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [adventure.keypaths :as adventure.keypaths]
            [mayvenn.concept.email-capture :as concept]
            [mayvenn.visual.tools :refer [within with]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.carousel :as carousel-2022]
            [storefront.routes :as routes]
            [storefront.ugc :as ugc]
            [spice.maps :as maps]
            [storefront.components.homepage-hero :as homepage-hero]))

(defn ^:private url->navigation-message [url]
  (when-not (nil? url)
    (let [[path query-params-string] (clojure.string/split url #"\?")
          query-params               (when (not (clojure.string/blank? query-params-string))
                                       (->> (clojure.string/split query-params-string #"\&")
                                            (map #(clojure.string/split % #"\="))
                                            (into {})))]
          (routes/navigation-message-for path query-params))))

(defn landing-page-slug [data]
  (->> (get-in data storefront.keypaths/navigation-args)
       :landing-page-slug
       keyword))

(defn look<-
  [skus-db looks-shared-carts-db facets-db look promotions album-keyword index remove-free-install?]
  (when-let [shared-cart-id (contentful/shared-cart-id look)]
    (let [shared-cart              (get looks-shared-carts-db shared-cart-id)
          album-copy               (get ugc/album-copy album-keyword)
          sku-ids-from-shared-cart (->> shared-cart
                                        :line-items
                                        (mapv :catalog/sku-id)
                                        sort)
          sku-id->quantity         (->> shared-cart
                                        :line-items
                                        (maps/index-by :catalog/sku-id)
                                        (maps/map-values :item/quantity))
          item-quantity            (reduce + (vals sku-id->quantity))
          all-skus                 (->> (select-keys skus-db sku-ids-from-shared-cart)
                                        vals
                                        vec
                                        (remove (fn [sku] (and remove-free-install?
                                                               ((:catalog/department sku) "service")))))
          ;; NOTE: assumes only one discountable service item for the look
          discountable-service-sku (->> all-skus
                                        (select ?discountable)
                                        first)
          product-items            (->> all-skus
                                        (select ?physical)
                                        (mapv (fn [{:as sku :keys [catalog/sku-id]}]
                                                (assoc sku :item/quantity (get sku-id->quantity sku-id))))
                                        vec)
          tex-ori-col              (some->> product-items
                                            (mapv #(select-keys % [:hair/color :hair/origin :hair/texture]))
                                            not-empty
                                            (apply merge-with clojure.set/union))

          origin-name  (get-in facets-db [:hair/origin :facet/options (first (:hair/origin tex-ori-col)) :sku/name])
          texture-name (get-in facets-db [:hair/texture :facet/options (first (:hair/texture tex-ori-col)) :option/name])

          discountable-service-title-component (when-let [discountable-service-category
                                                          (some->> discountable-service-sku
                                                                   :service/category
                                                                   first)]
                                                 (case discountable-service-category
                                                   "install"      "+ FREE Install Service"
                                                   "construction" "+ FREE Custom Wig"
                                                   nil))
          total-price                          (some->> all-skus
                                                        (mapv (fn [{:keys [catalog/sku-id sku/price]}]
                                                                (* (get sku-id->quantity sku-id 0) price)))
                                                        (reduce + 0))
          discounted-price                     (let [discountable-service-price (:product/essential-price discountable-service-sku)]
                                                 (cond-> total-price
                                                   discountable-service-price (- discountable-service-price)))
          look-id                              (:content/id look)
          any-sold-out-skus?                   (some false? (map :inventory/in-stock? all-skus))]
      (when-not any-sold-out-skus?
        (merge tex-ori-col ;; TODO(corey) apply merge-with into
               {:look/title      (clojure.string/join " " [origin-name
                                                           texture-name
                                                           "Hair"
                                                           discountable-service-title-component])
                :tags/event      (set (:tags-event look))
                :tags/face-shape (set (:tags-face-shape look))
                :tags/style      (set (:tags-style look))

                ;; TODO: only handles the free service discount,
                ;; other promotions can be back ported here after
                ;; #176485395 is completed
                :look/cart-number      shared-cart-id
                :look/total-price      (some-> total-price mf/as-money)
                :look/discounted?      (not= discounted-price total-price)
                :look/discounted-price (or (some-> discounted-price mf/as-money)
                                           (some-> total-price mf/as-money))
                :look/id               look-id

                ;; Look
                :look/secondary-id "item-quantity-in-look"
                :look/secondary    (str item-quantity " items in this " (:short-name album-copy))

                ;; Looks page
                :look/hero-imgs [{:url (:photo-url look)
                                  :platform-source
                                  (when-let [icon (svg/social-icon (:social-media-platform look))]
                                    (icon {:class "fill-white"
                                           :style {:opacity 0.7}}))}]
                :look/target    [events/shop-by-look|look-selected {:album-keyword album-keyword
                                                               :look-id       look-id
                                                               :card-index    index
                                                               :variant-ids   (map :legacy/variant-id all-skus)}]
                :look/items     product-items})))))

(defn determine-and-shape-layer
  [data body-layer]
  (let [skus-db               (get-in data storefront.keypaths/v2-skus)
        images-db             (get-in data storefront.keypaths/v2-images)
        promotions            (get-in data storefront.keypaths/promotions)
        remove-free-install?  (:remove-free-install (get-in data storefront.keypaths/features))
        facets-db             (->> (get-in data storefront.keypaths/v2-facets)
                                   (maps/index-by (comp keyword :facet/slug))
                                   (maps/map-values (fn [facet]
                                                      (update facet :facet/options
                                                              (partial maps/index-by :option/slug)))))
        looks-shared-carts-db (get-in data storefront.keypaths/v1-looks-shared-carts)]
    (case (:content/type body-layer)
      "homepageHero"   (assoc (homepage-hero/query body-layer)
                              :layer/type :hero)
      "titleSubtitle"  {:layer/type      :shop-text-block
                        :header/value    (:title body-layer)
                        :body/value      (:subtitle body-layer)}
      "ugc-collection" {:layer/type   :lp-tiles
                        :header/value (or (:title body-layer) "Shop By Look")
                        :images       (let [looks ((if (= "production" (get-in data keypaths/environment))
                                                     :looks
                                                     :acceptance-looks) body-layer)]
                                        (if (= "HD Looks" (:name body-layer))
                                          (->> looks
                                               (keep-indexed (fn [index look]
                                                               (merge look
                                                                      (look<- skus-db looks-shared-carts-db facets-db look promotions :look index remove-free-install?))))

                                               (map (fn [look]
                                                      (when (:look/id look)
                                                        {:image-url              (-> look :look/hero-imgs first :url)
                                                         :alt                    ""
                                                         :label                  (:title look)
                                                         :cta/navigation-message (:look/target look)}))))
                                          (map (fn [look]
                                                 (when (:content/id look)
                                                   {:image-url              (:photo-url look)
                                                    :alt                    ""
                                                    :label                  (:title look)
                                                    :cta/navigation-message [events/navigate-shop-by-look-details
                                                                             {:look-id       (:content/id look)
                                                                              :album-keyword :look}]}))
                                               looks)))
                        :cta {:id      "landing-page-see-more"
                              :attrs   {:navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
                              :content (or (:cta-copy body-layer) "see more")}}
      "faq"              (merge {:layer/type :faq
                                 :title      (:title body-layer)}
                                (faq/hd-lace-query data body-layer))
      "layerTextBlock"   {:layer/type   :lp-image-text-block
                          :header/value (:title body-layer)
                          :image/url    (:image-url body-layer) ;; For Upload care images
                          :image/alt    (:alt body-layer)
                          :image/image  (:image body-layer)     ;; For images hosted by Contentful
                          :text/copy    (:body body-layer)
                          :cta/button?  true
                          :cta/value    (:cta-copy body-layer)
                          :cta/id       (str "landing-page-" (:slug body-layer) "-cta")
                          :cta/target   (url->navigation-message (:cta-url body-layer))}
      "layerTilesAndCta" {:layer/type   :lp-tiles
                          :header/value (:title body-layer)
                          :images       (map (fn [tile]
                                               (case (:content/type tile)
                                                 "imageTextLink"
                                                 {:image-url              (:image-url tile) ; Ucare image
                                                  :image                  (:image tile)     ; contentful-hosted image
                                                  :alt                    (:description tile)
                                                  :label                  (:title tile)
                                                  :cta/navigation-message (url->navigation-message (:link-url tile))}

                                                 "imageTextExternalLink"
                                                 {:image-url              (:image-url tile) ; Ucare image
                                                  :image                  (:image tile)     ; contentful-hosted image
                                                  :alt                    (:description tile)
                                                  :label                  (:title tile)
                                                  :cta/navigation-message [events/external-redirect-url {:url (:link-url tile)}]}

                                                 "contentModuleImageTileInternalLinkTitleCopy"
                                                 {:image                  (:image tile)     ; contentful-hosted image
                                                  :alt                    (:description tile)
                                                  :label-shout            true
                                                  :label                  (:title tile)
                                                  :copy                   (:copy tile)
                                                  :cta/navigation-message [events/external-redirect-url {:url (:link-url tile)}]}

                                                 {}))
                                             (:tiles body-layer))
                          :cta {:id      (when (:cta-url body-layer) (str "landing-page-" (:slug body-layer) "-cta"))
                                :attrs   {:navigation-message (url->navigation-message (:cta-url body-layer))}
                                :content (:cta-copy body-layer)}}
      "layerImagesWCopy" {:layer/type   :lp-images-with-copy
                          :header/value (:title body-layer)
                          :images       (map (fn [tile]
                                               (case (:content/type tile)
                                                 "imageTitleCopy"
                                                 {:image (:image tile)     ; contentful-hosted image
                                                  :alt   (:description tile)
                                                  :label (:title tile)
                                                  :copy  (:copy tile)}

                                                 {}))
                                             (:images body-layer))}
      "imageCarousel" {:layer/type           :lp-image-carousel
                       :exhibits             (mapv (fn [image]
                                                     {:src  (-> image :image :file :url)
                                                      :alt  (:alt image)
                                                      :type "image"})
                                                   (:images body-layer))
                       :selected-exhibit-idx (:idx (carousel-2022/<- data (:content/id body-layer)))
                       :id                   (:content/id body-layer)}
      "video"         (let [youtube-id        (:youtube-id body-layer)
                            landing-page-slug (landing-page-slug data)]
                        {:layer/type  :lp-video
                         :open-modal? (:youtube-id (get-in data adventure.keypaths/adventure-home-video))
                         :title       (:title body-layer)
                         :video       {:youtube-id youtube-id
                                       :target     [(get-in data keypaths/navigation-event) {:query-params      {:video youtube-id}
                                                                                             :landing-page-slug landing-page-slug}]}
                         :opts        {:opts {:close-attrs (utils/route-to events/navigate-landing-page {:query-params      {:video "close"}
                                                                                                         :landing-page-slug landing-page-slug})}}})
      "reviews"       {:layer/type :lp-reviews
                       :title      (:title body-layer)
                       :reviews    (:reviews body-layer)}
      "emailCapture"  (merge {:layer/type          :lp-email-capture
                              :email-capture-id    (:email-capture-id body-layer)
                              :template-content-id (:template-content-id body-layer)
                              :incentive           (:incentive body-layer)
                              :fine-print-prefix   (:fine-print-prefix body-layer)}
                             (within :email-capture.cta
                                     {:value (:cta-text body-layer)
                                      :id    "email-capture-submit"})
                             (let [textfield-keypath concept/textfield-keypath]
                               (within :email-capture.text-field
                                       {:id          "email-capture-input"
                                        :placeholder "Sign up" ;TODO: bring in from contentful?
                                        :focused     (get-in data keypaths/ui-focus)
                                        :keypath     textfield-keypath
                                        :errors      (get-in data keypaths/field-errors ["email"])
                                        :email       (get-in data textfield-keypath)})))
      "split"         {:layer/type   :lp-split
                       :left-top     (determine-and-shape-layer data (:lefttop body-layer))
                       :right-bottom (determine-and-shape-layer data (:rightbottom body-layer))}

      "contentModuleTitleTextCtaBackgroundColor" {:layer/type       :lp-title-text-cta-background-color
                                                  :header/value     (:title body-layer)
                                                  :body/value       (:subtitle body-layer)
                                                  :body.html/value  (:md.html/subtitle body-layer)
                                                  :cta/value        (:cta-copy body-layer)
                                                  :cta/id           (str "landing-page-" (:slug body-layer) "-cta")
                                                  ;; TODO: make this resilient to external or internal links
                                                  :cta/target       (:cta-url body-layer) #_ (url->navigation-message (:cta-url body-layer))
                                                  :background/color (:background-color body-layer)
                                                  :content/color    (if (= "black" (:background-color body-layer))
                                                                      "white"
                                                                      "black")}
      "image"                                    {:layer/type         :lp-split-image
                                                  :alt                (:alt body-layer)
                                                  :image              (:image body-layer)
                                                  :navigation-message (url->navigation-message (:url body-layer))}
      "staticContent"                            {:layer/type (case (:module body-layer)
                                                                "contact-us"          :lp-contact-us
                                                                "divider-green-gray"  :lp-divider-green-gray
                                                                "divider-purple-pink" :lp-divider-purple-pink
                                                                "service-list"        :service-list
                                                                "promises-omni"       :promises-omni
                                                                "customize-wig"       :customize-wig)}
      {})))

(defn landing-page-body [data]
  (->> (landing-page-slug data)
       (conj storefront.keypaths/cms-landing-page-v2)
       (get-in data)
       :body))

(defn query [data]
  {:layers
   (mapv (partial determine-and-shape-layer data)
         (landing-page-body data))})

(defn built-component [data opts]
  (component/build layered/component (query data) nil))
