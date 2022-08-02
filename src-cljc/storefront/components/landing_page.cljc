(ns storefront.components.landing-page
  (:require [storefront.component :as component]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [adventure.keypaths :as adventure.keypaths]
            [mayvenn.concept.email-capture :as concept]
            [mayvenn.visual.tools :refer [within with]]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
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

(defn determine-and-shape-layer
  [data body-layer]
  (case (:content/type body-layer)
    "homepageHero"     (assoc (homepage-hero/query body-layer)
                              :layer/type :hero)
    "titleSubtitle"    {:layer/type   :shop-text-block
                        :header/value (:title body-layer)
                        :body/value   (:subtitle body-layer) }
    "ugc-collection"   {:layer/type   :lp-tiles
                        :header/value "Shop By Look"
                        :images       (map (fn [look]
                                             (when (:content/id look)
                                               {:image-url              (:photo-url look)
                                                :alt                    ""
                                                :label                  (:title look)
                                                :cta/navigation-message [events/navigate-shop-by-look-details
                                                                         {:look-id       (:content/id look)
                                                                          :album-keyword :look}]}))
                                           ((if (= "production" (get-in data keypaths/environment))
                                              :looks
                                              :acceptance-looks) body-layer))
                        :cta          {:id      "landing-page-see-more"
                                       :attrs   {:navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
                                       :content "see more"}}
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
    "imageCarousel" {:layer/type :lp-image-carousel
                     :images     (mapv (fn [image]
                                         (if (:image image) ; Prioritize contentful-hosted image over ucare
                                           {:url (-> image :image :file :url)
                                            :alt (:alt image)}
                                           {:url (:url image)
                                            :alt (:alt image)}))
                                       (:images body-layer))}
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
                                                              "divider-purple-pink" :lp-divider-purple-pink)}
    {}))

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
