(ns storefront.accessors.categories
  (:require [storefront.keypaths :as keypaths]))

(def new-facet?
  "NB: changes here should be reflected in accessors.named-searches until experiments/new-taxon-launch? is 100%"
  ;; [<facet-slug> <option-slug>]
  #{[:hair/family "360-frontals"]
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

(defn category->seo [category-name image-url]
  (let [description (str "Machine-wefted and backed by our 30 Day Quality Guarantee, our "
                         category-name
                         " are the best quality products on the market and ships free!")]
    {:title          (str category-name " | Mayvenn")
     :og-title       (str category-name " - Free shipping. Free 30 day returns.")
     :description    description
     :og-description description
     :image-url      image-url}))

(def initial-categories
  (let [fake-copy "In augue sem, tincidunt egestas rutrum a, vehicula in dolor. Donec id erat eu turpis semper maximus tempor ac erat. Aenean sit amet neque vitae neque finibus tristique id vitae odio. Maecenas aliquet ipsum est, in eleifend erat consequat ac. Integer dapibus nisl ac est interdum faucibus."
        fake-hero {:mobile-url  "//ucarecdn.com/2aaff96b-3b51-4eb2-9c90-2e7806e13b15/"
                   :desktop-url "//ucarecdn.com/c3299c7f-3c17-443d-9d5a-2b60d7ea7a72/"
                   :file-name   "Closure-Category-Hero.jpg"
                   :alt         "New Water Wave and Yaki Straight are here"}]
    [{:id           "0"
      :name         "Closures"
      :slug         "closures"
      :criteria     {:product/department #{"hair"} :hair/family #{"closures"}}
      :filter-tabs  [:hair/origin :hair/texture :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 8
      :seo          (category->seo "Closures" "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")}
     {:id           "1"
      :name         "Frontals"
      :slug         "frontals"
      :criteria     {:product/department #{"hair"} :hair/family #{"frontals"}}
      :filter-tabs  [:hair/origin :hair/texture :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 9
      :seo          (category->seo "Frontals" "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/")}
     {:id           "2"
      :name         "Straight"
      :slug         "straight"
      :criteria     {:product/department #{"hair"} :hair/texture #{"straight"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero
                     :home {:filename "StraightExtensionsMayvenn.jpg",
                            :url      "//ucarecdn.com/3a7983df-318e-41d7-a247-bb1d12c623d2/",
                            :alt      "Straight Hair Extensions Mayvenn"}}
      :home/order   0
      :footer/order 0
      :seo          (category->seo "Natural Straight Bundles" "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/")}
     {:id           "3"
      :name         "Yaki Straight"
      :slug         "yaki-straight"
      :criteria     {:product/department #{"hair"} :hair/texture #{"yaki-straight"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 1
      :seo          (category->seo "Yaki Straight Bundles" "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/")}
     {:id           "4"
      :name         "Kinky Straight"
      :slug         "kinky-straight"
      :criteria     {:product/department #{"hair"} :hair/texture #{"kinky-straight"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 2
      :seo          (category->seo "Kinky Straight Bundles" "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/")}
     {:id           "5"
      :name         "Body Wave"
      :slug         "body-wave"
      :criteria     {:product/department #{"hair"} :hair/texture #{"body-wave"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero
                     :home {:filename "BodyWaveExtensionsMayvenn.jpg",
                            :url      "//ucarecdn.com/7affd325-8cc8-4d25-be50-78ad6fcf8598/",
                            :alt      "Body Wave Hair Extensions Mayvenn"}}
      :home/order   1
      :footer/order 3
      :seo          (category->seo "Body Wave Bundles" "//ucarecdn.com/445c53df-f369-4ca6-a554-c9668c8968f1/")}
     {:id           "6"
      :name         "Loose Wave"
      :slug         "loose-wave"
      :criteria     {:product/department #{"hair"} :hair/texture #{"loose-wave"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero
                     :home {:filename "LooseWaveExtensionsMayvenn.jpg",
                            :url      "//ucarecdn.com/6dd2b51a-3af5-4925-a8de-010a6cf53717/",
                            :alt      "Loose Wave Hair Extensions Mayvenn"}}
      :home/order   2
      :footer/order 4
      :seo          (category->seo "Loose Wave Bundles" "//ucarecdn.com/31be9341-a688-4f03-b754-a22a0a1f267e/")}
     {:id           "7"
      :name         "Water Wave"
      :slug         "water-wave"
      :criteria     {:product/department #{"hair"} :hair/texture #{"water-wave"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 5
      :seo          (category->seo "Water Wave Bundles" "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/")}
     {:id           "8"
      :name         "Deep Wave"
      :slug         "deep-wave"
      :criteria     {:product/department #{"hair"} :hair/texture #{"deep-wave"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero
                     :home {:filename "DeepWaveExtensionsMayvenn.jpg",
                            :url      "//ucarecdn.com/1fda417f-4714-4c04-965a-9a136014343a/",
                            :alt      "Deep Wave Hair Extensions Mayvenn"}}
      :home/order   3
      :footer/order 6
      :seo          (category->seo "Deep Wave Bundles" "//ucarecdn.com/49cc5837-8321-4331-9cec-d299d0de1887/")}
     {:id           "9"
      :name         "Curly"
      :slug         "curly"
      :criteria     {:product/department #{"hair"} :hair/texture #{"curly"}}
      :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero
                     :home {:filename "CurlyExtensionsMayvenn.jpg",
                            :url      "//ucarecdn.com/9a6aee31-4990-4a14-b3ea-54065a0a985a/",
                            :alt      "Curly Hair Extensions Mayvenn"}}
      :home/order   4
      :footer/order 7
      :seo          (category->seo "Curly Bundles" "//ucarecdn.com/128b68e2-bf3a-4d72-8e39-0c71662f9c86/")}
     {:id           "10"
      :name         "360 Frontals"
      :slug         "360-frontals"
      :criteria     {:product/department #{"hair"} :hair/family #{"360-frontals"}}
      :filter-tabs  [:hair/origin :hair/texture :hair/base-material :hair/color]
      :copy         {:description fake-copy}
      :images       {:hero fake-hero}
      :footer/order 10
      :seo          (category->seo "360 Frontals" "//ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/")}
     {:id              "11"
      :name            "Bundles"
      :slug            "bundles"
      :criteria        {:product/department #{"hair"} :hair/family #{"bundles"}}
      :filter-tabs     [:hair/texture :hair/origin]
      :hamburger/order 0}
     {:id              "12"
      :name            "Closures & Frontals"
      :slug            "closures-and-frontals"
      :criteria        {:product/department #{"hair"} :hair/family #{"closures" "frontals" "360-frontals"}}
      :filter-tabs     [:hair/family :hair/origin :hair/texture :hair/base-material]
      :hamburger/order 1}]))

(defn id->category [id categories]
  (->> categories
       (filter (comp #{(str id)} :id))
       first))

(defn named-search->category [named-search-slug categories]
  (some-> named-search-slug
          named-search->category-id
          (id->category categories)))

(defn current-traverse-nav [data]
  (id->category (get-in data keypaths/current-traverse-nav-id)
                (get-in data keypaths/categories)))

(defn current-category [data]
  (id->category (get-in data keypaths/current-category-id)
                (get-in data keypaths/categories)))
