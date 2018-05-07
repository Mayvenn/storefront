(ns leads.flows
  (:require #?@(:cljs [[storefront.api :as api]])
            [storefront.config :as config]
            [leads.keypaths :as keypaths]))

(defn call-number
  [data]
  (case (get-in data keypaths/lead-flow-id)
    "a1" config/mayvenn-leads-a1-call-number
    config/mayvenn-leads-call-number))

(def ^:private allowed-keys
  #{:tracking-id
    :first-name
    :last-name
    :phone
    :email
    :flow-id
    :website-url
    :facebook-url
    :instagram-handle
    :number-of-clients
    :utm-source
    :utm-medium
    :utm-campaign
    :utm-content
    :utm-term})

(defn create-lead [app-state callback]
  #?(:cljs
     (cond-> (get-in app-state keypaths/lead)
       true (select-keys allowed-keys)

       (= "a1" (get-in app-state keypaths/lead-flow-id))
       (assoc :professional (get-in app-state keypaths/lead-professional false))

       true (api/create-lead callback))))
