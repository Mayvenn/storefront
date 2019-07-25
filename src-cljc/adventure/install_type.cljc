(ns adventure.install-type
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [storefront.component :as component]
            [storefront.keypaths :as keypaths]
            [adventure.utils.facets :as facets]
            [adventure.components.multi-prompt :as multi-prompt]
            [adventure.progress :as progress]))

(def subtexts
  {"bundles"      "Classic look with some hair left out"
   "closures"     "Low maintenance protective style"
   "frontals"     "High maintenance & price, requires expertise"
   "360-frontals" "Versatile, but highest maintenance & price"})

(defn ^:private query [data]
  (let [family-facet-options (facets/adventure-facet-options :hair/family (get-in data keypaths/v2-facets))
        nav-event            events/navigate-adventure-match-stylist]
    {:prompt       "Which type of install are you looking for?"
     :prompt-image "//ucarecdn.com/a159aafc-b096-46b9-88ae-901e96699795/-/format/auto/bg.png"
     :data-test    "install-type"
     :current-step 1
     :header-data  {:title                   "The New You"
                    :progress                progress/install-type
                    :back-navigation-message [events/navigate-adventure-home]
                    :subtitle                "Step 1 of 3"}
     :buttons      (map (fn [{:keys [option/slug] :as o}]
                          {:text             [:div.mynp6
                                              [:div (:adventure/name o)]
                                              [:div.light.h6 (get subtexts slug)]]
                           :data-test-suffix slug
                           :value            {:install-type slug}
                           :target-message   [nav-event]})
                        family-facet-options)}))

(defn ^:export built-component
  [data opts]
  (component/build multi-prompt/component (query data) opts))
