(defproject storefront "0.1.0-SNAPSHOT"
  :description "The front of the store"
  :url "https://github.com/Mayvenn/storefront"
  :license {:name "All rights reserved"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [com.launchdarkly/launchdarkly-java-server-sdk "4.11.1"]
                 [environ "1.1.0"]
                 [tocsin "0.1.4"]
                 [tugboat "0.1.27"]
                 [mayvenn/tracer "0.1.3"]
                 [mayvenn/spice "0.1.65"]
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
                 [cljsjs/react "16.9.0-0"]
                 [cljsjs/react-dom "16.9.0-0"]
                 [cljsjs/react-dom-server "16.9.0-0"]
                 [cljsjs/react-transition-group "4.2.1-0"]
                 ;; polyfill react 15 methods
                 [cljsjs/create-react-class "15.5.3-0"]
                 [org.clojure/core.async "0.3.443"]
                 [cljs-ajax "0.7.3" :exclusions [org.apache.httpcomponents/httpclient]]
                 [comb "0.1.1"]
                 [sablono "0.8.6"]
                 [bidi "2.1.5"]
                 [lambdaisland/uri "1.1.0"]
                 [com.cemerick/url "0.1.1"]
                 [overtone/at-at "1.2.0"]
                 [camel-snake-kebab "0.4.0"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [meander/epsilon "0.0.650"]
                 [markdown-clj "1.10.6"]]
  :repositories [["private" {:url "s3p://mayvenn-dependencies/releases/"}]]
  :plugins [[s3-wagon-private "1.3.1"]
            [lein-cljsbuild "1.1.7"]
            [lein-cljfmt "0.6.4"]]
  :main storefront.core
  :repl-options {:init-ns user}
  :jvm-opts ~(concat
              ["-Xmx1024m" "-XX:-OmitStackTraceInFastThrow"]
              (let [version               (System/getProperty "java.version")
                    [major _minor _patch] (clojure.string/split version #"\.")]
                (cond
                  (#{9 10} (Integer. major)) ["--add-modules" "java.xml.bind"]
                  :else                      [])))
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/out/"
                                    "resources/public/css/"
                                    "resources/public/cdn"]
  :source-paths ["src" "src-cljc" "src-cljs"]
  :resource-paths ["resources"]
  :aliases {"fig"    ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:pc" ["trampoline" "run" "-m" "figwheel.main" "-pc" "-b" "dev" "-r"]}
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
                        :asset-path       "/js/out" ; our CDN host is defined at startup time, so we're basically ignoring this value
                        :source-map       true
                        ;; Don't forget to update config.clj > define-frontend-modules
                        ;; Don't forget to update dev.cljs.edn
                        ;; Don't forget to update views.clj > js-modules
                        ;; Read: https://clojurescript.org/news/2017-07-10-code-splitting#_technical_description
                        ;; To predict which modules will contain what code
                        :modules          {:cljs-base     {:output-to "target/release/js/out/cljs_base.js"}
                                           :ui            {:output-to "target/release/js/out/ui.js"
                                                           :entries   #{storefront.components.ui}}
                                           :homepage      {:output-to  "target/release/js/out/homepage.js"
                                                           :entries    #{homepage.core}
                                                           :depends-on #{:ui}}
                                           :main          {:output-to  "target/release/js/out/main.js"
                                                           :entries    #{storefront.core}
                                                           :depends-on #{:ui}}
                                           :dashboard     {:output-to  "target/release/js/out/dashboard.js"
                                                           :entries    #{stylist.dashboard}
                                                           :depends-on #{:catalog}}
                                           :redeem        {:output-to  "target/release/js/out/redeem.js"
                                                           :entries    #{voucher.redeem}
                                                           :depends-on #{:catalog}}
                                           :design-system {:output-to  "target/release/js/out/design-system.js"
                                                           :entries    #{design-system.home}
                                                           :depends-on #{:dashboard :main :redeem :catalog :checkout}}
                                           :catalog       {:output-to  "target/release/js/out/catalog.js"
                                                           :entries    #{catalog.core stylist-profile.stylist-details}
                                                           :depends-on #{:ui}}
                                           :checkout      {:output-to  "target/release/js/out/checkout.js"
                                                           :entries    #{checkout.core}
                                                           :depends-on #{:catalog}}}
                        ;; rename-prefix: for a ~2% gzipped file size tax, prefixes all
                        ;; storefront's minified vars with m_ to avoid naming conflicts
                        ;; with minified code from google tag manager. Any
                        ;; exported function still retains its described name
                        :rename-prefix    "m_"
                        :closure-defines  {goog.DEBUG false}
                        :infer-externs    false
                        :static-fns       true
                        :fn-invoke-direct true
                        :parallel-build   true
                        :language-out     :es6-strict
                        :npm-deps         false
                        :install-deps     false
                        :pseudo-names     false
                        :foreign-libs     [{:file     "src-cljs/storefront/jsQR.js"
                                            :file-min "target/min-js/jsQR.js"
                                            :provides ["jsQR"]}
                                           {:file     "src-cljs/storefront/muuri.js"
                                            :file-min "target/min-js/muuri.js"
                                            :provides ["Muuri"]}
                                           {:file     "src-cljs/storefront/muuri_react.js"
                                            :file-min "target/min-js/muuri_react.js"
                                            :requires ["Muuri"]
                                            :provides ["MuuriReact"]}
                                           {:file     "src-cljs/storefront/bugsnag-2.5.0.js"
                                            :file-min "target/min-js/bugsnag-2.5.0.js"
                                            :provides ["bugsnag"]}]
                        :externs          ["externs/luckyorange.js"
                                           "externs/jsQR.js"
                                           "externs/muuri.js"
                                           "externs/muuri_react.js"
                                           "externs/bugsnag.js"
                                           "externs/browser.js"
                                           "externs/convert.js"
                                           "externs/facebook.js"
                                           "externs/google_maps.js"
                                           "externs/kustomer.js"
                                           "externs/quadpay.js"
                                           "externs/riskified.js"
                                           "externs/spreedly.js"
                                           "externs/stringer.js"
                                           "externs/stripe.js"
                                           "externs/uploadcare.js"
                                           "externs/wistia.js"
                                           "externs/yotpo.js"]
                        :optimizations    :advanced}}}}
  :auto-clean false
  :profiles {:uberjar {:aot :all}
             :test    {:plugins [[lein-test-report-junit-xml "0.2.0"]]}
             :repl    {:dependencies [[cider/piggieback "0.5.2"]
                                      [nrepl "0.8.3"]]}
             :dev     {:source-paths ["dev/clj"]
                       :dependencies [[com.bhauman/figwheel-main "0.2.10"]
                                      [com.bhauman/rebel-readline-cljs "0.1.4"]
                                      [org.clojure/tools.reader "1.3.2"]
                                      [org.clojure/tools.namespace "1.0.0"]
                                      [binaryage/devtools "0.9.10"]
                                      [pjstadig/humane-test-output "0.8.1"]
                                      [standalone-test-server "0.7.2"]
                                      [ring/ring-mock "0.3.0"]]
                       :injections   [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]
                       :cljsbuild
                       {:builds {:dev {:source-paths ["dev/cljs"]}}}}})
