(ns storefront.handler-test.redirect-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [routes GET POST]]
            [ring.mock.request :as mock]
            [standalone-test-server.core :refer [with-requests-chan txfm-request txfm-requests]]
            [storefront.handler-test :refer [set-cookies]]
            [ring.util.response :refer [content-type response status]]
            storefront.handler
            [storefront.handler-test.common :as common
             :refer [with-services with-handler assert-request storeback-shop-response]]))

(def realistic-order-token "2OkDy7X95jblOjaTEZHNDPQm+qkz4ucv3ukjfohQLiI=")

(def storeback-partially-fake-order-handler-response
  (-> (generate-string {:number "W123456"
                        :token realistic-order-token})
      (response)
      (status 200)
      (content-type "application/json")))

(deftest preferred-store-slug-does-not-redirect-away-from-shop
  (assert-request
   (set-cookies (mock/request :get "https://shop.mayvenn.com")
                {"preferred-store-slug" "bob"})
   common/storeback-stylist-response
   (fn [resp]
     (is (not= 302 (:status resp)) (pr-str (:status resp))))))

(deftest redirects-shop-to-store-subdomain-if-preferred-subdomain-is-invalid
  (assert-request (-> (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=non-existent-stylist"))
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/categories/hair/straight?utm_source=cats&redirect=shop"
                           (get-in resp [:headers "Location"])))
                    (let [cookie (first (get-in resp [:headers "Set-Cookie"]))]
                      (is (.contains cookie "preferred-store-slug=;Max-Age=0;") (str cookie))))))

(deftest redirects-to-shop-with-redirect-params-of-invalid-store-if-preferred-store-is-invalid
  (assert-request (-> (mock/request :get "https://invalid-stylist.mayvenn.com/categories/hair/straight?utm_source=cats")
                      (mock/header "cookie" "preferred-store-slug=other-invalid-stylist"))
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/categories/hair/straight?utm_source=cats&redirect=invalid-stylist"
                           (get-in resp [:headers "Location"])))
                    (let [cookie (first (get-in resp [:headers "Set-Cookie"]))]
                      (is (.contains cookie "preferred-store-slug=;Max-Age=0;") (str cookie))))))

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

(deftest redirects-old-categories-to-new-categories-preserving-utm-and-affiliate-params
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/straight?utm_source=cats&affiliate_stylist_id=10")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/categories/2-virgin-straight?utm_source=cats&affiliate_stylist_id=10"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-legacy-products-to-new-products
  (let [[storeback-requests storeback-handler]
        (with-requests-chan (routes
                             (GET "/v2/orders/:number" req {:status 404
                                                            :body   "{}"})
                             (GET "/v3/products" req
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

(deftest redirects-missing-stylists-to-store-while-preserving-query-params-and-adding-redirect-param
  (assert-request (mock/request :get "https://no-stylist.mayvenn.com/?yo=lo&mo=fo")
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/?yo=lo&mo=fo&redirect=no-stylist"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-missing-stylists-to-store-with-redirect-param-even-if-preferred-store-set
  (assert-request (-> (mock/request :get "https://no-stylist.mayvenn.com/")
                      (mock/header "cookie" "preferred-store-slug=bob"))
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/?redirect=no-stylist"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-missing-stylists-to-store-and-overwrites-previous-redirect-param-if
  (assert-request (-> (mock/request :get "https://no-stylist.mayvenn.com/?redirect=shop")
                      (mock/header "cookie" "preferred-store-slug=bob"))
                  common/storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "https://store.mayvenn.com/?redirect=no-stylist"
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

(deftest redirects-classes
  (with-services {}
    (with-handler handler
      (testing "http"
        (let [resp (handler (mock/request :get "http://classes.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"])))))

      (testing "https"
        (let [resp (handler (mock/request :get "https://classes.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-vistaprint
  (with-services {}
    (with-handler handler
      (testing "http"
        (let [resp (handler (mock/request :get "http://vistaprint.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"])))))

      (testing "https"
        (let [resp (handler (mock/request :get "https://vistaprint.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-the-literal-stylist-subdomain
  (with-services {}
    (with-handler handler
      (testing "http"
        (let [resp (handler (mock/request :get "http://stylist.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"])))))

      (testing "https"
        (let [resp (handler (mock/request :get "https://stylist.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-community
  (with-services {}
    (with-handler handler
      (testing "http"
        (let [resp (handler (mock/request :get "http://community.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"])))))

      (testing "https"
        (let [resp (handler (mock/request :get "https://community.mayvenn.com"))]
          (is (= 301 (:status resp)))
          (is (= "https://shop.mayvenn.com/"
                 (get-in resp [:headers "Location"]))))))))

(deftest create-shared-cart-from-url-redirects-to-cart-preserving-query-params
  (with-services {:storeback-handler (routes
                                      common/default-storeback-handler
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

(deftest marketing-site-redirect
  (testing "should redirect to a whitelisted domain"
    (assert-request
     (->
      (mock/request :get "https://shop.mayvenn.com/marketing-site?to=looks.mayvenn.com/abc"))
     common/storeback-stylist-response
     (fn [resp]
       (is (= 302 (:status resp)))
       (is (= "https://looks.mayvenn.com/abc"
              (get-in resp [:headers "Location"]))))))
  (testing "should *not* redirect to a domain which is not whitelisted"
    (assert-request
     (->
      (mock/request :get "https://shop.mayvenn.com/marketing-site?to=https://reddit.com/r/emacs"))
     common/storeback-stylist-response
     (fn [resp]
       (is (= 302 (:status resp)))
       (is (= "https://shop.mayvenn.com"
              (get-in resp [:headers "Location"])))))))

(deftest redirects-stylist-profile-to-correct-store-slug-in-url
  (let [stylist-not-found-handler (GET "/v1/stylist/matched-by-id" req
                                    {:status 404
                                     :body   (generate-string {})})
        stylist-found-handler     (GET "/v1/stylist/matched-by-id" req
                                    {:status 200
                                     :body   (generate-string {:stylist {:stylist-id "9"
                                                                         :store-slug "thebestmatch"}})})]
    (testing "when loading the stylist profile page"
      (testing "when the stylist is found"
        (let [[_ storeback-handler]
              (with-requests-chan (routes common/default-storeback-handler stylist-found-handler))]
          (with-services {:storeback-handler storeback-handler}
            (with-handler handler
              (let [resp (handler (mock/request :get "https://shop.mayvenn.com/stylist/9-foo"))]
                (is (= 302 (:status resp)) (pr-str resp))
                (is (= "/stylist/9-thebestmatch"
                       (get-in resp [:headers "Location"]))))))))

      (testing "when the stylist is not found"
        (let [[_ storeback-handler]
              (with-requests-chan (routes common/default-storeback-handler stylist-not-found-handler))]
          (with-services {:storeback-handler storeback-handler}
            (with-handler handler
              (let [resp (handler (mock/request :get "https://shop.mayvenn.com/stylist/9-foo"))]
                (is (= 302 (:status resp)) (pr-str resp))
                (is (= "/adv/find-your-stylist?error=stylist-not-found"
                       (get-in resp [:headers "Location"])))))))))

    (testing "when loading the stylist profile gallery page"
      (testing "when the stylist is found"
        (let [[_ storeback-handler]
              (with-requests-chan (routes common/default-storeback-handler stylist-found-handler))]
          (with-services {:storeback-handler storeback-handler}
            (with-handler handler
              (let [resp (handler (mock/request :get "https://shop.mayvenn.com/stylist/9-foo/gallery"))]
                (is (= 302 (:status resp)) (pr-str resp))
                (is (= "/stylist/9-thebestmatch/gallery"
                       (get-in resp [:headers "Location"]))))))))

      (testing "when the stylist is not found"
        (let [[_ storeback-handler]
              (with-requests-chan (routes common/default-storeback-handler stylist-not-found-handler))]
          (with-services {:storeback-handler storeback-handler}
            (with-handler handler
              (let [resp (handler (mock/request :get "https://shop.mayvenn.com/stylist/9-foo/gallery"))]
                (is (= 302 (:status resp)) (pr-str resp))
                (is (= "/adv/find-your-stylist?error=stylist-not-found"
                       (get-in resp [:headers "Location"])))))))))))

(defn is-redirected-from-freeinstall-to-shop [source-path dest-path]
  (with-handler handler
    (let [source-path source-path
          resp        (handler (mock/request :get (str "https://freeinstall.mayvenn.com" source-path)))
          dest-path   dest-path]
      (testing (format "should redirect from freeinstall %s to shop %s" source-path dest-path)
        (is (= 301 (:status resp)))
        (is (= (str "https://shop.mayvenn.com" dest-path)
               (-> resp :headers (get "Location"))))))))

(deftest redirects-dyed-virgin-to-virgin-pdp
  (testing "When a request comes for a dyed virgin pdp, the user is redirected to the virgin pdp"
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/89-dyed-virgin-indian-straight-bundles"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/products/12-indian-straight-bundles" (get-in resp [:headers "Location"]))))))
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/94-dyed-virgin-straight-brazilian-bundles"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/products/9-brazilian-straight-bundles" (get-in resp [:headers "Location"]))))))))

(deftest redirects-dyed-virgin-categories-to-corresponding-virgin-categories
  (testing "When a request comes for a dyed virgin category, the user is redirected to the virgin category page"
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/categories/16-dyed-virgin-hair"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/categories/2-virgin-straight" (get-in resp [:headers "Location"]))))))
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/categories/17-dyed-virgin-closures"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/categories/0-virgin-closures" (get-in resp [:headers "Location"]))))))
    (with-services {}
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/categories/18-dyed-virgin-frontals"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/categories/1-virgin-frontals" (get-in resp [:headers "Location"])))))) ))

(deftest redirects-discontinued-product-to-category
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/59-indian-deep-wave-lace-frontals"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/categories/1-virgin-frontals" (get-in resp [:headers "Location"])))))))

(deftest redirects-deals-page-to-homepage
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://classic.mayvenn.com/shop/deals"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://classic.mayvenn.com/" (get-in resp [:headers "Location"])))))))

(deftest redirects-how-it-works-to-homepage
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://shop.mayvenn.com/how-it-works"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/" (get-in resp [:headers "Location"])))
        (is (= "max-age=604800" (get-in resp [:headers "cache-control"])))))))

(deftest redirects-mayvenn-made-to-homepage
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://shop.mayvenn.com/mayvenn-made"))]
        (is (= 302 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/" (get-in resp [:headers "Location"])))))))

(deftest classic-and-aladdin-standalone-service-pages-redirect-redirect-to-shop-adventure-flow
  (with-services {:storeback-handler (routes (GET "/v3/products" req {:status 200
                                                                      :body
                                                                      (generate-string
                                                                       {:products
                                                                        [{:legacy/product-id                  214,
                                                                          :promo.mayvenn-install/eligible     [false],
                                                                          :copy/description
                                                                          ["Sometimes you need a stylist’s touch to maintain your hair between install appointments. This includes tightening tracks and light styling."],
                                                                          :selector/electives                 [],
                                                                          :catalog/released-at                "2020-04-22T00:00:00.000Z",
                                                                          :catalog/discontinued-at            nil,
                                                                          :catalog/sku-id                     ["SRV-WMBI-000"],
                                                                          :selector/essentials
                                                                          ["catalog/department"
                                                                           "service/type"
                                                                           "catalog/sku-id"
                                                                           "promo.mayvenn-install/discountable"],
                                                                          :service/type                       ["base"],
                                                                          :page/slug                          "weave-maintenance-service",
                                                                          :catalog/product-id                 "132",
                                                                          :promo.triple-bundle/eligible       false,
                                                                          :selector/sku-ids                   ["SRV-WMBI-000"],
                                                                          :page.meta/description
                                                                          "Find a sew-in weave maintenance hair stylist at a salon near you. Learn about this service by a Mayvenn-certified stylist and book an appointment today!",
                                                                          :promo.mayvenn-install/discountable [false],
                                                                          :legacy/product-name                "Weave Maintenance",
                                                                          :catalog/launched-at                "2020-04-22T00:00:00.000Z",
                                                                          :selector/skus                      ["SRV-WMBI-000"],
                                                                          :catalog/department                 ["service"],
                                                                          :copy/title                         "Weave Maintenance",
                                                                          :copy/duration                      "2 hours",
                                                                          :page/title                         "Weave Maintenance Service - Book Salon Appointment | Mayvenn",
                                                                          :copy/whats-included                "Shampoo, Condition, Tighten tracks, Style"}],
                                                                        :skus {"SRV-WMBI-000" {:legacy/product-id                            214,
                                                                                               :promo.mayvenn-install/eligible               [false],
                                                                                               :sku/title                                    "Weave Maintenance",
                                                                                               :inventory/in-stock?                          true,
                                                                                               :selector/electives                           [],
                                                                                               :catalog/released-at                          "2020-04-22T00:00:00.000Z",
                                                                                               :selector/from-products                       ["132"],
                                                                                               :catalog/discontinued-at                      nil,
                                                                                               :sku/name                                     "Weave Maintenance",
                                                                                               :catalog/sku-id                               "SRV-WMBI-000",
                                                                                               :catalog/stylist-only?                        false,
                                                                                               :selector/essentials
                                                                                               ["catalog/department"
                                                                                                "service/type"
                                                                                                "promo.mayvenn-install/discountable"
                                                                                                "temporary-placeholder.shim/unique-identifier"],
                                                                                               :service/type                                 ["base"],
                                                                                               :promo.triple-bundle/eligible                 false,
                                                                                               :legacy/variant-id                            1050,
                                                                                               :promo.mayvenn-install/discountable           [false],
                                                                                               :temporary-placeholder.shim/unique-identifier ["SRV-WMBI-000"],
                                                                                               :legacy/product-name                          "Weave Maintenance",
                                                                                               :catalog/launched-at                          "2020-04-22T00:00:00.000Z",
                                                                                               :catalog/department                           ["service"],
                                                                                               :catalog/discontinued?                        false,
                                                                                               :sku/price                                    65.0}}})})
                                             common/default-storeback-handler)}
    (with-handler handler
      (testing "À la carte services category"
        (let [resp (handler (mock/request :get "https://classic.mayvenn.com/categories/35-a-la-carte-salon-services"))]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/adv/find-your-stylist" (get-in resp [:headers "Location"])))))
      (testing "À la carte service PDP"
        (let [resp (handler (mock/request :get "https://jasmine.mayvenn.com/products/132-weave-maintenance-service"))]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/adv/find-your-stylist" (get-in resp [:headers "Location"])))))
      (testing "on shop, à la carte PDP"
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/132-weave-maintenance-service"))]
          (is (= 302 (:status resp)) (pr-str resp)))))))

(deftest redirects-cart-interstitial-to-cart
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://shop.mayvenn.com/added-to-cart"))]
        (is (= 302 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/cart" (get-in resp [:headers "Location"])))))))

(deftest aladdin-subpaths-redirect-back-to-shop-with-same-path
  (with-services {:storeback-handler
                  (routes
                   (GET "/store"       req common/storeback-aladdin-stylist-response)
                   (GET "/promotions"  req {:status 200 :body "{}"})
                   (GET "/v3/products" req {:status 200 :body "{}"})
                   (GET "/v2/skus"     req {:status 200 :body "{}"})
                   (GET "/v2/facets"   req {:status 200 :body (generate-string common/facets-body)}))}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://jasmine.mayvenn.com/categories/27-human-hair-bundles"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/categories/27-human-hair-bundles"
               (get-in resp [:headers "Location"])))))))

(deftest aladdin-subpaths-redirect-to-stylist-profile-on-shop
  (with-services {:storeback-handler
                  (routes
                   (GET "/store"       req common/storeback-aladdin-stylist-response)
                   (GET "/promotions"  req {:status 200 :body "{}"})
                   (GET "/v3/products" req {:status 200 :body "{}"})
                   (GET "/v2/skus"     req {:status 200 :body "{}"})
                   (GET "/v2/facets"   req {:status 200 :body (generate-string common/facets-body)}))}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://jasmine.mayvenn.com"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/stylist/10-jasmine"
               (get-in resp [:headers "Location"])))))))

(deftest redirects-match-stylist-to-find-your-stylist
  (with-services {}
    (with-handler handler
      (let [resp (handler (mock/request :get "https://shop.mayvenn.com/adv/match-stylist"))]
        (is (= 301 (:status resp)) (pr-str resp))
        (is (= "https://shop.mayvenn.com/adv/find-your-stylist" (get-in resp [:headers "Location"])))))))

(deftest service-categories-and-pdp-pages-direct-to-find-your-stylist
  (with-services {}
    (with-handler handler
      (doseq [category-id storefront.handler/service-category-ids]
        (let [resp (handler (mock/request :get (format "https://shop.mayvenn.com/categories/%s-some-category-slug"
                                                       category-id)))]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/adv/find-your-stylist" (get-in resp [:headers "Location"])))))

      (doseq [product-id storefront.handler/service-product-ids]
        (let [resp (handler (mock/request :get (format "https://shop.mayvenn.com/products/%s-some-product-detail-slug" product-id)))]
          (is (= 302 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/adv/find-your-stylist" (get-in resp [:headers "Location"]))))))))
