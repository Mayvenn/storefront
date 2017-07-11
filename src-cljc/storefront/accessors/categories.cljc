(ns storefront.accessors.categories)

(def named-search->category-id
  {"closures" "0"})

(def id->category
  {"0" {:id       "0"
        :slug     "closures"
        :criteria {:family #{"closures"}
                   :grade  #{"6a"}}
        :copy     {:description "In augue sem, tincidunt egestas rutrum a, vehicula in dolor. Donec id erat eu turpis semper maximus tempor ac erat. Aenean sit amet neque vitae neque finibus tristique id vitae odio. Maecenas aliquet ipsum est, in eleifend erat consequat ac. Integer dapibus nisl ac est interdum faucibus."}
        :images   {:hero {:mobile-url  "//ucarecdn.com/2aaff96b-3b51-4eb2-9c90-2e7806e13b15/"
                          :desktop-url "//ucarecdn.com/c3299c7f-3c17-443d-9d5a-2b60d7ea7a72/"
                          :file-name   "Closure-Category-Hero.jpg"
                          :alt         "New Water Wave and Yaki Straight are here"}}}})

(defn named-search->category [named-search-slug]
  (some-> named-search-slug
          named-search->category-id
          id->category))
