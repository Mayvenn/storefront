(ns mayvenn.live-help.core
  (:require #?@(:cljs [[storefront.hooks.kustomer :as kustomer]])
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.component :as component]))

(def ready (conj k/models-live-help :ready))

(def ^:private bug-hidden-nav-events #{events/navigate-adventure-stylist-profile})

(defn maybe-start
  [state]
  (when (and
         (-> state (get-in ready) not)
         (-> state (get-in k/navigation-undo-stack) count (> 0))
         (-> state (get-in k/navigation-event) bug-hidden-nav-events not))
    (messages/handle-message events/flow|live-help|reset)))

(defmethod fx/perform-effects events/flow|live-help|reset
  [_ _ _ _ state]
  #?(:cljs (kustomer/init)))

(defmethod transitions/transition-state events/flow|live-help|reset
  [_ event _ app-state]
  (-> app-state
      (assoc-in k/models-live-help {})))

(defmethod transitions/transition-state events/flow|live-help|ready
  [_ event _ app-state]
  (-> app-state
      (assoc-in ready true)))

(defmethod fx/perform-effects events/flow|live-help|opened
  [_ _ _ _ state]
  #?(:cljs (kustomer/open-conversation)))

(component/defcomponent bug-template [{:live-help-bug/keys [id target]} _owner _opts]
  (when id
    [:div.fixed.bottom-0.right-0.m4
     (merge {:data-test id}
            (utils/fake-href target))
     (svg/chat-bug {:width "50px"
                    :height "50px"})]))

(defn bug-component [state]
  (component/build bug-template {:live-help-bug/id     (when (and (get-in state ready)
                                                                  (not (bug-hidden-nav-events (get-in state k/navigation-event))))
                                                         "live-help-bug")
                                 :live-help-bug/target events/flow|live-help|opened}))
