(ns storefront.hooks.uploadcare
  (:require [storefront.browser.tags :refer [insert-tag-with-callback src-tag]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [goog.object :as object]))

(defn insert []
  (when-not (.hasOwnProperty js/window "uploadcare")
    (set! js/UPLOADCARE_PUBLIC_KEY config/uploadcare-public-key)
    (insert-tag-with-callback
     (src-tag "https://ucarecdn.com/libs/widget/2.10.3/uploadcare.full.min.js"
              "uploadcare")
     #(handle-message events/inserted-uploadcare))))

(def invalid-image-error-message
  "Invalid Image Uploaded")

(defn ^:private receive-file-info [file-info]
  (handle-message events/uploadcare-api-success-upload-image
                  {:file-info (js->clj file-info :keywordize-keys true)}))

(defn ^:private handle-error [error file-info]
  (handle-message events/uploadcare-api-failure
                  {:upload-was-not-valid-image? (= (.-message error) invalid-image-error-message)
                   :error error
                   :file-info (js->clj file-info :keywordize-keys true)}))

(defn ^:private handle-file [file]
  ;; Google Closure cannot detect externs correctly here...
  (let [promise (.call (object/get file "promise") file)
        fail (.call (object/get promise "fail") promise handle-error)]
    (.call (object/get fail "done") fail receive-file-info)))

(defn ^:private image-only [file-info]
  (let [file-info (js->clj file-info :keywordize-keys true)]
    (when (= (:isImage file-info) false) ;; nil means we don't have this information yet
      ;; as per protocol, uploadcare wants us to throw errors to fail validation
      (throw (js/Error. invalid-image-error-message)))))

(defn dialog []
  (when (.hasOwnProperty js/window "uploadcare")
    (-> js/uploadcare
        (.openDialog
         nil
         (clj->js {:imageShrink "1600x1600"
                   ;; note: validator functions get called 3 times:
                   ;;       1. When the file is first specified, only the fileinfo's name field is available
                   ;;       2. When the size is determined, only the fileinfo's name and size are available
                   ;;       3. After the file is uploaded to uploadcare, all of the fileinfo's fields are available
                   :validators [image-only]}))
        (.done handle-file))))
