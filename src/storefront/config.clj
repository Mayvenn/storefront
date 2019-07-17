(ns storefront.config
  (:require [environ.core :refer [env]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [storefront.assets :as assets]
            [storefront.platform.asset-mappings :as asset-mappings]))

(def welcome-subdomain "welcome")

(def freeinstall-subdomain "freeinstall")

(defn define-frontend-modules []
  ;; all module names must be cljs mangled (aka - hyphens get converted to underscore)
  ;; See implementation of cljs.loader: https://github.com/clojure/clojurescript/blob/master/src/main/cljs/cljs/loader.cljs#L15-L46
  {:cljs_base     [(assets/path "/js/out/cljs_base.js")]
   :main          [(assets/path "/js/out/main.js")]
   :dashboard     [(assets/path "/js/out/dashboard.js")]
   :redeem        [(assets/path "/js/out/redeem.js")]
   :design-system [(assets/path "/js/out/design-system.js")]})

(def frontend-modules (memoize define-frontend-modules))

(def frontend-assets
  "Asset mappings to send to the frontend"
  #{"/images/icons/success.png"
    "/images/icons/profile.png"
    "/images/share/fb.png"
    "/images/share/twitter.png"
    "/images/share/sms.png"
    "/images/sprites.svg"
    "/images/icons/caret-left.png"
    "/css/app.css"
    "/images/share/stylist-gallery-icon.png"
    "/images/share/instagram-icon.png"
    "/images/share/styleseat-logotype.png"
    "/images/icons/collapse.png"
    "/images/icons/expand.png"
    "/images/header_logo.svg"
    "/images/icons/gallery-profile.png"
    "/images/icons/stylist-bug-no-pic-fallback.png"})

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
                     :contentful-config {:cache-timeout 120000
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
(def voucherify {})
