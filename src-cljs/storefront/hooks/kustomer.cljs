(ns storefront.hooks.kustomer
  (:require [storefront.browser.tags :as tags]))

(defn- logging-cb
  [response error]
  (if error
    (prn "Updated information about the conversation, error: " error)
    (prn "Updated information about the conversation" response)))

(defn- describe-conversation
  [order-number-cb response]
  (let [description (->> {:chatPageUrl (-> js/window .-location .-href)}
                         (merge
                          (when-let [order-number (order-number-cb)]
                            {:chatCartOrderNumberStr order-number}))
                         (assoc {:conversationId (.-conversationId response)}
                                :customAttributes)
                         clj->js)]
    (doto js/Kustomer
      (.describeConversation description logging-cb))))

(defn init
  [order-number-cb]
  (-> "https://cdn.kustomerapp.com/chat-web/widget.js"
      (tags/src-tag "kustomer-script")
      (tags/insert-tag-with-dataset-and-callback
       "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjVkMWE1OGVmZWE2NTI3MDA5NjAxYjYyYiIsInVzZXIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYiLCJvcmciOiI1Y2Y2YzcxNTYyNGEwNzAwMTNhZDQ0YzgiLCJvcmdOYW1lIjoibWF5dmVubi1zYW5kYm94IiwidXNlclR5cGUiOiJtYWNoaW5lIiwicm9sZXMiOlsib3JnLnRyYWNraW5nIl0sImF1ZCI6InVybjpjb25zdW1lciIsImlzcyI6InVybjphcGkiLCJzdWIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYifQ.N_3t02QEEURClNfyBodzPmFSmv60CwdIROXArlVtudE"
       #(doto js/Kustomer
          .start
          (.addEventListener "onConversationCreate"
                             (partial describe-conversation order-number-cb))))))

(defn open-conversation
  []
  (.open js/Kustomer))
