(ns storefront.handler-test.common
  (:require [cheshire.core :refer [generate-string parse-string]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST]]
            [ring.util.response :refer [content-type response status]]
            [standalone-test-server.core :refer [txfm-request txfm-requests
                                                 with-standalone-server standalone-server
                                                 with-requests-chan]]
            [storefront.system :refer [create-system]]))

(def contentful-port 4335)

(def test-overrides {:environment       "test"
                     :server-opts       {:port 2390}
                     :logging           {:system-name "storefront.tests"}
                     :contentful-config {:endpoint (str "http://localhost:" contentful-port)
                                         :space-id "fake-space-id"
                                         :api-key  "fake-api-key"}
                     :storeback-config  {:endpoint          "http://localhost:4334/"
                                         :internal-endpoint "http://localhost:4334/"}})

(def default-req-params {:server-port 8080
                         :uri "/"
                         :scheme :http
                         :request-method :get})

(def storeback-stylist-response
  (-> (generate-string {:store_slug "bob"
                        :store_name "Bob's Hair Emporium"
                        :instagram_account nil
                        :stylist_id 3})
      (response)
      (status 200)
      (content-type "application/json")))

(def storeback-affiliate-stylist-response
  (-> (generate-string {:store_slug "phil"
                        :store_name "Affiliate Store"
                        :experience "affiliate"
                        :stylist_id 10})
      (response)
      (status 200)
      (content-type "application/json")))

(def storeback-no-stylist-response
  (-> (response "{}")
      (status 404)
      (content-type "application/json")))

(def storeback-shop-response
  (-> (generate-string {:store_slug "shop"
                        :store_name "Mayvenn Hair"
                        :instagram_account nil
                        :stylist_id 1})
      (response)
      (status 200)
      (content-type "application/json")))

(def contentful-ugc-collection-response
  {:body {:sys {:type "Array"},
          :total 5,
          :skip 0,
          :limit 100,
          :items
          [{:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "look",
             :name "Mayvenn Classic - Shop By Look "},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:22:54.792Z",
             :id "5vqi7q9EeO1ULNjQ1Q4DEp",
             :type "Entry"}}
           {:fields
            {:slug "acceptance-deals",
             :name "[ACCEPTANCE] Mayvenn Classic - Deals Page"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-21T21:02:06.413Z",
             :id "6Za8EE8Kpn8NeoJciqN3uA",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}
              {:sys {:type "Link", :linkType "Entry", :id "48c3sCi06BHRRMKJxmM4u3"}}
              {:sys {:type "Link", :linkType "Entry", :id "broken-link-id"}}],
             :slug "deals",
             :name "Mayvenn Classic - Deals Page"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-23T17:05:20.329Z",
             :id "2dZTVOLLqkNS9EoUJ1t6qn",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "bundle-sets-straight",
             :name "Adventure  Bundle Sets Straight"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:44:28.793Z",
             :id "4GfFV6dC7KjLhUxNDKvguP",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "shop-by-look-straight",
             :name "Adventure Shop By Look Straight"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:32:43.380Z",
             :id "4NNviXNUw1odQtzXOdHaNY",
             :type "Entry"}}],
          :includes
          {:Entry
           [{:sys
             {:space {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
              :updatedAt "2019-05-22T22:59:58.189Z",
              :revision 1,
              :locale "en-US",
              :createdAt "2019-05-22T22:59:58.189Z",
              :type "Entry",
              :id "2zSbLYFcRYjVoEMMlsWLsJ",
              :environment {:sys {:id "master", :type "Link", :linkType "Environment"}},
              :contentType {:sys {:type "Link", :linkType "ContentType", :id "look"}}},
             :fields
             {:title "Acceptance Virgin Peruvian Deep Wave 16 18 20 ",
              :texture "Deep Wave",
              :color "Natural Black",
              :description "16\" + 18\" + 20\" ",
              :sharedCartUrl "https://shop.mayvenn.com/c/XFoCrXR7Yx",
              :photoUrl
              "https://static.pixlee.com/photos/235267317/original/bundle-deal-template-f-r1-01-lm.jpg",
              :socialMediaHandle "@mayvennhair",
              :socialMediaPlatform "instagram"}}
            {:sys
             {:space {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
              :updatedAt "2019-05-23T00:38:22.078Z",
              :revision 1,
              :locale "en-US",
              :createdAt "2019-05-23T00:38:22.078Z",
              :type "Entry",
              :id "48c3sCi06BHRRMKJxmM4u3",
              :environment {:sys {:id "master", :type "Link", :linkType "Environment"}},
              :contentType {:sys {:type "Link", :linkType "ContentType", :id "look"}}},
             :fields
             {:title "BDW 12\" 12\" 12\"",
              :texture "Deep Wave",
              :color "Natural Black",
              :description "12\" 12\" 12\"",
              :sharedCartUrl "https://shop.mayvenn.com/c/nAOHqCV5Es",
              :photoUrl
              "https://static.pixlee.com/photos/270470339/original/Screen_Shot_2019-03-08_at_9.44.00_AM.png",
              :socialMediaHandle "@enevicky",
              :socialMediaPlatform "instagram"}}]}}
   :status 200})

(def contentful-response
  {:body
   {:sys {:type "Array"},
    :total 2,
    :skip 0,
    :limit 100,
    :items
    [{:sys
      {:space
       {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
       :updatedAt "2019-11-04T18:41:07.943Z",
       :revision 3,
       :locale "en-US",
       :createdAt "2019-11-01T18:46:02.781Z",
       :type "Entry",
       :id "3Kpv87YOM1lUZBjew3qrL4",
       :environment
       {:sys {:id "master", :type "Link", :linkType "Environment"}},
       :contentType
       {:sys {:type "Link", :linkType "ContentType", :id "homepage"}}},
      :fields
      {:title "Spring (May) - Shop",
       :experience "shop",
       :production "2019-05-01T00:00-07:00",
       :acceptance "2019-03-27T00:00-07:00",
       :hero
       {:sys
        {:type "Link", :linkType "Entry", :id "51hN2feT89KMPp39SXoNyD"}},
       :feature1
       {:sys
        {:type "Link", :linkType "Entry", :id "2d18SwoMssJ8ZaHVVaaBsa"}},
       :feature2
       {:sys
        {:type "Link", :linkType "Entry", :id "54YeAtKjqDdAJlkna7821I"}},
       :feature3
       {:sys
        {:type "Link", :linkType "Entry", :id "6QcM2vyLBXrMLhsqaigSSg"}}}}
     {:sys
      {:space
       {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
       :updatedAt "2019-11-04T21:32:49.876Z",
       :revision 14,
       :locale "en-US",
       :createdAt "2019-05-01T17:27:39.964Z",
       :type "Entry",
       :id "3KhauQN5ZAIpsfCtTXi79Y",
       :environment
       {:sys {:id "master", :type "Link", :linkType "Environment"}},
       :contentType
       {:sys {:type "Link", :linkType "ContentType", :id "homepage"}}},
      :fields
      {:title "Spring (May) - Classic",
       :experience "classic",
       :production "2019-05-01T00:00-07:00",
       :acceptance "2019-03-27T00:00-07:00",
       :hero
       {:sys
        {:type "Link", :linkType "Entry", :id "51hN2feT89KMPp39SXoNyD"}},
       :feature1
       {:sys
        {:type "Link", :linkType "Entry", :id "2d18SwoMssJ8ZaHVVaaBsa"}},
       :feature2
       {:sys
        {:type "Link", :linkType "Entry", :id "54YeAtKjqDdAJlkna7821I"}},
       :feature3
       {:sys
        {:type "Link",
         :linkType "Entry",
         :id "6QcM2vyLBXrMLhsqaigSSg"}}}}],
    :includes
    {:Entry
     [{:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :updatedAt "2019-05-01T17:25:58.167Z",
        :revision 1,
        :locale "en-US",
        :createdAt "2019-05-01T17:25:58.167Z",
        :type "Entry",
        :id "2d18SwoMssJ8ZaHVVaaBsa",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :contentType
        {:sys
         {:type "Link",
          :linkType "ContentType",
          :id "homepageFeatureImage"}}},
       :fields
       {:title "Dyed Virgin Hair Feature ",
        :alt "Dyed Virgin Hair ",
        :desktop
        {:sys
         {:type "Link", :linkType "Asset", :id "2rKfcYhvyrge72m5Gutw7k"}},
        :mobile
        {:sys
         {:type "Link", :linkType "Asset", :id "6dqCxh4DcVjd4bGNacUa5P"}},
        :path "/categories/16-dyed-virgin-hair"}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :updatedAt "2019-05-01T20:29:28.698Z",
        :revision 8,
        :locale "en-US",
        :createdAt "2019-05-01T17:27:34.660Z",
        :type "Entry",
        :id "51hN2feT89KMPp39SXoNyD",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :contentType
        {:sys
         {:type "Link", :linkType "ContentType", :id "homepageHero"}}},
       :fields
       {:title "Spring ",
        :alt "Spring, For 15% Off, Use Code: Spring  ",
        :desktop
        {:sys
         {:type "Link", :linkType "Asset", :id "lV0nzdWVyeTwjzURC7DGy"}},
        :mobile
        {:sys
         {:type "Link", :linkType "Asset", :id "Dn1lKquC80iwMltrNUgOG"}},
        :path "/shop/look"}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :updatedAt "2019-03-06T22:28:02.109Z",
        :revision 5,
        :locale "en-US",
        :createdAt "2019-02-01T17:16:20.189Z",
        :type "Entry",
        :id "54YeAtKjqDdAJlkna7821I",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :contentType
        {:sys
         {:type "Link",
          :linkType "ContentType",
          :id "homepageFeatureImage"}}},
       :fields
       {:title "Wigs Feature ",
        :alt "Wigs 100% Virgin Hair ",
        :desktop
        {:sys
         {:type "Link", :linkType "Asset", :id "1e9OnGb4PsAsNMgaT3ltS1"}},
        :mobile
        {:sys
         {:type "Link", :linkType "Asset", :id "2cEhFKaemsY1ZQwXG5ZAsB"}},
        :path "/categories/13-wigs"}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :updatedAt "2019-03-06T21:51:17.665Z",
        :revision 7,
        :locale "en-US",
        :createdAt "2019-02-01T17:18:46.417Z",
        :type "Entry",
        :id "6QcM2vyLBXrMLhsqaigSSg",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :contentType
        {:sys
         {:type "Link",
          :linkType "ContentType",
          :id "homepageFeatureImage"}}},
       :fields
       {:title "Clip-Ins Feature",
        :alt "Clip-Ins",
        :desktop
        {:sys
         {:type "Link", :linkType "Asset", :id "7KVT4KTckERJJoKHYmyVGS"}},
        :mobile
        {:sys
         {:type "Link", :linkType "Asset", :id "5OxfNd9IPZyBZCqtMX3yjY"}},
        :path "/categories/21-seamless-clip-ins"}}],
     :Asset
     [{:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "1e9OnGb4PsAsNMgaT3ltS1",
        :type "Asset",
        :createdAt "2019-02-01T17:15:20.392Z",
        :updatedAt "2019-02-27T01:06:25.066Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 2,
        :locale "en-US"},
       :fields
       {:title "Wigs 100% Virgin Hair ",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/1e9OnGb4PsAsNMgaT3ltS1/b07e132e059ddbf85c065424c3601f2c/feat-block-dsk-b-wigs-01-lm.jpg",
         :details {:size 377427, :image {:width 1272, :height 800}},
         :fileName "feat-block-dsk-b-wigs-01-lm.jpg",
         :contentType "image/jpeg"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "2cEhFKaemsY1ZQwXG5ZAsB",
        :type "Asset",
        :createdAt "2019-02-01T17:16:15.249Z",
        :updatedAt "2019-03-06T21:57:44.112Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 5,
        :locale "en-US"},
       :fields
       {:title "Wigs 100% Virgin Hair",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/2cEhFKaemsY1ZQwXG5ZAsB/394fd34475769dd6e3dc2abb215804fe/feat-block-mob-b-wigs-04-lm__1_.png",
         :details {:size 475386, :image {:width 1500, :height 600}},
         :fileName "feat-block-mob-b-wigs-04-lm (1).png",
         :contentType "image/png"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "2rKfcYhvyrge72m5Gutw7k",
        :type "Asset",
        :createdAt "2019-02-01T17:13:28.808Z",
        :updatedAt "2019-02-27T01:04:46.878Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 2,
        :locale "en-US"},
       :fields
       {:title "Dyed Virgin Hair, Available in 8 Colors",
        :description "Desktop",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/2rKfcYhvyrge72m5Gutw7k/7780c4e6f4d7a039a9b0ce96fe65d39b/feat-block-dsk-a-dyed-virgin-01-lm.jpg",
         :details {:size 565266, :image {:width 1272, :height 800}},
         :fileName "feat-block-dsk-a-dyed-virgin-01-lm.jpg",
         :contentType "image/jpeg"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "5OxfNd9IPZyBZCqtMX3yjY",
        :type "Asset",
        :createdAt "2019-02-01T17:18:32.043Z",
        :updatedAt "2019-03-06T21:51:06.946Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 7,
        :locale "en-US"},
       :fields
       {:title "Clip-Ins, Available in 9 Colors, 2 Textures",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/5OxfNd9IPZyBZCqtMX3yjY/326342981c72a289a00782a7c6e475ac/feat-block-mob-c-clip-ins-03-lm__1_.png",
         :details {:size 196211, :image {:width 750, :height 300}},
         :fileName "feat-block-mob-c-clip-ins-03-lm (1).png",
         :contentType "image/png"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "6dqCxh4DcVjd4bGNacUa5P",
        :type "Asset",
        :createdAt "2019-02-01T17:14:10.193Z",
        :updatedAt "2019-02-27T01:05:32.803Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 2,
        :locale "en-US"},
       :fields
       {:title "Dyed Virgin Hair, Available in 8 Colors",
        :description "Mobile",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/6dqCxh4DcVjd4bGNacUa5P/7e8e8564b1f09da2e3ecac418bad216b/feat-block-mob-a-dyed-virgin-01-lm.jpg",
         :details {:size 525159, :image {:width 1500, :height 600}},
         :fileName "feat-block-mob-a-dyed-virgin-01-lm.jpg",
         :contentType "image/jpeg"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "7KVT4KTckERJJoKHYmyVGS",
        :type "Asset",
        :createdAt "2019-02-01T17:17:03.154Z",
        :updatedAt "2019-02-27T01:08:23.549Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 2,
        :locale "en-US"},
       :fields
       {:title "Clip-Ins, Available in 9 Colors, 2 Textures",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/7KVT4KTckERJJoKHYmyVGS/a1c86e8414445f2945baba11dc7df04a/feat-block-dsk-c-clip-ins-01-lm__1_.jpg",
         :details {:size 505066, :image {:width 1272, :height 800}},
         :fileName "feat-block-dsk-c-clip-ins-01-lm (1).jpg",
         :contentType "image/jpeg"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "Dn1lKquC80iwMltrNUgOG",
        :type "Asset",
        :createdAt "2019-05-01T17:28:47.483Z",
        :updatedAt "2019-05-01T19:08:04.752Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 3,
        :locale "en-US"},
       :fields
       {:title "Spring Mobile",
        :description "Spring Mobile",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/Dn1lKquC80iwMltrNUgOG/a61f0650a6eea683b426c891a22aed78/may_classicpack_product_homepagehero-mobile-03.jpg",
         :details {:size 524790, :image {:width 750, :height 930}},
         :fileName "may_classicpack_product_homepagehero-mobile-03.jpg",
         :contentType "image/jpeg"}}}
      {:sys
       {:space
        {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
        :id "lV0nzdWVyeTwjzURC7DGy",
        :type "Asset",
        :createdAt "2019-05-01T17:22:14.248Z",
        :updatedAt "2019-05-01T20:29:21.445Z",
        :environment
        {:sys {:id "master", :type "Link", :linkType "Environment"}},
        :revision 5,
        :locale "en-US"},
       :fields
       {:title "Spring Hero Desktop",
        :description "Spring Hero Desktop",
        :file
        {:url
         "//images.ctfassets.net/76m8os65degn/lV0nzdWVyeTwjzURC7DGy/b84a44e9a1ace2d3fc8b51f36ea42665/may_classicpack_product_homepagehero-desktop-03.jpg",
         :details {:size 335267, :image {:width 1400, :height 600}},
         :fileName "may_classicpack_product_homepagehero-desktop-03.jpg",
         :contentType "image/jpeg"}}}]}}
   :status 200})

(def facets-body
  "This has been copied from cellar"
  {:facets [{:facet/slug    :catalog/department
             :facet/name    "Department"
             :filter/order  0
             :facet/options [{:option/slug  "stylist-exclusives"
                              :option/name  "Stylist Exclusives"
                              :filter/order 0}
                             {:option/slug  "hair"
                              :option/name  "Hair"
                              :filter/order 1}]}
            {:facet/slug    :stylist-exclusives/family
             :facet/name    "Category"
             :filter/order  2
             :facet/options [{:option/slug  "kits"
                              :option/name  "Kits"
                              :filter/order 0}]}
            {:facet/slug    :hair/grade
             :facet/name    "Grade"
             :filter/order  1
             :facet/options [{:option/slug  "6a"
                              :option/name  "6a Premier collection"
                              :filter/order 0}
                             {:option/slug  "7a"
                              :option/name  "7a Deluxe collection"
                              :filter/order 1}
                             {:option/slug  "8a"
                              :option/name  "8a Ultra collection"
                              :filter/order 2}]}
            {:facet/slug    :hair/family
             :facet/name    "Category"
             :filter/order  2
             :facet/options [{:option/slug    "bundles"
                              :option/name    "Bundles"
                              :sku/name       "Bundle"
                              :adventure/name "Leave Out"
                              :filter/order   1}
                             {:option/slug    "closures"
                              :option/name    "Closures"
                              :adventure/name "Closure"
                              :sku/name       "Closure"
                              :filter/order   2}
                             {:option/slug    "frontals"
                              :option/name    "Frontals"
                              :adventure/name "Frontal"
                              :sku/name       "Frontal"
                              :filter/order   3}
                             {:option/slug    "360-frontals"
                              :option/name    "360 frontals"
                              :adventure/name "360°"
                              :sku/name       "360 Frontal"
                              :filter/order   4}
                             {:option/slug  "lace-front-wigs"
                              :option/name  "Lace Front Wigs"
                              :sku/name     "Front Wig"
                              :filter/order 5}
                             {:option/slug  "360-wigs"
                              :option/name  "360 Wigs"
                              :sku/name     "360 Wig"
                              :filter/order 6}
                             {:option/slug  "ready-wigs"
                              :option/name  "Ready-to-Wear Wigs"
                              :sku/name     "Ready-to-Wear Wig"
                              :filter/order 7}
                             {:option/slug  "seamless-clip-ins"
                              :option/name  "Seamless Clip-Ins"
                              :sku/name     "Seamless Clip-In"
                              :filter/order 8}
                             {:option/slug  "tape-ins"
                              :option/name  "Tape-Ins"
                              :sku/name     "Tape-In"
                              :filter/order 9}
                             ]}
            {:facet/slug    :hair/color
             :facet/name    "Color"
             :filter/order  6
             :facet/options [{:option/slug             "black"
                              :option/name             "Natural Black"
                              :adventure/name          "Natural Black"
                              :sku/name                "Natural Black"
                              :option/image            "//ucarecdn.com/c172523d-b231-49da-a9c1-51ec8a8e802a/-/format/auto/black_v1.png"
                              :option/circle-swatch    "//ucarecdn.com/c172523d-b231-49da-a9c1-51ec8a8e802a/-/format/auto/black_v1.png"
                              :option/rectangle-swatch "//ucarecdn.com/cf2e6d44-4e93-4792-801b-1e2aacdac408/-/format/auto/swatchnatural.png"
                              :filter/order            0}
                             {:option/slug             "#1-jet-black" ; We can't remove '#' without breaking links to the category pages w/ filters
                              :option/name             "#1 Jet Black"
                              :adventure/name          "#1 Jet Black"
                              :sku/name                "#1 Jet Black"
                              :option/image            "//ucarecdn.com/81e6cd3d-6fae-41b1-a7a5-cbefd64a4526/-/format/auto/prodsitecolorswatch1.png"
                              :option/circle-swatch    "//ucarecdn.com/81e6cd3d-6fae-41b1-a7a5-cbefd64a4526/-/format/auto/prodsitecolorswatch1.png"
                              :option/rectangle-swatch "//ucarecdn.com/916da696-76a8-43cd-a9dd-4914f89e986d/-/format/auto/swatch001.png"
                              :filter/order            1}
                             {:option/slug             "1b-soft-black"
                              :option/name             "#1B Soft Black"
                              :adventure/name          "#1B Soft Black"
                              :sku/name                "#1B Soft Black"
                              :option/image            "//ucarecdn.com/41276dc8-e77f-4f7a-b73a-465af9474f04/-/format/auto/prodsitecolorswatch01b.png"
                              :option/circle-swatch    "//ucarecdn.com/41276dc8-e77f-4f7a-b73a-465af9474f04/-/format/auto/prodsitecolorswatch01b.png"
                              :option/rectangle-swatch "//ucarecdn.com/5b6a2321-5342-49b9-8362-4e5de87d50c4/-/format/auto/swatch01b.png"
                              :filter/order            2}
                             {:option/slug             "1c-mocha-brown"
                              :option/name             "#1C Mocha Brown"
                              :adventure/name          "#1C Mocha Brown"
                              :sku/name                "#1C Mocha Brown"
                              :option/image            "//ucarecdn.com/b0b1f53f-4900-47d8-ba50-0aec454222f4/-/format/auto/swatch-01c-mocha-black.png"
                              :option/circle-swatch    "//ucarecdn.com/b0b1f53f-4900-47d8-ba50-0aec454222f4/-/format/auto/swatch-01c-mocha-black.png"
                              :option/rectangle-swatch "//ucarecdn.com/82056301-cddb-4c8b-b731-7014d3e8a236/-/format/auto/swatch01c.png"
                              :filter/order            3}
                             {:option/slug             "#2-chocolate-brown" ; We can't remove '#' without breaking links to the category pages w/ filters
                              :option/name             "#2 Chocolate Brown"
                              :adventure/name          "#2 Chocolate Brown"
                              :sku/name                "#2 Chocolate Brown"
                              :option/image            "//ucarecdn.com/6f2160cb-bc75-48a5-8d2d-6c7bcf7f1215/-/format/auto/prodsitecolorswatch2.png"
                              :option/circle-swatch    "//ucarecdn.com/6f2160cb-bc75-48a5-8d2d-6c7bcf7f1215/-/format/auto/prodsitecolorswatch2.png"
                              :option/rectangle-swatch "//ucarecdn.com/4663aebe-baa4-4604-9b1c-67520fb24a7a/-/format/auto/swatch002.png"
                              :filter/order            4}
                             {:option/slug             "#4-caramel-brown" ; We can't remove '#' without breaking links to the category pages w/ filters
                              :option/name             "#4 Caramel Brown"
                              :adventure/name          "#4 Caramel Brown"
                              :sku/name                "#4 Caramel Brown"
                              :option/image            "//ucarecdn.com/b096b5c3-a469-4c2b-a188-c0de34e8231d/-/format/auto/prodsitecolorswatch4.png"
                              :option/circle-swatch    "//ucarecdn.com/b096b5c3-a469-4c2b-a188-c0de34e8231d/-/format/auto/prodsitecolorswatch4.png"
                              :option/rectangle-swatch "//ucarecdn.com/9830b374-9ad5-46f7-b9ed-842dc0770cee/-/format/auto/swatch004.png"
                              :filter/order            5}
                             {:option/slug             "dark-blonde"
                              :option/name             "Dark Blonde (#27)"
                              :adventure/name          "Dark Blonde (#27)"
                              :sku/name                "#27 Dark Blonde"
                              :option/image            "//ucarecdn.com/f7eb2f95-3283-4160-bdf9-38a87be676c2/-/format/auto/dark_blonde.png"
                              :option/circle-swatch    "//ucarecdn.com/f7eb2f95-3283-4160-bdf9-38a87be676c2/-/format/auto/dark_blonde.png"
                              :option/rectangle-swatch "//ucarecdn.com/fafb4300-d3fa-4bf1-9485-c934097aa660/-/format/auto/swatch027.png"
                              :filter/order            6}
                             {:option/slug             "6-hazelnut-brown"
                              :option/name             "#6 Hazelnut Brown"
                              :adventure/name          "#6 Hazelnut Brown"
                              :sku/name                "#6 Hazelnut Brown"
                              :option/image            "//ucarecdn.com/798955ee-84e2-402e-a130-14085825996a/-/format/auto/swatch-006-hazelnut-brown.png"
                              :option/circle-swatch    "//ucarecdn.com/798955ee-84e2-402e-a130-14085825996a/-/format/auto/swatch-006-hazelnut-brown.png"
                              :option/rectangle-swatch "//ucarecdn.com/496bf2bd-26e0-4445-a5d5-1777207b063b/-/format/auto/swatch006.png"
                              :filter/order            7}
                             {:option/slug             "dark-blonde-dark-roots"
                              :option/name             "Dark Blonde (#27) with Dark Roots (#1B)"
                              :adventure/name          "Dark Blonde (#27) with Dark Roots (#1B)"
                              :sku/name                "#27 Dark Blonde with #1B Dark Roots"
                              :option/image            "//ucarecdn.com/9e15a581-6e80-401a-8cb2-0608fef474e9/-/format/auto/dark_blonde_dark_roots.png"
                              :option/circle-swatch    "//ucarecdn.com/9e15a581-6e80-401a-8cb2-0608fef474e9/-/format/auto/dark_blonde_dark_roots.png"
                              :option/rectangle-swatch "//ucarecdn.com/882188a0-69ab-47da-b757-812b4adce7a5/-/format/auto/swatch02701b.png"
                              :filter/order            8}
                             {:option/slug             "blonde"
                              :option/name             "Blonde (#613)"
                              :adventure/name          "Blonde (#613)"
                              :sku/name                "#613 Blonde"
                              :option/image            "//ucarecdn.com/85ede6dd-8e84-4096-ad5c-685d50dd99ec/-/format/auto/blonde.png"
                              :option/circle-swatch    "//ucarecdn.com/85ede6dd-8e84-4096-ad5c-685d50dd99ec/-/format/auto/blonde.png"
                              :option/rectangle-swatch "//ucarecdn.com/4fd70f0a-96ce-4b6d-bc23-674fc8d37aa3/-/format/auto/swatch613.png"
                              :filter/order            9}
                             {:option/slug             "blonde-dark-roots"
                              :option/name             "Blonde (#613) with Dark Roots (#1B)"
                              :adventure/name          "Blonde (#613) with Dark Roots (#1B)"
                              :sku/name                "#613 Blonde with #1B Dark Roots"
                              :option/image            "//ucarecdn.com/02f4a86c-12fa-47b3-8f50-078568e4f905/-/format/auto/blonde_dark_roots.png"
                              :option/circle-swatch    "//ucarecdn.com/02f4a86c-12fa-47b3-8f50-078568e4f905/-/format/auto/blonde_dark_roots.png"
                              :option/rectangle-swatch "//ucarecdn.com/8d84b3ac-430b-4d96-8545-f742a9a929cf/-/format/auto/swatch61301b.png"
                              :filter/order            10}
                             {:option/slug             "18-chestnut-blonde"
                              :option/name             "#18 Chestnut Blonde"
                              :adventure/name          "#18 Chestnut Blonde"
                              :sku/name                "#18 Chestnut Blonde"
                              :option/image            "//ucarecdn.com/171ded35-1e70-4132-8563-9599501a336a/-/format/auto/swatch-018-chestnut-blonde.png"
                              :option/circle-swatch    "//ucarecdn.com/171ded35-1e70-4132-8563-9599501a336a/-/format/auto/swatch-018-chestnut-blonde.png"
                              :option/rectangle-swatch "//ucarecdn.com/73970def-580f-48a1-bba7-d03eb9ad7b99/-/format/auto/swatch018.png"
                              :filter/order            11}
                             {:option/slug             "60-golden-ash-blonde"
                              :option/name             "#60 Golden Ash Blonde"
                              :adventure/name          "#60 Golden Ash Blonde"
                              :sku/name                "#60 Golden Ash Blonde"
                              :option/image            "//ucarecdn.com/d49e6650-a8a0-4d9d-8732-0f2210bf219c/-/format/auto/swatch-060-golden-blonde.png"
                              :option/circle-swatch    "//ucarecdn.com/d49e6650-a8a0-4d9d-8732-0f2210bf219c/-/format/auto/swatch-060-golden-blonde.png"
                              :option/rectangle-swatch "//ucarecdn.com/f63586db-a0a0-4efa-9fec-903c439e651d/-/format/auto/swatch060.png"
                              :filter/order            12}
                             {:option/slug             "613-bleach-blonde"
                              :option/name             "#613 Bleach Blonde"
                              :adventure/name          "#613 Bleach Blonde"
                              :sku/name                "#613 Bleach Blonde"
                              :option/image            "//ucarecdn.com/361d56ce-97e5-48fc-842a-d848d1cdfefb/-/format/auto/swatch-613-bleach-blonde.png"
                              :option/circle-swatch    "//ucarecdn.com/361d56ce-97e5-48fc-842a-d848d1cdfefb/-/format/auto/swatch-613-bleach-blonde.png"
                              :option/rectangle-swatch "//ucarecdn.com/4fd70f0a-96ce-4b6d-bc23-674fc8d37aa3/-/format/auto/swatch613.png"
                              :filter/order            13}
                             {:option/slug             "vibrant-burgundy"
                              :option/name             "Vibrant Burgundy"
                              :adventure/name          "Vibrant Burgundy"
                              :sku/name                "Vibrant Burgundy"
                              :option/image            "//ucarecdn.com/3629dcae-412e-44a0-bff8-df441beb9975/-/format/auto/prodsitecolorswatch99j.png"
                              :option/circle-swatch    "//ucarecdn.com/3629dcae-412e-44a0-bff8-df441beb9975/-/format/auto/prodsitecolorswatch99j.png"
                              :option/rectangle-swatch "//ucarecdn.com/5eadafa5-87ac-42ab-9af7-9be9621a0de9/-/format/auto/swatch99j.png"
                              :filter/order            14}]}
            {:facet/slug    :hair/texture
             :facet/name    "Texture"
             :filter/order  4
             :facet/options [{:option/slug    "straight"
                              :option/name    "Straight"
                              :adventure/name "Straight"
                              :filter/order   0}
                             {:option/slug    "yaki-straight"
                              :option/name    "Yaki Straight"
                              :adventure/name "Yaki Straight"
                              :filter/order   1}
                             {:option/slug    "kinky-straight"
                              :option/name    "Kinky Straight"
                              :adventure/name "Kinky Straight"
                              :filter/order   2}
                             {:option/slug    "body-wave"
                              :option/name    "Body Wave"
                              :adventure/name "Body Wave"
                              :filter/order   3}
                             {:option/slug    "loose-wave"
                              :option/name    "Loose Wave"
                              :adventure/name "Loose Wave"
                              :filter/order   4}
                             {:option/slug    "water-wave"
                              :option/name    "Water Wave"
                              :adventure/name "Water Wave"
                              :filter/order   5}
                             {:option/slug    "deep-wave"
                              :option/name    "Deep Wave"
                              :adventure/name "Deep Wave"
                              :filter/order   6}
                             {:option/slug    "curly"
                              :option/name    "Curly"
                              :adventure/name "Curly"
                              :filter/order   7}]}
            {:facet/slug    :hair/origin
             :facet/name    "Origin"
             :filter/order  3
             :facet/options [{:option/slug  "brazilian"
                              :option/name  "Brazilian hair"
                              :sku/name     "Brazilian"
                              :filter/order 0}
                             {:option/slug  "malaysian"
                              :option/name  "Malaysian hair"
                              :sku/name     "Malaysian"
                              :filter/order 1}
                             {:option/slug  "peruvian"
                              :option/name  "Peruvian hair"
                              :sku/name     "Peruvian"
                              :filter/order 2}
                             {:option/slug  "indian"
                              :option/name  "Indian hair"
                              :sku/name     "Indian"
                              :filter/order 3}]}
            {:facet/slug    :hair/base-material
             :facet/name    "Material"
             :filter/order  5
             :facet/options [{:option/slug  "lace"
                              :option/name  "Lace"
                              :filter/order 0}
                             {:option/slug  "silk"
                              :option/name  "Silk"
                              :filter/order 1}]}
            {:facet/slug    :hair/length
             :facet/name    "Length"
             :filter/order  7
             :facet/options [{:option/slug  "10"
                              :option/name  "10″"
                              :filter/order 0}
                             {:option/slug  "11"
                              :option/name  "11″"
                              :filter/order 1}
                             {:option/slug  "12"
                              :option/name  "12″"
                              :filter/order 2}
                             {:option/slug  "13"
                              :option/name  "13″"
                              :filter/order 3}
                             {:option/slug  "14"
                              :option/name  "14″"
                              :filter/order 4}
                             {:option/slug  "15"
                              :option/name  "15″"
                              :filter/order 5}
                             {:option/slug  "16"
                              :option/name  "16″"
                              :filter/order 6}
                             {:option/slug  "18"
                              :option/name  "18″"
                              :filter/order 7}
                             {:option/slug  "20"
                              :option/name  "20″"
                              :filter/order 8}
                             {:option/slug  "22"
                              :option/name  "22″"
                              :filter/order 9}
                             {:option/slug  "24"
                              :option/name  "24″"
                              :filter/order 10}
                             {:option/slug  "26"
                              :option/name  "26″"
                              :filter/order 11}
                             {:option/slug  "28"
                              :option/name  "28″"
                              :filter/order 12}
                             {:option/slug  "30"
                              :option/name  "30″"
                              :filter/order 13}]}
            {:facet/slug    :hair/color.process
             :facet/name    "Color Process"
             :filter/order  8
             :facet/options [{:option/slug  "dyed"
                              :option/name  "Dyed"
                              :sku/name     "Dyed"
                              :filter/order 0}]}
            {:facet/slug    :hair/source
             :facet/name    "Hair Source"
             :filter/order  9
             :facet/options [{:option/slug  "human"
                              :option/name  "100% Human"
                              :sku/name     "100% Human"
                              :filter/order 0}
                             {:option/slug  "virgin"
                              :option/name  "Virgin"
                              :sku/name     "Virgin"
                              :filter/order 1}]}
            {:facet/slug    :hair/weight
             :facet/name    "Weight"
             :filter/order  10
             :facet/options [{:option/slug  "50g"
                              :option/name  "50g"
                              :sku/name     "50g"
                              :filter/order 0}
                             {:option/slug  "160g"
                              :option/name  "160g"
                              :sku/name     "160g"
                              :filter/order 1}
                             {:option/slug  "220g"
                              :option/name  "220g"
                              :sku/name     "220g"
                              :filter/order 2}]}]})

(def default-storeback-handler
  (routes
   (GET "/store" req storeback-stylist-response)
   (GET "/promotions" req {:status 200
                           :body   "{}"})
   (GET "/v2/products" req {:status 200
                            :body   "{}"})
   (GET "/v2/facets" req {:status 200
                          :body   (generate-string facets-body)})))

(def default-contentful-handler
  (routes
   (GET "/spaces/fake-space-id/entries" req
     {:status 200
      :body   (generate-string (:body contentful-response))})))

(defmacro with-resource
  [bindings close-fn & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (~close-fn ~(bindings 0))))))

(defmacro with-services
  "Setup responses for outbound requests for various services"
  [handlers & body]
  `(let [h# ~handlers]
     (with-standalone-server
       [fake-storeback# (standalone-server (:storeback-handler h# default-storeback-handler))
        fake-contentful# (standalone-server (:contentful-handler h# default-contentful-handler)
                                            {:port contentful-port})]
       ~@body)))

(defmacro with-handler
  "Override storefront handler"
  [handler & body]
  `(let [unstarted-system# (create-system test-overrides)]
     (with-resource [sys# (component/start unstarted-system#)
                     ~handler (-> sys# :app-handler :handler)]
       component/stop
       ~@body)))

(defmacro with-handler-and-cms-data
  "Override storefront handler and cms data atom"
  [handler cms-overrides & body]
  `(let [unstarted-system# (create-system test-overrides)]
     (with-resource [sys# (component/start unstarted-system#)
                     ~handler  (-> sys# :app-handler :handler)]

       component/stop
       (do
         (-> sys# :contentful :cache (reset! ~cms-overrides))
         ~@body))))

(defn assert-request [req storeback-resp asserter]
  (with-services {:storeback-handler (constantly storeback-resp)}
    (with-handler handler
      (asserter (handler (merge default-req-params req))))))
