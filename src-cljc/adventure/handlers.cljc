(ns adventure.handlers
  (:require #?@(:cljs
                [[storefront.hooks.pixlee :as pixlee.hook]
                 [storefront.history :as history]
                 [storefront.hooks.stringer :as stringer]
                 [storefront.browser.cookie-jar :as cookie]])
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
     (let [destination (:target choice)]
       (if (map? destination)
         (history/enqueue-navigate (:event destination)
                                   (:args destination))
         (history/enqueue-navigate destination nil))
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
   "free-install"   {:youtube-id "sM6dvM1Q45k"}})

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
