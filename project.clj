(defproject storefront "0.1.0-SNAPSHOT"
  :description "The front of the store"
  :url "https://github.com/Mayvenn/storefront"
  :license {:name "All rights reserved"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [environ "1.1.0"]
                 [tocsin "0.1.4"]
                 [tugboat "0.1.22-SNAPSHOT"]
                 [mayvenn/spice "0.1.60"]
                 [compojure "1.6.1"]
                 [noir-exception "0.2.3"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.3.2" :exclusions [[javax.servlet/servlet-api]]]
                 [ring-jetty-component "0.3.1"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [org.eclipse.jetty/jetty-server                "9.4.12.v20180830"]
                 [org.eclipse.jetty.websocket/websocket-servlet "9.4.12.v20180830"]
                 [org.eclipse.jetty.websocket/websocket-server  "9.4.12.v20180830"]
                 [hiccup "1.0.5"]
                 [cheshire "5.8.0"]
                 [cljsjs/google-maps "3.18-1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [cljsjs/react "15.6.1-2"]
                 [cljsjs/react-dom "15.6.1-2"]
                 [cljsjs/react-dom-server "15.6.1-2"]
                 [cljsjs/react-transition-group "1.2.0-0"]
                 [org.omcljs/om "1.0.0-beta1" :exclusions [cljsjs/react com.cognitect/transit-cljs]]
                 [org.clojure/core.async "0.3.443"]
                 [cljs-ajax "0.7.3" :exclusions [org.apache.httpcomponents/httpclient]]
                 [comb "0.1.1"]
                 [sablono "0.8.0"]
                 [bidi "2.1.5"]
                 [lambdaisland/uri "1.1.0"]
                 [com.cemerick/url "0.1.1"]
                 [overtone/at-at "1.2.0"]
                 [camel-snake-kebab "0.4.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.clojure/test.check "0.10.0-alpha3"]]
  :repositories [["private" {:url "s3p://mayvenn-dependencies/releases/"}]]
  :plugins [[s3-wagon-private "1.3.1"]
            [lein-cljsbuild "1.1.7"]
            [lein-cljfmt "0.1.10"]]
  :figwheel {:nrepl-port 4000
             :css-dirs   ["resources/public/css"]}
  :main storefront.core
  :repl-options {:init-ns user}
  :jvm-opts ~(concat
              ["-Xmx1024m" "-XX:-OmitStackTraceInFastThrow"]
              (let [version     (System/getProperty "java.version")
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
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]}
  :cljsbuild
  {:builds
   {:release
    {:source-paths     ["src-cljc" "src-cljs"]
     :warning-handlers [cljs.analyzer/default-warning-handler
                        (fn [warning-type env extra]
                          (when (warning-type cljs.analyzer/*cljs-warnings*)
                            (when-let [s (cljs.analyzer/error-message warning-type extra)]
                              (binding [*out* *err*]
                                (println (cljs.analyzer/message env s))
                                (System/exit 1)))))]
     :compiler         {:output-dir       "target/release/js/out"
                        :asset-path       "/js/out"
                        :source-map       false
                        :modules          {:cljs-base {:output-to "target/release/js/out/cljs_base.js"}
                                           :main      {:output-to "target/release/js/out/main.js"
                                                       :entries   #{storefront.core}}
                                           :redeem    {:output-to  "target/release/js/out/redeem.js"
                                                       :entries    #{voucher.redeem voucher.redeemed}
                                                       :depends-on #{:main}}}
                        :infer-externs    false
                        :static-fns       true
                        :fn-invoke-direct true
                        :parallel-build   true
                        :npm-deps         false
                        :install-deps     false
                        :pseudo-names     true
                        :libs             ["src-cljs/rng/rng.js"]
                        :foreign-libs     [{:file     "src-cljs/storefront/jsQR.js"
                                            :file-min "target/min-js/jsQR.js"
                                            :provides ["jsQR"]}
                                           {:file     "src-cljs/storefront/react-slick.js"
                                            :file-min "target/min-js/react-slick.js" ;; created by gulp
                                            :provides ["react-slick"]}
                                           {:file     "src-cljs/storefront/bugsnag-2.5.0.js"
                                            :file-min "target/min-js/bugsnag-2.5.0.js"
                                            :provides ["bugsnag"]}]
                        :externs          ["externs/jsQR.js"
                                           "externs/bugsnag.js"
                                           "externs/convert.js"
                                           "externs/facebook.js"
                                           "externs/google_autocomplete.js"
                                           "externs/pixlee.js"
                                           "externs/quadpay.js"
                                           "externs/react-slick.js"
                                           "externs/riskified.js"
                                           "externs/spreedly.js"
                                           "externs/stringer.js"
                                           "externs/stripe.js"
                                           "externs/talkable.js"
                                           "externs/uploadcare.js"
                                           "externs/wistia.js"
                                           "externs/yotpo.js"]
                        :optimizations    :advanced}}}}
  :auto-clean false
  :profiles {:uberjar {:aot :all}
             :test    {:plugins [[lein-test-report-junit-xml "0.2.0"]]}
             :repl    {:dependencies
                       [[cider/piggieback "0.4.0"]
                        [nrepl "0.6.0"]]}
             :dev     {:source-paths ["dev/clj"]
                       :dependencies [[cider/piggieback "0.4.0"]
                                      [com.bhauman/figwheel-main "0.2.1-SNAPSHOT"]
                                      [com.bhauman/rebel-readline-cljs "0.1.4"]
                                      [nrepl "0.6.0"]
                                      [binaryage/devtools "0.9.10"]
                                      [org.clojure/tools.reader "1.3.0"]
                                      [pjstadig/humane-test-output "0.8.1"]
                                      [standalone-test-server "0.7.2"]
                                      [ring/ring-mock "0.3.0"]
                                      [org.clojure/tools.namespace "0.2.11"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]
                       :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                       :cljsbuild
                       {:builds {:dev {:source-paths ["dev/cljs"]}}}}})
