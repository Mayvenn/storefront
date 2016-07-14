(ns storefront.views
  (:require [storefront.assets :refer [asset-map asset-path]]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.component-shim :as component]
            [storefront.seo-tags :as seo]
            [storefront.keypaths :as keypaths]
            [storefront.config :as config]
            [storefront.accessors.experiments :as experiments]
            [clojure.string :as string]
            [cheshire.core :refer [generate-string]]
            [storefront.safe-hiccup :refer [html5 raw]]
            [hiccup.page :as page]
            [hiccup.element :as element]
            [clojure.java.io :as io])
  (:import [java.util.zip GZIPInputStream]))

  (def mayvenn-logo-splash "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 158 120.1\"><path fill=\"#222\" d=\"M0 120.1l2.3-16.4h.3l6.7 13.5 6.6-13.5h.3l2.4 16.4H17l-1.6-11.7-5.8 11.7h-.5l-5.9-11.8-1.6 11.8H0zm145.1 0v-16.4h.3l10.9 12.6v-12.6h1.6v16.4h-.4l-10.8-12.4v12.4h-1.6zm-25.6 0v-16.4h.4l10.9 12.6v-12.6h1.6v16.4h-.4l-10.8-12.4v12.4h-1.7zm-22.1-16.4h9.4v1.6H99v5.1h7.7v1.6H99v6.4h7.7v1.6h-9.3v-16.3zm-23.9 0h1.8l5.4 12.7 5.5-12.7H88l-7.1 16.4h-.3l-7.1-16.4zm-20.6 0h1.9l4.2 6.8 4.1-6.8H65l-5.2 8.6v7.8h-1.6v-7.8l-5.3-8.6zm-15.2 0l7.7 16.4h-1.8l-2.6-5.4h-7l-2.6 5.4h-1.8l7.8-16.4h.3zm-.2 3.4l-2.8 5.9h5.6l-2.8-5.9z\"/><path clip-rule=\"evenodd\" fill-rule=\"evenodd\" fill=\"#77c8bd\" d=\"M56.7 57.1c-.7-8 3.3-14.7 8.4-20 7.5-7.8 17.9-14.9 21.2-20.2-14.1 11.9-35.6 15.2-29.6 40.2zM91.1 0c9.7 6.2 9.2 15.9 2.6 24.7-7.4 9.8-18.4 17.6-24.3 28.6-6.9 13-7.1 34.3 14 33.9-16.7-.2-17.4-18.9-8.9-33C82.7 40.5 121 15.4 91.1 0zM93 6.5C96.8 28.2 41.2 39 64 78.8 50.6 40.5 104.7 27.1 93 6.5z\"/></svg>")

(def spinner-content
  [:div {:style "height:100vh;"}
   [:div {:style "margin:auto; width:50%; position: relative; top: 50%; transform: translateY(-50%);"} (raw mayvenn-logo-splash)
    [:div {:style (str "height: 2em;"
                       "margin-top: 2em;"
                       "background-image: url('/images/spinner.svg');"
                       "background-size: contain;"
                       "background-position: center center;"
                       "background-repeat: no-repeat;")}]]])

(defn escape-js-string [v]
  (if (string? v)
    ;; Uses hiccup style escape instead of clojure.string/escape
    ;; as dealing with strings is easier than chars and ", ' are
    ;; handled by cheshire
    (.. ^String v
        (replace "<" "&lt;")
        (replace ">" "&gt;"))
    v))

(defn sanitize [v]
  (cond
    (vector? v)     (mapv sanitize v)
    (set? v)        (into #{} (map sanitize v))
    (sequential? v) (map sanitize v)
    (map? v)        (zipmap (keys v) (map sanitize (vals v)))
    :else           (escape-js-string v)))

(defn read-css []
  (with-open [css (->> (asset-map "css/full.css")
                       (str "public/cdn/")
                       io/resource
                       io/input-stream
                       GZIPInputStream.)]
    (slurp css)))
(def css-styles (memoize read-css))

(defn layout [{:keys [leads-config storeback-config environment]} data initial-content]
  (html5
   [:head
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
    (into '() (seo/tags-for-page data))

    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    [:script {:type "text/javascript"}
     ;; need to make sure the edn which has double quotes is validly escaped as
     ;; json as it goes into the JS file
     (raw (str "var data = " (generate-string (pr-str (sanitize data))) ";"))]
    [:script {:type "text/javascript"}
     (raw
      (str "var environment=\"" environment "\";"
           "var canonicalImage=\"" (asset-path "/images/home_image.jpg") "\";"
           "window.optimizely=" (generate-string (into ["bucketVisitor"] (get-in data keypaths/optimizely-buckets))) ";"
           "var apiUrl=\"" (:endpoint storeback-config) "\";"))]
    ;; in production, we want to load the script tag asynchronously which has better
    ;; support when that script tag is in the <head>
    (when-not (config/development? environment)
      [:script {:src (asset-path "/js/out/main.js") :async true}])
    ;; inline styles in production because our css file is so small and it avoids another round
    ;; trip request. At time of writing this greatly includes our pagespeed score
    (if (config/development? environment)
      (page/include-css (asset-path "/css/full.css"))
      [:style (css-styles)])]
   [:body {:data-snap-to "top"}
    [:div#content initial-content]
    ;; in development, figwheel uses document.write which can't be done asynchronously
    ;; additionally, we want developers to see the server side render, so we don't want
    ;; to put this tag in <head> and be synchronous
    (when (config/development? environment)
      [:script {:src (asset-path "/js/out/main.js")}])]))

(defn index [render-ctx data]
  (layout render-ctx data spinner-content))

(defn prerendered-page [render-ctx data]
  (layout render-ctx data (first (component/normalize-elements (top-level-component data nil {})))))

(def not-found
  (html5
   [:head
    [:title "Not Found | Mayvenn"]
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (page/include-css (asset-path "/css/app.css"))]
   [:body {:data-snap-to "top"}
    [:div.sans-serif.lg-up-col-6.mx-auto.flex.flex-column.items-center
     [:img.py2 {:src (asset-path "/images/header_logo.png")}]
     [:img.mx-auto.block {:src (asset-path "/images/not_found_head.png")
                          :style "max-width: 80%"}]
     [:div.h2.mt3.mb2.center "We can't seem to find the page you're looking for."]
     [:a.mx-auto.btn.btn-primary.col-10
      {:href "/"}
      [:div.h3.p1.letter-spacing-1 "Return to Homepage"]]]]))
