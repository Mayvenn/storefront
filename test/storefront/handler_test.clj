(ns storefront.handler-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.test :refer [are deftest is testing]]
            [compojure.core :refer [GET POST routes]]
            [lambdaisland.uri :as uri]
            [ring.mock.request :as mock]
            [ring.util.codec :as codec]
            [ring.util.response :refer [content-type response status]]
            [standalone-test-server.core
             :refer
             [txfm-request txfm-requests with-requests-chan]]
            [storefront.handler-test.common
             :as
             common
             :refer
             [with-handler with-services]]
            [ajax.json :as json]))

(defn set-cookies [req cookies]
  (update req :headers assoc "cookie" (string/join "; " (map (fn [[k v]] (str k "=" v)) cookies))))

(def storeback-one-time-login-response
  (-> (generate-string {:user  {:email "acceptance+bob@mayvenn.com"
                                :id    3
                                :token "USERTOKEN"}
                        :order {:number "W123456"
                                :token  "ORDERTOKEN"}})
      (response)
      (status 200)
      (content-type "application/json")))

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

(deftest one-time-login-sets-cookies
  (with-services {:storeback-handler (routes
                                      (GET "/store" _ common/storeback-stylist-response)
                                      (POST "/v2/one-time-login" _ storeback-one-time-login-response))}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/one-time-login?token=USERTOKEN&user-id=1&sha=FIRST&target=%2F"))
            cookies (get-in resp [:headers "Set-Cookie"])
            location (get-in resp [:headers "Location"])]
        (testing "It removes one-time-login params, but keeps other query params in the url it redirects to"
          (is-redirected-to resp "bob" "/?sha=FIRST"))
        (testing "It assigns cookies to the client to automatically log them into storefront frontend"
          (is (some #{"user-token=USERTOKEN;Max-Age=2592000;Secure;Path=/;Domain=bob.mayvenn.com"} cookies))
          (is (some #{(format "email=%s;Max-Age=2592000;Secure;Path=/;Domain=bob.mayvenn.com"
                              (ring.util.codec/form-encode "acceptance+bob@mayvenn.com"))}
                    cookies)))))))

(deftest install-path-redirects-to-freeinstall
  (with-services
    (with-handler handler
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/install"))]
        (testing "It redirects to the freeinstall subdomain"
          (is-redirected-to resp "freeinstall" "/"))))))

(deftest renders-page-when-matches-stylist-subdomain-and-sets-the-preferred-subdomain
  (common/assert-request
   (mock/request :get "https://bob.mayvenn.com")
   common/storeback-stylist-response
   (fn [resp]
     (is (= 200 (:status resp)))
     (is (.contains (first (get-in resp [:headers "Set-Cookie"])) "preferred-store-slug=bob;")))))

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

(deftest server-side-renders-product-details-page
  (testing "when the product does not exist storefront returns 404"
    (let [[storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      (GET "/v2/orders/:number" req {:status 404
                                                                                                     :body   "{}"})
                                                                      (GET "/v2/products" req
                                                                           {:status 200
                                                                            :body   (generate-string {:products []})})
                                                                      (GET "/store" req common/storeback-stylist-response)))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/99-red-balloons"))]
            (is (= 404 (:status resp))))))))
  (testing "when whitelisted for discontinued"
    (let [[storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      (GET "/store" req common/storeback-stylist-response)
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
                               (GET "/store" req
                                    common/storeback-stylist-response)))]
      (with-services {:storeback-handler storeback-handler}
        (with-handler handler
          (has-canonized-link handler "//bob.mayvenn.com/products/67-peruvian-water-wave-lace-closures?SKU=PWWLC10")
          (has-canonized-link handler "//bob.mayvenn.com/categories/10-virgin-360-frontals")
          (has-canonized-link handler "//bob.mayvenn.com/our-hair")
          (has-canonized-link handler "//bob.mayvenn.com/"))))))

(deftest server-side-fetching-of-orders
  (testing "storefront retrieves an order from storeback"
    (let [number                                 "W123456"
          token                                  "iA1bjIUAqCfyS3cuvdNYindmlRZ3ICr3g+vSfzvUM1c="
          [storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      (GET "/v2/orders/:number" req {:status 200
                                                                                                     ;; TODO: fixme
                                                                                                     :body   "{\"number\": \"W123456\"}"})
                                                                      (GET "/store" req common/storeback-stylist-response)))]
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
          "Disallow: /content")))))

(deftest robots-allows-all-pages-on-stylist-stores
  (with-services {}
    (with-handler handler
      (let [{:keys [status body]} (handler (mock/request :get "https://bob.mayvenn.com/robots.txt"))]
        (is (= 200 status))
        (is (not-any? #{"Disallow: /"} (string/split-lines body)) body)))))

(deftest fetches-data-from-contentful
  (let [number-of-contentful-entities-to-fetch 3]
    (testing "caching content"
      (let [[storeback-requests storeback-handler]   (with-requests-chan (GET "/store" req common/storeback-stylist-response))
            [contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                              {:status 200
                                                                               :body   (generate-string (:body common/contentful-response))}))]
        (with-services {:storeback-handler  storeback-handler
                        :contentful-handler contentful-handler}
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
      (let [[storeback-requests storeback-handler]   (with-requests-chan (GET "/store" req common/storeback-stylist-response))
            [contentful-requests contentful-handler] (with-requests-chan (GET "/spaces/fake-space-id/entries" req
                                                                              {:status 500
                                                                               :body   "{}"}))]
        (with-services {:storeback-handler  storeback-handler
                        :contentful-handler contentful-handler}
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
                         handler)]
            (is (= 200 (:status resp))
                (get-in resp [:headers "Location"]))
            (is (= 1 (count (txfm-requests storeback-requests identity))))))))))
