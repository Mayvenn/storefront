(ns storefront.components.landing-page
  (:require api.orders
            [storefront.component :as component]
            [storefront.components.money-formatters :as mf]
            [api.catalog :refer [select ?physical]]
            [storefront.components.svg :as svg]
            [storefront.accessors.contentful :as contentful]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [adventure.keypaths :as adventure.keypaths]
            [mayvenn.concept.email-capture :as concept]
            [mayvenn.concept.account :as accounts]
            [mayvenn.visual.tools :refer [within with]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.carousel :as carousel-2022]
            [storefront.routes :as routes]
            [storefront.ugc :as ugc]
            [spice.maps :as maps]
            [storefront.components.homepage-hero :as homepage-hero]
            lambdaisland.uri
            [catalog.cms-dynamic-content :as cms-dynamic-content]))

(defn ^:private url->navigation-message [url]
  (when-not (nil? url)
    (let [parsed-path (lambdaisland.uri/uri url)
          nav-message (routes/navigation-message-for (:path parsed-path)
                                                     (lambdaisland.uri/query-map parsed-path)
                                                     nil
                                                     (:fragment parsed-path))]
      (when (not= events/navigate-not-found (first nav-message))
        nav-message))))

(defn ^:private button-navigation-message [{:keys [url]}]
  (cond
    url
    (let [parsed-path (lambdaisland.uri/uri url)
          nav-message (routes/navigation-message-for (:path parsed-path)
                                                     (lambdaisland.uri/query-map parsed-path)
                                                     nil
                                                     (:fragment parsed-path))]
      (when (not= events/navigate-not-found (first nav-message))
        nav-message))

    :else [events/navigate-not-found]))

(defn ^:private button-target
  [{:keys [event-target]}]
  (when (= "OpenModal" event-target)
    [events/email-modal-opened]))

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

          total-price                          (some->> all-skus
                                                        (mapv (fn [{:keys [catalog/sku-id sku/price]}]
                                                                (* (get sku-id->quantity sku-id 0) price)))
                                                        (reduce + 0))
          look-id                              (:content/id look)
          any-sold-out-skus?                   (some false? (map :inventory/in-stock? all-skus))]
      (when-not any-sold-out-skus?
        (merge tex-ori-col ;; TODO(corey) apply merge-with into
               {:look/title      (clojure.string/join " " [origin-name
                                                           texture-name
                                                           "Hair"])
                :tags/event      (set (:tags-event look))
                :tags/face-shape (set (:tags-face-shape look))
                :tags/style      (set (:tags-style look))

                ;; TODO: only handles the free service discount,
                ;; other promotions can be back ported here after
                ;; #176485395 is completed
                :look/cart-number      shared-cart-id
                :look/total-price      (some-> total-price mf/as-money)
                :look/discounted?      false
                :look/discounted-price (some-> total-price mf/as-money)
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

(defn determine-mobile-and-desktop-class
  [{:keys [mobile-layout
           desktop-layout]
    :as cms-data}]
  (let [base-class "sections-"]
    (-> cms-data
        (assoc :mobile-class (str base-class mobile-layout)
               :desktop-class (str base-class desktop-layout "-on-tb-dt")))))

(defn determine-alignment
  [{:keys [alignment-to-container] :as cms-data}]
  (assoc cms-data :alignment (if (= "left" alignment-to-container)
                               "left-align"
                               "justify-center")))

(def navigate-show-page
  {"katy"          events/navigate-retail-walmart-katy
   "houston"       events/navigate-retail-walmart-houston
   "grand-prairie" events/navigate-retail-walmart-grand-prairie
   "dallas"        events/navigate-retail-walmart-dallas
   "mansfield"     events/navigate-retail-walmart-mansfield})

(defn retail-location-query
  [{:keys [email facebook hero state hours name phone-number instagram tiktok
           location address-1 address-2 address-zipcode address-city slug] :as data}]
  (when (and name slug)
    {:name             (str name ", " state)
     :slug             slug
     :img-url          (-> hero :file :url)
     :address1-2       (when address-1 (str address-1 (when address-2 (str ", " address-2))))
     :city-state-zip   (when address-city (str address-city ", " state " " address-zipcode))
     :phone            phone-number
     :mon-sat-hours    (first hours)
     :sun-hours        (last hours)
     :show-page-target (get navigate-show-page slug)
     :directions       #?(:cljs (when (:lat location)
                                  (str "https://www.google.com/maps/search/?api=1&query="
                                       (goog.string/urlEncode (str "Mayvenn Beauty Lounge "
                                                                   address-1
                                                                   address-2)
                                                              ","
                                                              (:lat location)
                                                              ","
                                                              (:lon location))))
                          :clj "")
     :instagram        (when instagram (str "https://www.instagram.com/" instagram))
     :facebook         (when facebook (str "https://www.facebook.com/" facebook))
     :tiktok           (when tiktok (str "https://www.tiktok.com/@" tiktok))
     :email            email}))

(defn determine-and-shape-layer
  [data body-layer]
  (let [skus-db               (get-in data storefront.keypaths/v2-skus)
        images-db             (get-in data storefront.keypaths/v2-images)
        promotions            (get-in data storefront.keypaths/promotions)
        order                 (api.orders/current data)
        remove-free-install?  (:remove-free-install (get-in data storefront.keypaths/features))
        in-omni?              (:experience/omni (:experiences (accounts/<- data)))
        phone-consult-cta     (get-in data storefront.keypaths/cms-phone-consult-cta)
        facets-db             (->> (get-in data storefront.keypaths/v2-facets)
                                   (maps/index-by (comp keyword :facet/slug))
                                   (maps/map-values (fn [facet]
                                                      (update facet :facet/options
                                                              (partial maps/index-by :option/slug)))))
        looks-shared-carts-db (get-in data storefront.keypaths/v1-looks-shared-carts)]
    (case (:content/type body-layer)
      "homepageHero"   (assoc (homepage-hero/query body-layer)
                              :layer/type :hero)
      "titleSubtitle"  {:layer/type   :shop-text-block
                        :header/value (:title body-layer)
                        :body/value   (:subtitle body-layer) }
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
                          :subtitle     (:subtitle body-layer)
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
      "split"         {:layer/type       :lp-split
                       ;; TODO: rebinding to make explicit that the IDs for these fields in contentful is
                       ;; vestigial and that the elements are should be referred to by their mobile positions
                       :desktop-ordering (:desktop-ordering body-layer)
                       :top              (determine-and-shape-layer data (:lefttop body-layer))
                       :bottom           (determine-and-shape-layer data (:rightbottom body-layer))}

      "contentModuleTitleTextCtaBackgroundColor" {:layer/type       :lp-title-text-cta-background-color
                                                  :header/value     (:title body-layer)
                                                  :body/value       (:subtitle body-layer)
                                                  :cta/value        (:cta-copy body-layer)
                                                  :cta/id           (str "landing-page-" (:slug body-layer) "-cta")
                                                  ;; TODO: make this resilient to external or internal links
                                                  :cta/target       (:cta-url body-layer) #_ (url->navigation-message (:cta-url body-layer))
                                                  :background/color (:background-color body-layer)
                                                  :content/color    (if (= "black" (:background-color body-layer))
                                                                      "white"
                                                                      "black")}
      "image"                                    {:layer/type    :image
                                                  :alt           (:alt body-layer)
                                                  :image         (:image body-layer)
                                                  :desktop-image (:desktop-image body-layer)}
      "icon"                                     {:layer/type :icon
                                                  :size       (case (:size body-layer)
                                                                "Small (1 rem)"   "1rem"
                                                                "Medium (2 rem)"  "2rem"
                                                                "Large (4 rem)"   "4rem"
                                                                "X-Large (8 rem)" "8rem")
                                                  :icon       (:icon body-layer)}
      "text"                                     (-> body-layer
                                                     (select-keys [:font :size :alignment :content :long-content])
                                                     (assoc :layer/type :text))
      "richText"                                 (-> body-layer
                                                     (update :content #(->> %
                                                                            :content
                                                                            (map cms-dynamic-content/build-hiccup-tag)
                                                                            (into [:div])))
                                                     (assoc :layer/type :rich-text))
      "retailLocation"                           (-> body-layer
                                                     retail-location-query
                                                     (assoc :layer/type :retail-location))
      "staticContent"                            (merge {:layer/type (case (:module body-layer)
                                                                       "contact-us"                  :lp-contact-us
                                                                       "divider-green-gray"          :lp-divider-green-gray
                                                                       "divider-purple-pink"         :lp-divider-purple-pink
                                                                       "service-list"                :service-list
                                                                       "promises-omni"               :promises-omni
                                                                       "customize-wig"               :customize-wig
                                                                       "why-mayvenn"                 :why-mayvenn
                                                                       "animated-value-props"        :animated-value-props
                                                                       "phone-consult-cta"           :phone-consult-cta
                                                                       "phone-consult-message"       :phone-consult-message
                                                                       "phone-consult-calendly"      :phone-consult-calendly
                                                                       "call-to-reserve-monfort-cta" :call-to-reserve-monfort-cta
                                                                       nil)
                                                         :in-omni?   in-omni?}
                                                        (when (get-in data keypaths/loaded-calendly)
                                                          {:show-calendly true})
                                                        (when (= "phone-consult-message" (:module body-layer))
                                                          phone-consult-cta)
                                                        (when (= "phone-consult-cta" (:module body-layer))
                                                          (merge {:place-id :section}
                                                                 phone-consult-cta
                                                                 order)))
      "section"                                  (-> body-layer
                                                     (select-keys [:contents :mobile-layout :desktop-layout :title
                                                                   :desktop-reverse-order :background-color :url
                                                                   :padding :gap])
                                                     (assoc :show-section? (or
                                                                            ;; Happy Path: Not designed to be used with the Phone Consult CTA (phone-consult-cta)
                                                                            (not (:phone-cta-toggled body-layer))
                                                                            ;; Designed to be used with Phone Consult CTA
                                                                            ;; Only show this section if CTA is released and on "shopping-section"
                                                                            (and (:phone-cta-toggled body-layer)
                                                                                 (:shopping-section phone-consult-cta)
                                                                                 (:released phone-consult-cta))))
                                                     (update :contents (partial map #(determine-and-shape-layer data %)))
                                                     (assoc :navigation-message (url->navigation-message (:url body-layer)))
                                                     determine-mobile-and-desktop-class
                                                     (assoc :layer/type :section))
      "tiles"                                    (-> body-layer
                                                     (select-keys [:contents :mobile-columns :desktop-columns
                                                                   :desktop-reverse-order :background-color :url
                                                                   :padding :gap])
                                                     (update :contents (partial map #(determine-and-shape-layer data %)))
                                                     (assoc :navigation-message (url->navigation-message (:url body-layer)))
                                                     (assoc :layer/type :tiles))
      "button"                                   (-> body-layer
                                                     (select-keys [:copy :alignment-to-container :color :size :url])
                                                     determine-alignment
                                                     (assoc :navigation-message (button-navigation-message body-layer))
                                                     (assoc :target (button-target body-layer))
                                                     (assoc :layer/type :button))
      "title"                                    (-> body-layer
                                                     (select-keys [:primary :secondary :tertiary :template])
                                                     (assoc :layer/type :title))
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
