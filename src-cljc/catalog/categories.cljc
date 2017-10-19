(ns catalog.categories
  (:require [catalog.category-filters :as category-filters]
            [clojure.string :as string]
            [spice.maps :as maps]
            [storefront.keypaths :as keypaths]))

(def new-facet?
  ;; [<facet-slug> <option-slug>]
  #{[:hair/family "360-frontals"]
    [:hair/family "360-wigs"]
    [:hair/family "lace-front-wigs"]})

(def new-category?
  #{"360-frontals" "wigs"})

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
   "360-frontals"   "10"
   "wigs"           "13"})

(defn copy [& sentences]
  (string/join " " sentences))

(defn category->seo [category-name description image-url]
  {:title          (str category-name " | Mayvenn")
   :og-title       (str category-name " - Free shipping. Free 30 day returns.")
   :description    description
   :og-description (copy "Machine-wefted and backed by our 30 Day Quality Guarantee, our"
                         category-name
                         "are the best quality products on the market and ships free!")
   :image-url      image-url})

(def initial-categories
  [{:catalog/category-id      "0"
    :name                     "Closures"
    :page/slug                "closures"
    :legacy/named-search-slug "closures"
    :criteria                 {:product/department #{"hair"} :hair/family #{"closures"}}
    :filter-tabs              [:hair/origin :hair/texture :hair/base-material :hair/color]
    :copy                     {:description (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                                  "Our collection of closures and frontals blend seamlessly with our bundles"
                                                  "and can be customized to fit your unique look.")}
    :images                   {:hero {:filename    "Closures.jpg",
                                      :desktop-url "//ucarecdn.com/8d0f63d5-495f-4fa5-a1d0-eb8f95e59235/",
                                      :mobile-url  "//ucarecdn.com/a4812b2f-d314-4aaa-9ea7-6a770e82c3c1/",
                                      :alt         "Closures"}}
    :footer/order             8
    :header/order             8
    :header/group             1
    :seo                      (category->seo "Closures"
                                             (copy "Lace Closures in Brazilian Straight, Malaysian Body Wave,"
                                                   "Peruvian Straight, Peruvian Body Wave,"
                                                   "Peruvian Yaki Straight, Indian Straight Lace and more.")
                                             "//ucarecdn.com/12e8ebfe-06cd-411a-a6fb-909041723333/")}
   {:catalog/category-id      "1"
    :name                     "Frontals"
    :page/slug                "frontals"
    :legacy/named-search-slug "frontals"
    :criteria                 {:product/department #{"hair"} :hair/family #{"frontals"}}
    :filter-tabs              [:hair/origin :hair/texture :hair/base-material :hair/color]
    :copy                     {:description (copy "Save your precious strands and top your look off with the ultimate tool in protective weave styling."
                                                  "Our collection of frontals blend seamlessly with our bundles and can be customized to fit your unique look.")}
    :images                   {:hero {:filename    "Frontals.jpg",
                                      :desktop-url "//ucarecdn.com/20a24c49-9216-4445-8a49-47ea53f88d32/",
                                      :mobile-url  "//ucarecdn.com/923e0942-7a07-49c3-9fbb-2efe0835221f/",
                                      :alt         "Frontals"}}
    :footer/order             9
    :header/order             9
    :header/group             1
    :seo                      (category->seo "Frontals"
                                             (copy "Brazilian, Peruvian, Indian and Malaysian hair frontals."
                                                   "Choose from popular textures such as Deep Wave,"
                                                   "Yaki Straight, Water Wave, Straight and more.")
                                             "//ucarecdn.com/0c7d94c3-c00e-4812-9526-7bd669ac679c/")}
   {:catalog/category-id      "2"
    :name                     "Straight"
    :page/slug                "straight"
    :legacy/named-search-slug "straight"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"straight"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "For those who want it given to them"
                                                  "straight, our collection of 100% virgin straight hair"
                                                  "is your go-to for a sleek look with"
                                                  "minimal effort.")}
    :images                   {:hero {:filename    "Straight.jpg",
                                      :desktop-url "//ucarecdn.com/4170d82e-9fa6-4a4b-bc30-17a578604ca5/",
                                      :mobile-url  "//ucarecdn.com/a9128570-7860-45e5-b7b4-85c098245a24/",
                                      :alt         "Straight"}
                               :home {:filename "StraightExtensionsMayvenn.jpg",
                                      :url      "//ucarecdn.com/3a7983df-318e-41d7-a247-bb1d12c623d2/",
                                      :alt      "Straight Hair Extensions Mayvenn"}}
    :home/order               0
    :footer/order             0
    :header/order             0
    :header/group             0
    :seo                      (category->seo "Natural Straight Extensions"
                                             (copy "Straight Brazilian weave, straight Indian hair and straight Peruvian hair."
                                                   "Our straight bundles are sleek from root to tip.")
                                             "//ucarecdn.com/61662cc7-59f5-454b-8031-538516557eb0/")}
   {:catalog/category-id      "3"
    :name                     "Yaki Straight"
    :page/slug                "yaki-straight"
    :legacy/named-search-slug "yaki-straight"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"yaki-straight"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Tired of having to break out the hot"
                                                  "tools for a textured straight look? Our Yaki"
                                                  "Straight hair collection is here to save your"
                                                  "strands! Yaki Straight hair matches the rhythm of"
                                                  "your natural hair that's been pressed straight or"
                                                  "freshly relaxed. Your flat iron has been officially"
                                                  "cancelled.")}
    :images                   {:hero {:filename    "YakiStraight.jpg",
                                      :desktop-url "//ucarecdn.com/ccac8a7f-2443-4ba0-a33a-03bb87fc73fb/",
                                      :mobile-url  "//ucarecdn.com/5ec46b49-0326-483d-9545-4956d3000cc3/",
                                      :alt         "Yaki Straight"}}
    :footer/order             1
    :header/order             1
    :header/group             0
    :seo                      (category->seo "Yaki Straight Extensions"
                                             (copy "Our Yaki Straight hair collection features both Peruvian and Brazilian straight hair bundles."
                                                   "With Lace Closure or Lace Frontals in different lengths.")
                                             "//ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/")}
   {:catalog/category-id      "4"
    :name                     "Kinky Straight"
    :page/slug                "kinky-straight"
    :legacy/named-search-slug "kinky-straight"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"kinky-straight"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Blending is a breeze with our Kinky Straight"
                                                  "hair collection! Like a fresh blow out, the"
                                                  "Kinky Straight hair texture moves freely and gives"
                                                  "a naturally flawless look that mimics your own"
                                                  "locks.")}
    :images                   {:hero {:filename    "KinkyStraight.jpg",
                                      :desktop-url "//ucarecdn.com/71a77939-739a-4959-9d38-7efe472c4e9e/",
                                      :mobile-url  "//ucarecdn.com/49a684e3-f347-427d-b027-cd65564d386c/",
                                      :alt         "Kinky Straight"}}
    :footer/order             2
    :header/order             2
    :header/group             0
    :seo                      (category->seo "Kinky Straight Extensions"
                                             (copy "100% human hair bundles and extensions from Mayvenn."
                                                   "Peruvian and Brazilian Kinky Straight Lace Closures and Frontals.")
                                             "//ucarecdn.com/7fe5f90f-4dad-454a-aa4b-b453fc4da3c4/")}
   {:catalog/category-id      "5"
    :name                     "Body Wave"
    :page/slug                "body-wave"
    :legacy/named-search-slug "body-wave"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"body-wave"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Step into the spotlight with our collection of luscious Body Wave hair."
                                                  "Body Wave is unbelievably soft and goes from straight to wavy and back again with ease.")}
    :images                   {:hero {:filename    "BodyWave.jpg",
                                      :desktop-url "//ucarecdn.com/7ee66aa8-7025-404a-8556-fbffc89f419d/",
                                      :mobile-url  "//ucarecdn.com/98042345-510e-4fe5-9ca3-002f0f02a085/",
                                      :alt         "Body Wave"}
                               :home {:filename "BodyWaveExtensionsMayvenn.jpg",
                                      :url      "//ucarecdn.com/7affd325-8cc8-4d25-be50-78ad6fcf8598/",
                                      :alt      "Body Wave Hair Extensions Mayvenn"}}
    :home/order               1
    :footer/order             3
    :header/order             3
    :header/group             0
    :seo                      (category->seo "Body Wave Extensions"
                                             (copy "Malaysian and Peruvian body wave silk, lace and 360 frontal bundles."
                                                   "Unbelievably soft and goes from straight to wavy and back again.")
                                             "//ucarecdn.com/445c53df-f369-4ca6-a554-c9668c8968f1/")}
   {:catalog/category-id      "6"
    :name                     "Loose Wave"
    :page/slug                "loose-wave"
    :legacy/named-search-slug "loose-wave"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"loose-wave"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "For hair that holds a curl beautifully,"
                                                  "our collection of 100% virgin Loose Wave hair"
                                                  "is the perfect foundation for all your carefree,"
                                                  "flirty, wavy looks.")}
    :images                   {:hero {:filename    "LooseWave.jpg",
                                      :desktop-url "//ucarecdn.com/616f7dbc-ca08-444f-9b44-87f154a97676/",
                                      :mobile-url  "//ucarecdn.com/deaea3de-9c38-4dc1-804f-e6619e7b7820/",
                                      :alt         "Loose Wave"}
                               :home {:filename "LooseWaveExtensionsMayvenn.jpg",
                                      :url      "//ucarecdn.com/6dd2b51a-3af5-4925-a8de-010a6cf53717/",
                                      :alt      "Loose Wave Hair Extensions Mayvenn"}}
    :home/order               2
    :footer/order             4
    :header/order             4
    :header/group             0
    :seo                      (category->seo "Loose Wave Extensions"
                                             (copy "Mayvenn’s Brazilian, Peruvian and Indian loose wave bundles."
                                                   "Also includes loose wave lace closures. All are 100% virgin Loose Wave hair.")
                                             "//ucarecdn.com/31be9341-a688-4f03-b754-a22a0a1f267e/")}
   {:catalog/category-id      "7"
    :name                     "Water Wave"
    :page/slug                "water-wave"
    :legacy/named-search-slug "water-wave"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"water-wave"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Ride the lush, carefree waves of the bundles,"
                                                  "closures, and frontals in our Water Wave hair"
                                                  "collection. For curls you can rock everywhere from"
                                                  "the office to your tropical vacation, make a"
                                                  "statement with Water Wave hair.")}
    :images                   {:hero {:filename    "WaterWave.jpg",
                                      :desktop-url "//ucarecdn.com/98dc7761-157b-4c44-96f6-38289cb3fe24/",
                                      :mobile-url  "//ucarecdn.com/f02150b4-c02b-42a3-b96b-42fea544b0ad/",
                                      :alt         "Water Wave"}}
    :footer/order             5
    :header/order             5
    :header/group             0
    :seo                      (category->seo "Water Wave Extensions"
                                             (copy "Water Wave Bundles, Closures, and Frontals."
                                                   "Peruvian and Brazilian bundles."
                                                   "Mayvenn has hair extensions, bundles, closures, and frontals.")
                                             "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/")}
   {:catalog/category-id      "8"
    :name                     "Deep Wave"
    :page/slug                "deep-wave"
    :legacy/named-search-slug "deep-wave"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"deep-wave"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Reigning supreme in versatility, the Deep Wave hair collection features"
                                                  "a soft, spiral wave full of body and bounce. Our deep wave hair is perfect"
                                                  "for those who want big waves that make an even bigger splash.")}
    :images                   {:hero {:filename    "DeepWave.jpg",
                                      :desktop-url "//ucarecdn.com/9042d464-d164-4241-8c6e-ba5350ac6b08/",
                                      :mobile-url  "//ucarecdn.com/f56c21d3-86dc-49d0-a9da-6f6e84618a70/",
                                      :alt         "Deep Wave"}
                               :home {:filename "DeepWaveExtensionsMayvenn.jpg",
                                      :url      "//ucarecdn.com/1fda417f-4714-4c04-965a-9a136014343a/",
                                      :alt      "Deep Wave Hair Extensions Mayvenn"}}
    :home/order               3
    :footer/order             6
    :header/order             6
    :header/group             0
    :seo                      (category->seo "Deep Wave Extensions"
                                             (copy "Deep Wave bundles and closures, including Brazilian, Peruvian and Indian Deep Wave."
                                                   "Soft, spiral wave full of body and bounce.")
                                             "//ucarecdn.com/49cc5837-8321-4331-9cec-d299d0de1887/")}
   {:catalog/category-id      "9"
    :name                     "Curly"
    :page/slug                "curly"
    :legacy/named-search-slug "curly"
    :criteria                 {:product/department #{"hair"} :hair/texture #{"curly"}}
    :filter-tabs              [:hair/family :hair/origin :hair/base-material :hair/color]
    :copy                     {:description (copy "Let your bold, beautiful curls take center stage! Our curly hair collection is a tight,"
                                                  "kinky curl perfect for creating voluminous coily styles that mimic natural 3C hair textures.")}
    :images                   {:hero {:filename    "Curly.jpg",
                                      :desktop-url "//ucarecdn.com/e1fc3b5d-7142-49e9-ba85-91776eabd666/",
                                      :mobile-url  "//ucarecdn.com/eec186f3-5e73-47c8-b53b-98ce87a3540e/",
                                      :alt         "Curly"}
                               :home {:filename "CurlyExtensionsMayvenn.jpg",
                                      :url      "//ucarecdn.com/9a6aee31-4990-4a14-b3ea-54065a0a985a/",
                                      :alt      "Curly Hair Extensions Mayvenn"}}
    :home/order               4
    :footer/order             7
    :header/order             7
    :header/group             0
    :seo                      (category->seo "Curly Extensions"
                                             (copy "Shop our Brazilian curly bundle, Peruvian Curly Bundle,"
                                                   "Peruvian Curly Lace closures and Curly Lace frontals."
                                                   "Perfect for creating voluminous coily styles.")
                                             "//ucarecdn.com/128b68e2-bf3a-4d72-8e39-0c71662f9c86/")}
   {:catalog/category-id      "10"
    :name                     "360 Frontals"
    :page/slug                "360-frontals"
    :legacy/named-search-slug "360-frontals"
    :criteria                 {:product/department #{"hair"} :hair/family #{"360-frontals"}}
    :filter-tabs              [:hair/origin :hair/texture :hair/base-material :hair/color]
    :copy                     {:description (copy "From your hairline to nape, we’ve got you covered with our revolutionary 360 Lace Frontal."
                                                  "This one-of-a-kind frontal piece features freestyle parting, baby hairs,"
                                                  "and low-density edges for a naturally flawless look.")}
    :images                   {:hero {:filename    "360Frontals.jpg"
                                      :desktop-url "//ucarecdn.com/b3df4ee4-2a8e-4226-b5ab-2e5158835b0d/"
                                      :mobile-url  "//ucarecdn.com/65261bbd-672a-4b0f-8172-8fd49b1ad273/"
                                      :alt         "360 Frontals"}}
    :footer/order             10
    :header/order             10
    :header/group             1
    :seo                      (category->seo "360 Frontals"
                                             (copy "Mayvenn’s bundles and extensions with a naturally flawless look."
                                                   "These are our 360 Lace Frontals - Brazilian, Peruvian and Malaysian,"
                                                   "as Body, Straight and Loose Wave.")
                                             "//ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/")}
   {:catalog/category-id "11"
    :name                "Hair"
    :page/slug           "hair"
    :criteria            {:product/department #{"hair"}}
    :filter-tabs         [:hair/texture]
    :hamburger/order     0}
   {:catalog/category-id "12"
    :name                "Closures & Frontals"
    :page/slug           "closures-and-frontals"
    :criteria            {:product/department #{"hair"} :hair/family #{"closures" "frontals" "360-frontals"}}
    :filter-tabs         [:hair/family]
    :copy                {:description (copy "Save your precious strands and top your look"
                                             "off with the ultimate tool in protective weave"
                                             "styling. Our collection of closures and frontals blend"
                                             "seamlessly with our bundles and can be customized"
                                             "to fit your unique look.")}
    :hamburger/order     1}
   {:catalog/category-id "13"
    :footer/order        20
    :header/order        20
    :header/group        1
    :name                "Wigs"
    :page/slug           "wigs"
    :copy                {:description (copy "When getting a bundle install isn’t an option,"
                                             "these units will be your go-to protective style"
                                             "for achieving a brand new look at home. Made"
                                             "from 100% virgin human hair, and customizable"
                                             "to fit your unique look.") }
    :images              {:hero {:filename    "Wigs.jpg"
                                 :desktop-url "//ucarecdn.com/5f2f6800-7b35-4471-9653-42455d7cf76d/"
                                 :mobile-url  "//ucarecdn.com/dd01c16f-983b-44a1-a1de-101b9430666f/"
                                 :alt         "Wigs"}}
    :criteria            {:product/department #{"hair"}
                          :hair/family        #{"360-wigs" "lace-front-wigs"}}
    :filter-tabs         [:hair/family :hair/texture :hair/origin :hair/color]

    :seo {:title          "Human Hair Wigs | Mayvenn"
          :og-title       (copy "Mayvenn 360 and Lace Frontal Wigs - Free shipping."
                                "Free 30 day returns. Made with 100% virgin human hair.")
          :description    (copy "Mayvenn’s Natural Lace Front Wigs and 360 Wigs."
                                "Comes in different variations such as Brazilian and Malaysian, straight, deep wave and loose wave.")
          :og-description (copy "100% virgin human hair, machine-wefted and backed by our"
                                "30 Day Quality Guarantee, our Wigs can be customized to fit"
                                "your unique look using the built-in combs and adjustable strap.")
          :image-url      "//ucarecdn.com/5269d7ef-b5c7-4475-9e9c-d16d7cbbdda3/"}}
   {:catalog/category-id    "14"
    :auth/requires          #{:stylist}
    :footer/order           30
    :header/order           30
    :header/group           1
    :name                   "Stylist Exclusives"
    :page/slug              "stylist-exclusives"
    :direct-to-details/id   "49"
    :direct-to-details/slug "rings-kits"
    :criteria               {:product/department #{"stylist-exclusives"}, :kits/contents #{"rings"}, :stylist-exclusives/family #{"kits"}}
    :filter-tabs            []
    :seo                    {:title          "Stylist Exclusives | Mayvenn"
                             :og-title       (copy "Stylist Exclusives - Free shipping."
                                                   "Free 30 day returns. Made with 100% virgin human hair.")
                             :description    (copy "")
                             :og-description (copy "")}}])

(defn id->category [id categories]
  (->> categories
       (filter (comp #{(str id)} :catalog/category-id))
       first))

(defn named-search->category [named-search-slug categories]
  (->> categories
       (filter #(= named-search-slug
                   (:legacy/named-search-slug %)))
       first))

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
   (map (partial hydrate-sku-set (maps/index-by :sku skus)) sku-sets)
   (get-in app-state keypaths/facets)))
