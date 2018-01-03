(defproject storefront "0.1.0-SNAPSHOT"
  :description "The front of the store"
  :url "https://github.com/Mayvenn/storefront"
  :license {:name "All rights reserved"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "3.4.0" :exclusions [org.clojure/tools.reader]]
                 [com.stuartsierra/component "0.3.2"]
                 [environ "1.1.0"]
                 [tocsin "0.1.2"]
                 [tugboat "0.1.6"]
                 [mayvenn/spice "0.1.31"]
                 [compojure "1.4.0"]
                 [noir-exception "0.2.3"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.4"]
                 [ring-jetty-component "0.3.1"]
                 [org.eclipse.jetty/jetty-server "9.3.9.v20160517"]
                 [hiccup "1.0.5"]
                 [cheshire "5.5.0"]
                 [cljsjs/google-maps "3.18-1"]
                 [org.clojure/clojurescript "1.9.671"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]
                 [cljsjs/react "15.6.1-2"]
                 [cljsjs/react-dom "15.6.1-2"]
                 [cljsjs/react-dom-server "15.6.1-2"]
                 [cljsjs/react-transition-group "1.2.0-0"]
                 [org.omcljs/om "1.0.0-beta1" :exclusions [cljsjs/react]]
                 [comb "0.1.0"]
                 [sablono "0.8.0"]
                 [cljs-ajax "0.3.11"]
                 [bidi "2.0.16"]
                 [com.cemerick/url "0.1.1"]]
  :repositories [["private" {:url "s3p://mayvenn-dependencies/releases/" :no-auth true}]]
  :plugins [[s3-wagon-private "1.3.0"]
            [lein-cljsbuild "1.1.2"]
            [lein-cljfmt "0.1.10"]
            [lein-figwheel "0.5.11"]]
  :figwheel {:nrepl-port 4000
             :css-dirs ["resources/public/css"]}
  :main storefront.core
  :repl-options {:init-ns user}
  :jvm-opts ~(concat
              ["-Xmx512m" "-XX:-OmitStackTraceInFastThrow"]
              (let [version (System/getProperty "java.version")
                    [major _ _] (clojure.string/split version #"\.")]
                (if (>= (Integer. major) 9)
                  ["--add-modules" "java.xml.bind"]
                  [])))
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/out/"
                                    "resources/public/css/"
                                    "resources/public/cdn"]
  :source-paths ["src" "src-cljc" "src-cljs"]
  :resource-paths ["resources"]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src-cljc" "src-cljs"]
     :figwheel {:on-jsload "storefront.core/on-jsload"}
     :compiler {:main "storefront.core"
                :asset-path "/js/out"
                :output-to "resources/public/js/out/main.js"
                :output-dir "resources/public/js/out"
                :pretty-print true
                :foreign-libs [{:file "src-cljs/storefront/react-slick.js"
                                :provides ["react-slick"]}
                               {:file "src-cljs/storefront/bugsnag-2.5.0.js"
                                :provides ["bugsnag"]}]
                :externs ["externs/bugsnag.js"
                          "externs/convert.js"
                          "externs/facebook.js"
                          "externs/pixlee.js"
                          "externs/react-slick.js"
                          "externs/riskified.js"
                          "externs/stringer.js"
                          "externs/stripe.js"
                          "externs/talkable.js"
                          "externs/uploadcare.js"
                          "externs/wistia.js"
                          "externs/yotpo.js"]}}
    :release
    {:source-paths ["src-cljc" "src-cljs"]
     :warning-handlers [cljs.analyzer/default-warning-handler
                        (fn [warning-type env extra]
                          (when (warning-type cljs.analyzer/*cljs-warnings*)
                            (when-let [s (cljs.analyzer/error-message warning-type extra)]
                              (binding [*out* *err*]
                                (println (cljs.analyzer/message env s))
                                (System/exit 1)))))]
     :compiler {:main "storefront.core"
                :output-to "target/release/js/out/main.js"
                :output-dir "target/release/js/out"
                :source-map "target/release/js/out/main.js.map"
                :source-map-path "/js/out"
                :pretty-print false
                :infer-externs true
                :foreign-libs [{:file "storefront/react-slick.js"
                                :file-min "target/min-js/react-slick.js" ;; created by gulp
                                :provides ["react-slick"]}
                               {:file "storefront/bugsnag-2.5.0.js"
                                :file-min "target/min-js/bugsnag-2.5.0.js"
                                :provides ["bugsnag"]}]
                :externs ["externs/bugsnag.js"
                          "externs/convert.js"
                          "externs/facebook.js"
                          "externs/pixlee.js"
                          "externs/react-slick.js"
                          "externs/riskified.js"
                          "externs/stringer.js"
                          "externs/stripe.js"
                          "externs/talkable.js"
                          "externs/uploadcare.js"
                          "externs/wistia.js"
                          "externs/yotpo.js"]
                :optimizations :advanced}}}}
  :auto-clean false
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev/clj"]
                   :dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [pjstadig/humane-test-output "0.8.1"]
                                  [standalone-test-server "0.7.2"]
                                  [ring/ring-mock "0.3.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [figwheel-sidecar "0.5.11"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :cljsbuild
                   {:builds {:dev {:source-paths ["dev/cljs"]}}}}})
