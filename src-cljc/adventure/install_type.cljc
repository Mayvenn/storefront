(ns adventure.install-type
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.utils.facets :as facets]
            [adventure.components.multi-prompt :as multi-prompt]))

(def subtexts
  {"bundles"      "Classic look with some hair left out"
   "closures"     "Low maintenance protective style"
   "frontals"     "High maintenance & price, requires expertise"
   "360-frontals" "Versatile, but highest maintenance & price"})

(defn ^:private query [data]
  (let [family-facet-options (facets/adventure-facet-options (get-in data keypaths/v2-facets) :hair/family)]
    {:prompt       "Which type of install are you looking for?"
     :prompt-image "//ucarecdn.com/a159aafc-b096-46b9-88ae-901e96699795/-/format/auto/bg.png"
     :data-test    "install-type"
     :current-step 1
     :header-data  {:title                   "The New You"
                    :progress                3
                    :back-navigation-message [events/navigate-adventure-budget]
                    :subtitle                "Step 1 of 3"}
     :buttons      (map (fn [{:keys [option/slug] :as o}]
                          {:text             [:div.mynp6
                                              [:div (:adventure/name o)]
                                              [:div.light.h6 (get subtexts slug)]]
                           :data-test-suffix slug
                           :value            {:install-type slug}
                           :target-message   [events/navigate-adventure-what-next]})
                        family-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
