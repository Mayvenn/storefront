(ns storefront.handler-test.common
  (:require [cheshire.core :refer [generate-string parse-string]]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [com.stuartsierra.component :as component]
            [compojure.core :refer [routes GET POST]]
            [ring.util.response :refer [content-type response status]]
            [standalone-test-server.core :refer [txfm-request txfm-requests
                                                 with-standalone-server standalone-server
                                                 with-requests-chan]]
            [storefront.system :refer [create-system]]
            [storefront.system.contentful :as contentful]
            [spice.maps :as maps]))

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

(def contentful-ugc-collection-response
  {:body {:sys {:type "Array"},
          :total 5,
          :skip 0,
          :limit 100,
          :items
          [{:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "look",
             :name "Mayvenn Classic - Shop By Look "},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:22:54.792Z",
             :id "5vqi7q9EeO1ULNjQ1Q4DEp",
             :type "Entry"}}
           {:fields
            {:slug "acceptance-deals",
             :name "[ACCEPTANCE] Mayvenn Classic - Deals Page"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-21T21:02:06.413Z",
             :id "6Za8EE8Kpn8NeoJciqN3uA",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}
              {:sys {:type "Link", :linkType "Entry", :id "48c3sCi06BHRRMKJxmM4u3"}}
              {:sys {:type "Link", :linkType "Entry", :id "broken-link-id"}}],
             :slug "deals",
             :name "Mayvenn Classic - Deals Page"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-23T17:05:20.329Z",
             :id "2dZTVOLLqkNS9EoUJ1t6qn",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "bundle-sets-straight",
             :name "Adventure  Bundle Sets Straight"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:44:28.793Z",
             :id "4GfFV6dC7KjLhUxNDKvguP",
             :type "Entry"}}
           {:fields
            {:acceptanceLooks
             [{:sys {:type "Link", :linkType "Entry", :id "2zSbLYFcRYjVoEMMlsWLsJ"}}],
             :slug "shop-by-look-straight",
             :name "Adventure Shop By Look Straight"},
            :sys
            {:contentType
             {:sys {:type "Link", :linkType "ContentType", :id "ugc-collection"}},
             :updatedAt "2019-05-22T23:32:43.380Z",
             :id "4NNviXNUw1odQtzXOdHaNY",
             :type "Entry"}}],
          :includes
          {:Entry
           [{:sys
             {:space {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
              :updatedAt "2019-05-22T22:59:58.189Z",
              :revision 1,
              :locale "en-US",
              :createdAt "2019-05-22T22:59:58.189Z",
              :type "Entry",
              :id "2zSbLYFcRYjVoEMMlsWLsJ",
              :environment {:sys {:id "master", :type "Link", :linkType "Environment"}},
              :contentType {:sys {:type "Link", :linkType "ContentType", :id "look"}}},
             :fields
             {:title "Acceptance Virgin Peruvian Deep Wave 16 18 20 ",
              :texture "Deep Wave",
              :color "Natural Black",
              :description "16\" + 18\" + 20\" ",
              :sharedCartUrl "https://shop.mayvenn.com/c/XFoCrXR7Yx",
              :photoUrl
              "https://static.pixlee.com/photos/235267317/original/bundle-deal-template-f-r1-01-lm.jpg",
              :socialMediaHandle "@mayvennhair",
              :socialMediaPlatform "instagram"}}
            {:sys
             {:space {:sys {:type "Link", :linkType "Space", :id "76m8os65degn"}},
              :updatedAt "2019-05-23T00:38:22.078Z",
              :revision 1,
              :locale "en-US",
              :createdAt "2019-05-23T00:38:22.078Z",
              :type "Entry",
              :id "48c3sCi06BHRRMKJxmM4u3",
              :environment {:sys {:id "master", :type "Link", :linkType "Environment"}},
              :contentType {:sys {:type "Link", :linkType "ContentType", :id "look"}}},
             :fields
             {:title "BDW 12\" 12\" 12\"",
              :texture "Deep Wave",
              :color "Natural Black",
              :description "12\" 12\" 12\"",
              :sharedCartUrl "https://shop.mayvenn.com/c/nAOHqCV5Es",
              :photoUrl
              "https://static.pixlee.com/photos/270470339/original/Screen_Shot_2019-03-08_at_9.44.00_AM.png",
              :socialMediaHandle "@enevicky",
              :socialMediaPlatform "instagram"}}]}}
   :status 200})

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
