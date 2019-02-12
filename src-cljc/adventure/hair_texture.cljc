(ns adventure.hair-texture
  (:require [clojure.set :as set]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]
            [storefront.accessors.experiments :as experiments]))

(def option-metadata
  {"straight"   {:subtitle "For silky, sleek looks"}
   "body-wave"  {:subtitle "Soft, bouncy S-wave"}
   "loose-wave" {:subtitle "Big, glamorous waves"}
   "deep-wave"  {:subtitle "C-shaped ringlet curls"}})

(defn enriched-buttons [facet-options]
  (for [option facet-options]
    {:text             [:div.mynp6
                        [:div (:adventure/name option)]
                        [:div.light.h6 (-> option-metadata (get (:option/slug option)) :subtitle)]]
     :data-test-suffix (:option/slug option)
     :value            {:texture (:option/slug option)}
     :target-message   [events/navigate-adventure-select-new-look
                        {:album-keyword :shop-by-look}]}))

(defn ^:private query [data]
  (let [texture-facet-options       (facets/available-adventure-facet-options :hair/texture
                                                                              (get-in data keypaths/v2-facets)
                                                                              (get-in data adventure-keypaths/adventure-matching-products))
        adventure-choices           (get-in data adventure-keypaths/adventure-choices)
        current-step                (if (-> adventure-choices :flow #{"match-stylist"}) 3 2)
        ;; TODO: move all progress value to a namespace
        progress                    12]
    {:prompt       "Which texture are you looking for?"
     :prompt-image "//ucarecdn.com/3346657d-a039-487f-98fb-68b9b050e042/-/format/auto/aladdinMatchingOverlayImagePurpleER203Lm3x.png"
     :data-test    "hair-texture"
     :current-step current-step
     :footer       ""
     :header-data  {:title                   "The New You"
                    :progress                progress
                    :back-navigation-message [events/navigate-adventure-how-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (enriched-buttons texture-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
