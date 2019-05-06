(ns adventure.hair-texture
  (:require #?@(:cljs [[storefront.platform.messages :as messages]])
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]))

(def option-metadata
  {"straight"   {:subtitle   "For silky, sleek looks"
                 :album-slug :shop-by-look-straight}
   "body-wave"  {:subtitle   "Soft, bouncy S-wave"
                 :album-slug :shop-by-look-body-wave}
   "loose-wave" {:subtitle   "Big, glamorous waves"
                 :album-slug :shop-by-look-loose-wave}
   "deep-wave"  {:subtitle   "C-shaped ringlet curls"
                 :album-slug :shop-by-look-deep-wave}})

(defn enriched-buttons [facet-options]
  (for [option facet-options
        :let [{:keys [subtitle album-slug]} (get option-metadata (:option/slug option))]
        :when album-slug]
    {:text             [:div.mynp6
                        [:div (:adventure/name option)]
                        [:div.light.h6 subtitle]]
     :data-test-suffix (:option/slug option)
     :value            {:texture (:option/slug option)}
     :target-message   [events/navigate-adventure-select-new-look
                        {:album-keyword album-slug}]}))

(defn ^:private query [data]
  (let [texture-facet-options (facets/available-adventure-facet-options :hair/texture
                                                                        (get-in data keypaths/v2-facets)
                                                                        (vals (get-in data keypaths/v2-skus)))
        servicing-stylist?    (get-in data adventure.keypaths/adventure-servicing-stylist)
        current-step          (if servicing-stylist? 3 2)]
    {:prompt       "Which texture are you looking for?"
     :prompt-image "//ucarecdn.com/454b7522-de21-4f7c-a6cd-0288574ae672/-/format/auto/"
     :data-test    "hair-texture"
     :current-step current-step
     :spinning?    (utils/requesting-from-endpoint? data request-keys/search-v2-skus)
     :footer       [:div.dark-gray.col-9.mx-auto.h5.pb2
                    [:div.my1.line-height-2 "Looking for Yaki Straight, Kinky Straight, Water Wave, or Curly?"]
                    [:a.block.teal.medium
                     (utils/route-to events/navigate-adventure-how-shop-hair)
                     "Shop pre-made bundle sets"]]
     :header-data  {:title                   "The New You"
                    :progress                progress/hair-texture
                    :back-navigation-message [events/navigate-adventure-how-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (enriched-buttons texture-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-hair-texture
  [_ _ args _ app-state]
  #?(:cljs (messages/handle-message events/adventure-fetch-matched-skus)))
