(ns storefront.system.contentful.graphql
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [environ.core :refer [env]]
            [tugboat.core :as tugboat]
            [cheshire.core :as json]))

(defn- request
  "Please use [[query]] when possible"
  [{:keys [graphql-endpoint preview-api-key api-key space-id env-id]} gql variables]
  (try
    (tugboat/request {:endpoint graphql-endpoint}
                     :post (str "/content/v1/spaces/" space-id "/environments/" env-id)
                     {:socket-timeout 10000
                      :conn-timeout   10000
                      :as             :json
                      :headers        {"Authorization" (str "Bearer " (if (get variables "preview")
                                                                        preview-api-key
                                                                        api-key))
                                       "Content-Type" "application/json"}
                      :body           (json/generate-string {:query     (str gql)
                                                             :variables variables})})
    (catch java.io.IOException ioe
      nil)))

(defn query [contentful-ctx file variables]
  (if-not (= (env :environment) "development")
    (request contentful-ctx (slurp (io/resource (str "gql/" file))) variables)
    (request contentful-ctx (slurp (io/resource (str "gql/" file))) variables)))


(comment
  (query (:contentful dev-system/the-system) "static_page.gql" {"$preview" false "$path" "/policy/privacy"})
  (query (:contentful dev-system/the-system) "all_static_pages.gql" {"$preview" false}))
