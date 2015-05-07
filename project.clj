(defproject storefront "0.1.0-SNAPSHOT"
  :description "The front of the store"
  :url "https://github.com/Mayvenn/storefront"
  
            
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 [org.omcljs/om "0.8.8"]
                 [sablono "0.3.4"]
                 [cljs-ajax "0.3.11"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [bidi "1.18.10"]]
  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.1"]]
  :figwheel {:nrepl-port 4000
             :css-dirs ["resources/public/css"]}
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src-cljs"]
     :figwheel {:on-jsload "storefront.core/on-jsload"}
     :compiler {:main "storefront.core"
                :asset-path "/js/out"
                :output-to "resources/public/js/out/main.js"
                :pretty-print true
                :output-dir "resources/public/js/out"}}}}
  :profiles
  {:dev {:source-paths ["dev/clj"]
         :cljsbuild
         {:builds {:dev {:source-paths ["dev/cljs"]}}}}})
