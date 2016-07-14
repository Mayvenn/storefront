(ns storefront.handler-test
  (:require [storefront.handler :refer :all]
            [storefront.system :refer [create-system]]
            [clojure.test :refer :all]
            [standalone-test-server.core :refer :all]
            [cheshire.core :refer [generate-string]]
            [com.stuartsierra.component :as component]
            [ring.util.response :refer [response status content-type]]))

(def test-overrides {:server-opts {:port 2390}
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
                        :profile_picture_url nil})
      (response)
      (status 200)
      (content-type "application/json")))

(def storeback-stylist-response
  (-> (generate-string {:store_slug "bob"
                        :store_name "Bob's Hair Emporium"
                        :instagram_account nil
                        :profile_picture_url nil})
      (response)
      (status 200)
      (content-type "application/json")))

(defmacro with-resource
  [bindings close-fn & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (~close-fn ~(bindings 0))))))

(defmacro with-handler
  [handler & body]
  `(let [unstarted-system# (-> (create-system test-overrides))]
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

(deftest redirects-missing-stylists-to-store-while-preserving-query-params
  (assert-request
   {:server-name "no-stylist.mayvenn.com"
    :query-string "yo=lo&mo=fo"}
   storeback-no-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)))
     (is (= "http://store.mayvenn.com:8080/?yo=lo&mo=fo"
            (get-in resp [:headers "Location"]))))))

(deftest redirects-www-prefixed-stylists-to-stylist-without-prefix
  (assert-request
   {:server-name "www.bob.mayvenn.com"}
   storeback-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)) (pr-str resp))
     (is (= "http://bob.mayvenn.com:8080/"
            (get-in resp [:headers "Location"]))))))

(deftest redirects-www-to-welcome-preserving-query-params
  (with-handler handler
    (let [resp (handler (merge default-req-params {:server-name "www.mayvenn.com"
                                                   :query-string "world=true"}))]
      (is (= 302 (:status resp)))
      (is (= "http://shop.mayvenn.com:8080/?world=true"
             (get-in resp [:headers "Location"]))))))

(deftest redirects-no-subdomain-to-welcome-preserving-query-params
  (with-handler handler
    (let [resp (handler (merge default-req-params {:server-name "mayvenn.com"
                                                   :query-string "world=true"}))]
      (is (= 302 (:status resp)))
      (is (= "http://shop.mayvenn.com:8080/?world=true"
             (get-in resp [:headers "Location"]))))))

(deftest redirects-blonde-category-to-straight-hair
  (assert-request {:server-name "shop.mayvenn.com"
                   :uri         "/categories/hair/blonde"
                   :headers     {}}
                  storeback-shop-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "http://shop.mayvenn.com:8080/categories/hair/straight"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-shop-to-preferred-subdomain-preserving-path-and-query-strings
  (assert-request {:server-name "shop.mayvenn.com"
                   :uri         "/categories/hair/straight?utm_source=cats"
                   :headers     {"cookie" "preferred-store-slug=bob"}}
                  storeback-shop-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "http://bob.mayvenn.com:8080/categories/hair/straight?utm_source=cats"
                           (get-in resp [:headers "Location"]))))))

(deftest redirects-shop-to-store-subdomain-if-preferred-subdomain-is-invalid
  (assert-request {:server-name "shop.mayvenn.com"
                   :uri         "/categories/hair/straight?utm_source=cats"
                   :headers     {"cookie" "preferred-store-slug=non-existent-stylist"}}
                  storeback-no-stylist-response
                  (fn [resp]
                    (is (= 302 (:status resp)))
                    (is (= "http://store.mayvenn.com:8080/categories/hair/straight?utm_source=cats"
                           (get-in resp [:headers "Location"])))
                    (let [cookie (first (get-in resp [:headers "Set-Cookie"]))]
                      (is (.contains cookie "preferred-store-slug=;Max-Age=0;") cookie)))))

(deftest redirects-vistaprint
  (with-handler handler
    (let [resp (handler (merge default-req-params {:server-name "vistaprint.mayvenn.com"}))]
      (is (= 302 (:status resp)))
      (is (= "http://www.vistaprint.com/vp/gateway.aspx?sr=no&s=6797900262"
             (get-in resp [:headers "Location"]))))))

(deftest renders-page-when-matches-stylist-subdomain-and-sets-the-preferred-subdomain
  (assert-request
   {:server-name "bob.mayvenn.com"}
   storeback-stylist-response
   (fn [resp]
     (is (= 200 (:status resp)))
     (is (.contains (first (get-in resp [:headers "Set-Cookie"])) "preferred-store-slug=bob;")))))
