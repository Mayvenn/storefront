(ns storefront.hooks.kustomer
  (:require [storefront.browser.tags :as tags]
            [storefront.effects :as effects]
            [storefront.events :as e]
            [storefront.platform.messages :as messages]))

(defn- logging-cb
  [response error]
  (if error
    (prn "Updated information about the conversation, error: " error)
    (prn "Updated information about the conversation" response)))

(defn- describe-conversation
  [order-number response]
  (let [description
        (cond-> {:conversationId (.-conversationId response)
                 :customAttributes
                 {:chatPageUrl (-> js/window .-location .-href)}}
          order-number
          (assoc-in [:customAttributes :chatCartOrderNumberStr] order-number))]
    (doto js/Kustomer
      (.describeConversation description logging-cb))))

(defn init
  []
  (-> "https://cdn.kustomerapp.com/chat-web/widget.js"
      (tags/src-tag "kustomer-script")
      (tags/insert-tag-with-dataset-and-callback
       "kustomerApiKey"
       "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjVkMWE1OGVmZWE2NTI3MDA5NjAxYjYyYiIsInVzZXIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYiLCJvcmciOiI1Y2Y2YzcxNTYyNGEwNzAwMTNhZDQ0YzgiLCJvcmdOYW1lIjoibWF5dmVubi1zYW5kYm94IiwidXNlclR5cGUiOiJtYWNoaW5lIiwicm9sZXMiOlsib3JnLnRyYWNraW5nIl0sImF1ZCI6InVybjpjb25zdW1lciIsImlzcyI6InVybjphcGkiLCJzdWIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYifQ.N_3t02QEEURClNfyBodzPmFSmv60CwdIROXArlVtudE"
       (partial messages/handle-message e/inserted-kustomer))))

(defmethod effects/perform-effects e/inserted-kustomer
  [_ _ _ _]
  (.start js/Kustomer
          #js{"brandId" "5daf7912124be4f2960cafcc"}
          (partial messages/handle-message e/started-kustomer)))

(defn open-conversation
  [order-number]
  (.open js/Kustomer
         (partial describe-conversation order-number)))
