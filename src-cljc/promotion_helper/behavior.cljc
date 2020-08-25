(ns promotion-helper.behavior
  (:require #?@(:cljs [[storefront.frontend-trackings :as frontend-trackings]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]])
            [storefront.transitions :refer [transition-state]]
            [storefront.events :as e]
            [storefront.effects :refer [perform-effects]]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :as messages]
            [storefront.accessors.orders :as orders]
            [promotion-helper.keypaths :as k]
            [storefront.trackings :refer [perform-track]]))

;;; COREY
;;; Open and close might happen from user input or something else, should that be in the event data?

(declare track)

;;;;
;;;; Event Domain
;;;;

(def promotion-helper [:ui :promotion-helper])

(def ^{:doc "The drawer is opened"}      opened   (conj promotion-helper :opened))
(def ^{:doc "The drawer is closed"}      closed   (conj promotion-helper :closed))
(def ^{:doc "A suggestion was followed"} followed (conj promotion-helper :followed))

;;;
;;; Event promotion-helper/opened
;;;

(defmethod transition-state opened
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened true)))

(defmethod perform-track opened
  [_ _ _ app-state]
  (track "helper_opened"
         (get-in app-state keypaths/order)
         (get-in app-state keypaths/v2-images)
         (get-in app-state keypaths/v2-skus)))

;;;
;;; Event promotion-helper/closed
;;;

(defmethod transition-state closed
  [_ event args app-state]
  (-> app-state
      (assoc-in k/ui-promotion-helper-opened false)))

(defmethod perform-track closed
  [_ _ _ app-state]
  (track "helper_closed"
         (get-in app-state keypaths/order)
         (get-in app-state keypaths/v2-images)
         (get-in app-state keypaths/v2-skus)))

;;;
;;; Event promotion-helper/followed
;;;

(defmethod perform-effects followed
  [_ _ {:keys [target]} _ app-state]
  #?(:cljs (apply history/enqueue-navigate target)))

(defmethod perform-track followed
  [_ _ {:keys [condition]} app-state]
  (when-let [data-team-event (case condition
                               "add-hair"    "helper_add_hair_button_pressed"
                               "add-stylist" "helper_add_stylist_button_pressed"
                               "view-cart"   "helper_view_cart_button_pressed"
                               nil)]
    (track data-team-event
           (get-in app-state keypaths/order)
           (get-in app-state keypaths/v2-images)
           (get-in app-state keypaths/v2-skus))))

;;;;
;;;; utils
;;;;

(defn ^:private track
  [event-name {:keys [servicing-stylist-id] :as waiter-order} images-db skus-db]
  #?(:cljs
     (stringer/track-event
      event-name
      (-> {:helper_name "promotion-helper"}
          (assoc :current_servicing_stylist_id servicing-stylist-id)
          (assoc :cart_items
                 (frontend-trackings/cart-items-model<- waiter-order
                                                        images-db
                                                        skus-db))))))
