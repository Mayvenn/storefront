(ns mayvenn.concept.persona-results
  (:require #?@(:cljs
                [[storefront.hooks.stringer :as stringer]
                 [storefront.api :as api]
                 [storefront.hooks.reviews :as review-hooks]
                 storefront.frontend-trackings])
            api.orders
            [clojure.string :refer [join]]
            [mayvenn.concept.booking :as booking]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.visual.tools :refer [with within]]
            [spice.selector :as selector]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.trackings :as trk]
            [storefront.transitions :as t]))

(def product-annotations
  {"120" {:customer/persona :customer.persona/p1}
   "236" {:customer/persona :customer.persona/p1}
   "9"   {:customer/persona :customer.persona/p1}
   "353" {:customer/persona :customer.persona/p1}
   "335" {:customer/persona :customer.persona/p2}
   "354" {:customer/persona :customer.persona/p2}
   "268" {:customer/persona :customer.persona/p2}
   "249" {:customer/persona :customer.persona/p2}
   "352" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p3}
   "235" {:customer/persona :customer.persona/p3}
   "313" {:customer/persona :customer.persona/p3}
   #_#_"354" {:customer/persona :customer.persona/p4}
   "128" {:customer/persona :customer.persona/p4}
   "252" {:customer/persona :customer.persona/p4}
   "15" {:customer/persona :customer.persona/p4}})

(def look-annotations
  {:7e4F9lGZHmTphADJxRFL6a {:customer/goals :customer.goals/enhance-natural}
   :2LcC986WiwwveAsh9HwEKZ {:customer/goals :customer.goals/protect-natural}
   :2KKdNG4k1JlFQWtOIXWHWB {:customer/goals :customer.goals/save-money}
   :74i7UgWjKVrAtV1mqMVMoh {:customer/goals :customer.goals/easy-maintenance}})

(def select
  (comp seq
        (partial selector/match-all
                 {:selector/strict? true})))

;;;; Models

(defn <-
  "Get the results model of a look suggestion"
  [state id]
  (->> id
       (conj k/models-persona-results)
       (get-in state)))


;;;; Behavior

;; Reset

(defmethod t/transition-state e/persona-results|reset
  [_ _ {:keys [id]} state]
  (-> state
      (assoc-in (conj k/models-persona-results id) nil)))

;; Queried

(defmethod fx/perform-effects e/persona-results|queried
  [_ _ {:questioning/keys [id] :keys [on/success-fn answers]} _ state]
  (let [#_#_looks (->> (get-in state k/cms-ugc-collection-all-looks)
                       (merge-with merge look-annotations)
                       vals)
        products  (->> (get-in state k/models-products)
                      (merge-with merge product-annotations)
                      vals)
        persona   (select-keys answers [:customer/persona])
        results   (select persona products)]
    (success-fn "1")
    (publish e/persona-results|resulted
             {:id      id
              :results results})))

;; Resulted

(defmethod t/transition-state e/persona-results|resulted
  [_ _ {:keys [id results]} state]
  (-> state
      (assoc-in (conj k/models-persona-results id)
                results)))

(defmethod fx/perform-effects e/persona-results|resulted
  [_ _ {:keys [id results]} _]
  (publish e/ensure-sku-ids
           {:sku-ids (set
                      (concat
                       (mapcat :product/sku-ids results)
                       (map :service/sku-id results)))}))
