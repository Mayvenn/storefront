(ns storefront.hooks.kustomer
  (:require [storefront.browser.tags :as tags]
            [storefront.config :as config]
            [storefront.effects :as effects]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.macros :refer [defpath]]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]))

(defpath kustomer|started)
(defpath kustomer|conversationDescribed)
(defpath kustomer|customerDescribed)

(defpath kustomer|onUnread)
(defpath kustomer|onOpen)
(defpath kustomer|onClose)
(defpath kustomer|onConversationCreate)
(defpath kustomer|onLogin)
(defpath kustomer|onLogout)

(defn init
  []
  (-> "https://cdn.kustomerapp.com/chat-web/widget.js"
      (tags/src-tag "kustomer-script")
      (tags/insert-tag-with-dataset-and-callback
       "kustomerApiKey"
       config/kustomer-api-key
       (partial messages/handle-message e/inserted-kustomer))))
(defn open-conversation [] (.open js/Kustomer))
(defn describe-conversation [description]
  (.describeConversation js/Kustomer (clj->js description)
                         #(messages/handle-message kustomer|conversationDescribed {:description description
                                                                                   :response    %1
                                                                                   :error       %2})))

(defmethod transitions/transition-state e/inserted-kustomer
  [_ event args app-state]
  (assoc-in app-state k/loaded-kustomer true))

(defmethod effects/perform-effects e/inserted-kustomer
  [_ _ _ _ _]
  (.start js/Kustomer
          #js{"brandId"      config/kustomer-brand-id
              "hideChatIcon" true}
          (partial messages/handle-message kustomer|started)))

(defmethod effects/perform-effects kustomer|started
  [_ _ _ _ _]
  (doto js/Kustomer
    (.addListener "onUnread"             #(messages/handle-message kustomer|onUnread             {:response %1 :error %2}))
    (.addListener "onOpen"               #(messages/handle-message kustomer|onOpen               {:response %1 :error %2}))
    (.addListener "onClose"              #(messages/handle-message kustomer|onClose              {:response %1 :error %2}))
    (.addListener "onConversationCreate" #(messages/handle-message kustomer|onConversationCreate {:response %1 :error %2}))
    (.addListener "onLogin"              #(messages/handle-message kustomer|onLogin              {:response %1 :error %2}))
    (.addListener "onLogout"             #(messages/handle-message kustomer|onLogout             {:response %1 :error %2}))))

(defmethod effects/perform-effects kustomer|onConversationCreate
  [_ _ {:keys [response error]} _ app-state]
  (let [order-number (get-in app-state k/order-number)]
    (describe-conversation (cond-> {:conversationId   (.-conversationId response)
                                    :customAttributes {:chatPageUrl (-> js/window .-location .-href)}}
                             order-number
                             (assoc-in [:customAttributes :chatCartOrderNumberStr] order-number)))))
