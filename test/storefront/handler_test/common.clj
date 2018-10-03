(ns storefront.handler-test.common
  (:require [cheshire.core :refer [generate-string parse-string]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST]]
            [spice.core :as spice]
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

(def default-storeback-handler
  (routes
    (GET "/store" req storeback-stylist-response)))

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

(defn assert-request [req storeback-resp asserter]
  (with-services {:storeback-handler (constantly storeback-resp)}
    (with-handler handler
      (asserter (handler (merge default-req-params req))))))
