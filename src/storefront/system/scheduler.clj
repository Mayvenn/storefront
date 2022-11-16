(ns storefront.system.scheduler
  (:require [overtone.at-at :as at-at]
            [com.stuartsierra.component :as component]))

(defrecord Scheduler [pool exception-handler logger]
  component/Lifecycle
  (start [c]
    (assert exception-handler)
    (assoc c :pool (at-at/mk-pool)))
  (stop [c]
    (when pool (at-at/stop-and-reset-pool! pool))
    (assoc c :pool nil))
  Object
  (toString [s] (pr-str s)))

(comment
  (prn (:scheduler dev-system/the-system))
  (prn (at-at/show-schedule (:pool (:scheduler dev-system/the-system)))))

(defmethod print-method Scheduler [v ^java.io.Writer w]
  (.write w (format "#:scheduler [%s]" (if (:pool v)
                                       (with-out-str (at-at/show-schedule (:pool v)))
                                       "(No Scheduled Tasks)"))))

(defn- safe-task [t {:keys [exception-handler logger]}]
  (fn []
    (try
      (t)
      (catch Throwable t
      ;; Ideally, we should never get here, but at-at halts all polls that throw exceptions silently.
      ;; This simply reports it and lets the polling continue
        (when exception-handler (exception-handler t))
        (when logger (logger :error {:throwable t}))))))

(defn every
  "Schedules task-f to be called every interval-ms in milliseconds. The next invocation is interval-ms after task-f completes."
  ([scheduler interval-ms task-f]
   (at-at/interspaced interval-ms (safe-task task-f scheduler) (:pool scheduler)))
  ([scheduler interval-ms desc task-f]
   (at-at/interspaced interval-ms (safe-task task-f scheduler) (:pool scheduler) :desc desc)))

(defn at
  "Schedules task-f to be called when the future date, unix-msec, passes"
  ([scheduler unix-msec task-f]
   (at-at/at unix-msec (safe-task task-f scheduler) (:pool scheduler)))
  ([scheduler unix-msec desc task-f]
   (at-at/at unix-msec (safe-task task-f scheduler) (:pool scheduler) :desc desc)))