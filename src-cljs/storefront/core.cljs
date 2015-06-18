(ns storefront.core
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]])
  (:require [storefront.state :as state]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.routes :as routes]
            [storefront.messages :refer [enqueue-message]]
            [storefront.sync-messages :refer [send-message]]
            [cljs.core.async :refer [<! chan close!]]
            [om.core :as om]))

(enable-console-print!)

(defn start-event-loop [app-state]
  (let [event-ch (get-in @app-state keypaths/event-ch)]
    (go-loop []
      (when-let [event-and-args (<! event-ch)]
        (send-message app-state event-and-args)
        (recur)))
    (enqueue-message event-ch [events/app-start])))

(defn main [app-state]
  (routes/install-routes app-state)
  (om/root
   top-level-component
   app-state
   {:target (.getElementById js/document "content")})

  (start-event-loop app-state)
  (routes/set-current-page @app-state))

(defonce app-state (atom (state/initial-state)))

(defn debug-force-token [token]
  (swap! app-state assoc-in [:user :token] "f766e9e3ea1f7b8bf25f1753f395cf7bd34cef0430360b7d"))

(defn debug-app-state []
  (clj->js @app-state))

(defn on-jsload []
  (let [event-ch (get-in @app-state keypaths/event-ch)]
    (enqueue-message event-ch [events/app-stop])
    (close! event-ch))
  (swap! app-state assoc-in keypaths/event-ch (chan))
  (main app-state))

(main app-state)
