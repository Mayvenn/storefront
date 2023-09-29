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
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.asset-mappings :as asset-mappings]
            [storefront.safe-hiccup :refer [html5 raw]]
            [storefront.seo-tags :as seo]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes])
  (:import java.util.zip.GZIPInputStream
           java.io.ByteArrayOutputStream))

  (def mayvenn-logo-splash "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 208 121\"><path fill=\"#000\" d=\"M104 62.855L81.778 31.513 104 .004l22.222 31.509L104 62.855zM208 121c-1.495-18.277-9.127-36.945-17.365-49.61-10.964-16.856-22.524-31.478-22.524-48.62 0-11.409 5.647-21.514 6.268-22.672L104.003 0H104h-.003L33.62.098c.622 1.158 6.269 11.263 6.269 22.673 0 17.141-11.12 32.059-22.524 48.62C9.413 82.937 1.495 102.722 0 121h52.664a53.858 53.858 0 01-2.475-16.177 53.626 53.626 0 0112.36-34.33L97.964 121h12.072l35.415-50.508a53.626 53.626 0 0112.36 34.331c0 5.638-.87 11.07-2.475 16.177H208z\"/><path clip-rule=\"evenodd\" fill-rule=\"evenodd\"</svg>")

(def spinner-content
  [:div {:style "height: 100vh"}
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
  '([:link {:href  "/apple-touch-icon.png",
            :sizes "180x180",
            :rel   "apple-touch-icon"}]
    [:link {:href  "/favicon-32.png",
            :sizes "32x32",
            :type  "image/png",
            :rel   "icon"}]
    [:link {:href  "/favicon-16.png",
            :sizes "16x16",
            :type  "image/png",
            :rel   "icon"}]
    [:link {:rel "manifest" :href "/web_app_manifest.json"}]
    [:link {:href  "/safari-pinned-tab.svg"
            :rel   "mask-icon"
            :color "black"}]
    [:meta {:content "#FFF", :name "msapplication-TileColor"}]))

(defn data->transit [data]
  (let [sanitized-data (-> data
                           sanitize
                           (assoc-in keypaths/static (get-in data keypaths/static)))
        out            (ByteArrayOutputStream.)
        writer         (transit/writer out :json)
        _              (transit/write writer sanitized-data)]
    (generate-string (.toString out "UTF-8"))))

(def ^:private dashboard-events
  #{events/navigate-stylist-dashboard-balance-transfer-details
    events/navigate-stylist-dashboard-order-details
    events/navigate-stylist-dashboard-cash-out-begin
    events/navigate-stylist-dashboard-cash-out-pending
    events/navigate-stylist-dashboard-cash-out-success
    events/navigate-stylist-share-your-store
    events/navigate-stylist-account-profile
    events/navigate-stylist-account-portrait
    events/navigate-stylist-account-password
    events/navigate-stylist-account-payout
    events/navigate-stylist-account-social
    events/navigate-v2-stylist-dashboard-payments
    events/navigate-v2-stylist-dashboard-orders
    events/navigate-v2-stylist-dashboard-payout-rates
    events/navigate-gallery-image-picker
    events/navigate-gallery-edit})

(def ^:private checkout-events
  #{events/navigate-checkout-returning-or-guest
    events/navigate-checkout-sign-in
    events/navigate-checkout-address
    events/navigate-checkout-payment
    events/navigate-checkout-confirmation
    events/navigate-order-complete
    events/navigate-checkout-processing})

(def ^:private catalog-events
  #{events/navigate-shop-by-look
    events/navigate-shop-by-look-details
    events/navigate-category
    events/navigate-product-details
    events/navigate-shared-cart
    events/navigate-cart
    events/navigate-adventure-find-your-stylist
    events/navigate-adventure-stylist-results
    events/navigate-adventure-match-success
    events/navigate-adventure-stylist-profile
    events/navigate-adventure-stylist-profile-reviews
    events/navigate-adventure-stylist-gallery})

(def ^:private homepage-events
  #{events/navigate-home
    events/navigate-landing-page})

(def ^:private voucher-redeem
  #{events/navigate-voucher-redeem events/navigate-voucher-redeemed})

(defn js-modules [nav-event]
  (let [files (mapcat (config/frontend-modules)
                      (concat [:cljs_base :ui]
                              (cond
                                (homepage-events nav-event)  [:catalog :homepage]
                                (dashboard-events nav-event) [:catalog :dashboard]
                                (checkout-events nav-event)  [:catalog :checkout]
                                (catalog-events nav-event)   [:catalog]
                                (voucher-redeem nav-event)   [:catalog :redeem])
                              [:main]))]
    (assert (every? (complement nil?) files)
            (str "Incorrectly wired module to load: " (pr-str files)))
    files))

(defn layout
  [{:keys [storeback-config environment client-version wirewheel-config]} data initial-content]
  (let [home?    (= events/navigate-home (get-in data keypaths/navigation-event))
        shop?    (or (= "shop" (get-in data keypaths/store-slug))
                    (= "retail-location" (get-in data keypaths/store-experience)))
        follow?  (and (not= "acceptance" environment)
                     shop?)
        index?   (and (not= "acceptance" environment)
                      (or shop? home?))
        js-files (js-modules (get-in data keypaths/navigation-event))]
    (html5 {:lang "en"}
           [:head
            (when home?
              [:link {:rel  "preload"
                      :as   "image"
                      :href "https://images.ctfassets.net/76m8os65degn/3G0TE1RlgrcTA0rPEpkQJ1/faa7337aa049c535f78ab07f2f869198/homepage_hero_012921_image_mob-04.jpg?fm=webp&q=75&w=1600"}])
            [:meta {:name "fragment" :content "!"}]
            [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
            [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
            [:meta {:name "theme-color" :content "#ffffff"}]
            [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
            [:meta {:name "apple-mobile-web-app-status-bar-style" :content "white"}]
            [:meta {:name "mobile-web-app-capable" :content "yes"}]

            (when-not index?
              [:meta {:name "robots" :content "noindex"}])
            (when-not follow?
              [:meta {:name "robots" :content "nofollow"}])

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
            [:link {:rel "preconnect" :href "https://widgets.quadpay.com"}]
            [:link {:rel "preconnect" :href "https://www.google.com"}]
            [:link {:rel "preconnect" :href "https://settings.luckyorange.net"}]
            [:link {:rel "preconnect" :href "https://beacon.riskified.com"}]
            [:link {:rel "preconnect" :href "https://www.googletagmanager.com"}]
            [:link {:rel "preconnect" :href "https://connect.facebook.net"}]
            [:link {:rel "preconnect" :href "https://cx.atdmt.com"}]
            [:link {:rel "preconnect" :href "https://s.pinimg.com"}]
            [:link {:rel "preconnect" :href "https://googleads.g.doubleclick.net"}]
            [:link {:rel "preconnect" :href "https://d10lpsik1i8c69.cloudfront.net"}] ;; luckyorange

            [:script {:defer true
                      :type  "text/javascript"
                      :src   "https://cdnjs.cloudflare.com/ajax/libs/tiny-slider/2.9.2/min/tiny-slider.js"}]

            (when-not (config/development? environment)
              (for [n js-files]
                [:link {:rel "preload" :as "script" :href n}]))

            [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Canela-Light-Web.woff2") :crossorigin "anonymous"}]
            [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Proxima-Nova.woff2") :crossorigin "anonymous"}]
            [:link {:rel "preload" :as "font" :href (assets/path "/fonts/Proxima-Nova-Black.woff2") :crossorigin "anonymous"}]

            [:script {:type "text/javascript"} (raw prefetch-script)]

            ;; Quadpay Widget
            #_[:script {:type  "text/javascript"
                        :src   "https://widgets.quadpay.com/mayvenn/quadpay-widget-2.2.1.js"
                        :defer true}]

            [:script {:type  "text/javascript"
                      :async true
                      :src   "https://static.klaviyo.com/onsite/js/klaviyo.js?company_id=WbkmAm"}]

            ;; Storefront server-side data
            [:script {:type "text/javascript"}
             (raw (str "var assetManifest=" (generate-string (select-keys asset-mappings/image-manifest (map #(subs % 1) config/frontend-assets))) ";"
                       "var cdnHost=" (generate-string asset-mappings/cdn-host) ";"
                       (when-not (config/development? environment)
                         ;; Use CDN urls when not in dev, otherwise let figwheel control the compiled modules
                         (str "var COMPILED_MODULE_URIS=" (json/generate-string (config/frontend-modules)) ";"))
                       ;; need to make sure the edn which has double quotes is validly escaped as
                       ;; json as it goes into the JS file
                       (format "var data = %s;" (data->transit (dissoc data (first keypaths/categories))))
                       "var environment=\"" environment "\";"
                       "var clientVersion=\"" client-version "\";"
                       "var apiUrl=\"" (:endpoint storeback-config) "\";"
                       "var wwUpcpUrl=\"" (:upcp-iframe-src wirewheel-config) "\";"
                       "var WireWheelUPCPConfig=" (json/generate-string {:wwupcp_apiurl               (:api-base-url wirewheel-config)
                                                                         :wwupcp_apikey               (:api-key wirewheel-config)
                                                                         :wwupcp_cookie_domain        (routes/environment->hostname environment)
                                                                         :wwupcp_cookie_duration      400
                                                                         :wwupcp_cmp_default_consents [{:target "doNotSellOrShareMyPersonalInformationChoosingOptOutMeansWeWillNotSellOrShare"
                                                                                                        :action "ACCEPT"}]}) ";"))]

            (when-not (config/development? environment)
              (for [n js-files]
                [:script {:src n
                          :defer true}]))

          ;;;;;;;;; "Third party" libraries
            ;; Stringer
            [:script {:type "text/javascript"}
             (raw (str "(function(d,e){function g(a){return function(){var b=Array.prototype.slice.call(arguments);b.unshift(a);c.push(b);return d.stringer}}var c=d.stringer=d.stringer||[],a=[\"init\",\"track\",\"identify\",\"clear\",\"getBrowserId\"];if(!c.snippetRan&&!c.loaded){c.snippetRan=!0;for(var b=0;b<a.length;b++){var f=a[b];c[f]=g(f)}a=e.createElement(\"script\");a.type=\"text/javascript\";a.async=!0;a.src=\"https://d6w7wdcyyr51t.cloudfront.net/cdn/stringer/stringer-8537ec7.js\";b=e.getElementsByTagName(\"script\")[0];b.parentNode.insertBefore(a,b);c.init({environment:\"" environment "\",sourceSite:\"storefront\"})}})(window,document);"))]

            ;; Google Tag Manager
            (let [gtm-container-id (case environment
                                     "production"  "GTM-NNC8T99"
                                     "acceptance"  "GTM-NNC8T99"
                                     "GTM-KLFHMCS"  ; Dev & test
                                     )]
              [:script {:type "text/javascript"}
               (raw (str "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','" gtm-container-id "');"))])

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
                [:script {:src n}]))])))

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
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
          [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
          favicon-links
          (page/include-css (assets/path "/css/app.css"))]
         [:body.bg-pale-purple.proxima.px2
          [:div.col-9-on-tb-dt.mx-auto.flex.justify-center.items-center.stretch.center.relative
           [:div.absolute.top-0.proxima.title-1.white
            {:style "font-size: 250px; opacity: 50%;"}
            "404"]
           [:div.col-10
            [:div.py2
             [:img {:height "22px"
                    :width  "38px"
                    :src    (assets/path "/images/mayvenn_m_logo.svg")}]]
            [:div.canela.title-1.my3 "Sorry"]
            [:div.content-1.mt3.mb4 "We could not connect you to the page you are looking for."]
            [:div.col-8.col-6-on-dt.mx-auto (ui/button-large-primary (utils/route-to events/navigate-home) "Back to Home")]]]]))

(defn error-page [debug? reason]
  (html5 {:lang "en"}
         [:head
          [:meta {:name "fragment" :content "!"}]
          [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
          [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
          [:title "Something went wrong | Mayvenn"]]
         [:body.proxima
          [:h3.content-2 "Mayvenn Will Be Back Soon"]
          [:h4.content-3 "We apologize for the inconvenience and appreciate your patience. Please check back soon."]
          (when debug?
            [:div
             [:h2 "Debug:"]
             [:pre reason]])]))
