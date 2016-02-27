(ns storefront.tee
  (:require [cljs.reader :refer [read-string]]))

(defn create-producer []
  (def connection (js/WebSocket. "ws://localhost:3012"))
  (def buffer (atom []))
  (def connected (atom false))

  (set! (.-onmessage connection)
        (fn [event]
          (js/console.log (pr-str (.-data event)))))

  (set! (.-onopen connection)
        (fn [event]
          (reset! connected true)
          (doseq [message @buffer]
            (.send connection (pr-str [:event message])))
          (swap! buffer empty))))

(defn tee [[event args]]
  (let [args (dissoc args :xhr)
        message [event args]]
    (if @connected
      (do
        (.send connection (pr-str [:event message])))
      (swap! buffer conj message))))

(defn create-listener [room-id handler]
  (def connection (js/WebSocket. "ws://localhost:3013"))
  (def connected (atom false))

  (set! (.-onmessage connection)
        (fn [event]
          (when (seq? (.-data event))
            (let [[server-msg room-id [_ message]] (read-string (.-data event))]
              (when (#{:broadcast :snapshot} server-msg)
                (js/console.log "Hi" (pr-str message))
                (handler message))))))

  (set! (.-onopen connection)
        (fn [event]
          (reset! connected true)
          (.send connection (pr-str [:join-room room-id])))))

