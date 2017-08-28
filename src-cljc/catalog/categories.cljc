(ns catalog.categories
  (:require [storefront.keypaths :as keypaths]
            [clojure.string :as string]
            [storefront.utils.maps :as maps]
            [storefront.accessors.category-filters :as category-filters]))

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

(defn copy [& sentences]
  (string/join " " sentences))

(defn category->seo [category-name image-url]
  (let [description (copy "Machine-wefted and backed by our 30 Day Quality Guarantee, our"
                          category-name
                          "are the best quality products on the market and ships free!")]
    {:title          (str category-name " | Mayvenn")
     :og-title       (str category-name " - Free shipping. Free 30 day returns.")
     :description    description
     :og-description description
     :image-url      image-url}))

(def initial-categories
  [{:id           "0"
    :name         "Closures"
    :slug         "closures"
    :criteria     {:product/department #{"hair"} :hair/family #{"closures"}}
    :filter-tabs  [:hair/origin :hair/texture :hair/base-material :hair/color]
    :copy         {:description (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                      "Our collection of closures and frontals blend seamlessly with our bundles"
                                      "and can be customized to fit your unique look.")}
    :images       {:hero {:filename "Closures.jpg",
                          :desktop-url "//ucarecdn.com/8d0f63d5-495f-4fa5-a1d0-eb8f95e59235/",
                          :mobile-url "//ucarecdn.com/a4812b2f-d314-4aaa-9ea7-6a770e82c3c1/",
                          :alt "Closures"}}
    :footer/order 8
    :seo          (category->seo "Closures" "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")}
   {:id           "1"
    :name         "Frontals"
    :slug         "frontals"
    :criteria     {:product/department #{"hair"} :hair/family #{"frontals"}}
    :filter-tabs  [:hair/origin :hair/texture :hair/base-material :hair/color]
    :copy         {:description (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                      "Our collection of frontals blend seamlessly with our bundles and can be customized to fit your unique look.")}
    :images       {:hero {:filename "Frontals.jpg",
                          :desktop-url "//ucarecdn.com/20a24c49-9216-4445-8a49-47ea53f88d32/",
                          :mobile-url "//ucarecdn.com/923e0942-7a07-49c3-9fbb-2efe0835221f/",
                          :alt "Frontals"}}
    :footer/order 9
    :seo          (category->seo "Frontals" "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/")}
   {:id           "2"
    :name         "Straight"
    :slug         "straight"
    :criteria     {:product/department #{"hair"} :hair/texture #{"straight"}}
    :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy         {:description (copy "For those who want it given to them"
                                      "straight, our collection of 100% virgin straight hair"
                                      "is your go-to for a sleek look with"
                                      "minimal effort.")}
    :images       {:hero {:filename "Straight.jpg",
                          :desktop-url "//ucarecdn.com/4170d82e-9fa6-4a4b-bc30-17a578604ca5/",
                          :mobile-url "//ucarecdn.com/a9128570-7860-45e5-b7b4-85c098245a24/",
                          :alt "Straight"}
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
    :copy         {:description (copy "Tired of having to break out the hot"
                                      "tools for a textured straight look? Our Yaki"
                                      "Straight hair collection is here to save your"
                                      "strands! Yaki Straight hair matches the rhythm of"
                                      "your natural hair that's been pressed straight or"
                                      "freshly relaxed. Your flat iron has been officially"
                                      "cancelled.")}
    :images       {:hero {:filename "YakiStraight.jpg",
                          :desktop-url "//ucarecdn.com/ccac8a7f-2443-4ba0-a33a-03bb87fc73fb/",
                          :mobile-url "//ucarecdn.com/5ec46b49-0326-483d-9545-4956d3000cc3/",
                          :alt "Yaki Straight"}}
    :footer/order 1
    :seo          (category->seo "Yaki Straight Bundles" "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/")}
   {:id           "4"
    :name         "Kinky Straight"
    :slug         "kinky-straight"
    :criteria     {:product/department #{"hair"} :hair/texture #{"kinky-straight"}}
    :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy         {:description (copy "Blending is a breeze with our Kinky Straight"
                                      "hair collection! Like a fresh blow out, the"
                                      "Kinky Straight hair texture moves freely and gives"
                                      "a naturally flawless look that mimics your own"
                                      "locks.")}
    :images       {:hero {:filename "KinkyStraight.jpg",
                          :desktop-url "//ucarecdn.com/71a77939-739a-4959-9d38-7efe472c4e9e/",
                          :mobile-url "//ucarecdn.com/49a684e3-f347-427d-b027-cd65564d386c/",
                          :alt "Kinky Straight"}}
    :footer/order 2
    :seo          (category->seo "Kinky Straight Bundles" "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/")}
   {:id           "5"
    :name         "Body Wave"
    :slug         "body-wave"
    :criteria     {:product/department #{"hair"} :hair/texture #{"body-wave"}}
    :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy         {:description (copy "Step into the spotlight with our collection of luscious Body Wave hair."
                                      "Body Wave is unbelievably soft and goes from straight to wavy and back again with ease.")}
    :images       {:hero {:filename "BodyWave.jpg",
                          :desktop-url "//ucarecdn.com/7ee66aa8-7025-404a-8556-fbffc89f419d/",
                          :mobile-url "//ucarecdn.com/98042345-510e-4fe5-9ca3-002f0f02a085/",
                          :alt "Body Wave"}
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
    :copy         {:description (copy "For hair that holds a curl beautifully, a"
                                      "our collection of 100% virgin Loose Wave hair"
                                      "is the perfect foundation for all your carefree,"
                                      "flirty, wavy looks.")}
    :images       {:hero {:filename "LooseWave.jpg",
                          :desktop-url "//ucarecdn.com/616f7dbc-ca08-444f-9b44-87f154a97676/",
                          :mobile-url "//ucarecdn.com/deaea3de-9c38-4dc1-804f-e6619e7b7820/",
                          :alt "Loose Wave"}
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
    :copy         {:description (copy "Ride the lush, carefree waves of the bundles,"
                                      "closures, and frontals in our Water Wave hair"
                                      "collection. For curls you can rock everywhere from"
                                      "the office to your tropical vacation, make a"
                                      "statement with Water Wave hair.")}
    :images       {:hero {:filename "WaterWave.jpg",
                          :desktop-url "//ucarecdn.com/98dc7761-157b-4c44-96f6-38289cb3fe24/",
                          :mobile-url "//ucarecdn.com/f02150b4-c02b-42a3-b96b-42fea544b0ad/",
                          :alt "Water Wave"}}
    :footer/order 5
    :seo          (category->seo "Water Wave Bundles" "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/")}
   {:id           "8"
    :name         "Deep Wave"
    :slug         "deep-wave"
    :criteria     {:product/department #{"hair"} :hair/texture #{"deep-wave"}}
    :filter-tabs  [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy         {:description (copy "Reigning supreme in versatility, the Deep Wave hair collection features"
                                      "a soft, spiral wave full of body and bounce. Our deep wave hair is perfect"
                                      "for those who want big waves that make an even bigger splash.")}
    :images       {:hero {:filename "DeepWave.jpg",
                          :desktop-url "//ucarecdn.com/9042d464-d164-4241-8c6e-ba5350ac6b08/",
                          :mobile-url "//ucarecdn.com/f56c21d3-86dc-49d0-a9da-6f6e84618a70/",
                          :alt "Deep Wave"}
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
    :copy         {:description (copy "Let your bold, beautiful curls take center stage! Our curly hair collection is a tight,"
                                      "kinky curl perfect for creating voluminous coily styles that mimic natural 3C hair textures.")}
    :images       {:hero {:filename "Curly.jpg",
                          :desktop-url "//ucarecdn.com/e1fc3b5d-7142-49e9-ba85-91776eabd666/",
                          :mobile-url "//ucarecdn.com/eec186f3-5e73-47c8-b53b-98ce87a3540e/",
                          :alt "Curly"} 
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
    :copy         {:description (copy "From your hairline to nape, we’ve got you covered with our revolutionary 360 Lace Frontal."
                                      "This one-of-a-kind frontal piece features freestyle parting, baby hairs,"
                                      "and low-density edges for a naturally flawless look.")}
    :images       {:hero {:filename    "360Frontals.jpg"
                          :desktop-url "//ucarecdn.com/b3df4ee4-2a8e-4226-b5ab-2e5158835b0d/"
                          :mobile-url  "//ucarecdn.com/65261bbd-672a-4b0f-8172-8fd49b1ad273/"
                          :alt         "360 Frontals"}}
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
    :copy            {:description (copy "Save your precious strands and top your look"
                                         "off with the ultimate tool in protective weave"
                                         "styling. Our collection of closures and frontals blend"
                                         "seamlessly with our bundles and can be customized"
                                         "to fit your unique look.")}
    :hamburger/order 1}])

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

(defn- hydrate-sku-set [id->skus sku-set]
  (-> sku-set
      (update :criteria #(maps/map-values set %))
      (assoc :sku-set/full-skus (map #(get id->skus %) (:sku-set/skus sku-set)))))

(defn make-category-filters [app-state {:keys [sku-sets skus category-id]}]
  (category-filters/init
   (id->category category-id (get-in app-state keypaths/categories))
   (map (partial hydrate-sku-set (maps/key-by :sku skus)) sku-sets)
   (get-in app-state keypaths/facets)))
