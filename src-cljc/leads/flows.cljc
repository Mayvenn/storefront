(ns leads.flows
  (:require #?@(:cljs [[storefront.api :as api]])
            [leads.keypaths :as keypaths]))

(def ^:private required-keys
  #{:tracking-id
    :first-name
    :last-name
    :phone
    :email
    :call-slot
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
     (-> (get-in app-state keypaths/lead)
         (select-keys required-keys)
         (api/create-lead callback))))
