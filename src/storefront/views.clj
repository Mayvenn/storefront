(ns storefront.views
  (:require [cheshire.core :refer [generate-string]]
            [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [cheshire.core :as json]
            [hiccup.page :as page]
            [storefront.assets :as assets]
            [storefront.component :as component]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.keypaths :as keypaths]
            [storefront.platform.asset-mappings :as asset-mappings]
            [storefront.safe-hiccup :refer [html5 raw]]
            [storefront.seo-tags :as seo])
  (:import java.util.zip.GZIPInputStream
           java.io.ByteArrayOutputStream))

(def mayvenn-logo-splash "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 158 120.1\"><path fill=\"#222\" d=\"M0 120.1l2.3-16.4h.3l6.7 13.5 6.6-13.5h.3l2.4 16.4H17l-1.6-11.7-5.8 11.7h-.5l-5.9-11.8-1.6 11.8H0zm145.1 0v-16.4h.3l10.9 12.6v-12.6h1.6v16.4h-.4l-10.8-12.4v12.4h-1.6zm-25.6 0v-16.4h.4l10.9 12.6v-12.6h1.6v16.4h-.4l-10.8-12.4v12.4h-1.7zm-22.1-16.4h9.4v1.6H99v5.1h7.7v1.6H99v6.4h7.7v1.6h-9.3v-16.3zm-23.9 0h1.8l5.4 12.7 5.5-12.7H88l-7.1 16.4h-.3l-7.1-16.4zm-20.6 0h1.9l4.2 6.8 4.1-6.8H65l-5.2 8.6v7.8h-1.6v-7.8l-5.3-8.6zm-15.2 0l7.7 16.4h-1.8l-2.6-5.4h-7l-2.6 5.4h-1.8l7.8-16.4h.3zm-.2 3.4l-2.8 5.9h5.6l-2.8-5.9z\"/><path clip-rule=\"evenodd\" fill-rule=\"evenodd\" fill=\"#77c8bd\" d=\"M56.7 57.1c-.7-8 3.3-14.7 8.4-20 7.5-7.8 17.9-14.9 21.2-20.2-14.1 11.9-35.6 15.2-29.6 40.2zM91.1 0c9.7 6.2 9.2 15.9 2.6 24.7-7.4 9.8-18.4 17.6-24.3 28.6-6.9 13-7.1 34.3 14 33.9-16.7-.2-17.4-18.9-8.9-33C82.7 40.5 121 15.4 91.1 0zM93 6.5C96.8 28.2 41.2 39 64 78.8 50.6 40.5 104.7 27.1 93 6.5z\"/></svg>")

(def spinner-content
  [:div {:style "height:100vh;"}
   [:div {:style "margin:auto; width:50%; position: relative; top: 50%; transform: translateY(-50%);"}
    (raw mayvenn-logo-splash)
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
  (with-open [css (->> (asset-mappings/manifest "css/app.css")
                       (str "public/cdn/")
                       io/resource
                       io/input-stream
                       GZIPInputStream.)]
    (slurp css)))
(def css-styles (memoize read-css))

(defn prefetch-image [name src]
  (format "var %s=new Image();%s.src=\"%s\";"
          name
          name
          (assets/path src)))

(def prefetch-script
  (format "(function(){%s;%s;})();"
          (prefetch-image "spinner" "/images/spinner.svg")
          (prefetch-image "large_spinner" "/images/large-spinner.svg")))

(def favicon-links
  '([:link {:href  "/apple-touch-icon-precomposed.png",
            :sizes "180x180",
            :rel   "apple-touch-icon"}]
    [:link {:href  "/favicon-32x32.png",
            :sizes "32x32",
            :type  "image/png",
            :rel   "icon"}]
    [:link {:href  "/favicon-16x16.png",
            :sizes "16x16",
            :type  "image/png",
            :rel   "icon"}]
    [:link {:rel "manifest" :href "/web_app_manifest.json"}]
    [:link {:color "#40cbac",
            :href  "/safari-pinned-tab.svg",
            :rel   "mask-icon"}]
    [:link {:href "/favicon.ico", :rel "shortcut icon"}]
    [:meta {:content "#000000", :name "msapplication-TileColor"}]))

(defn data->transit [data]
  (let [sanitized-data (-> data
                           sanitize
                           (assoc-in keypaths/static (get-in data keypaths/static)))
        out            (ByteArrayOutputStream.)
        writer         (transit/writer out :json)
        _              (transit/write writer sanitized-data)]
    (generate-string (.toString out "UTF-8"))))

(def js-files ["cljs_base.js" "main.js"])

(defn layout
  [{:keys [storeback-config environment client-version]} data initial-content]
  (html5 {:lang "en"}
         [:head
          [:meta {:name "fragment" :content "!"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
          [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
          [:meta {:name "theme-color" :content "#ffffff"}]
          [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
          [:meta {:name "apple-mobile-web-app-status-bar-style" :content "white"}]
          [:meta {:name "mobile-web-app-capable" :content "yes"}]

          (into '() (seo/tags-for-page data))

          favicon-links
          (when asset-mappings/cdn-host [:link {:rel "preconnect" :href (str "https://" asset-mappings/cdn-host)}])
          [:link {:rel "preconnect" :href (:endpoint storeback-config)}]
          (case environment
            "production" [:link {:rel "preconnect" :href "https://t.mayvenn.com"}]
            "acceptance" [:link {:rel "preconnect" :href "https://t.diva-acceptance.com"}]
            [:link {:rel "preconnect" :href "http://byliner.localhost"}])
          [:link {:rel "preconnect" :href "https://t.mayvenn.com"}]
          [:link {:rel "preconnect" :href "https://ucarecdn.com"}]
          [:link {:rel "preconnect" :href "https://www.facebook.com"}]
          [:link {:rel "preconnect" :href "https://analytics.twitter.com"}]
          [:link {:rel "preconnect" :href "https://t.co"}]
          [:link {:rel "preconnect" :href "https://stats.g.doubleclick.net"}]
          [:link {:rel "preconnect" :href "https://static.ads-twitter.com"}]
          [:link {:rel "preconnect" :href "https://c.riskified.com"}]
          [:link {:rel "preconnect" :href "https://www.googleadservices.com"}]
          [:link {:rel "preconnect" :href "https://img.riskified.com"}]
          [:link {:rel "preconnect" :href "https://cdn-3.convertexperiments.com"}]
          [:link {:rel "preconnect" :href "https://widgets.quadpay.com"}]
          [:link {:rel "preconnect" :href "https://www.google.com"}]
          [:link {:rel "preconnect" :href "https://settings.luckyorange.net"}]
          [:link {:rel "preconnect" :href "https://beacon.riskified.com"}]
          [:link {:rel "preconnect" :href "https://www.googletagmanager.com"}]
          [:link {:rel "preconnect" :href "https://connect.facebook.net"}]
          [:link {:rel "preconnect" :href "https://cx.atdmt.com"}]
          [:link {:rel "preconnect" :href "https://d2jjzw81hqbuqv.cloudfront.net"}] ;; talkable
          [:link {:rel "preconnect" :href "https://s.pinimg.com"}]
          [:link {:rel "preconnect" :href "https://googleads.g.doubleclick.net"}]
          [:link {:rel "preconnect" :href "https://www.google-analytics.com"}]
          [:link {:rel "preconnect" :href "https://d10lpsik1i8c69.cloudfront.net"}] ;; luckyorange

          (when-not (config/development? environment)
            (for [n js-files]
              [:link {:rel "preload" :as "script" :href (assets/path (str "/js/out/" n))}]))

          [:script {:type "text/javascript"} (raw prefetch-script)]

          ;; Quadpay Widget -- TODO: can we move this below our app scripts in prod?
          [:script {:type  "text/javascript"
                    :src   "https://widgets.quadpay.com/mayvenn/quadpay-widget-2.2.1.js"
                    :defer true}]

          ;; Talkable -- TODO: try to move below app scripts (in prod)
          [:script {:type  "text/javascript"
                    :src   (case environment
                             "production" "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn.min.js"
                             "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn-staging.min.js")
                    :defer true}]

          ;; Storefront server-side data
          [:script {:type "text/javascript"}
           (raw (str "var assetManifest=" (generate-string (select-keys asset-mappings/image-manifest (map #(subs % 1) config/frontend-assets))) ";"
                     "var cdnHost=" (generate-string asset-mappings/cdn-host) ";"
                     (when-not (config/development? environment)
                       ;; Use CDN urls when not in dev, otherwise let figwheel control the compiled modules
                       (str "var COMPILED_MODULE_URIS=" (json/generate-string (config/frontend-modules)) ";"))
                     ;; need to make sure the edn which has double quotes is validly escaped as
                     ;; json as it goes into the JS file
                     (format "var data = %s;" (data->transit data))
                     "var environment=\"" environment "\";"
                     "var clientVersion=\"" client-version "\";"
                     "var apiUrl=\"" (:endpoint storeback-config) "\";"))]

          (when-not (config/development? environment)
            (for [n js-files]
              [:script {:src   (assets/path (str "/js/out/" n))
                        :defer true}]))

          ;;;;;;;;; "Third party" libraries
          ;; Stringer
          [:script {:type "text/javascript"}
           (raw (str "(function(d,e){function g(a){return function(){var b=Array.prototype.slice.call(arguments);b.unshift(a);c.push(b);return d.stringer}}var c=d.stringer=d.stringer||[],a=[\"init\",\"track\",\"identify\",\"clear\",\"getBrowserId\"];if(!c.snippetRan&&!c.loaded){c.snippetRan=!0;for(var b=0;b<a.length;b++){var f=a[b];c[f]=g(f)}a=e.createElement(\"script\");a.type=\"text/javascript\";a.async=!0;a.src=\"https://d6w7wdcyyr51t.cloudfront.net/cdn/stringer/stringer-8537ec7.js\";b=e.getElementsByTagName(\"script\")[0];b.parentNode.insertBefore(a,b);c.init({environment:\"" environment "\",sourceSite:\"storefront\"})}})(window,document);"))]

          ;; Facebook Pixel
          [:script {:type "text/javascript"}
           (let [facebook-pixel-id (case environment
                                     "production" "721931104522825"
                                     "139664856621138")]
             (raw (str
                   "!function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){n.callMethod?
n.callMethod.apply(n,arguments):n.queue.push(arguments)};if(!f._fbq)f._fbq=n;
n.push=n;n.loaded=!0;n.version='2.0';n.queue=[];t=b.createElement(e);t.async=!0;
t.src=v;s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}(window,
document,'script','https://connect.facebook.net/en_US/fbevents.js');
fbq('init', '" facebook-pixel-id "');")))]
          ;; Google Analytics
          [:script {:type "text/javascript"}

           (let [google-analytics-property (case environment
                                             "production" "UA-36226630-1"
                                             "UA-36226630-2")]
             (raw (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
 m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
 })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', '" google-analytics-property "', 'auto');
ga('require', 'displayfeatures');")))]

          ;; Google Tag Manager
          [:script {:type "text/javascript"}
           (raw "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');")]
          ;; Twitter Pixel
          [:script {:type "text/javascript"}
           (let [twitter-pixel-id (case environment
                                    "production" "o1tn1"
                                    "TEST")]
             (raw
              (str
               "!function(e,t,n,s,u,a){e.twq||(s=e.twq=function(){s.exe?s.exe.apply(s,arguments):s.queue.push(arguments);
},s.version='1.1',s.queue=[],u=t.createElement(n),u.async=!0,u.src='//static.ads-twitter.com/uwt.js',
a=t.getElementsByTagName(n)[0],a.parentNode.insertBefore(u,a))}(window,document,'script');
twq('init','" twitter-pixel-id "');")))]

          ;; Pinterest
          [:script {:type "text/javascript"}
           (let [pinterest-tag-id (case environment
                                    "production" 2617847432239
                                    2612961581995)]
             (raw (str "!function(e){if(!window.pintrk){window.pintrk=function(){window.pintrk.queue.push(Array.prototype.slice.call(arguments))};var n=window.pintrk;n.queue=[],n.version='3.0';var t=document.createElement('script');t.async=!0,t.src=e;var r=document.getElementsByTagName('script')[0];r.parentNode.insertBefore(t,r)}}('https://s.pinimg.com/ct/core.js');pintrk('load','" pinterest-tag-id "');pintrk('page');")))]

          ;;;;;;;;;;;;



          ;; inline styles in production because our css file is so small and it avoids another round
          ;; trip request. At time of writing this greatly includes our pagespeed score
          (if (#{"development" "test"} environment)
            (page/include-css (assets/path "/css/app.css"))
            [:style (raw (css-styles))])]
         [:body {:itemscope "itemscope" :itemtype "http://schema.org/Corporation"}
          [:div#content initial-content]
          (when (config/development? environment)
            ;; in development, figwheel uses document.write which can't be done asynchronously
            ;; additionally, we want developers to see the server side render, so we don't want
            ;; to put this tag in <head> and be synchronous
            (for [n js-files]
              [:script {:src (str "/js/out/" n)}]))]))

(defn index [render-ctx data]
  (layout render-ctx data spinner-content))

(defn prerendered-page [render-ctx data]
  (layout render-ctx data (-> (top-level-component data nil {})
                              component/normalize-elements
                              first)))

(def not-found
  (html5 {:lang "en"}
         [:head
          [:title "Not Found | Mayvenn"]
          [:meta {:name "fragment" :content "!"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
          [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
          favicon-links
          (page/include-css (assets/path "/css/app.css"))]
         [:body.bg-light-gray
          [:div.container
           [:div.col-9-on-tb-dt.mx-auto.px2.flex.flex-column.items-center
            {:style "min-height: 100vh;"}
            [:img.py2 {:src (assets/path "/images/header_logo.svg")}]
            (ui/ucare-img {:class "mx-auto block"
                           :style "max-width: 80%"} "2c16b22e-2a8c-4ac6-83c8-78e8aff1d558")
            [:div.h3.mt3.mb2.center "We can't seem to find the page you're looking for."]
            [:a.mx-auto.btn.btn-primary.col-10
             {:href "/"}
             [:div.h4.p1.letter-spacing-1 "Return to Homepage"]]]]]))

(defn error-page [debug? reason]
  (html5 {:lang "en"}
         [:head
          [:meta {:name "fragment" :content "!"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
          [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
          [:title "Something went wrong | Mayvenn"]]
         [:body
          {:itemscope "itemscope" :itemtype "http://schema.org/Corporation"}
          [:h3.h4 "Mayvenn Will Be Back Soon"]
          [:h4.h5 "We apologize for the inconvenience and appreciate your patience. Please check back soon."]
          (when debug?
            [:div
             [:h2 "Debug:"]
             [:pre reason]])]))
