(ns adventure.bundlesets.hair-texture
  (:require #?@(:cljs [[storefront.platform.messages :as messages]])
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.request-keys :as request-keys]
            [storefront.platform.component-utils :as utils]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]))

(def option-metadata
  {"straight"       {:subtitle   "For silky, sleek looks"
                     :album-slug :bundle-sets-straight}
   "yaki-straight"  {:subtitle   "A fresh pressed, relaxed style"
                     :album-slug :bundle-sets-yaki-straight}
   "kinky-straight" {:subtitle   "Mimics blown-out natural hair"
                     :album-slug :bundle-sets-kinky-straight}
   "body-wave"      {:subtitle   "Soft, bouncy S-wave"
                     :album-slug :bundle-sets-body-wave}
   "loose-wave"     {:subtitle   "Big, glamorous waves"
                     :album-slug :bundle-sets-loose-wave}
   "water-wave"     {:subtitle   "Carefree, free-flowing curls"
                     :album-slug :bundle-sets-water-wave}
   "deep-wave"      {:subtitle   "C-shaped ringlet curls"
                     :album-slug :bundle-sets-deep-wave}
   "curly"          {:subtitle   "Mimics natural 3C-4A textures"
                     :album-slug :bundle-sets-curly}})

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
        adventure-choices     (get-in data adventure-keypaths/adventure-choices)
        stylist-selected?     (some-> adventure-choices :flow #{"match-stylist"})
        current-step          (if stylist-selected? 3 2)]
    {:prompt       "Which texture are you looking for?"
     :prompt-image "//ucarecdn.com/3346657d-a039-487f-98fb-68b9b050e042/-/format/auto/"
     :data-test    "hair-texture"
     :current-step current-step
     :footer       (when-not stylist-selected?
                     [:div.h6.center.pb8.pt1
                      [:div.dark-gray "Not ready to shop hair?"]
                      [:a.teal (utils/route-to events/navigate-adventure-find-your-stylist)
                       "Find a stylist"]])
     :spinning?    (utils/requesting-from-endpoint? data request-keys/search-v2-skus)
     :header-data  {:title                   "The New You"
                    :progress                progress/hair-texture
                    :back-navigation-message [events/navigate-adventure-how-shop-hair]
                    :subtitle                (str "Step " current-step " of 3")}
     :buttons      (enriched-buttons texture-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-bundlesets-hair-texture
  [_ _ args _ app-state]
  #?(:cljs (messages/handle-message events/adventure-fetch-matched-skus)))
