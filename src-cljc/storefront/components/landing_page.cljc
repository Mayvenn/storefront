(ns storefront.components.landing-page
  (:require [storefront.component :as component]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.routes :as routes]
            [storefront.components.homepage-hero :as homepage-hero]))

(defn url->navigation-message [url]
  (let [[path query-params-string] (clojure.string/split url #"\?")
        query-params               (when (not (clojure.string/blank? query-params-string))
                                     (->> (clojure.string/split query-params-string #"\&")
                                          (map #(clojure.string/split % #"\="))
                                          (into {})))]
    (routes/navigation-message-for path query-params)))

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
                                             (if (:content/id look)
                                               {:image-url              (:photo-url look)
                                                :alt                    ""
                                                :label                  (:title look)
                                                :cta/navigation-message [events/navigate-shop-by-look-details
                                                                         {:look-id       (:content/id look)
                                                                          :album-keyword :look}]}
                                               (prn "MISSING CONTENT ID " look " BODY LAYER: " body-layer)))
                                           ((if (= "production" (get-in data keypaths/environment))
                                              :looks
                                              :acceptance-looks) body-layer))
                        :cta          {:id      "landing-page-see-more"
                                       :attrs   {:navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
                                       :content "see more"}}
    "faq"              (merge {:layer/type :faq}
                              (faq/hd-lace-query data body-layer))
    "layerTextBlock"   {:layer/type   :lp-image-text-block
                        :header/value (:title body-layer)
                        :image/url    (:image-url body-layer)
                        :image/alt    (:alt body-layer)
                        :text/copy    (:body body-layer)
                        :cta/button?  true
                        :cta/value    (:cta-copy body-layer)
                        :cta/id       (str "landing-page-" (:slug body-layer) "-cta")
                        :cta/target   (url->navigation-message (:cta-url body-layer))}
    "layerTilesAndCta" {:layer/type   :lp-tiles
                        :header/value (:title body-layer)
                        :images       (map (fn [tile]
                                             {:image-url              (:image-url tile)
                                              :alt                    (:title tile)
                                              :label                  (:title tile)
                                              :cta/navigation-message (url->navigation-message (:link-url tile))})
                                           (:tiles body-layer))
                        :cta          {:id      (str "landing-page-" (:slug body-layer) "-cta")
                                       :attrs   {:navigation-message (url->navigation-message (:cta-url body-layer))}
                                       :content (:cta-copy body-layer)}}
    {}))

(defn query [data]
  (let [landing-page-slug (-> data (get-in storefront.keypaths/navigation-args) :landing-page-slug keyword)
        landing-page-data (get-in data (conj storefront.keypaths/cms-landing-page-v2 landing-page-slug))
        landing-page-body (:body landing-page-data)]
    {:layers
     (mapv (partial determine-and-shape-layer data) landing-page-body)}))

(defn built-component [data opts]
  (component/build layered/component (query data) nil))
