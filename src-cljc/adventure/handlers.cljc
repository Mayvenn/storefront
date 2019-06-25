(ns adventure.handlers
  (:require #?@(:cljs
                [[storefront.history :as history]
                 [storefront.hooks.stringer :as stringer]
                 [storefront.hooks.talkable :as talkable]
                 [storefront.browser.cookie-jar :as cookie]
                 [storefront.api :as api]
                 [storefront.platform.messages :as messages]
                 [clojure.string :as string]])
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]
            [adventure.keypaths :as keypaths]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [catalog.products :as products]))

(defmethod transitions/transition-state events/control-adventure-choice
  [_ event {:keys [choice]} app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge (:value choice))))

(defmethod effects/perform-effects events/control-adventure-choice
  [_ _ {:keys [choice]} _ app-state]
  #?(:cljs
     (when-let [destination-message (:target-message choice)]
       (apply history/enqueue-navigate destination-message)
       (let [cookie    (get-in app-state storefront.keypaths/cookie)
             adventure (get-in app-state keypaths/adventure)]
         (cookie/save-adventure cookie adventure)))))

(defmethod trackings/perform-track events/control-adventure-choice
  [_ event {:keys [prompt buttons current-step choice]} app-state]
  #?(:cljs
     (stringer/track-event "adventure_question_answered"
                           {:question_text   (if (string? prompt)
                                               prompt
                                               (string/join " " (filter string? prompt)))
                            :answer_options  (mapv #(select-keys % [:text :value]) buttons)
                            :current_step    current-step
                            :answer_selected (:value choice)})))

(defmethod effects/perform-effects events/navigate-adventure
  [_ event {:keys [query-params]} app-state-before app-state]
  #?(:cljs
     (do
       (let [adventure-choices         (get-in app-state keypaths/adventure-choices)
             from-shop-to-freeinstall? (get-in app-state keypaths/adventure-from-shop-to-freeinstall?)]
         (when (and (not= events/navigate-adventure-home event)
                    (empty? adventure-choices)
                    (not (and (= events/navigate-adventure-install-type event)
                              (or from-shop-to-freeinstall?
                                  (some-> query-params :utm_source (string/includes? "toadventure"))))))
           (history/enqueue-navigate events/navigate-adventure-home nil))
         (when (boolean (:em_hash query-params))
           (messages/handle-message events/adventure-visitor-identified))))))


(defmethod effects/perform-effects events/adventure-visitor-identified
  [_ _ _ _ app-state]
  #?(:cljs
     (cookie/save-email-capture-session (get-in app-state storefront.keypaths/cookie) "opted-in")))

(defmethod transitions/transition-state events/adventure-visitor-identified
  [_ event {:keys [query-params]} app-state]
  (assoc-in app-state storefront.keypaths/email-capture-session "opted-in"))

(def ^:private slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "oR1keQ-31yc"}})

;; Perhaps there is a better way to "start" the flow in the app-state
;;   e.g. {:flow/version 1}
;; Perhaps the basic_prompt and multi_prompt could both do control-adventure
(defmethod transitions/transition-state events/navigate-adventure-home
  [_ event {:keys [query-params]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-choices {:adventure :started})
      (assoc-in keypaths/adventure-home-video (slug->video (:video query-params)))))


(defmethod effects/perform-effects events/navigate-adventure-home
  [_ _ args prev-app-state app-state]
  #?(:cljs (let [cookie    (get-in app-state storefront.keypaths/cookie)
                 adventure (get-in app-state keypaths/adventure)]
             (cookie/save-adventure cookie adventure))))

(defn ^:private adventure-choices->criteria
  [choices]
  ;; Always return bundles for a la carte
  {:hair/family  (conj #{"bundles"} (:install-type choices))
   :hair/texture (:texture choices)})

(defmethod effects/perform-effects events/adventure-fetch-matched-skus
  [_ _ {:keys [criteria] :or {criteria [:hair/family]}} _ app-state]
  #?(:cljs (api/search-v2-skus (get-in app-state storefront.keypaths/api-cache)
                               (-> (get-in app-state keypaths/adventure-choices)
                                   adventure-choices->criteria
                                   (select-keys criteria)
                                   (assoc :catalog/department    "hair"
                                          :catalog/discontinued? "false"))
                               #(messages/handle-message events/api-success-adventure-fetch-skus %))))

(defmethod transitions/transition-state events/api-success-adventure-fetch-skus
  [_ event {:keys [skus]} app-state]
  (-> app-state
      (assoc-in storefront.keypaths/v2-skus (products/index-skus skus))))

(defmethod effects/perform-effects events/adventure-fetch-matched-products
  [_ _ {:keys [criteria] :or {criteria [:hair/family]}} _ app-state]
  #?(:cljs (api/search-v2-products (get-in app-state storefront.keypaths/api-cache)
                               (-> (get-in app-state keypaths/adventure-choices)
                                   adventure-choices->criteria
                                   (select-keys criteria)
                                   (assoc :catalog/department "hair"))
                               #(messages/handle-message events/api-success-v2-products %))))

(defmethod effects/perform-effects events/adventure-clear-servicing-stylist [_ _ _ _ app-state]
  #?(:cljs
     (let [order (get-in app-state storefront.keypaths/order)]
       (when-let [servicing-stylist-id (:servicing-stylist-id order)]
         (api/remove-servicing-stylist servicing-stylist-id
                                       (:number order)
                                       (:token order)
                                       #(messages/handle-message events/api-success-adventure-cleared-servicing-stylist {:order %}))))))

(defmethod transitions/transition-state events/api-success-adventure-cleared-servicing-stylist [_ _ {:keys [order]} app-state]
  (-> app-state
      (assoc-in keypaths/adventure-choices-selected-stylist-id nil)
      (assoc-in keypaths/adventure-servicing-stylist nil)))

(defmethod effects/perform-effects events/api-success-adventure-cleared-servicing-stylist [_ _ {:keys [order]} _ app-state]
  #?(:cljs
     (messages/handle-message events/save-order {:order order})))

(defmethod transitions/transition-state events/navigate-adventure-match-success-post-purchase
  [_ _ _ {:keys [completed-order] :as app-state}]
  #?@(:cljs
      [(prn completed-order)
       (assoc-in app-state storefront.keypaths/pending-talkable-order (talkable/completed-order completed-order))]))

(defmethod effects/perform-effects events/navigate-adventure-match-success-post-purchase [_ _ _ _ app-state]
  #?@(:cljs
      [(talkable/show-pending-offer app-state)
       (api/fetch-matched-stylist (get-in app-state storefront.keypaths/api-cache)
                                  (get-in app-state keypaths/adventure-choices-selected-stylist-id))]))
