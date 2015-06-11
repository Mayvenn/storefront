(ns storefront.prerender
  (:require [clojure.string :as string]
            [clj-http.client :as http]
            [ring.util.response :refer [response content-type]]))

(def crawler-user-agents
  #{"baiduspider"
    "facebookexternalhit"
    "facebot"
    "twitterbot"
    "rogerbot"
    "linkedinbot"
    "embedly"
    "bufferbot"
    "quora link preview"',
    "showyoubot"
    "outbrain"
    "pinterest"
    "developers.google.com/+/web/snippet"
    "slackbot"
    "vkShare"
    "W3C_Validator"})

(def ignored-extensions
  #{".js"
    ".css"
    ".xml"
    ".less"
    ".png"
    ".jpg"
    ".jpeg"
    ".gif"
    ".pdf"
    ".txt"
    ".ico"
    ".rss"
    ".zip"
    ".mp3"
    ".rar"
    ".exe"
    ".wmv"
    ".doc"
    ".avi"
    ".ppt"
    ".mpg"
    ".mpeg"
    ".tif"
    ".wav"
    ".mov"
    ".psd"
    ".ai"
    ".xls"
    ".mp4"
    ".m4a"
    ".swf"
    ".dat"
    ".dmg"
    ".iso"
    ".flv"
    ".m4v"
    ".torrent"})

(defn show-prerendered? [req]
  (let [agent (get-in req [:headers "user-agent"])]
    (and (= :get (:request-method req))
         agent
         (not (ignored-extensions (-> req :uri (string/split #"\.") last)))
         (or (contains? (:params req) :_escaped_fragment_)
             (some #(.contains (string/lower-case agent) %) crawler-user-agents)
             (get-in req [:headers "x-bufferbot"])))))

(defn prerender-service-url [development?]
  (if development?
    "http://localhost:4001"
    "http://service.prerender.io"))

(defn request-scheme [req]
  (if-let [forwarded-proto (get-in req [:headers "x-forwarded-proto"])]
    (keyword forwarded-proto)
    (:scheme req)))

(defn prerendered-resp [req development? token]
  (let [url (str (prerender-service-url development?)
                 "/" (name (request-scheme req)) "://"
                 (:server-name req)
                 ":" (if development? (:server-port req) 443) (:uri req))
        opts {:throw-exceptions false
              :socket-timeout 40000
              :conn-timeout 40000
              :headers {"User-Agent" (get-in req [:headers "user-agent"])
                        "X-Prerender-Token" token}}]
    (-> (http/get url opts)
        (select-keys [:body :status])
        (content-type "text/html"))))

(defn wrap-prerender [handler development? token]
  (fn [req]
    (if (show-prerendered? req)
      (prerendered-resp req development? token)
      (handler req))))
