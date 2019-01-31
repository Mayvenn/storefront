(ns adventure.install-type
  (:require [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.keypaths :as adventure-keypaths]
            [adventure.components.multi-prompt :as multi-prompt]))



(defn ^:private query [data]
  (let [facets               (get-in data keypaths/v2-facets)
        family-facet-options (->> facets
                                  (filter (comp #{:hair/family} :facet/slug))
                                  first
                                  :facet/options
                                  (filter :adventure/name)
                                  (sort-by :filter/order))]
    {:prompt       "Which type of install are you looking for?"
     :prompt-image "//ucarecdn.com/a159aafc-b096-46b9-88ae-901e96699795/-/format/auto/bg.png"
     :data-test    "install-type"
     :header-data  {:title        "The New You"
                    :current-step 1
                    :back-link    events/navigate-adventure-budget
                    :subtitle     "Step 1 of 3"}
     :buttons      (map (fn [o]
                          {:text             (:adventure/name o)
                           :data-test-suffix (:option/slug o)
                           :value            {:install-type (:option/slug o)}
                           :target           events/navigate-adventure-what-next})
                        family-facet-options)}))

(defn built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
