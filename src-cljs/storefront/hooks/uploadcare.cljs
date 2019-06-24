(ns storefront.hooks.uploadcare
  (:require [storefront.browser.tags :refer [insert-tag-with-callback src-tag]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.assets :as assets]
            [storefront.platform.messages :refer [handle-message]]))

(defn ^:private loaded? [] (.hasOwnProperty js/window "uploadcare"))

(defn insert []
  (when-not (loaded?)
    (set! js/UPLOADCARE_PUBLIC_KEY config/uploadcare-public-key)
    (set! js/UPLOADCARE_LIVE false)
    (insert-tag-with-callback
     (src-tag "https://ucarecdn.com/libs/widget/2.10.3/uploadcare.full.min.js"
              "uploadcare")
     #(do
        ;; These custom styles don't work on localhost... test on diva-acceptance.com
        (when config/secure?
          (.addUrl js/uploadcare.tabsCss (assets/path "/css/app.css")))
        (handle-message events/inserted-uploadcare)))))

(defn ^:private receive-file-or-group-info [on-success file-or-group-info]
  (handle-message on-success
                  (js->clj file-or-group-info :keywordize-keys true)))

(defn ^:private handle-error [error data]
  (handle-message events/uploadcare-api-failure
                  {:error error
                   :error-data (js->clj data :keywordize-keys true)}))

(defn ^:private handle-file [on-success file]
  (-> file
      .promise
      (.fail handle-error)
      (.done (partial receive-file-or-group-info on-success))))

(defn dialog [{:keys [selector resizable-url on-success widget-config]}]
  (when (loaded?)
    (-> js/uploadcare
        (.openPanel
         selector
         (->> [resizable-url]
              (remove nil?)
              (map #(js/uploadcare.fileFrom "uploaded" %))
              (apply array))
         (clj->js (merge
                   {:imageShrink "1600x1600"
                    :imagesOnly  true
                    :crop        "1:1"
                    :tabs        "instagram facebook file"}
                   widget-config)))
        (.done (partial handle-file on-success)))))
