(ns storefront.core
  (:require-macros [cljs.core.async.macros :refer [go-loop alt!]])
  (:require [storefront.state :as state]
            [storefront.components.top-level :refer [top-level-component]]
            [storefront.controllers.core :refer [perform-effects]]
            [storefront.transitions :refer [transition-state]]
            [storefront.routes :as routes]
            [cljs.core.async :refer [put!]]
            [om.core :as om]))

(defn transition [app-state [event args]]
  (reduce #(transition-state %2 event args %1) app-state (rest (reductions conj [] event))))

(defn effects [app-state [event args]]
  (doseq [event-fragment (rest (reductions conj [] event))]
    (perform-effects event-fragment event args app-state)))

(defn start-event-loop [app-state]
  (go-loop []
    (alt!

      (get-in @app-state state/event-ch-path)
      ([event-and-args]
       (swap! app-state transition event-and-args)
       (effects @app-state event-and-args)
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

(defn debug-app-state []
  (js/console.log (clj->js @app-state)))

(defn on-jsload []
  (put! (get-in @app-state state/stop-ch-path) "STOP")
  (main app-state))

(main app-state)
