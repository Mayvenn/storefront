(ns adventure.a-la-carte.hair-texture
  (:require #?@(:cljs [[storefront.platform.messages :refer [handle-message]]])
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.effects :as effects]
            [adventure.progress :as progress]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.utils.facets :as facets]
            [catalog.selector :as selector]))

(def option-metadata
  {"straight"       {:subtitle "For silky, sleek looks"}
   "yaki-straight"  {:subtitle "A fresh pressed, relaxed style"}
   "kinky-straight" {:subtitle "Mimics blown-out natural hair"}
   "body-wave"      {:subtitle "Soft, bouncy S-wave"}
   "loose-wave"     {:subtitle "Big, glamorous waves"}
   "water-wave"     {:subtitle "Carefree, free-flowing curls"}
   "deep-wave"      {:subtitle "C-shaped ringlet curls"}
   "curly"          {:subtitle "Mimics natural 3C-4A textures"}})

(defn enriched-buttons
  [facet-options]
  (for [option facet-options
        :let   [{:keys [subtitle]} (get option-metadata (:option/slug option))]]
    {:text             [:div.mynp6
                        [:div (:adventure/name option)]
                        [:div.light.h6 subtitle]]
     :data-test-suffix (:option/slug option)
     :value            {:texture (:option/slug option)}
     :target-message   [events/navigate-adventure-a-la-carte-hair-color]}))

(defn ^:private query
  [data]
  (let [;; TODO(cwr,jjh) these ought to be underneath the catalog api
        facets (get-in data keypaths/v2-facets)
        skus   (vals (get-in data keypaths/v2-skus))

        ;; TODO(cwr,jjh) these ought to be stored in app-state ready-to-go
        {:keys [install-type]} (get-in data adventure-keypaths/adventure-choices)
        selections             {:hair/family         #{install-type}
                                :inventory/in-stock? #{true}}]
    (let [selected-skus   (selector/query skus selections)
          texture-choices (facets/available-options facets :hair/texture selected-skus)

          adventure-choices (get-in data adventure-keypaths/adventure-choices)
          stylist-selected? (some-> adventure-choices :flow #{"match-stylist"})
          current-step      (if stylist-selected? 3 2)]
      {:prompt       "Which texture are you looking for?"
       :prompt-image "//ucarecdn.com/3346657d-a039-487f-98fb-68b9b050e042/-/format/auto/"
       :data-test    "hair-texture"
       :current-step current-step
       :footer       (when-not stylist-selected?
                       [:div.h6.center.pb8
                        [:div.dark-gray "Not ready to shop hair?"]
                        [:a.teal (utils/fake-href events/navigate-adventure-find-your-stylist)
                         "Find a stylist"]])
       :header-data  {:title                   "The New You"
                      :progress                progress/hair-texture
                      :back-navigation-message [events/navigate-adventure-how-shop-hair]
                      :subtitle                (str "Step " current-step " of 3")}
       :buttons      (enriched-buttons texture-choices)})))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))

(defmethod effects/perform-effects events/navigate-adventure-a-la-carte-hair-texture
  [_ _ args _ app-state]
  #?(:cljs (handle-message events/adventure-fetch-matched-products)))
