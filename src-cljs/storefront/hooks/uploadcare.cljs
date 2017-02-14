(ns storefront.hooks.uploadcare
  (:require [storefront.browser.tags :refer [insert-tag-with-callback src-tag]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]))

(defn insert []
  (when-not (.hasOwnProperty js/window "uploadcare")
    (set! js/UPLOADCARE_PUBLIC_KEY config/uploadcare-public-key)
    (insert-tag-with-callback
     (src-tag "https://ucarecdn.com/libs/widget/2.10.3/uploadcare.full.min.js"
              "uploadcare")
     #(handle-message events/inserted-uploadcare))))


(defn ^:private receive-file-info [file-info]
  (handle-message events/uploadcare-api-success-upload-image
                  {:file-info (js->clj file-info :keywordize-keys true)}))

(defn ^:private handle-error [error file-info]
  (handle-message events/uploadcare-api-failure
                  {:error error
                   :file-info (js->clj file-info :keywordize-keys true)}))

(defn ^:private handle-file [file]
  (-> file
      .promise
      (.fail handle-error)
      (.done receive-file-info)))

(defn dialog []
  (when (.hasOwnProperty js/window "uploadcare")
    (-> js/uploadcare
        .openDialog
        (.done handle-file))))
