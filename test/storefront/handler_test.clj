(ns storefront.handler-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure
             [string :as string]
             [test :refer :all]]
            [com.stuartsierra.component :as component]
            [ring.mock.request :as mock]
            [ring.util
             [codec :as codec]
             [response :refer [content-type response status]]]
            [standalone-test-server.core :refer :all]
            [storefront
             [handler :refer :all]
             [system :refer [create-system]]]))

(def test-overrides {:environment      "test"
                     :server-opts      {:port 2390}
                     :logging          (constantly nil)
                     :dc-logo-config   {:endpoint "https://logo.server/?m=100"}
                     :storeback-config {:endpoint          "http://localhost:4334/"
                                        :internal-endpoint "http://localhost:4334/"}})

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

(def storeback-stylist-response
  (-> (generate-string {:store_slug "bob"
                        :store_name "Bob's Hair Emporium"
                        :instagram_account nil
                        :stylist_id 3})
      (response)
      (status 200)
      (content-type "application/json")))

(def omakase-create-order-response
  (-> (generate-string {:updated-at 1469492961613
                        :tax-total 0.0
                        :number "W231894629"
                        :stylist-ids [3]
                        :state "cart"
                        :rma-total 0.0
                        :payments-total 0.0
                        :shipments [{:number "S1"
                                     :state "pending"
                                     :line-items [{:source "waiter"
                                                   :product-name "Shipping"
                                                   :unit-price 0.0
                                                   :sku "WAITER-SHIPPING-1"
                                                   :unit-taxable-amount 0.0
                                                   :id -1
                                                   :quantity 1
                                                   :product-id 0}
                                                  {:applied-promotions []
                                                   :source "spree"
                                                   :product-name "Brazilian Natural Straight Hair"
                                                   :unit-price 55.0
                                                   :sku "BNS10"
                                                   :unit-taxable-amount 55.0
                                                   :variant-attrs {:category "hair"
                                                                   :color "dark-gray"
                                                                   :grade "6a premier collection"
                                                                   :origin "brazilian"
                                                                   :style "straight"
                                                                   :length "10\""}
                                                   :id 479
                                                   :quantity 1
                                                   :product-id 13}]
                                     :created-at 1469492961611
                                     :updated-at 1469492961613}]
                        :adjustments []
                        :token "DMmy3ey8sfO07myJt833hfVhJPe1LD6o591K+cdbCyU="
                        :total 55.0
                        :total-debited-amount 0.0
                        :total-store-credit-used 0.0
                        :payment-state "balance-due"
                        :line-items-total 55.0
                        :returnable-line-items []
                        :promotion-discount 0.0
                        :created-at 1469492961599
                        :returns []})
      (response)
      (status 200)
      (content-type "application/json")))

(defn parsed-url [url]
  (let [[base query] (.split (str url) "\\?")]
    [base (codec/form-decode query)]))

(defmacro with-resource
  [bindings close-fn & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (~close-fn ~(bindings 0))))))

(defmacro with-handler
  [handler & body]
  `(let [unstarted-system# (create-system test-overrides)]
     (with-resource [sys# (component/start unstarted-system#)
                     ~handler (-> sys# :app-handler :handler)]
       component/stop
       ~@body)))

(defn parse-json-body [req]
  (update req :body #(parse-string % true)))

(defn first-json-request
  ([requests] (first-json-request requests conj))
  ([requests xf]
   (txfm-request requests
                 (comp (map parse-json-body) xf)
                 {:timeout 1000})))

(def default-req-params {:server-port 8080
                         :uri "/"
                         :scheme :http
                         :request-method :get})

(defn assert-request [req storeback-resp asserter]
  (with-standalone-server [ss (standalone-server (constantly storeback-resp))]
    (with-handler handler
      (asserter (handler (merge default-req-params req))))))

(deftest redirects-to-https-preserving-query-params
  (testing "mayvenn.com"
    (with-handler handler
      (let [resp (handler (mock/request :get "http://mayvenn.com"))]
        (is (= 302 (:status resp)))
        (is (= "https://shop.mayvenn.com/"
               (get-in resp [:headers "Location"]))))))

  (testing "no www-prefix stylist"
    (with-handler handler
      (let [resp (handler (mock/request :get "http://no-stylist.mayvenn.com/?yo=lo&mo=fo"))]
        (is (= 301 (:status resp)))
        (is (= "https://no-stylist.mayvenn.com/?yo=lo&mo=fo"
               (get-in resp [:headers "Location"]))))))

  (testing "www-prefix stylist doesn't redirect to https://www.bob.mayvenn.com - because we don't have a wildcard ssl cert for multiple subdomains"
    (assert-request (mock/request :get "http://www.bob.mayvenn.com/?yo=lo&mo=fo")
                    storeback-stylist-response
                    (fn [resp]
                      (is (= 302 (:status resp)))
                      (is (not= "https://www.bob.mayvenn.com/?yo=lo&mo=fo"
                                (get-in resp [:headers "Location"])))
                      (is (= "https://bob.mayvenn.com/?yo=lo&mo=fo"
                             (get-in resp [:headers "Location"])))))))

(deftest redirects-missing-stylists-to-store-while-preserving-query-params
  (assert-request (mock/request :get "https://no-stylist.mayvenn.com/?yo=lo&mo=fo")
                  storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/?yo=lo&mo=fo"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-www-prefixed-stylists-to-stylist-without-prefix
  (assert-request (mock/request :get "https://www.bob.mayvenn.com")
                  storeback-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)) (pr-str resp))
                    (is (= "https://bob.mayvenn.com/"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-www-to-shop-preserving-query-params
  (with-handler handler
    (let [resp (handler (mock/request :get "https://www.mayvenn.com/?world=true"))]
      (is (= 302 (:status resp)))
      (is (= "https://shop.mayvenn.com/?world=true"
             (get-in resp [:headers "Location"]))))))

(deftest redirects-no-subdomain-to-shop-preserving-query-params
  (with-handler handler
    (let [resp (handler (mock/request :get "https://mayvenn.com/?world=true"))]
      (is (= 302 (:status resp)))
      (is (= "https://shop.mayvenn.com/?world=true"
             (get-in resp [:headers "Location"]))))))

(deftest redirects-old-categories-to-new-categories
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/straight")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/categories/2-straight"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-blonde-category-to-home
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/blonde")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-categories-to-home
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories?utm_source=cats")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/?utm_source=cats"
                           (get-in resp [:headers "Location"])))))

  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/?utm_source=cats")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/?utm_source=cats"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-products-to-home
  (assert-request (mock/request :get "https://shop.mayvenn.com/products?utm_source=cats")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/?utm_source=cats"
                           (get-in resp [:headers "Location"])))))
  (assert-request (mock/request :get "https://shop.mayvenn.com/products/?utm_source=cats")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/?utm_source=cats"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-shop-to-preferred-subdomain-preserving-path-and-query-strings
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=bob"))
                  storeback-shop-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://bob.mayvenn.com/categories/hair/straight?utm_source=cats"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-shop-to-store-subdomain-if-preferred-subdomain-is-invalid
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=non-existent-stylist"))
                  storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/categories/hair/straight?utm_source=cats"
                           (get-in resp [:headers "Location"])))
                    (let [cookie (first (get-in resp [:headers "Set-Cookie"]))]
                      (is (.contains cookie "preferred-store-slug=;Max-Age=0;") (str cookie))))))

(deftest redirects-classes
  (with-handler handler
    (testing "http"
      (let [resp (handler (mock/request :get "http://classes.mayvenn.com"))]
        (is (= 302 (:status resp)))
        (is (= "https://docs.google.com/a/mayvenn.com/forms/d/e/1FAIpQLSdpA5Kvl8hhI5TkPRGwWLyFcWLtUpRyQksrbA-cikQvTXekwQ/viewform"
               (get-in resp [:headers "Location"])))))

    (testing "https"
      (let [resp (handler (mock/request :get "https://classes.mayvenn.com"))]
        (is (= 302 (:status resp)))
        (is (= "https://docs.google.com/a/mayvenn.com/forms/d/e/1FAIpQLSdpA5Kvl8hhI5TkPRGwWLyFcWLtUpRyQksrbA-cikQvTXekwQ/viewform"
               (get-in resp [:headers "Location"])))))))

(deftest redirects-vistaprint
  (with-handler handler
    (testing "http"
      (let [resp (handler (mock/request :get "http://vistaprint.mayvenn.com"))]
        (is (= 302 (:status resp)))
        (is (= "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262"
               (get-in resp [:headers "Location"])))))

    (testing "https"
      (let [resp (handler (mock/request :get "https://vistaprint.mayvenn.com"))]
        (is (= 302 (:status resp)))
        (is (= "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262"
               (get-in resp [:headers "Location"])))))))

(deftest redirects-to-logo-server
  (with-handler handler
    (testing "http"
      (let [resp (handler (mock/request :get "http://bob.mayvenn.com/logo.htm"))]
        (is (= 302 (:status resp)))
        (is (= "https://logo.server/?m=100&s=missing-session-id"
               (get-in resp [:headers "Location"])))))
    (testing "https"
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/logo.htm"))]
        (is (= 302 (:status resp)))
        (is (= "https://logo.server/?m=100&s=missing-session-id"
               (get-in resp [:headers "Location"])))))
    (testing "with session id"
      (let [resp (handler (-> (mock/request :get "https://bob.mayvenn.com/logo.htm")
                              (mock/header "Cookie" ":session-id=present-id")))]
        (is (= 302 (:status resp)))
        (is (= "https://logo.server/?m=100&s=present-id"
               (get-in resp [:headers "Location"])))))))

(deftest handles-welcome-subdomain
  (with-handler handler
    (testing "will not render leads pages on stylist site"
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/stylists/welcome"))]
       (is (= 404 (:status resp)))))
    (testing "welcome.mayvenn.com/ redirects to welcome.mayvenn.com/stylists/welcome, preserving query params"
      (let [resp (handler (mock/request :get "https://welcome.mayvenn.com?a=b"))]
        (is (= 301 (:status resp)))
        (is (= "/stylists/welcome?a=b"
               (get-in resp [:headers "Location"])))))
    (testing "preserves leads.tracking-id cookie"
      (let [resp (handler (-> (mock/request :get "https://welcome.mayvenn.com/stylists/welcome")
                              (mock/header "Cookie" "leads.tracking-id=present-id")))
            cookies (get-in resp [:headers "Set-Cookie"])]
        (is (= 200 (:status resp)))
        (is (some #{"leads.tracking-id=present-id;Max-Age=31536000;Secure;Path=/;Domain=.mayvenn.com"} cookies))))
    (testing "migrates tracking_id cookie from old leads site"
      (let [resp (handler (-> (mock/request :get "https://welcome.mayvenn.com/stylists/welcome")
                              (mock/header "Cookie" "tracking_id=old-id")))
            cookies (get-in resp [:headers "Set-Cookie"])]
        (is (= 200 (:status resp)))
        (is (some #{"leads.tracking-id=old-id;Max-Age=31536000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"tracking_id=;Max-Age=0;Secure;Path=/"} cookies))))
    (testing "the utm_params is passed to cljs application"
      (let [resp (handler (mock/request :get "https://welcome.mayvenn.com/stylists/welcome?utm_content=stylistsfb"))
            cookies (get-in resp [:headers "Set-Cookie"])]
        (is (= 200 (:status resp)))
        (is (.contains (:body resp) ":content \\\"stylistsfb\\\"")
            (pr-str (:body resp)))))
    (testing "migrates utm cookies from old leads site"
      (let [resp (handler (-> (mock/request :get "https://welcome.mayvenn.com/stylists/welcome")
                              (mock/header "Cookie" (string/join "; "
                                                                 ["utm_source=utm_source"
                                                                  "utm_medium=utm_medium"
                                                                  "utm_campaign=utm_campaign"
                                                                  "utm_content=utm_content"
                                                                  "utm_term=utm_term"]))))
            cookies (get-in resp [:headers "Set-Cookie"])]
        (is (= 200 (:status resp)))
        (is (some #{"leads.utm-source=utm_source;Max-Age=2592000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"leads.utm-medium=utm_medium;Max-Age=2592000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"leads.utm-campaign=utm_campaign;Max-Age=2592000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"leads.utm-content=utm_content;Max-Age=2592000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"leads.utm-term=utm_term;Max-Age=2592000;Secure;Path=/;Domain=.mayvenn.com"} cookies))
        (is (some #{"utm_source=;Max-Age=0;Secure;Path=/"} cookies))
        (is (some #{"utm_medium=;Max-Age=0;Secure;Path=/"} cookies))
        (is (some #{"utm_campaign=;Max-Age=0;Secure;Path=/"} cookies))
        (is (some #{"utm_content=;Max-Age=0;Secure;Path=/"} cookies))
        (is (some #{"utm_term=;Max-Age=0;Secure;Path=/"} cookies))))))

(deftest submits-paypal-redirect-to-waiter
  (testing "when waiter returns a 200 response"
    (let [[waiter-requests waiter-handler] (with-requests-chan
                                             (constantly {:status  200
                                                          :headers {"Content-Type" "application/json"}
                                                          :body    "{}"}))]
      (with-standalone-server [waiter (standalone-server waiter-handler)]
        (with-handler handler
          (let [set-cookies (fn [req cookies]
                              (update req :headers assoc "cookie" (string/join "; " (map (fn [[k v]] (str k "=" v)) cookies))))
                resp        (-> (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token")
                                (set-cookies {":storefront/utm-source"   "source"
                                              ":storefront/utm-campaign" "campaign"
                                              ":storefront/utm-term"     "term"
                                              ":storefront/utm-content"  "content"
                                              ":storefront/utm-medium"   "medium"
                                              "expires"                  "Sat, 03 May 2025 17:44:22 GMT"})
                                handler)]
            (is (= 302 (:status resp)))
            (testing "it redirects to order complete page"
              (is (= "/orders/W123456/complete?paypal=true&order-token=order-token"
                     (get-in resp [:headers "Location"]))))

            (testing "it records the utm params associated with the request"
              (is (= {:utm-source   "source"
                      :utm-campaign "campaign"
                      :utm-term     "term"
                      :utm-content  "content"
                      :utm-medium   "medium"}
                     (-> waiter-requests first-json-request :body :utm-params)))))))))

  (testing "when waiter returns a non-200 response without an error-code"
    (with-standalone-server [waiter (standalone-server (constantly {:status  500
                                                                    :headers {"Content-Type" "application/json"}
                                                                    :body    "{}"}))]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token"))]
          (is (= 302 (:status resp)))
          (is (= "/cart?error=paypal-incomplete"
                 (get-in resp [:headers "Location"])))))))

  (testing "when waiter returns a non-200 response with an error-code"
    (with-standalone-server [waiter (standalone-server (constantly {:status  400
                                                                    :headers {"Content-Type" "application/json"}
                                                                    :body    "{\"error-code\": \"bad-request\"}"}))]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token"))]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "/cart?error=bad-request"
                 (get-in resp [:headers "Location"]))))))))

(deftest renders-page-when-matches-stylist-subdomain-and-sets-the-preferred-subdomain
  (assert-request
   (mock/request :get "https://bob.mayvenn.com")
   storeback-stylist-response
   (fn [resp]
     (is (= 200 (:status resp)))
     (is (.contains (first (get-in resp [:headers "Set-Cookie"])) "preferred-store-slug=bob;")))))
