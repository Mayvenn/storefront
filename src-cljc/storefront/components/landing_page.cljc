(ns storefront.components.landing-page
  (:require [storefront.component :as component]
            [adventure.components.layered :as layered]
            [adventure.faq :as faq]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.routes :as routes]))

(defn url->navigation-message [url]
  (let [[path query-params-string] (clojure.string/split url #"\?")
        query-params               (when (not (clojure.string/blank? query-params-string))
                                     (->> (clojure.string/split query-params-string #"\&")
                                          (map #(clojure.string/split % #"\="))
                                          (into {})))]
    (routes/navigation-message-for path query-params)))

(defn determine-and-shape-layer
  [landing-page-data data body-layer]
  (let [hero-data (:hero landing-page-data)
        faq-data  (:faq landing-page-data)
        looks     (:looks landing-page-data)]
    (case (:content/type body-layer)
      "homepageHero"     {:layer/type :hero
                          :dsk-url    (-> hero-data :desktop :file :url)
                          :mob-url    (-> hero-data :mobile :file :url)
                          :alt        (-> hero-data :alt)
                          :file-name  (-> hero-data :desktop :file :file-name)}
      "titleSubtitle"    {:layer/type   :shop-text-block
                          :header/value (:title body-layer)
                          :body/value   (:subtitle body-layer) }
      "ugc-collection"   {:layer/type   :lp-tiles
                                :header/value "Shop By Look"
                                :images       (map (fn [look]
                                                     {:image-url              (:photo-url look)
                                                      :alt                    "Look Photo"
                                                      :label                  (:title look)
                                                #_#_      :cta/navigation-message [events/navigate-shop-by-look-details
                                                                               {:look-id       (:content/id look)
                                                                                :album-keyword :look}]})
                                                   (:looks body-layer))
                                :cta          {:id      "landing-page-see-more"
                                               :attrs   {:navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
                                               :content "see more"}}
      "faq"              {} #_ (merge {:layer-type :faq}
                                  (faq/hd-lace-query data faq-data))
      "layerTextBlock"   {:layer/type   :shop-text-block
                          :header/value (:title body-layer)
                          :body/value   [(ui/img {:src   (:image-url body-layer)
                                                  :style {:width "100%"}})
                                         [:div.content-2 (:body body-layer)]]
                          :cta/button?  true
                          :cta/value    (:cta-copy body-layer)
                          :cta/id       (str "landing-page-" (:slug body-layer) "-cta")
                          :cta/target   (url->navigation-message (:cta-url body-layer))}
      "layerTilesAndCta" {:layer/type   :lp-tiles
                          :header/value (:title body-layer)
                          :images       (map (fn [example]
                                               {:image-url              (:image-url example)
                                                :alt                    (:title example)
                                                :label                  (:title example)
                                                :cta/navigation-message (url->navigation-message (:link-url example))})
                                             (:examples (:tiles body-layer)))
                          :cta          {:id      (str "landing-page-" (:slug body-layer) "-cta")
                                         :attrs   {:navigation-message (url->navigation-message (:cta-url body-layer))}
                                         :content (:cta-copy body-layer)}}
      :else              {}

      )))

(defn query [data]
  (let [landing-page-slug (-> data (get-in storefront.keypaths/navigation-args) :landing-page-slug keyword)
        landing-page-data (get-in data (conj storefront.keypaths/cms-landing-page landing-page-slug))
        landing-page-body (:body landing-page-data)]
    {:layers
     (spice.core/spy (mapv (partial determine-and-shape-layer landing-page-data data) landing-page-body))
     #_[(let [cms-hero-data (:hero cms-data)]
        {:layer/type :hero
         :dsk-url    (-> cms-hero-data :desktop :file :url)
         :mob-url    (-> cms-hero-data :mobile :file :url)
         :alt        (-> cms-hero-data :alt)
         :file-name  (-> cms-hero-data :desktop :file :file-name)})
      {:layer/type   :shop-text-block
       :header/value (:title cms-data)
       :body/value   (:subtitle cms-data)}
      {:layer/type   :lp-tiles
       :header/value "Shop By Look"
       :images       (map (fn [look]
                            {:image-url              (:photo-url look)
                             :alt                    "Look Photo"
                             :label                  (:title look)
                             :cta/navigation-message [events/navigate-shop-by-look-details
                                                      {:look-id       (:content/id look)
                                                       :album-keyword :look}]})
                          (:looks cms-data))
       :cta          {:id      "landing-page-see-more"
                      :attrs   {:navigation-message [events/navigate-shop-by-look {:album-keyword :look}]}
                      :content "see more"}}
      (let [explanation (:explanation cms-data)]
        {:layer/type   :shop-text-block
         :header/value (:title explanation)
         :body/value   [(ui/img {:src   (:image-url explanation)
                                 :style {:width "100%"}})
                        [:div.content-2 (:body explanation)]]
         :cta/button?  true
         :cta/value    (:cta-copy explanation)
         :cta/id       (str "landing-page-" (:slug explanation) "-cta")
         :cta/target   (url->navigation-message (:cta-url explanation))})
      (let [examples-layer (:examples-layer cms-data)]
        {:layer/type   :lp-tiles
         :header/value (:title examples-layer)
         :images       (map (fn [example]
                              {:image-url              (:image-url example)
                               :alt                    (:title example)
                               :label                  (:title example)
                               :cta/navigation-message (url->navigation-message (:link-url example))})
                            (:examples cms-data))
         :cta          {:id      (str "landing-page-" (:slug examples-layer) "-cta")
                        :attrs   {:navigation-message (url->navigation-message (:cta-url examples-layer))}
                        :content (:cta-copy examples-layer)}})
      (merge {:layer/type :faq}
             (faq/hd-lace-query data (:faq cms-data)))]}))

(defn built-component [data opts]
  (component/build layered/component (query data) nil))
