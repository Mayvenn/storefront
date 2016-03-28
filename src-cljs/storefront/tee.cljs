(ns storefront.tee
  (:require [cljs.reader :refer [read-string]]
            [storefront.events :as events]
            [storefront.config :as config]))

(defprotocol IConnection
  (send-command [connection command]
    "Sends the given command through the connection")
  (close [connection]))

(defrecord WebSocketConnection [socket buffer auto-reconnect]
  IConnection
  (send-command [_ command]
    (if (= js/WebSocket.OPEN (.-readyState @socket))
      (.send @socket (pr-str command))
      (swap! buffer conj command)))
  (close [_]
    (.close @socket)))

(defn open-websocket [url {:keys [reconnect-on-disconnect?
                                  on-open on-message on-error on-close]
                           :or {reconnect-on-disconnect? false
                                on-open identity
                                on-message identity
                                on-error identity
                                on-close identity}}]
  (let [socket (atom (js/WebSocket. url))
        buffer (atom [])
        reconnect-on-disconnect (atom reconnect-on-disconnect?)
        connection (->WebSocketConnection socket buffer reconnect-on-disconnect)
        attach-to (fn attach-to [socket]
                    (set! (.-onmessage @socket) (fn [event]
                                                  (js/console.log "event" event)
                                                  (on-message connection event)))
                    (set! (.-onopen @socket)
                          (fn [event]
                            (js/console.log event)
                            (doseq [command @buffer]
                              (.send @socket (pr-str command)))
                            (swap! buffer empty)
                            (on-open connection event)))
                    (set! (.-onerror @socket) (fn [event]
                                                (js/console.error event)
                                                (on-error connection connection)))
                    (set! (.-onclose @socket)
                          (fn [event]
                            (js/console.log event)
                            (when @reconnect-on-disconnect
                              (let [new-ws (js/WebSocket. url)]
                                (attach-to @socket)
                                (reset! socket new-ws)))
                            (on-close connection event))))]
    (attach-to socket)
    connection))

(defn ^:private log-message [_ event]
  (js/console.log "Tap Response" (pr-str (.-data event))))

(defn create-producer []
  (open-websocket config/tee-url {:on-message log-message}))

(defn create-listener [room-id handler]
  (open-websocket config/listen-url
                  {:on-open (fn [c _] (send-command c [:join-room room-id]))
                   :on-message (fn [c event]
                                 (let [command (read-string (str (.-data event)))
                                       [server-msg room-id body] command]
                                   (js/console.log "[Listener]" (pr-str command))
                                   (when (= :broadcast server-msg)
                                     (let [[source-cmd storefront-data] body]
                                       (condp = source-cmd
                                         :event (apply handler storefront-data)
                                         :snapshot (apply handler events/listener-sync storefront-data)
                                         :disconnect (js/console.log "Source Disconnected")
                                         :connect (js/console.log "Source Connected"))))))}))

(defn install-tap [app-state handle-message listener-handle-message]
  (if-let [room-id (last (re-find #"listen=(.*)&?" js/window.location.search))]
    (do
      (js/console.log "Listening mode active:" room-id)
      (swap! app-state assoc :tap (create-listener room-id listener-handle-message))
      listener-handle-message)
    (do
      (swap! app-state assoc :tap (create-producer))
      handle-message)))

(defn tee [connection [event args]]
  (send-command connection [:event [event (dissoc args :xhr)]]))

(defn snapshot [connection app-state]
  (send-command connection [:snapshot (dissoc app-state
                                              :cookie
                                              :tap
                                              :api-cache
                                              :routes)]))
