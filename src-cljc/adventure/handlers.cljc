(ns adventure.handlers
  (:require #?@(:cljs
                [[storefront.hooks.pixlee :as pixlee.hook]
                 [storefront.platform.messages :refer [handle-message]]
                 [storefront.history :as history]
                 [storefront.hooks.stringer :as stringer]
                 [storefront.browser.cookie-jar :as cookie]
                 [storefront.api :as api]])
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.accessors.pixlee :as pixlee]
            [adventure.keypaths :as keypaths]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [clojure.string :as string]))

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
  [_ event args app-state-before app-state]
  #?(:cljs
     (when (and (not= events/navigate-adventure-home event)
                (empty? (get-in app-state keypaths/adventure-choices)))
       (history/enqueue-navigate events/navigate-adventure-home nil))))

(def ^:private slug->video
  {"we-are-mayvenn" {:youtube-id "hWJjyy5POTE"}
   "free-install"   {:youtube-id "yDNgbKM2CrU"}})

;; Perhaps there is a better way to "start" the flow in the app-state
;;   e.g. {:flow/version 1}
;; Perhaps the basic_prompt and multi_prompt could both do control-adventure
(defmethod transitions/transition-state events/navigate-adventure-home
  [_ event {:keys [query-params]} app-state]
  (-> app-state
      (update-in keypaths/adventure-choices
                 merge {:adventure :started})
      (assoc-in keypaths/adventure-home-video (slug->video (:video query-params)))))


(defmethod effects/perform-effects events/navigate-adventure-home
  [_ _ args prev-app-state app-state]
  #?(:cljs (do (pixlee.hook/fetch-album-by-keyword :sleek-and-straight)
               (pixlee.hook/fetch-album-by-keyword :waves-and-curly)
               (pixlee.hook/fetch-album-by-keyword :free-install-mayvenn))))

(defn ^:private adventure-choices->criteria
  [choices]
  ;; Always return bundles for a la carte
  {:hair/family        (conj #{"bundles"} (:install-type choices))
   :hair/texture       (:texture choices)})

(defmethod effects/perform-effects events/adventure-fetch-matched-skus
  [_ _ {:keys [criteria] :or {criteria [:hair/family]}} _ app-state]
  #?(:cljs (api/search-v2-skus (get-in app-state storefront.keypaths/api-cache)
                               (-> (get-in app-state keypaths/adventure-choices)
                                   adventure-choices->criteria
                                   (select-keys criteria)
                                   (assoc :catalog/department    "hair"
                                          :catalog/discontinued? "false"))
                               #(handle-message events/api-success-adventure-fetch-skus %))))

(defmethod transitions/transition-state events/api-success-adventure-fetch-skus
  [_ event {:keys [skus]} app-state]
  (-> app-state
     (assoc-in keypaths/adventure-matching-skus skus)))

(defmethod effects/perform-effects events/adventure-fetch-matched-products
  [_ _ {:keys [criteria] :or {criteria [:hair/family]}} _ app-state]
  #?(:cljs (api/search-v2-products (get-in app-state storefront.keypaths/api-cache)
                               (-> (get-in app-state keypaths/adventure-choices)
                                   adventure-choices->criteria
                                   (select-keys criteria)
                                   (assoc :catalog/department "hair"))
                               #(handle-message events/api-success-adventure-fetch-products %))))

(defmethod transitions/transition-state events/api-success-adventure-fetch-products
  [_ event {:keys [products skus]} app-state]
  (let [selected-color      (:color (get-in app-state keypaths/adventure-choices))
        skus-matching-color (filter #(contains? (set (:hair/color %)) selected-color) skus)
        product-ids         (set (flatten (map :selector/from-products skus-matching-color)))
        products-indexed    (spice.maps/index-by :catalog/product-id products)
        correct-products    (map #(get products-indexed %) product-ids)]
    (-> app-state
        #_(assoc-in storefront.keypaths/v2-products (spice.maps/index-by :product-id products))
        (assoc-in keypaths/adventure-matching-products correct-products)
        (assoc-in keypaths/adventure-matching-skus skus)
        (assoc-in keypaths/adventure-matching-skus-color skus-matching-color))))

