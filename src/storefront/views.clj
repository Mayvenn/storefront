(ns storefront.views
  (:require [storefront.assets :refer [asset-path]]
            [clojure.string :as string]
            [cheshire.core :refer [generate-string]]
            [hiccup.page :as page]
            [hiccup.element :as element]))

  (def mayvenn-logo-splash "<svg version=\"1.1\" id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" x=\"0px\" y=\"0px\" viewBox=\"0 0 158 120.1\" style=\"enable-background:new 0 0 158 120.1;\" xml:space=\"preserve\"> <style type=\"text/css\"> .st0{fill-rule:evenodd;clip-rule:evenodd;stroke:#25211D;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st1{fill-rule:evenodd;clip-rule:evenodd;fill:#232222;} .st2{fill-rule:evenodd;clip-rule:evenodd;fill:#CBCBCB;} .st3{fill-rule:evenodd;clip-rule:evenodd;fill:#FFFFFF;} .st4{fill-rule:evenodd;clip-rule:evenodd;fill:#232222;stroke:#25211D;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st5{fill:#FFFFFF;} .st6{fill-rule:evenodd;clip-rule:evenodd;fill:#77C8BD;} .st7{fill:#232222;} .st8{fill-rule:evenodd;clip-rule:evenodd;fill:#EAEAEA;} .st9{fill-rule:evenodd;clip-rule:evenodd;fill:#FFFFFF;stroke:#E5E5E5;stroke-width:0;stroke-linecap:square;stroke-miterlimit:10;} .st10{fill:#6D6C6C;} .st11{fill:#00DDB6;} </style> <g> <path class=\"st7\" d=\"M0,120.1l2.3-16.4h0.3l6.7,13.5l6.6-13.5h0.3l2.4,16.4h-1.6l-1.6-11.7l-5.8,11.7H9.1l-5.9-11.8l-1.6,11.8H0z M145.1,120.1v-16.4h0.3l10.9,12.6v-12.6h1.6v16.4h-0.4l-10.8-12.4v12.4H145.1z M119.5,120.1v-16.4h0.4l10.9,12.6v-12.6h1.6v16.4 H132l-10.8-12.4v12.4H119.5z M97.4,103.7h9.4v1.6H99v5.1h7.7v1.6H99v6.4h7.7v1.6h-9.3V103.7z M73.5,103.7h1.8l5.4,12.7l5.5-12.7H88 l-7.1,16.4h-0.3L73.5,103.7z M52.9,103.7h1.9l4.2,6.8l4.1-6.8H65l-5.2,8.6v7.8h-1.6v-7.8L52.9,103.7z M37.7,103.7l7.7,16.4h-1.8 l-2.6-5.4H34l-2.6,5.4h-1.8l7.8-16.4H37.7z M37.5,107.1l-2.8,5.9h5.6L37.5,107.1z\"/> <path class=\"st6\" d=\"M56.7,57.1c-0.7-8,3.3-14.7,8.4-20c7.5-7.8,17.9-14.9,21.2-20.2C72.2,28.8,50.7,32.1,56.7,57.1L56.7,57.1z M91.1,0c9.7,6.2,9.2,15.9,2.6,24.7c-7.4,9.8-18.4,17.6-24.3,28.6c-6.9,13-7.1,34.3,14,33.9C66.7,87,66,68.3,74.5,54.2 C82.7,40.5,121,15.4,91.1,0L91.1,0z M93,6.5c3.8,21.7-51.8,32.5-29,72.3C50.6,40.5,104.7,27.1,93,6.5L93,6.5z\"/></g></svg>")

(defn index [{:keys [store]} storeback-config env]
  (page/html5
   [:head
    [:title "Shop | Mayvenn"]
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]

    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    [:script.bugsnag-script-src {:type "text/javascript"
                                 :src "//d2wy8f7a9ursnm.cloudfront.net/bugsnag-2.min.js"
                                 :data-apikey "acbe770e8c0942f8bf97bd9c107483b1"}]
    [:script.bugsnag-script-src {:type "text/javascript"}
     (str "Bugsnag.releaseStage = \"" env "\";"
          "Bugsnag.notifyReleaseStages = ['acceptance', 'production'];")]
    [:script {:type "text/javascript"}
     (str "store = " (generate-string store) ";")]
    (page/include-css (asset-path "/css/all.css"))
    (page/include-css (asset-path "/css/app.css"))]
   [:body
    [:div#content
     [:div {:style "height:100vh;"}
      [:div {:style "margin:auto; width:50%; position: relative; top: 50%; transform: translateY(-50%);"} mayvenn-logo-splash
       [:div {:style (str "height: 2em;"
                          "margin-top: 2em;"
                          "background-image: url('/images/spinner.svg');"
                          "background-size: contain;"
                          "background-position: center center;"
                          "background-repeat: no-repeat;")}]]]]
    (element/javascript-tag (str "var environment=\"" env "\";"
                                 "var canonicalImage=\"" (asset-path "/images/home_image.jpg") "\";"
                                 "var apiUrl=\"" (:endpoint storeback-config) "\";"))
    [:script {:src (asset-path "/js/out/main.js")}]]))

(def not-found
  (page/html5
   [:head
    [:title "Not Found | Mayvenn"]
    [:meta {:name "fragment" :content "!"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
    [:meta {:http-equiv "Content-type" :content "text/html;charset=UTF-8"}]
    [:link {:href (asset-path "/images/favicon.png") :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (page/include-css (asset-path "/css/app.css"))]
   [:body
    [:div.sans-serif.lg-col-6.mx-auto.flex.flex-column.items-center
     [:img.py2 {:src (asset-path "/images/header_logo.png")}]
     [:img.mx-auto.block {:src (asset-path "/images/not_found_head.png")
                          :style "max-width: 80%"}]
     [:div.h2.mt3.mb2.center "We can't seem to find the page you're looking for."]
     [:a.mx-auto.btn.btn-primary.bg-green.col-10
      {:href "/"}
      [:div.h3.p1.letter-spacing-1 "Return to Homepage"]]]]))
