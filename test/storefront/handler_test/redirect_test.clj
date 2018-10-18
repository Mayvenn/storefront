(ns storefront.handler-test.redirect-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [routes GET POST]]
            [ring.mock.request :as mock]
            [standalone-test-server.core :refer [with-requests-chan txfm-request txfm-requests]]
            [storefront.handler-test :refer [ set-cookies ]]
            [ring.util.response :refer [content-type response status]]
            [storefront.handler-test.common :as common
             :refer [with-services with-handler assert-request storeback-shop-response]]))

(def realistic-order-token "2OkDy7X95jblOjaTEZHNDPQm+qkz4ucv3ukjfohQLiI=")

(def storeback-partially-fake-order-handler-response
  (-> (generate-string {:number "W123456"
                        :token realistic-order-token})
      (response)
      (status 200)
      (content-type "application/json")))

(deftest redirect-from-shop-to-preferred-store-adds-redirect-query-param
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com")
                      (mock/header "cookie" "preferred-store-slug=bob"))
                  common/storeback-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://bob.mayvenn.com/?redirect=shop"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-shop-to-store-subdomain-if-preferred-subdomain-is-invalid
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=non-existent-stylist"))
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/categories/hair/straight?utm_source=cats"
                           (get-in resp [:headers "Location"])))
                    (let [cookie (first (get-in resp [:headers "Set-Cookie"]))]
                      (is (.contains cookie "preferred-store-slug=;Max-Age=0;") (str cookie))))))

(deftest redirects-shop-to-preferred-subdomain-preserving-path-and-query-strings
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=bob"))
                  storeback-shop-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://bob.mayvenn.com/categories/hair/straight?utm_source=cats&redirect=shop"
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

(deftest redirects-old-categories-to-new-categories
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/straight")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/categories/2-virgin-straight"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-legacy-products-to-new-products
  (let [[storeback-requests storeback-handler]
        (with-requests-chan (routes
                             (GET "/v2/orders/:number" req {:status 404
                                                            :body   "{}"})
                             (GET "/v2/products" req
                                  {:status 200
                                   :body   (generate-string {:products [{:catalog/product-id "33"
                                                                         :page/slug          "brazilian-loose-wave-lace-closures"}]})})
                             (GET "/store" req common/storeback-stylist-response)))]
    (with-services {:storeback-handler storeback-handler}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/brazilian-loose-wave-lace-closure"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/products/33-brazilian-loose-wave-lace-closures"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-no-subdomain-to-shop-preserving-query-params
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://mayvenn.com/?world=true"))]
        (is (= 301 (:status resp)))
        (is (= "https://shop.mayvenn.com/?world=true"
               (get-in resp [:headers "Location"])))))))

(deftest redirects-www-to-shop-preserving-query-params
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://www.mayvenn.com/?world=true"))]
        (is (= 301 (:status resp)))
        (is (= "https://shop.mayvenn.com/?world=true"
               (get-in resp [:headers "Location"])))))))

(deftest redirects-www-prefixed-stylists-to-stylist-without-prefix
  (assert-request (mock/request :get "https://www.bob.mayvenn.com")
                  common/storeback-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)) (pr-str resp))
                    (is (= "https://bob.mayvenn.com/"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-www-prefixed-unreal-stylists-to-stylist-without-prefix
  (assert-request (mock/request :get "https://www.not-a-real-store-slug.mayvenn.com")
                  common/storeback-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)) (pr-str resp))
                    (is (= "https://not-a-real-store-slug.mayvenn.com/"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-missing-stylists-to-store-while-preserving-query-params
  (assert-request (mock/request :get "https://no-stylist.mayvenn.com/?yo=lo&mo=fo")
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/?yo=lo&mo=fo"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-to-https-preserving-query-params
  (testing "bob.mayvenn.com"
    (assert-request (mock/request :get "http://bob.mayvenn.com")
                    common/storeback-stylist-response
                    (fn [resp]
                      (is (= 301 (:status resp)) (pr-str resp))
                      (is (= "https://bob.mayvenn.com/"
                             (get-in resp [:headers "Location"]))))))

  (testing "no www-prefix stylist"
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "http://no-stylist.mayvenn.com/?yo=lo&mo=fo"))]
          (is (= 301 (:status resp)))
          (is (= "https://no-stylist.mayvenn.com/?yo=lo&mo=fo"
                 (get-in resp [:headers "Location"])))))))

  (testing "www-prefix stylist doesn't redirect to https://www.bob.mayvenn.com - because we don't have a wildcard ssl cert for multiple subdomains"
    (assert-request (mock/request :get "http://www.bob.mayvenn.com/?yo=lo&mo=fo")
                    common/storeback-stylist-response
                    (fn [resp]
                      (is (= 302 (:status resp)))
                      (is (not= "https://www.bob.mayvenn.com/?yo=lo&mo=fo"
                                (get-in resp [:headers "Location"])))
                      (is (= "https://bob.mayvenn.com/?yo=lo&mo=fo"
                             (get-in resp [:headers "Location"])))))))

(deftest redirects-requests-without-store-slug-to-mayvenn-made-page
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
        (let [resp     (-> (mock/request :get "https://mayvenn.com/mayvenn-made")
                           (set-cookies {"number"  "W123456"
                                         "token"   "order-token"
                                         "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                           handler)
              location (get-in resp [:headers "Location"])]
          (is (= 301 (:status resp)) location)
          (is (= "https://shop.mayvenn.com/mayvenn-made" location) location)
          (is (= 1 (count (txfm-requests storeback-requests identity)))))))))

(deftest redirects-classes
  (with-services {}
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
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-vistaprint
  (with-services {}
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
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-the-literal-stylist-subdomain-to-community
  (with-services {}
    (with-handler handler
      (testing "http"
        (let [resp (handler (mock/request :get "http://stylist.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://community.mayvenn.com"
                 (get-in resp [:headers "Location"])))))

      (testing "https"
        (let [resp (handler (mock/request :get "https://stylist.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://community.mayvenn.com"
                 (get-in resp [:headers "Location"]))))))))

(deftest create-shared-cart-from-url-redirects-to-cart-preserving-query-params
  (with-services {:storeback-handler (routes
                                      (GET "/store" _ common/storeback-stylist-response)
                                      (POST "/create-order-from-shared-cart" _ storeback-partially-fake-order-handler-response))}
    (with-handler handler
      (let [resp     (handler (mock/request :get "https://bob.mayvenn.com/create-cart-from/my-shared-cart-id?utm_content=test"))
            cookies  (get-in resp [:headers "Set-Cookie"])
            location (get-in resp [:headers "Location"])]
        (is (= "https://bob.mayvenn.com/cart?utm_content=test" location))
        (testing "It assigns cookies to the client to automatically associate the created order"
          (is (some #{"number=W123456;Max-Age=2419200;Secure;Path=/"} cookies))
          (is (some #{(format "token=%s;Max-Age=2419200;Secure;Path=/"
                              (ring.util.codec/form-encode realistic-order-token))} cookies)))))))

(deftest submits-paypal-redirect-to-waiter
  (testing "when waiter knows the order"
    (with-services {:storeback-handler (constantly {:status  200
                                                    :headers {"Content-Type" "application/json"}
                                                    :body    (generate-string {:number "W123456"
                                                                               :token  "order-token"
                                                                               :state  "cart"})})}
      (with-handler handler
        (let [resp (-> (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token")
                       (set-cookies {"number"  "W123456"
                                     "token"   "order-token"
                                     "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                       handler)]
          (is (= 302 (:status resp)))
          (testing "it redirects to order processing page"
            (is (= "/checkout/processing"
                   (get-in resp [:headers "Location"]))))))))

  (testing "when waiter does not recognize the order"
    (with-services {:storeback-handler (constantly {:status  404
                                                    :headers {"Content-Type" "application/json"}
                                                    :body    (generate-string {})})}
      (with-handler handler
        (let [resp (-> (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token")
                       (set-cookies {"number"  "W123456"
                                     "token"   "order-token"
                                     "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                       handler)]
          (is (= 302 (:status resp)))
          (is (= "/cart?error=bad-request"
                 (get-in resp [:headers "Location"])))))))
  (testing "when cookies and order token from paypal do not match"
    (with-services {:storeback-handler (constantly {:status  200
                                                    :headers {"Content-Type" "application/json"}
                                                    :body    (generate-string {:number "W123456"
                                                                               :token  "order-token"
                                                                               :state  "cart"})})}
      (with-handler handler
        (let [resp (-> (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token-v2")
                       (set-cookies {"number"  "W123456"
                                     "token"   "order-token"
                                     "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                       handler)]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "/cart?error=bad-request"
                 (get-in resp [:headers "Location"])))))))
  (testing "when cookies and order number from paypal do not match"
    (with-services {:storeback-handler (constantly {:status  200
                                                    :headers {"Content-Type" "application/json"}
                                                    :body    (generate-string {:number "W123456"
                                                                               :token  "order-token"
                                                                               :state  "cart"})})}
      (with-handler handler
        (let [resp (-> (mock/request :get "https://shop.mayvenn.com/orders/W234567/paypal/order-token")
                       (set-cookies {"number"  "W123456"
                                     "token"   "order-token"
                                     "expires" "Sat, 03 May 2025 17:44:22 GMT"})
                       handler)]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "/cart?error=bad-request"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-product-details-sku-to-sku-in-query-params
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/12-indian-straight-bundles/INSDB14"))]
        (is (= 301 (:status resp)))
        (is (= "/products/12-indian-straight-bundles?SKU=INSDB14"
               (get-in resp [:headers "Location"])))))))
