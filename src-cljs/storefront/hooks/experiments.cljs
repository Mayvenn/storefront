(ns storefront.hooks.experiments
  (:require [storefront.browser.tags :as tags]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.messages :as m]
            [storefront.config :as config]))

(def stylist-shop? (comp #{"shop"} :store_slug))
(def stylist-store? (comp #{"store"} :store_slug))
(defn stylist-mod? [m v]
  (fn [stylist]
    (= (mod (:stylist_id stylist) m) v)))

(def control-1 0)
(def control-2 1)
(def color-variation 2)
(def shop-variation 3)
(def store-variation 4)

(def experiment->buckets
  (if (or config/development? config/acceptance?)
    {6368621011 [[stylist-shop?      shop-variation]
                 [stylist-store?     store-variation]
                 [(stylist-mod? 3 0) control-1]
                 [(stylist-mod? 3 1) control-2]
                 [(stylist-mod? 3 2) color-variation]]}
    {}))

(defn- bucket [store experiment-id]
  (some->> (get experiment->buckets experiment-id)
           (some (fn [[pred bucket-variation-idx]]
                   (when (pred store) bucket-variation-idx)))
           (conj ["bucketVisitor" experiment-id])))

(defn insert-optimizely [store]
  (when (seq experiment->buckets)
    (set! (.-optimizely js/window) (clj->js (keep (partial bucket store) (keys experiment->buckets)))))
  (tags/insert-tag-with-callback (tags/src-tag (str "//cdn.optimizely.com/js/" config/optimizely-app-id ".js") "optimizely")
                                 #(m/handle-message events/inserted-optimizely))
  (js/setTimeout #(m/handle-message events/inserted-optimizely) 15000))

(defn remove-optimizely []
  (tags/remove-tags-by-class "optimizely"))

(defn set-dimension [dimension-name value]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["setDimensionValue" dimension-name value]))))

(defn track-event [event-name & [opts]]
  (when (.hasOwnProperty js/window "optimizely")
    (.push js/optimizely (clj->js ["trackEvent" event-name opts]))))

(defn activate-universal-analytics []
  (when (and (.hasOwnProperty js/window "optimizely") js/optimizely.activateUniversalAnalytics)
    (.activateUniversalAnalytics js/optimizely)))
