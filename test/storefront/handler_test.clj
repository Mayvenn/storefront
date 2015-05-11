(ns storefront.handler-test
  (:require [storefront.handler :refer :all]
            [storefront.system :refer [create-system]]
            [clojure.test :refer :all]
            [standalone-test-server.core :refer :all]
            [cheshire.core :refer [generate-string]]
            [ring.util.response :refer [response status content-type]]))

(def test-overrides {:server-opts {:port 2390}
                     :logger (constantly nil)})

(def storeback-no-stylist-response
  (-> (response "{}")
      (status 404)
      (content-type "application/json")))

(def storeback-stylist-response
  (-> (generate-string {:store_slug "bob"
                        :store_name "Bob's Hair Emporium"
                        :instagram_account nil
                        :profile_picture_url nil})
      (response)
      (status 200)
      (content-type "application/json")))

(defmacro with-test-system
  [sys & body]
  `(let [unstarted-system# (-> (create-system test-overrides)
                               (assoc :auth-workflows [no-auth/workflow]))]
     (with-resource [~sys (component/start unstarted-system#)]
      component/stop
      ~@body)))

(defn assert-request [req storeback-resp asserter]
  (let [[get-requests endpoint]
        (recording-endpoint :handler (constantly storeback-resp))]
    (with-standalone-server [ss (standalone-server endpoint)]
      (let [handler (create-handler {:logger (constantly nil)
                                     :exception-handler (constantly nil)
                                     :storeback-config {:endpoint "http://localhost:4334/"}})
            resp (handler (merge {:server-name "welcome.mayvenn.com"
                                  :server-port 8080
                                  :uri "/"
                                  :request-method :get}
                                 req))]
        (asserter resp)))))

(deftest redirects-missing-stylists-to-store
  (assert-request
   {:server-name "no-stylist.mayvenn.com"}
   storeback-no-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)))
     (is (= "http://store.mayvenn.com:8080"
            (get-in resp [:headers "Location"]))))))

(deftest redirects-www-prefixed-stylists-to-stylist-without-prefix
  (assert-request
   {:server-name "www.bob.mayvenn.com"}
   storeback-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)))
     (is (= "http://bob.mayvenn.com:8080"
            (get-in resp [:headers "Location"]))))))

(deftest redirects-www-to-welcome
  (assert-request
   {:server-name "www.mayvenn.com"}
   storeback-no-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)))
     (is (= "http://welcome.mayvenn.com:8080"
            (get-in resp [:headers "Location"]))))))

(deftest redirects-no-subdomain-to-welcome
  (assert-request
   {:server-name "mayvenn.com"}
   storeback-no-stylist-response
   (fn [resp]
     (is (= 302 (:status resp)))
     (is (= "http://welcome.mayvenn.com:8080"
            (get-in resp [:headers "Location"]))))))

(deftest renders-page-when-matches-stylist-subdomain
  (assert-request
   {:server-name "bob.mayvenn.com"}
   storeback-stylist-response
   (fn [resp]
     (is (= 200 (:status resp))))))
