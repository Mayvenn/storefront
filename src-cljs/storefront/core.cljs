(ns storefront.core
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]])
  (:require [storefront.state :as state]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.controllers.core :refer [perform-effects]]
            [storefront.transitions.core :refer [transition-state]]
            [storefront.routes :as routes]
            [cljs.core.async :refer [put!]]
            [om.core :as om]))

(defn start-event-loop [app-state]
  (go-loop []
    (alt!

      (get-in @app-state state/event-ch-path)
      ([event-and-args]
       (swap! app-state #(apply transition-state (conj event-and-args %)))
       (apply perform-effects (conj event-and-args @app-state))
       (recur))

      (get-in @app-state state/stop-ch-path)
      ([_] nil))))

(defn main [app-state]
  (routes/install-routes app-state)
  (om/root
   top-level-component
   app-state
   {:target (.getElementById js/document "content")})

  (start-event-loop app-state)
  (routes/set-current-page @app-state))

(defonce app-state (atom (state/initial-state)))

(defn on-jsload []
  (put! (get-in @app-state state/stop-ch-path) "STOP")
  (main app-state))

(main app-state)
