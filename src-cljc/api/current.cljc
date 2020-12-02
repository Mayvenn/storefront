(ns api.current
  "Current is the model of user-specific session"
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]])
            api.stylist
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as m]))

;; NOTE(corey) This will probably be expanded if it seems to work out.

(defn current<-
  "Generates the current model from state"
  [state]
  {:stylist/id (some-> state (get-in k/order) :servicing-stylist-id)})

;;;; ---- Domains

(comment
  "Business Domain: Current Stylist

   Represents the stylist that the customer has selected to perform the service.

   The domain has the following behavior:

   - Selected
     When the stylist has been selected to perform the service

   - Deselected
     When the stylist has been deselected or the stylist is not available

   The model is the composition of the current state with a stylist model. The
   current state for the current stylist is stored on the current order.

   Cache Domain: Current Stylist

   behavior:

   - Requested
     Requests the stylist model referred to by current state (via the order) if
     it is out of sync")

;; - Read API

(defn stylist
  "Retrieves the cached current stylist model"
  [state]
  (some->> (current<- state)
           :stylist/id
           (api.stylist/by-id state)))

;; - Behavior API

(def ^:private unavailable-stylist-copy
  (str
   "Your previously selected stylist is no longer available "
   "and has been removed from your order."))

(defmethod fx/perform-effects e/cache|current-stylist|requested
  [_ _ _ _ state]
  (when-let [stylist-id (:stylist/id (current<- state))]
    (m/handle-message e/cache|stylist|requested
                      {:stylist/id stylist-id
                       :forced?    true
                       :on/failure
                       (let [order (get-in state k/order)]
                         #(when (<= 400 (:status %) 499)
                            #?(:cljs
                               (api/remove-servicing-stylist stylist-id
                                                             (:number order)
                                                             (:token order)))
                            (m/handle-message e/flash-show-failure
                                              {:message unavailable-stylist-copy})))})))

(defmethod fx/perform-effects e/biz|current-stylist|selected
  [_ _ {:keys [order stylist] :on/keys [success]} _ _]
  #?(:cljs (facebook-analytics/track-event "AddToCart"
                                           {:content_type "stylist"
                                            :content_ids  [(:stylist-id stylist)]
                                            :num_items    1}))
  (m/handle-message e/save-order {:order order})
  (when (some? success)
    (success)))

(defmethod fx/perform-effects e/biz|current-stylist|deselected
  [_ _ {:keys [order]} _ _]
  (m/handle-message e/save-order {:order order}))






