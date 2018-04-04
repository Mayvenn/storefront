(ns storefront.handler-test
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure
             [string :as string]
             [test :refer :all]]
            [compojure.core :refer :all]
            [com.stuartsierra.component :as component]
            [ring.mock.request :as mock]
            [ring.util
             [codec :as codec]
             [response :refer [content-type response status]]]
            [standalone-test-server.core :refer :all]
            [storefront
             [handler :refer :all]
             [system :refer [create-system]]]
            [storefront.backend-api :as api]
            [storefront.config :as config]
            [spice.core :as spice]))

(def test-overrides {:environment       "test"
                     :server-opts       {:port 2390}
                     :logging           (constantly nil)
                     :contentful-config {:endpoint "http://localhost:4335"
                                         :space-id "fake-space-id"
                                         :api-key  "fake-api-key"}
                     :storeback-config  {:endpoint          "http://localhost:4334/"
                                         :internal-endpoint "http://localhost:4334/"}})

(defn set-cookies [req cookies]
  (update req :headers assoc "cookie" (string/join "; " (map (fn [[k v]] (str k "=" v)) cookies))))

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


(def storeback-one-time-login-response
  (-> (generate-string {:user  {:email      "acceptance+bob@mayvenn.com"
                                :id         3
                                :token "USERTOKEN"}
                        :order {:number "W123456"
                                :token  "ORDERTOKEN"}})
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
  (testing "bob.mayvenn.com"
    (assert-request (mock/request :get "http://bob.mayvenn.com")
                    storeback-stylist-response
                    (fn [resp]
                      (is (= 301 (:status resp)) (pr-str resp))
                      (is (= "https://bob.mayvenn.com/"
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
      (is (= 301 (:status resp)))
      (is (= "https://shop.mayvenn.com/?world=true"
             (get-in resp [:headers "Location"]))))))

(deftest redirects-no-subdomain-to-shop-preserving-query-params
  (with-handler handler
    (let [resp (handler (mock/request :get "https://mayvenn.com/?world=true"))]
      (is (= 301 (:status resp)))
      (is (= "https://shop.mayvenn.com/?world=true"
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
                             (GET "/store" req storeback-stylist-response)))]
    (with-standalone-server [storeback (standalone-server storeback-handler)]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/products/brazilian-loose-wave-lace-closure"))]
          (is (= 301 (:status resp)) (pr-str resp))
          (is (= "https://shop.mayvenn.com/products/33-brazilian-loose-wave-lace-closures"
                 (get-in resp [:headers "Location"]))))))))

(deftest redirects-old-categories-to-new-categories
  (assert-request (mock/request :get "https://shop.mayvenn.com/categories/hair/straight")
                  storeback-shop-response
                  (fn [resp]
                    (is (= 301 (:status resp)) (pr-str resp))
                    (is (= "https://shop.mayvenn.com/categories/2-virgin-straight"
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

(deftest redirects-the-literal-stylist-subdomain-to-community
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
        (is (.contains (:body resp) ":utm-content \\\"stylistsfb\\\"")
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
        (is (some #{"utm_term=;Max-Age=0;Secure;Path=/"} cookies))))
    (testing "has no canonical url"
      (let [resp (handler (-> (mock/request :get "https://welcome.mayvenn.com/stylists/welcome")))]
        (is (= 200 (:status resp)))
        (is (-> resp
                :body
                (.contains "rel=\"canonical\"")
                not))))))

(defmacro is-redirected-to [resp domain path]
  `(let [resp# ~resp
         domain# ~domain
         path# ~path]
     (is (= 302 (:status resp#)))
     (is (= (format "https://%s.mayvenn.com%s" domain# path#)
            (-> resp# :headers (get "Location"))))))

(def lead-cookie "lead-id=MOCK-LEAD-ID;")

(deftest one-time-login-sets-cookies
  (with-standalone-server [storeback (standalone-server (routes
                                                         (GET "/store" _ storeback-stylist-response)
                                                         (POST "/v2/one-time-login" _ storeback-one-time-login-response)))]
    (with-handler handler
      (let [resp (handler (mock/request :get "https://bob.mayvenn.com/one-time-login?token=USERTOKEN&user-id=1&sha=FIRST&target=%2F"))
            cookies (get-in resp [:headers "Set-Cookie"])
            location (get-in resp [:headers "Location"])]
        (testing "It removes one-time-login params, but keeps other query params in the url it redirects to"
          (is-redirected-to resp "bob" "/?sha=FIRST"))
        (testing "It assigns cookies to the client to automatically log them into storefront frontend"
          (is (some #{"user-token=USERTOKEN;Max-Age=2592000;Secure;Path=/;Domain=bob.mayvenn.com"} cookies)))))))


(defn- get-leads-req [handler cookie path]
  (-> (mock/request :get (str "https://welcome.mayvenn.com/stylists" path))
      (mock/header "Cookie" cookie)
      handler))

(deftest welcome-subdomain-remembers-leads-last-step
  (testing "When you are unknown"
    (with-handler handler
      (testing "in the original flow"
        (testing "you can go home"
          (is (= 200 (:status (get-leads-req handler "" "/welcome")))))
        (testing "going to thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/thank-you")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-applied-thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-thank-you")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-applied-self-reg takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-self-reg")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-registered-thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/registered-thank-you")
                            "welcome" "/stylists/welcome")))
      (testing "in the a1 flow"
        (testing "you can go home"
          (is (= 200 (:status (get-leads-req handler "" "/welcome?flow=a1")))))
        (testing "going to thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/thank-you?flow=a1")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-applied-thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-thank-you?flow=a1")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-applied-self-reg takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-self-reg?flow=a1")
                            "welcome" "/stylists/welcome"))
        (testing "going to a1-registered-thank-you takes you back to welcome"
          (is-redirected-to (get-leads-req handler "" "/flows/a1/registered-thank-you?flow=a1")
                            "welcome" "/stylists/welcome")))))
  (testing "When you are a known lead and don't have a flow"
    (let [fake-lead {:id "MOCK-LEAD-ID"}]
      (with-standalone-server [storeback (standalone-server
                                          (constantly {:status 200
                                                       :body   (generate-string {:lead fake-lead})}))]
        (with-handler handler
          (testing "you can go home"
            (is (= 200 (:status (get-leads-req handler "" "/welcome")))))
          (testing "going to thank-you works"
            (is (= 200 (:status (get-leads-req handler lead-cookie "/thank-you")))))
          (testing "going to a1 applied-thank-you redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-thank-you")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 applied-self-reg redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-self-reg")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 applied-self-reg redirects to thank-you (lead-id from query params)"
            (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-self-reg?lead_id=MOCK-LEAD-ID")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 registered-thank-you redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/registered-thank-you")
                              "welcome" "/stylists/thank-you"))))))
  (testing "When you are a known lead and in [original initial]"
    (let [fake-lead {:id "MOCK-LEAD-ID"
                     :flow-id "original"
                     :step-id "initial"}]
      (with-standalone-server [storeback (standalone-server
                                          (constantly {:status 200
                                                       :body   (generate-string {:lead fake-lead})}))]
        (with-handler handler
          (testing "you can go home"
            (is (= 200 (:status (get-leads-req handler "" "/welcome")))))
          (testing "going to thank-you works"
            (is (= 200 (:status (get-leads-req handler lead-cookie "/thank-you")))))
          (testing "going to a1 applied-thank-you redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-thank-you")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 applied-self-reg redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-self-reg")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 applied-self-reg redirects to thank-you (lead-id from query params)"
            (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-self-reg?lead_id=MOCK-LEAD-ID")
                              "welcome" "/stylists/thank-you"))
          (testing "going to a1 registered-thank-you redirects to thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/registered-thank-you")
                              "welcome" "/stylists/thank-you"))))))
  (testing "When you are a known lead and in [a1 applied]"
    (let [fake-lead {:id "MOCK-LEAD-ID"
                     :flow-id "a1"
                     :step-id "applied"}]
      (with-standalone-server [storeback (standalone-server
                                          (constantly {:status 200
                                                       :body   (generate-string {:lead fake-lead})}))]
        (with-handler handler
          (testing "going home takes you to applied-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/welcome")
                              "welcome" "/stylists/flows/a1/applied-thank-you"))
          (testing "going to thank-you redirects to applied-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/thank-you")
                              "welcome" "/stylists/flows/a1/applied-thank-you"))
          (testing "going to a1 applied-thank-you works"
            (is (= 200 (:status (get-leads-req handler lead-cookie "/flows/a1/applied-thank-you")))))
          (testing "going to a1 applied-self-reg works (lead-id from cookie)"
            (is (= 200 (:status (get-leads-req handler lead-cookie "/flows/a1/applied-self-reg")))))
          (testing "going to a1 applied-self-reg works (lead-id from query params)"
            (is (= 200 (:status (get-leads-req handler "" "/flows/a1/applied-self-reg?lead_id=MOCK-LEAD-ID")))))
          (testing "going to a1 registered-thank-you redirects to applied-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/registered-thank-you")
                              "welcome" "/stylists/flows/a1/applied-thank-you"))))))
  (testing "When you are a known lead and in [a1 registered]"
    (let [fake-lead {:id "MOCK-LEAD-ID"
                     :flow-id "a1"
                     :step-id "registered"}]
      (with-standalone-server [storeback (standalone-server
                                          (constantly {:status 200
                                                       :body   (generate-string {:lead fake-lead})}))]
        (with-handler handler
          (testing "going home redirects to thank you"
            (is-redirected-to (get-leads-req handler lead-cookie "/welcome")
                              "welcome" "/stylists/flows/a1/registered-thank-you"))
          (testing "going to thank-you redirects to registered-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/thank-you")
                              "welcome" "/stylists/flows/a1/registered-thank-you"))
          (testing "going to a1 applied-thank-you redirects to registered-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-thank-you")
                              "welcome" "/stylists/flows/a1/registered-thank-you"))
          (testing "going to a1 applied-self-reg redirects to registered-thank-you"
            (is-redirected-to (get-leads-req handler lead-cookie "/flows/a1/applied-self-reg")
                              "welcome" "/stylists/flows/a1/registered-thank-you"))
          (testing "going to a1 applied-self-reg redirects to registered-thank-you (with query params)"
            (is-redirected-to (get-leads-req handler "" "/flows/a1/applied-self-reg?lead_id=MOCK-LEAD-ID")
                              "welcome" "/stylists/flows/a1/registered-thank-you"))
          (testing "going to a1 registered-thank-you works"
            (is (= 200 (:status (get-leads-req handler lead-cookie "/flows/a1/registered-thank-you"))))))))))

(deftest submits-paypal-redirect-to-waiter
  (testing "when waiter returns a 200 response"
    (let [[waiter-requests waiter-handler] (with-requests-chan
                                             (constantly {:status  200
                                                          :headers {"Content-Type" "application/json"}
                                                          :body    "{}"}))]
      (with-standalone-server [waiter (standalone-server waiter-handler)]
        (with-handler handler
          (let [resp        (-> (mock/request :get "https://shop.mayvenn.com/orders/W123456/paypal/order-token")
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

(deftest redirects-product-details-sku-to-sku-in-query-params
  (with-handler handler
    (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/12-indian-straight-bundles/INSDB14"))]
      (is (= 301 (:status resp)))
      (is (= "/products/12-indian-straight-bundles?SKU=INSDB14"
             (get-in resp [:headers "Location"]))))))

(deftest server-side-renders-product-details-page
  (testing "when the product does not exist storefront returns 404"
    (let [[storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      (GET "/v2/orders/:number" req {:status 404
                                                                                                     :body   "{}"})
                                                                      (GET "/v2/products" req
                                                                           {:status 200
                                                                            :body   (generate-string {:products []})})
                                                                      (GET "/store" req storeback-stylist-response)))]
      (with-standalone-server [storeback (standalone-server storeback-handler)]
        (with-handler handler
          (let [resp (handler (mock/request :get "https://bob.mayvenn.com/products/99-red-balloons"))]
            (is (= 404 (:status resp))))))))
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
                                                                           :selector/essentials [:catalog/department :hair/grade :hair/origin :hair/texture :hair/base-material :hair/family :hair/color.process :hair/source]
                                                                           :page/slug           "peruvian-water-wave-lace-closures"}]
                                                               :skus     [{:catalog/sku-id        "PWWLC18"
                                                                           :catalog/stylist-only? false
                                                                           :catalog/launched-at   "2016-01-01T00:00:00.000Z"
                                                                           :catalog/department    #{"hair"}
                                                                           :selector/essentials   [:hair/family :hair/color :hair/origin :hair/base-material :hair/length :catalog/department :hair/color.process :hair/grade :hair/texture :hair/source]
                                                                           :selector/electives    []
                                                                           :hair/base-material    #{"lace"}
                                                                           :hair/color.process    #{"natural"}
                                                                           :hair/family           #{"closures"}
                                                                           :hair/grade            #{"6a"}
                                                                           :hair/origin           #{"peruvian"}
                                                                           :hair/source           #{"virgin"}
                                                                           :hair/texture          #{"water-wave"}
                                                                           :hair/color            #{"black"}
                                                                           :selector/images              [{:filename  "Water-Wave-Bundle.jpg"
                                                                                                           :url       "//ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/"
                                                                                                           :use-cases {:seo      {:alt ""}
                                                                                                                       :carousel {:alt   ""
                                                                                                                                  :order 5}
                                                                                                                       :catalog  {:alt ""}}
                                                                                                           :criteria/attributes
                                                                                                           {:catalog/department "hair" :image/of "product" :hair/grade "6a" :hair/texture "water-wave" :hair/color "black" :hair/family "bundles"}}]
                                                                           :selector/from-products       ["67"]
                                                                           :inventory/in-stock?          true
                                                                           :sku/price                    104.0
                                                                           :sku/title                    "A balloon"
                                                                           :legacy/product-name          "A balloon"
                                                                           :legacy/product-id            133
                                                                           :legacy/variant-id            641
                                                                           :promo.triple-bundle/eligible true}]})})
                               (GET "/store" req
                                    storeback-stylist-response)))]
      (with-standalone-server [storeback (standalone-server storeback-handler)]
        (with-handler handler
          (are [url] (string/includes?
                      (->> (str "https:" url)
                           (mock/request :get)
                           handler
                           :body
                           (re-find #"<link[^>]+rel=\"canonical[^>]+>"))
                      (string/replace url #"bob" "shop"))
            "//bob.mayvenn.com/products/67-peruvian-water-wave-lace-closures?SKU=PWWLC10"
            "//bob.mayvenn.com/categories/10-virgin-360-frontals"
            "//bob.mayvenn.com/our-hair"
            "//bob.mayvenn.com/"))))))

(deftest server-side-fetching-of-orders
  (testing "storefront retrieves an order from storeback"
    (let [number "W123456"
          token "iA1bjIUAqCfyS3cuvdNYindmlRZ3ICr3g+vSfzvUM1c="
          [storeback-requests storeback-handler] (with-requests-chan (routes
                                                                      (GET "/v2/orders/:number" req {:status 200
                                                                                                     ;; TODO: fixme
                                                                                                     :body "{\"number\": \"W123456\"}"})
                                                                      (GET "/store" req storeback-stylist-response)))]
      (with-standalone-server [storeback (standalone-server storeback-handler)]
        (with-handler handler
          (let [resp (handler (-> (mock/request :get "https://bob.mayvenn.com/")
                                  (set-cookies {":number" number
                                                ":token"  token})))]
            (is (= 200 (:status resp)))
            (is (.contains (:body resp) "W123456") (pr-str resp))

            (testing "storefront properly reads order tokens with pluses in them"
              (let [requests (txfm-requests storeback-requests identity)
                    waiter-request (nth requests 1)]
                (is (= "/v2/orders/W123456" (:uri waiter-request)))
                (is (= {"token" token} (:query-params waiter-request)))))))))))

(deftest sitemap-on-a-valid-store-domain
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-standalone-server [storeback (standalone-server handler)]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://shop.mayvenn.com/sitemap.xml"))]
          (is (= 200 (:status resp))))))))

(deftest sitemap-does-not-exist-on-root-domain
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-standalone-server [storeback (standalone-server handler)]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://mayvenn.com/sitemap.xml"))]
          (is (= 404 (:status resp))))))))

(deftest sitemap-does-not-exist-on-welcome-subdomain
  (let [[requests handler] (with-requests-chan (constantly {:status 200
                                                            :body   (generate-string {:skus []
                                                                                      :products []})}))]
    (with-standalone-server [storeback (standalone-server handler)]
      (with-handler handler
        (let [resp (handler (mock/request :get "https://welcome.mayvenn.com/sitemap.xml"))]
          (is (= 404 (:status resp))))))))

(deftest robots-disallows-content-storefront-pages-on-shop
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
        "Disallow: /content"))))

(deftest robots-allows-all-pages-on-stylist-stores
  (with-handler handler
    (let [{:keys [status body]} (handler (mock/request :get "https://bob.mayvenn.com/robots.txt"))]
      (is (= 200 status))
      (is (not-any? #{"Disallow: /"} (string/split-lines body)) body))))

(deftest robots-disallows-leads-pages-on-welcome-subdomain
  (with-handler handler
    (let [{:keys [status body]} (handler (mock/request :get "https://welcome.mayvenn.com/robots.txt"))]
      (is (= 200 status))
      (is (.contains body "Disallow: /stylists/thank-you") body)
      (is (.contains body "Disallow: /stylists/flows/") body))))

(def contentful-response
  {:body
   {:sys   {:type "Array"}
    :total 2
    :skip  0
    :limit 100
    :items
    [{:sys
      {:space     {:sys {:type "Link" :linkType "Space" :id "76m8os65degn"}}
       :id        "6VN7YwJeAoQagIaEyOqk4S"
       :type      "Entry"
       :createdAt "2018-04-03T00:24:17.795Z"
       :updatedAt "2018-04-03T21:47:26.208Z"
       :revision  2
       :contentType
       {:sys {:type "Link" :linkType "ContentType" :id "homepage"}}
       :locale    "en-US"}
      :fields
      {:heroImageDesktopUuid       "8cb671b1-33b8-496b-a77b-7281ac72c571"
       :heroImageMobileUuid        "666b02ba-26f2-4349-aa98-1d251edc701c"
       :heroImageFileName          "Hair-Always-On-Beat.jpg"
       :heroImageAltText           "Hair always on beat! 15% off everything! Shop looks!"
       :leftFeatureBlockFileName   "Left"
       :middleFeatureBlockFileName "Middle"
       :rightFeatureBlockFileName  "Right"}}
     {:sys
      {:space     {:sys {:type "Link" :linkType "Space" :id "76m8os65degn"}}
       :id        "7kFmCirU3uO2w6ykgAQ4gY"
       :type      "Entry"
       :createdAt "2018-04-03T21:49:47.237Z"
       :updatedAt "2018-04-03T21:49:47.237Z"
       :revision  1
       :contentType
       {:sys {:type "Link" :linkType "ContentType" :id "homepage"}}
       :locale    "en-US"}
      :fields
      {:heroImageDesktopUuid       "8cb671b1-33b8-496b-a77b-7281ac72c571"
       :heroImageMobileUuid        "666b02ba-26f2-4349-aa98-1d251edc701c"
       :heroImageFileName          "Hair-Always-On-Beat.jpg"
       :heroImageAltText           "Hair always on beat! 15% off everything! Shop looks!"
       :leftFeatureBlockFileName   "Left"
       :middleFeatureBlockFileName "Middle"
       :rightFeatureBlockFileName  "Right"}}]}
   :status 200})

(deftest fetches-data-from-contentful
  (testing "caching content"
    (let [[storeback-requests storeback-handler]   (with-requests-chan (routes (GET "/store" req storeback-stylist-response)))
          [contentful-requests contentful-handler] (with-requests-chan (routes (GET "/spaces/fake-space-id/entries" req
                                                                                    {:status 200
                                                                                     :body   (generate-string (:body contentful-response))})))]
      (with-standalone-server [storeback (standalone-server storeback-handler)
                               contentful (standalone-server contentful-handler {:port 4335})]
        (with-handler handler
          (let [responses (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                requests  (txfm-requests contentful-requests identity)]
            (is (every? #(= 200 (:status %)) responses))
            (is (= 1 (count requests))))))))

  (testing "fetches data on system start"
    (let [[contentful-requests contentful-handler] (with-requests-chan (routes (GET "/spaces/fake-space-id/entries" req
                                                                                    {:status 200
                                                                                     :body   (generate-string (:body contentful-response))})))]
      (with-standalone-server [contentful (standalone-server contentful-handler {:port 4335})]
        (with-handler handler
          (is (= 1 (count (txfm-requests contentful-requests identity))))))))

  (testing "attempts-to-retry-fetch-from-contentful"
    (let [[storeback-requests storeback-handler]   (with-requests-chan (routes (GET "/store" req storeback-stylist-response)))
          [contentful-requests contentful-handler] (with-requests-chan (routes (GET "/spaces/fake-space-id/entries" req
                                                                                    {:status 500
                                                                                     :body   "{}"})))]
      (with-standalone-server [storeback (standalone-server storeback-handler)
                               contentful (standalone-server contentful-handler {:port 4335})]
        (with-handler handler
          (let [responses (repeatedly 5 (partial handler (mock/request :get "https://bob.mayvenn.com/")))
                requests  (txfm-requests contentful-requests identity)]
            (is (every? #(= 200 (:status %)) responses))
            (is (= 2 (count requests)))))))))
