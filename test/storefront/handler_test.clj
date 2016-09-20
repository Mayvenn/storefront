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

(def test-overrides {:environment "test"
                     :server-opts {:port 2390}
                     :logging (constantly nil)
                     :storeback {:endpoint "http://localhost:4334/"
                                 :internal-endpoint "http://localhost:4334/"}})

(def storeback-no-stylist-response
  (-> (response "{}")
      (status 404)
      (content-type "application/json")))

(def storeback-shop-response
  (-> (generate-string {:store_slug "shop"
                        :store_name "Mayvenn Hair"
                        :instagram_account nil
                        :profile_picture_url nil
                        :stylist_id 1})
      (response)
      (status 200)
      (content-type "application/json")))

(def storeback-stylist-response
  (-> (generate-string {:store_slug "bob"
                        :store_name "Bob's Hair Emporium"
                        :instagram_account nil
                        :profile_picture_url nil
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

(def default-req-params {:server-name "welcome.mayvenn.com"
                         :server-port 8080
                         :uri "/"
                         :scheme :http
                         :request-method :get})

(defn assert-request [req storeback-resp asserter]
  (let [[get-requests endpoint]
        (recording-endpoint {:handler (constantly storeback-resp)})]
    (with-standalone-server [ss (standalone-server endpoint)]
      (with-handler handler
        (asserter (handler (merge default-req-params req)))))))

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

(deftest redirects-blonde-category-to-categories
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/blonde")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 302 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/categories"
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

(deftest submits-paypal-redirect-to-waiter
  (testing "when waiter returns a 200 response"
    (let [[waiter-requests waiter-handler] (recording-endpoint
                                            {:handler (constantly {:status  200
                                                                   :headers {"Content-Type" "application/json"}
                                                                   :body    "{}"})})]
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
              (is (= 1 (count (waiter-requests))))
              (is (= {:utm-source   "source"
                      :utm-campaign "campaign"
                      :utm-term     "term"
                      :utm-content  "content"
                      :utm-medium   "medium"}
                     (-> (waiter-requests) first :body (parse-string true) :utm-params)))))))))

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
