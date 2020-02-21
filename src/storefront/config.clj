(ns storefront.config
  (:require [environ.core :refer [env]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [spice.maps :as maps]
            [storefront.assets :as assets]
            [storefront.platform.asset-mappings :as asset-mappings]))

(def welcome-subdomain "welcome")

;; human-readable phone number to call support. Use a helper function to
;; format it properly for "tel:" (see ui/phone-url)
;;
;; Don't forget to update static html pages & config.cljs if you change this.
(def support-phone-number "1 (855) 287-6868")

(defn define-frontend-modules []
  ;; all module names must be cljs mangled (aka - hyphens get converted to underscore)
  ;; See implementation of cljs.loader: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/loader.cljs#L15-L46
  (letfn [(snake->underscore [kw]
            (keyword (string/replace (name kw) #"-" "_")))]
    (maps/map-keys snake->underscore
                   {:cljs-base     [(assets/path "/js/out/cljs_base.js")]
                    :main          [(assets/path "/js/out/main.js")]
                    :dashboard     [(assets/path "/js/out/dashboard.js")]
                    :redeem        [(assets/path "/js/out/redeem.js")]
                    :design-system [(assets/path "/js/out/design-system.js")]
                    :catalog       [(assets/path "/js/out/catalog.js")]
                    :checkout      [(assets/path "/js/out/checkout.js")]})))

(def frontend-modules (memoize define-frontend-modules))

(def frontend-assets
  "Asset mappings to send to the frontend"
  #{"/images/sprites.svg"
    "/css/app.css"
    "//ucarecdn.com/fa4eefff-7856-4a1b-8cdb-c8b228b62967/-/format/auto/stylist-gallery-icon"
    "//ucarecdn.com/1a4a3bd5-0fda-45f2-9bb4-3739b911390f/-/format/auto/instagram-icon"
    "//ucarecdn.com/c8f0a4b8-24f7-4de8-9c20-c6634b865bc1/-/format/auto/styleseat-logotype"
    "//ucarecdn.com/6cfa1b3c-ed89-4e71-b702-b6fdfba72c0a/-/format/auto/collapse"
    "//ucarecdn.com/dbdcce35-e6da-4247-be57-22991d086fc1/-/format/auto/expand"
    "/images/header_logo.svg"
    "//ucarecdn.com/81bd063f-56ba-4e9c-9aef-19a1207fd422/-/format/auto/stylist-bug-no-pic-fallback"})

(defn development? [environment]
  {:pre [(#{"development" "test" "acceptance" "production"} environment)]}
  (= environment "development"))

(defn dev-print! [environment & msg]
  (when (development? environment)
    (newline)
    (println "********************************")
    (apply println msg)
    (println "********************************")
    (newline)
    (newline)))

(def feature-block-look-ids
  ;;NOTE edit the cljs config too!
  ;;NOTE @Ryan, please only change the top map
  (if (= (env :environment) "production")
    {:left  191567494
     :right 191567299}
    {:left  144863121
     :right 144863121}))

(def client-version
  (try
    (slurp (io/resource "client_version.txt"))
    (catch java.lang.IllegalArgumentException e
      "unknown")))

(def default-config {:server-opts       {:port 3006}
                     :client-version    client-version
                     :contentful-config {:cache-timeout 60000
                                         :endpoint      "https://cdn.contentful.com"}
                     :logging           {:system-name "storefront.system"}})

(def env-config {:environment       (env :environment)
                 :bugsnag-token     (env :bugsnag-token)
                 ;;TODO Update env-vars
                 :welcome-config    {:url (env :welcome-url)}
                 :contentful-config {:api-key  (env :contentful-content-delivery-api-key)
                                     :space-id (env :contentful-space-id)}
                 :storeback-config  {:endpoint          (env :storeback-endpoint)
                                     :internal-endpoint (or
                                                         (env :storeback-v2-internal-endpoint)
                                                         (env :storeback-internal-endpoint))}})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))

(def pixlee {})
