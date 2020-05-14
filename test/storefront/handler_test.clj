(ns storefront.handler-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.edn :as edn]
            [cemerick.url :as cemerick-url]
            [clojure.string :as string]
            [clojure.test :refer [are deftest is testing]]
            [compojure.core :refer [GET POST routes]]
            [lambdaisland.uri :as uri]
            [ring.mock.request :as mock]
            [ring.util.codec :as codec]
            [ring.util.response :refer [content-type response status]]
            [spice.maps :as maps]
            [standalone-test-server.core
             :refer
             [txfm-request txfm-requests with-requests-chan]]
            [storefront.handler-test.common
             :as
             common
             :refer
             [with-handler with-services]]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.java.io :as io]
            [storefront.config :as config])
  (:import java.io.ByteArrayInputStream))

(defn set-cookies [req cookies]
  (update req :headers assoc "cookie" (string/join "; " (map (fn [[k v]] (str k "=" v)) cookies))))

(defn parsed-url [url]
  (let [[base query] (.split (str url) "\\?")]
    [base (codec/form-decode query)]))

(defn parse-json-body [req]
  (update req :body #(parse-string % true)))

(defn first-json-request
  ([requests] (first-json-request requests conj))
  ([requests xf]
   (txfm-request requests
                 (comp (map parse-json-body) xf)
                 {:timeout 1000})))

(defmacro is-redirected-to [resp domain path]
  `(let [resp# ~resp
         domain# ~domain
         path# ~path]
     (is (= 302 (:status resp#)))
     (is (= (format "https://%s.mayvenn.com%s" domain# path#)
            (-> resp# :headers (get "Location"))))))

(defmacro is-permanently-redirected-to [resp domain path]
  `(let [resp# ~resp
         domain# ~domain
         path# ~path]
     (is (= 301 (:status resp#)))
     (is (= (format "https://%s.mayvenn.com%s" domain# path#)
            (-> resp# :headers (get "Location"))))))

(defn parse-canonical-url
  [body]
  (some->> body
           (re-find #"<link[^>]+rel=\"canonical[^>]+>")
           (re-find #"href=\"[^\"]+\"")
           (re-find #"\"[^\"]+\"")
           (re-find #"[^\"]+")
           (#(string/replace % #"&amp;" "&"))))

(defn parse-canonical-uri
  [body]
  (some->> body
           parse-canonical-url
           uri/uri))

(defn response->canonical-uri-query-string
  [resp]
  (->> (:body resp)
       parse-canonical-uri
       :query))


(deftest affiliate-store-urls-redirect-to-shop
  (with-services {:storeback-handler (routes
                                      (GET "/v2/products" req {:status 200
                                                               :body "{}"})
                                      (GET "/v2/facets" req {:status 200
                                                             :body   "{}"})
                                      (GET "/store" _ common/storeback-affiliate-stylist-response))}
    (with-handler handler
      (testing "every page redirects to shop"
        (doseq [[to-path redirect-path] [["/categories/2-virgin-straight" "/categories/2-virgin-straight?affiliate_stylist_id=10"]
                                         ["/categories/hair/straight" "/categories/hair/straight?affiliate_stylist_id=10"]
                                         ["/products/9-brazilian-straight-bundles?SKU=BNS10" "/products/9-brazilian-straight-bundles?SKU=BNS10&affiliate_stylist_id=10"]
                                         ["/about-us" "/about-us?affiliate_stylist_id=10"]
                                         ["/" "/?affiliate_stylist_id=10"]]]
          (testing (format "Visiting %s should redirect to %s" to-path redirect-path)
            (let [resp (handler (mock/request :get (str "https://phil.mayvenn.com" to-path)))]
              (is-redirected-to resp "shop" redirect-path)))))

      (testing "preserves utm params"
        (let [resp (handler (mock/request :get "https://phil.mayvenn.com/?utm_source=blog&utm_medium=referral&utm_campaign=HomePage&utm_term=Button"))]
          (is-redirected-to resp "shop" "/?utm_source=blog&utm_medium=referral&utm_campaign=HomePage&utm_term=Button&affiliate_stylist_id=10")))

      (testing "but not robot pages"
        (doseq [path ["/robots.txt"
                      "/sitemap.xml"]]
          (let [resp (handler (mock/request :get (str "https://phil.mayvenn.com" path)))]
            (is (= 200 (:status resp)))))))))

(deftest install-path-redirects-to-shop
  (with-services
    (with-handler handler
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/install"))]
        (testing "It redirects to the shop subdomain"
          (is-redirected-to resp "shop" "/"))))))

(deftest peakmill-subdomain-redirects-to-shop-and-preserves-path
  (with-services
    (with-handler handler
      (let [resp (handler (mock/request :get "https://peakmill.mayvenn.com/about-us"))]
        (testing "It redirects to the shop subdomain"
          (is-redirected-to resp "shop" "/about-us"))))))

(deftest www-subdomain-redirects-to-shop-and-preserves-path
  (with-services
    (with-handler handler
      (let [resp (handler (mock/request :get "https://www.mayvenn.com/about-us"))]
        (testing "It redirects to the shop subdomain"
          (is-permanently-redirected-to resp "shop" "/about-us"))))))

(deftest renders-page-when-matches-stylist-subdomain-and-sets-the-preferred-subdomain
  (common/assert-request
   (mock/request :get "https://bob.mayvenn.com")
   common/storeback-stylist-response
   (fn [resp]
     (is (= 200 (:status resp)))
     (is (.contains (first (get-in resp [:headers "Set-Cookie"])) "preferred-store-slug=bob;")))))

(deftest server-properly-encodes-data
  (testing "properly encoding query-params as edn without error"
    (with-services {}
      (with-handler handler
        ;; Note: double && is important
        (let [resp (handler (mock/request :get "https://bob.mayvenn.com/?utm_source=blog&utm_medium=social&&utm_campaign=HomePage&utm_term=Button"))
              data-edn (->> resp :body (re-find #"var data = (.*);") last)]
          (is (edn/read-string (parse-string data-edn))
              (format "Invalid EDN read-string: " (pr-str data-edn)))
          (is (= 200 (:status resp))))))))

(defmacro has-canonized-link [handler url]
  `(let [handler#       ~handler
         url#           ~url
         canonized-url# (-> (uri/uri url#)
                            (assoc :host "shop.mayvenn.com"
                                   :scheme "https"))
         link-url#      (some->> (mock/request :get (str "https:" url#))
                                 handler#
                                 :body
                                 (re-find #"<link[^>]+rel=\"canonical[^>]+>")
                                 (re-find #"href=\"[^\"]+\"")
                                 (re-find #"\"[^\"]+\"")
                                 (re-find #"[^\"]+")
                                 uri/uri)]
     (is (= canonized-url# link-url#))))

(deftest server-side-renders-product-details-page
  (testing "when the product does not exist storefront returns 404"
    (let [[_ storeback-handler] (with-requests-chan (routes
                                                     common/default-storeback-handler
                                                     (GET "/v2/orders/:number"
                                                       req {:status 404
                                                            :body   "{}"})
                                                     (GET "/v2/products" req
                                                       {:status 200
                                                        :body   (generate-string {:products []})})))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/999-red-balloons"))]
            (is (= 404 (:status resp))))))))
  (testing "when whitelisted for discontinued"
    (let [[_ storeback-handler] (with-requests-chan (routes
                                                     common/default-storeback-handler
                                                     (GET "/v2/products" req
                                                       {:status 200
                                                        :body   (generate-string {:products []
                                                                                  :skus     []})})))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/104-dyed-100-human-hair-brazilian-loose-wave-bundle?SKU=BLWLR1JB1"))]
            (is (= 301 (:status resp))))))))
  (testing "when the product exists"
    (let [[_ storeback-handler]
          (with-requests-chan (routes
                               (GET "/v2/orders/:number" req {:status 404
                                                              :body   "{}"})
                               (GET "/v2/products" req
                                 {:status 200
                                  :body   (generate-string {:products [{:catalog/product-id  "67"
                                                                        :catalog/department  #{"hair"}
                                                                        :selector/skus       ["PWWLC18"]
                                                                        :selector/sku-ids    ["PWWLC18"]
                                                                        :hair/base-material  #{"lace"}
                                                                        :hair/color.process  #{"natural"}
                                                                        :hair/family         #{"closures"}
                                                                        :hair/grade          #{"6a"}
                                                                        :hair/origin         #{"peruvian"}
                                                                        :hair/source         #{"virgin"}
                                                                        :hair/texture        #{"water-wave"}
                                                                        :selector/electives  [:hair/color :hair/length]
                                                                        :selector/essentials [:catalog/department
                                                                                              :hair/grade
                                                                                              :hair/origin
                                                                                              :hair/texture
                                                                                              :hair/base-material
                                                                                              :hair/family
                                                                                              :hair/color.process
                                                                                              :hair/source]
                                                                        :page/slug           "peruvian-water-wave-lace-closures"}]
                                                            :skus     [{:catalog/sku-id               "PWWLC18"
                                                                        :catalog/stylist-only?        false
                                                                        :catalog/launched-at          "2016-01-01T00:00:00.000Z"
                                                                        :catalog/department           #{"hair"}
                                                                        :selector/essentials          [:hair/family
                                                                                                       :hair/color
                                                                                                       :hair/origin
                                                                                                       :hair/base-material
                                                                                                       :hair/length
                                                                                                       :catalog/department
                                                                                                       :hair/color.process
                                                                                                       :hair/grade
                                                                                                       :hair/texture
                                                                                                       :hair/source]
                                                                        :selector/electives           []
                                                                        :hair/base-material           #{"lace"}
                                                                        :hair/color.process           #{"natural"}
                                                                        :hair/family                  #{"closures"}
                                                                        :hair/grade                   #{"6a"}
                                                                        :hair/origin                  #{"peruvian"}
                                                                        :hair/source                  #{"virgin"}
                                                                        :hair/texture                 #{"water-wave"}
                                                                        :hair/color                   #{"black"}
                                                                        :selector/images              [{:filename  "Water-Wave-Bundle.jpg"
                                                                                                        :url       "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/"
                                                                                                        :use-cases {:seo      {:alt ""}
                                                                                                                    :carousel {:alt   ""
                                                                                                                               :order 5}
                                                                                                                    :catalog  {:alt ""}}
                                                                                                        :criteria/attributes
                                                                                                        {:catalog/department "hair"
                                                                                                         :image/of           "product"
                                                                                                         :hair/grade         "6a"
                                                                                                         :hair/texture       "water-wave"
                                                                                                         :hair/color         "black"
                                                                                                         :hair/family        "bundles"}}]
                                                                        :selector/from-products       ["67"]
                                                                        :inventory/in-stock?          true
                                                                        :sku/price                    104.0
                                                                        :sku/title                    "A balloon"
                                                                        :legacy/product-name          "A balloon"
                                                                        :legacy/product-id            133
                                                                        :legacy/variant-id            641
                                                                        :promo.triple-bundle/eligible true}]})})
                               common/default-storeback-handler))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (has-canonized-link handler "//bob.mayvenn.com/products/67-peruvian-water-wave-lace-closures")
          (has-canonized-link handler "//bob.mayvenn.com/categories/10-360-frontals")
          (has-canonized-link handler "//bob.mayvenn.com/our-hair")
          (has-canonized-link handler "//bob.mayvenn.com/"))))))

(deftest server-side-fetching-of-orders
  (testing "storefront retrieves an order from storeback"
    (let [number                                 "W123456"
          token                                  "iA1bjIUAqCfyS3cuvdNYindmlRZ3ICr3g+vSfzvUM1c="
          [storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      common/default-storeback-handler
                                                                      (GET "/v2/orders/:number" req {:status 200
                                                                                                     ;; TODO: fixme
                                                                                                     :body   "{\"number\": \"W123456\"}"})))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (let [resp (handler (-> (mock/request :get "https://bob.mayvenn.com/")
                                  (set-cookies {":number" number
                                                ":token"  token})))]
            (is (= 200 (:status resp)))
            (is (.contains (:body resp) "W123456") (pr-str resp))

            (testing "storefront properly reads order tokens with pluses in them"
              (let [requests       (txfm-requests storeback-requests (filter (fn [req] (= "/v2/orders/W123456" (:uri req)))))
                    waiter-request (first requests)]
                (is (= {"token" token} (:query-params waiter-request)))))))))))

(deftest sitemap-on-a-valid-store-domain
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-services {:storeback-handler handler}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/sitemap.xml"))]
          (is (= 200 (:status resp))))))))

(deftest sitemap-does-not-exist-on-root-domain
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-services {:storeback-handler handler}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://mayvenn.com/sitemap.xml"))]
          (is (= 404 (:status resp))))))))

(deftest sitemap-includes-blog-sitemap-and-pages-sitemap
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-services {:storeback-handler handler}
      (with-handler handler
        (let [{:keys [body]} (handler (mock/request :get "https://shop.mayvenn.com/sitemap.xml"))
            parsed-body    (xml/parse (ByteArrayInputStream. (.getBytes ^String body)))
            urls           (into []
                                 (comp
                                  (filter (comp #{:sitemap} :tag))
                                  (mapcat :content)
                                  (filter (comp #{:loc} :tag))
                                  (map (comp string/trim first :content)))
                                 (:content parsed-body))]
        (is (= ["https://shop.mayvenn.com/sitemap-pages.xml"
                "https://shop.mayvenn.com/blog/sitemap-posts.xml"]
               urls)))))))

(deftest most-sitemap-urls-are-their-own-canonical-url
  "- marketing/branded pages
  - product category ICP
  - child product category
  - product category URLs with only 1 parameter
  - non-parameter PDP
  - non-parameter /shop/ pages"
  (with-services {}
    (with-handler handler
      (let [{:keys [body]} (handler (mock/request :get "https://shop.mayvenn.com/sitemap-pages.xml"))
            parsed-body    (xml/parse (ByteArrayInputStream. (.getBytes ^String body)))
            urls           (into []
                                 (comp
                                  (filter (comp #{:url} :tag))
                                  (mapcat :content)
                                  (filter (comp #{:loc} :tag))
                                  (map (comp string/trim first :content)))
                                 (:content parsed-body))
            excluded-urls #{"https://mayvenn.com/"
                            (str "https://" config/welcome-subdomain ".mayvenn.com/")}]
        (is (not-empty urls))
        (doseq [url urls
                :when (not (contains? excluded-urls url))]
          (testing (format "'%s' in is canonical" url)
            (let [{:keys [body] :as response} (handler (mock/request :get url))]
              (is (= (parse-canonical-url body) url)
                  (pr-str response)))))))))

(deftest robots-disallows-content-storefront-pages-on-shop
  (with-services {}
    (with-handler handler
      (let [{:keys [status body]} (handler (mock/request :get "https://shop.mayvenn.com/robots.txt"))]
        (is (= 200 status))
        (are [line] (.contains body line)
          "Disallow: /account"
          "Disallow: /checkout"
          "Disallow: /orders"
          "Disallow: /stylist"
          "Disallow: /cart"
          "Disallow: /m/"
          "Disallow: /c/"
          "Disallow: /admin"
          "Disallow: /content"
          "Sitemap: https://shop.mayvenn.com/sitemap.xml")))))

(deftest robots-allows-all-pages-on-stylist-stores
  (with-services {}
    (with-handler handler
      (let [{:keys [status body]} (handler (mock/request :get "https://bob.mayvenn.com/robots.txt"))]
        (is (= 200 status))
        (is (not-any? #{"Disallow: /"} (string/split-lines body)) body)))))

(deftest fetches-data-from-contentful
  (testing "transforming content"
    (testing "transforming 'homepage' content"
      (let [[contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                           {:status 200
                                                                            :body   (generate-string (:body common/contentful-response))}))]
        (with-services {:contentful-handler contentful-handler}
          (with-handler handler
            (let [responses                         (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                  requests                          (txfm-requests contentful-requests identity)
                  {:keys [hero feature-1
                          feature-2 feature-3]
                   :as   classic-homepage-response} (-> (mock/request :get "https://bob.mayvenn.com/cms/homepage")
                                                        handler
                                                        :body
                                                        (parse-string true)
                                                        :homepage
                                                        :classic)]
              (is (every? #(= 200 (:status %)) responses))
              (is (=
                   #{:title :alt :desktop :mobile :path}
                   (set (keys feature-1))
                   (set (keys feature-2))
                   (set (keys feature-3))
                   (set (keys hero)))))))))

    (testing "transforming ugc-collections"
      (let [[contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                           {:status 200
                                                                            :body   (generate-string (:body common/contentful-ugc-collection-response))}))]
        (with-services {:contentful-handler contentful-handler}
          (with-handler handler
            (let [responses (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                  requests  (txfm-requests contentful-requests identity)
                  look-1    {:content/type          "look"
                             :content/id            "2zSbLYFcRYjVoEMMlsWLsJ"
                             :content/updated-at    1558565998189
                             :title                 "Acceptance Virgin Peruvian Deep Wave 16 18 20 "
                             :texture               "Deep Wave"
                             :color                 "Natural Black"
                             :description           "16\" + 18\" + 20\" "
                             :shared-cart-url       "https://shop.mayvenn.com/c/XFoCrXR7Yx"
                             :photo-url             "https://static.pixlee.com/photos/235267317/original/bundle-deal-template-f-r1-01-lm.jpg"
                             :social-media-handle   "@mayvennhair"
                             :social-media-platform "instagram"}
                  look-2    {:content/type          "look"
                             :content/id            "48c3sCi06BHRRMKJxmM4u3"
                             :content/updated-at    1558571902078
                             :title                 "BDW 12\" 12\" 12\""
                             :texture               "Deep Wave"
                             :color                 "Natural Black"
                             :description           "12\" 12\" 12\""
                             :shared-cart-url       "https://shop.mayvenn.com/c/nAOHqCV5Es"
                             :photo-url             "https://static.pixlee.com/photos/270470339/original/Screen_Shot_2019-03-08_at_9.44.00_AM.png"
                             :social-media-handle   "@enevicky"
                             :social-media-platform "instagram"}]
              (is (= {:all-looks {(keyword "2zSbLYFcRYjVoEMMlsWLsJ") look-1
                                  (keyword "48c3sCi06BHRRMKJxmM4u3") look-2}
                      :deals     {:content/id         "2dZTVOLLqkNS9EoUJ1t6qn"
                                  :content/type       "ugc-collection"
                                  :content/updated-at 1558631120329
                                  :slug               "deals"
                                  :name               "Mayvenn Classic - Deals Page"
                                  :looks              [look-1 look-2]}
                      :acceptance-deals
                      {:slug               "acceptance-deals"
                       :name               "[ACCEPTANCE] Mayvenn Classic - Deals Page"
                       :content/updated-at 1558472526413
                       :content/type       "ugc-collection"
                       :content/id         "6Za8EE8Kpn8NeoJciqN3uA"}
                      :shop-by-look-straight
                      {:slug               "shop-by-look-straight",
                       :name               "Adventure Shop By Look Straight",
                       :content/updated-at 1558567963380,
                       :content/type       "ugc-collection",
                       :content/id         "4NNviXNUw1odQtzXOdHaNY",
                       :looks              [look-1]}
                      :bundle-sets-straight
                      {:slug               "bundle-sets-straight",
                       :name               "Adventure  Bundle Sets Straight",
                       :content/updated-at 1558568668793,
                       :content/type       "ugc-collection",
                       :content/id         "4GfFV6dC7KjLhUxNDKvguP",
                       :looks              [look-1]}
                      :look
                      {:slug               "look"
                       :name               "Mayvenn Classic - Shop By Look "
                       :content/updated-at 1558567374792
                       :content/type       "ugc-collection"
                       :content/id         "5vqi7q9EeO1ULNjQ1Q4DEp"
                       :looks              [look-1]}}
                     (-> (mock/request :get "https://bob.mayvenn.com/cms/ugc-collection")
                         handler
                         :body
                         (parse-string true)
                         :ugc-collection)))))))))

  (let [number-of-contentful-entities-to-fetch 4]
    (testing "caching content"
      (let [[contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                           {:status 200
                                                                            :body   (generate-string (:body common/contentful-response))}))]
        (with-services {:contentful-handler contentful-handler}
          (with-handler handler
            (let [responses (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                  requests  (txfm-requests contentful-requests identity)]
              (is (every? #(= 200 (:status %)) responses))
              (is (= number-of-contentful-entities-to-fetch (count requests))))))))

    (testing "fetches data on system start"
      (let [[contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                           {:status 200
                                                                            :body   (generate-string (:body common/contentful-response))}))]
        (with-services {:contentful-handler contentful-handler}
          (with-handler handler
            (is (= number-of-contentful-entities-to-fetch (count (txfm-requests contentful-requests identity))))))))

    (testing "attempts-to-retry-fetch-from-contentful"
      (let [[contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                           {:status 500
                                                                            :body   "{}"}))]
        (with-services {:contentful-handler contentful-handler}
          (with-handler handler
            (let [responses (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                  requests  (txfm-requests contentful-requests identity)]
              (is (every? #(= 200 (:status %)) responses))
              (is (= (* 2 number-of-contentful-entities-to-fetch) (count requests))))))))))

(deftest we-do-not-ask-waiter-more-than-once-for-the-order
  (testing "Fetching normal pages fetches order once"
    (let [[storeback-requests storeback-handler]
          (with-requests-chan (constantly {:status  200
                                           :headers {"Content-Type" "application/json"}
                                           :body    (generate-string {:number "W123456"
                                                                      :token  "order-token"
                                                                      :state  "cart"})}))]
      (with-services {:storeback-handler (routes (GET "/store" req common/storeback-stylist-response)
                                                 (GET "/v2/facets" req {:status 200
                                                                        :body   ""})
                                                 storeback-handler)}
        (with-handler handler
          (let [resp (-> (mock/request :get "https://bob.mayvenn.com/")
                         (set-cookies {"number"  "W123456"
                                       "token"   "order-token"
                                       "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                         handler)
                requests (txfm-requests storeback-requests
                                        (comp
                                         (map :uri)
                                         (filter #(string/starts-with? % "/v2/orders"))))]
            (is (= 200 (:status resp)) (get-in resp [:headers "Location"]))
            (is (= 1 (count requests)))))))))

(deftest canonical-uris-query-params
  (with-services {}
    (with-handler handler
      (testing "category page"
        (testing "with allowed and extraneous query params"
          (let [resp (->> "https://shop.mayvenn.com/categories/7-Virgin-water-wave?base-material=lace&origin=peruvian&foo=bar"
                          (mock/request :get)
                          handler)]
            (is (= 200 (:status resp)))
            (is (= "origin=peruvian" (response->canonical-uri-query-string resp)))))
        (testing "with one query param"
          (let [resp (->> "https://shop.mayvenn.com/categories/7-Virgin-water-wave?origin=peruvian"
                          (mock/request :get)
                          handler)]
            (is (= 200 (:status resp)))
            (is (= "origin=peruvian" (response->canonical-uri-query-string resp)))))
        (testing "without query params"
          (let [resp (->> "https://shop.mayvenn.com/categories/7-Virgin-water-wave"
                          (mock/request :get)
                          handler)]
            (is (= 200 (:status resp)))
            (is (= nil (response->canonical-uri-query-string resp))))))
      (testing "non category page"
        (testing "without query params"
          (let [resp (->> "https://shop.mayvenn.com"
                          (mock/request :get)
                          handler)]
            (is (= 200 (:status resp)))
            (is (= nil (response->canonical-uri-query-string resp)))))
        (testing "with query params"
          (let [resp (->> "https://shop.mayvenn.com?utm_something=deal_with_it"
                          (mock/request :get)
                          handler)]
            (is (= 200 (:status resp)))
            (is (= nil (response->canonical-uri-query-string resp)))))))))

(defn parse-meta-tag-content [body name]
  (some->> body
           (re-find (re-pattern (format "<meta[^>]+name=\"%s\"[^>]*>" name)))
           (re-find #"content=\"[^\"]+\"")
           (re-find #"\"[^\"]+\"")
           (re-find #"[^\"]+")
           (#(string/replace % #"&amp;" "&"))
           (#(string/replace % #"&apos;" "'"))))

(defn parse-title [body]
  (some->> body
           (re-find #"<title[^>]*>(.*)</title>")
           second
           (#(string/replace % #"&amp;" "&"))))

(defmacro validate-title-and-description-and-canonical
  [resp expected-title expected-description expected-canonical expected-query-params]
  `(let [resp#                  ~resp
         expected-title#        ~expected-title
         expected-description#  ~expected-description
         expected-canonical#    ~expected-canonical
         expected-query-params# ~expected-query-params]
     (is (= 200 (:status resp#)))
     (is (= expected-title#
            (parse-title (:body resp#))))
     (is (= expected-description#
            (parse-meta-tag-content (:body resp#) "description")))
     (is (= expected-canonical#
            (-> resp# :body parse-canonical-uri :path)))
     (is (= expected-query-params#
            (response->canonical-uri-query-string resp#)))))

(def default-closure-title
  "Hair Closures: Human Hair Closure Sew-Ins | Mayvenn")

(def default-closure-description
  (str "Mayvenn’s hair closures allow you to "
       "close off any unit or install and come in a variety of "
       "different combinations. Shop now to create your look."))

(def virgin-closures-category-url
  "https://shop.mayvenn.com/categories/0-virgin-closures")

(deftest category-page-title-and-description-templates
  (with-services {}
    (with-handler handler
      (testing "a category page has a template"
        (testing "no options are selected we get generic description"
          (-> (mock/request :get virgin-closures-category-url)
              handler
              (validate-title-and-description-and-canonical default-closure-title
                                                            default-closure-description
                                                            "/categories/0-virgin-closures"
                                                            nil)))

        (testing "when one facet is selected"
          (-> (mock/request :get (str virgin-closures-category-url
                                      "?origin=indian"))
              handler
              (validate-title-and-description-and-canonical
               "Indian Virgin Hair Closures | Mayvenn"
               (str "Mayvenn's Indian Virgin Hair Closures are beautifully "
                    "crafted and provide a realistic part to close off any unit "
                    "or install.")
               "/categories/0-virgin-closures"
               "origin=indian")))

        (testing "two options are selected"
          (testing "when one facet is selected"
            (-> (mock/request :get (str virgin-closures-category-url
                                        "?texture=loose-wave"
                                        "&color=%232-chocolate-brown"))
                handler
                (validate-title-and-description-and-canonical "Loose Wave #2 Chocolate Brown Virgin Hair Closures | Mayvenn"
                                                              (str "Mayvenn's Loose Wave #2 Chocolate Brown Virgin Hair "
                                                                   "Closures are beautifully crafted and provide a realistic part to "
                                                                   "close off any unit or install.")
                                                              "/categories/0-virgin-closures"
                                                              "texture=loose-wave&color=%232-chocolate-brown"))))

        (testing "three options are selected"
          (-> (mock/request :get (str virgin-closures-category-url
                                      "?texture=loose-wave"
                                      "&origin=indian"
                                      "&color=%232-chocolate-brown"))
              handler
              (validate-title-and-description-and-canonical "Indian Loose Wave #2 Chocolate Brown Virgin Hair Closures | Mayvenn"
                                                            (str "Mayvenn's Indian Loose Wave #2 Chocolate Brown Virgin "
                                                                 "Hair Closures are beautifully crafted and provide a realistic "
                                                                 "part to close off any unit or install.")
                                                            "/categories/0-virgin-closures"
                                                            "origin=indian&texture=loose-wave&color=%232-chocolate-brown")))

        (testing "Four options from four facets are selected we get generic description"
          (-> (mock/request :get (str virgin-closures-category-url
                                      "?texture=loose-wave"
                                      "&origin=indian"
                                      "&color=%232-chocolate-brown"
                                      "&base-material=lace"))
              handler
              (validate-title-and-description-and-canonical
               default-closure-title
               default-closure-description
               "/categories/0-virgin-closures"
               "origin=indian&texture=loose-wave&color=%232-chocolate-brown&base-material=lace")))

        (testing "two options from the same facet are selected we get a generic description"
          (-> (mock/request :get (str virgin-closures-category-url
                                      "?origin=indian~brazilian"))
              handler
              (validate-title-and-description-and-canonical default-closure-title
                                                            default-closure-description
                                                            "/categories/0-virgin-closures"
                                                            "origin=indian%7Ebrazilian"))))

      (testing "when a category page does not have a template and has selections"
        (-> (mock/request :get "https://shop.mayvenn.com/categories/23-mayvenn-install?origin=indian")
            handler
            (validate-title-and-description-and-canonical "Mayvenn Install Eligible | Mayvenn"
                                                          (str "Mayvenn’s Natural Lace Front Wigs and 360 Wigs. "
                                                               "Comes in different variations such as Brazilian "
                                                               "and Malaysian, straight, deep wave and loose wave.")
                                                          "/categories/23-mayvenn-install"
                                                          "origin=indian")))

      (testing "when mayvenn-install category has a category selected, that categories seo is used"
        (-> (mock/request :get "https://shop.mayvenn.com/categories/23-mayvenn-install?family=closures")
            handler
            (validate-title-and-description-and-canonical
             "Hair Closures: Human Hair Closure Sew-Ins | Mayvenn"
             (str "Mayvenn’s hair closures allow you to close off any unit or "
                  "install and come in a variety of different combinations. Shop now to create your look.")
             "/categories/0-virgin-closures"
             nil))))))

(def default-wig-title
  "Human Hair Wigs: 100% Human Hair Wigs | Mayvenn")

(def default-wig-description
  (str "Mayvenn’s virgin human hair wigs allow you to achieve a new "
       "look in minutes & come in different variations such as "
       "Brazilian, Malaysian, straight, & deep wave."))

(def wig-category-url
  "https://shop.mayvenn.com/categories/13-wigs")

(def ready-wig-category-url
  "https://shop.mayvenn.com/categories/25-ready-wear-wigs")

(def lace-front-wig-category-url
  "https://shop.mayvenn.com/categories/24-virgin-lace-front-wigs")

(def human-hair-bundles-category-url
  "https://shop.mayvenn.com/categories/27-human-hair-bundles")

(def hair-extensions-category-url
  "https://shop.mayvenn.com/categories/28-hair-extensions")

(deftest wig-page-seo
  (with-services {}
    (with-handler handler
      (testing "a wig page has a template"
        (testing "no options are selected we get generic description"
          (-> (mock/request :get wig-category-url)
              handler
              (validate-title-and-description-and-canonical default-wig-title
                                                            default-wig-description
                                                            "/categories/13-wigs"
                                                            nil)))

        (testing "when one family is selected,"
          (testing "uses that family's category seo (canonical uri, page-title, meta description)"
            (-> (mock/request :get (str wig-category-url
                                        "?family=lace-front-wigs"))
                handler
                (validate-title-and-description-and-canonical
                 "Lace Front Wigs: Virgin Lace Front Wigs | Mayvenn"
                 (str "Mayvenn’s human hair lace front wigs mimic a natural hairline and come "
                      "in different variations such as Brazilian, Malaysian, straight, and deep wave.")
                 "/categories/24-virgin-lace-front-wigs"
                 nil))))

        (testing "two different non-family facets are selected the wig interstitial seo template are used"
          (-> (mock/request :get (str wig-category-url
                                      "?origin=brazilian"
                                      "&texture=loose-wave"))
              handler
              (validate-title-and-description-and-canonical
               "Brazilian Loose Wave Wigs | Mayvenn"
               (str "Mayvenn’s Brazilian Loose Wave Wigs allow you to change up "
                    "and achieve your desired look. Shop our collection of virgin "
                    "hair wigs today.")
               "/categories/13-wigs"
               "origin=brazilian&texture=loose-wave")))

        (testing "two families are selected the default wig seo is used"
          (-> (mock/request :get (str wig-category-url
                                      "?family=lace-front-wigs~ready-wigs"))
              handler
              (validate-title-and-description-and-canonical default-wig-title
                                                            default-wig-description
                                                            "/categories/13-wigs"
                                                            "family=lace-front-wigs%7Eready-wigs")))

        (testing "three options are selected- one is a subcategory and the other two are general filter options"
          (-> (mock/request :get (str wig-category-url
                                      "?texture=loose-wave"
                                      "&origin=indian"
                                      "&family=lace-front-wigs"))
              handler
              (validate-title-and-description-and-canonical
               "Indian Loose Wave Virgin Lace Front Wigs | Mayvenn"
               (str "Mayvenn’s Indian Loose Wave Virgin Lace Front Wigs "
                    "allow you to change up and achieve your desired "
                    "look. Shop our collection of virgin hair wigs today.")
               "/categories/24-virgin-lace-front-wigs"
               "origin=indian&texture=loose-wave")))

        (testing "two options from the same facet are selected we get a generic description"
          (-> (mock/request :get (str wig-category-url
                                      "?origin=indian~brazilian"))
              handler
              (validate-title-and-description-and-canonical default-wig-title
                                                            default-wig-description
                                                            "/categories/13-wigs"
                                                            "origin=indian%7Ebrazilian")))

        (testing "two options from the same facet and another from another facet are selected we get a generic description"
          (-> (mock/request :get (str wig-category-url
                                      "?origin=indian~brazilian"
                                      "&texture=loose-wave"))
              handler
              (validate-title-and-description-and-canonical default-wig-title
                                                            default-wig-description
                                                            "/categories/13-wigs"
                                                            "origin=indian%7Ebrazilian&texture=loose-wave")))

        (testing "when 360-wigs is selected,"
          (testing "uses that family's category canonical uri"
            (-> (mock/request :get (str wig-category-url "?family=360-wigs"))
                handler
                (validate-title-and-description-and-canonical
                 "360 Lace Wigs: Virgin 360 Lace Frontal Wigs | Mayvenn"
                 (str "Mayvenn’s human hair 360 lace wigs give you all around protection and "
                      "come in different variations such as Brazilian, Malaysian, straight, and deep wave.")
                 "/categories/26-virgin-360-wigs"
                 nil))))

        (testing "when ready-wigs is selected,"
          (testing "uses that family's category canonical uri"
            (-> (mock/request :get (str wig-category-url "?family=ready-wigs"))
                handler
                (validate-title-and-description-and-canonical
                 "Ready-to-Wear Wigs: Short, Bob, Side-Part & More | Mayvenn"
                 (str "Mayvenn’s ready-to-wear human hair lace wigs provide a quick style "
                      "switch-up and come in different variations such as Brazilian, straight, and loose wave.")
                 "/categories/25-ready-wear-wigs"
                 nil))))))))

(deftest hair-extensions-category-seo
  (with-services {}
    (with-handler handler
      (testing "hair-extensions"
        (-> (mock/request :get (str hair-extensions-category-url "?color=%231-jet-black"))
            handler
            (validate-title-and-description-and-canonical
             "#1 Jet Black Virgin Hair Extensions | Mayvenn"
             (str "Get the hair of your dreams with our #1 Jet Black Hair Extensions."
                  " Featuring a thin, polyurethane weft that flawlessly blends with your own hair.")
             "/categories/28-hair-extensions"
             "color=%231-jet-black"))))))

#_(deftest human-hair-bundles-and-description-templates
  (with-services {}
    (with-handler handler
      (testing "If there is a texture facet applied, change the canonical to the texture page"
        (-> (mock/request :get (str human-hair-bundles-category-url
                                    "?texture=loose-wave"))
            handler
            (validate-title-and-description-and-canonical
             "Loose Wave Extensions | Mayvenn"
             "Mayvenn’s Brazilian, Peruvian and Indian loose wave bundles. Also includes loose wave lace closures. All are 100% virgin Loose Wave hair."
             "/categories/6-virgin-loose-wave"
             "family=bundles")))
      (testing "If only origin and/or color facets are applied, change the metadata but keep the url" ; PASS
        (-> (mock/request :get (str human-hair-bundles-category-url
                                    "?origin=indian"
                                    "&color=%232-chocolate-brown"))
            handler
            (validate-title-and-description-and-canonical
             "Indian #2 Chocolate Brown Human Virgin Hair Bundles | Mayvenn"
             "Mayvenn's Indian #2 Chocolate Brown human Virgin Hair Bundles are machine-wefted and made with virgin hair for unbeatable quality. Shop to achieve your desired look!"
             "/categories/27-human-hair-bundles"
             "origin=indian&color=%232-chocolate-brown"))))))

(deftest category-page-facet-url-conversion
  (with-services {}
    (with-handler handler
      (testing "on category pages, url parameters are translateable to facets"
        (-> (mock/request :get (str ready-wig-category-url
                                    "?texture=straight&style=center-part"))
            handler
            (validate-title-and-description-and-canonical
             "Center Part Straight Ready-to-Wear Wigs | Mayvenn"
             (str "Mayvenn’s Center Part Straight Ready-to-Wear Wigs "
                  "allow you to change up and achieve your desired look. "
                  "Shop our collection of virgin hair wigs today.")
             "/categories/25-ready-wear-wigs"
             "style=center-part&texture=straight"))))))
