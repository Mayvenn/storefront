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
  {:facets [{:facet/slug   "hair/origin",
             :facet/name   "Origin",
             :filter/order 3,
             :facet/options
             [{:option/slug  "brazilian",
               :option/name  "Brazilian hair",
               :sku/name     "Brazilian",
               :filter/order 0}
              {:option/slug  "indian",
               :option/name  "Indian hair",
               :sku/name     "Indian",
               :filter/order 3}]}
            {:facet/slug   "hair/base-material",
             :facet/name   "Material",
             :filter/order 5,
             :facet/options
             [{:option/slug  "lace",
               :option/name  "Lace",
               :filter/order 0}]}
            {:facet/slug   "hair/texture",
             :facet/name   "Texture",
             :filter/order 4,
             :facet/options
             [{:option/slug    "loose-wave",
               :option/name    "Loose Wave",
               :adventure/name "Loose Wave",
               :filter/order   4}]}
            {:facet/slug   "hair/color",
             :facet/name   "Color",
             :filter/order 6,
             :facet/options
             [{:option/slug             "#2-chocolate-brown",
               :option/name             "#2 Chocolate Brown",
               :adventure/name          "#2 Chocolate Brown",
               :sku/name                "#2 Chocolate Brown",
               :option/image            "chocolate-brown.jpg",
               :option/circle-swatch    "chocolate-brown-circle.jpg",
               :option/rectangle-swatch "chocolate-brown-rectangle.jpg",
               :filter/order            4}]}]})

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
