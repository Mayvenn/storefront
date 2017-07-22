(ns storefront.accessors.categories
  (:require [storefront.keypaths :as keypaths]))

(def new-facet?
  "NB: changes here should be reflected in accessors.named-searches until experiments/new-taxon-launch? is 100%"
  ;; [<facet-slug> <option-slug>]
  #{[:product/family "360-frontals"]
    [:hair/texture "yaki-straight"]
    [:hair/texture "water-wave"]})

(def named-search->category-id
  {"closures"       "0"
   "frontals"       "1"
   "straight"       "2"
   "yaki-straight"  "3"
   "kinky-straight" "4"
   "body-wave"      "5"
   "loose-wave"     "6"
   "water-wave"     "7"
   "deep-wave"      "8"
   "curly"          "9"
   "360-frontals"   "10"})

(def initial-categories
  (let [fake-copy "In augue sem, tincidunt egestas rutrum a, vehicula in dolor. Donec id erat eu turpis semper maximus tempor ac erat. Aenean sit amet neque vitae neque finibus tristique id vitae odio. Maecenas aliquet ipsum est, in eleifend erat consequat ac. Integer dapibus nisl ac est interdum faucibus."
        fake-hero {:mobile-url  "//ucarecdn.com/2aaff96b-3b51-4eb2-9c90-2e7806e13b15/"
                   :desktop-url "//ucarecdn.com/c3299c7f-3c17-443d-9d5a-2b60d7ea7a72/"
                   :file-name   "Closure-Category-Hero.jpg"
                   :alt         "New Water Wave and Yaki Straight are here"}]
    [{:id                   "0"
      :slug                 "closures"
      :criteria             {:product/department #{"hair"} :product/family #{"closures"}}
      :unconstrained-facets #{:hair/origin :hair/texture :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "1"
      :slug                 "frontals"
      :criteria             {:product/department #{"hair"} :product/family #{"frontals"}}
      :unconstrained-facets #{:hair/origin :hair/texture :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "2"
      :slug                 "straight"
      :criteria             {:product/department #{"hair"} :hair/texture #{"straight"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "3"
      :slug                 "yaki-straight"
      :criteria             {:product/department #{"hair"} :hair/texture #{"yaki-straight"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "4"
      :slug                 "kinky-straight"
      :criteria             {:product/department #{"hair"} :hair/texture #{"kinky-straight"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "5"
      :slug                 "body-wave"
      :criteria             {:product/department #{"hair"} :hair/texture #{"body-wave"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "6"
      :slug                 "loose-wave"
      :criteria             {:product/department #{"hair"} :hair/texture #{"loose-wave"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "7"
      :slug                 "water-wave"
      :criteria             {:product/department #{"hair"} :hair/texture #{"water-wave"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "8"
      :slug                 "deep-wave"
      :criteria             {:product/department #{"hair"} :hair/texture #{"deep-wave"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "9"
      :slug                 "curly"
      :criteria             {:product/department #{"hair"} :hair/texture #{"curly"}}
      :unconstrained-facets #{:product/family :hair/origin :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}
     {:id                   "10"
      :slug                 "360-frontals"
      :criteria             {:product/department #{"hair"} :product/family #{"360-frontals"}}
      :unconstrained-facets #{:hair/origin :hair/texture :hair/base-material :hair/color}
      :copy                 {:description fake-copy}
      :images               {:hero fake-hero}}]))

(defn id->category [id categories]
  (->> categories
       (filter (comp #{(str id)} :id))
       first))

(defn named-search->category [named-search-slug categories]
  (some-> named-search-slug
          named-search->category-id
          (id->category categories)))

(defn current-category [data]
  (id->category (get-in data keypaths/current-category-id)
                (get-in data keypaths/categories)))
