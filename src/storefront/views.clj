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

  (def mayvenn-logo-splash "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 208 121\"><path fill=\"#000\" d=\"M104 62.855L81.778 31.513 104 .004l22.222 31.509L104 62.855zM208 121c-1.495-18.277-9.127-36.945-17.365-49.61-10.964-16.856-22.524-31.478-22.524-48.62 0-11.409 5.647-21.514 6.268-22.672L104.003 0H104h-.003L33.62.098c.622 1.158 6.269 11.263 6.269 22.673 0 17.141-11.12 32.059-22.524 48.62C9.413 82.937 1.495 102.722 0 121h52.664a53.858 53.858 0 01-2.475-16.177 53.626 53.626 0 0112.36-34.33L97.964 121h12.072l35.415-50.508a53.626 53.626 0 0112.36 34.331c0 5.638-.87 11.07-2.475 16.177H208z\"/><path clip-rule=\"evenodd\" fill-rule=\"evenodd\"</svg>")

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
    [:link {:href  "/safari-pinned-tab.svg"}]
    [:link {:href "/favicon.ico", :rel "shortcut icon"}]
    [:meta {:content "#FFF", :name "msapplication-TileColor"}]))

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
          [:link {:rel "preconnect" :href "https://cdnjs.cloudflare.com"}]
          [:link {:rel "preconnect" :href "https://www.facebook.com"}]
          [:link {:rel "preconnect" :href "https://analytics.twitter.com"}]
          [:link {:rel "preconnect" :href "https://t.co"}]
          [:link {:rel "preconnect" :href "https://stats.g.doubleclick.net"}]
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
          [:link {:rel "preconnect" :href "https://d10lpsik1i8c69.cloudfront.net"}] ;; luckyorange

          [:script {:defer true
                    :type  "text/javascript"
                    :src   "https://cdnjs.cloudflare.com/ajax/libs/tiny-slider/2.9.2/min/tiny-slider.js"}]

          (when-not (config/development? environment)
            (for [n js-files]
              [:link {:rel "preload" :as "script" :href (assets/path (str "/js/out/" n))}]))

          [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Canela-Light-Web.woff2")}]
          [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Proxima-Nova.woff")}]
          [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Proxima-Nova-Black.woff")}]

          [:script {:type "text/javascript"} (raw prefetch-script)]

          ;; Quadpay Widget
          #_[:script {:type  "text/javascript"
                      :src   "https://widgets.quadpay.com/mayvenn/quadpay-widget-2.2.1.js"
                      :defer true}]

          ;; Talkable
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

          ;; Google Tag Manager
          [:script {:type "text/javascript"}
           (raw "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');")]
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
         [:body
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
         [:body.bg-cool-gray
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
          [:h3.h4 "Mayvenn Will Be Back Soon"]
          [:h4.h5 "We apologize for the inconvenience and appreciate your patience. Please check back soon."]
          (when debug?
            [:div
             [:h2 "Debug:"]
             [:pre reason]])]))
