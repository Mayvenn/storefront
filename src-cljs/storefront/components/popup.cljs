(ns storefront.components.popup
  (:require [storefront.accessors.nav :as nav]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.routes :as routes]
            [storefront.transitions :as transitions]))

(defmulti query (fn [data] (get-in data keypaths/popup :default)))
(defmethod query :default [_] nil)

(defmulti component (fn [query-data _ _] (:popup-type query-data)))
(defmethod component :default [_ _ _] [:div])

(defn query-with-popup-type [data]
  (assoc (query data)
         :popup-type (get-in data keypaths/popup :default)))

(defn built-component
  [data _]
  (component/html
   (let [query-data (query-with-popup-type data)]
     (component query-data
                nil
                {:close-attrs (utils/fake-href events/control-popup-hide)}))))

(defmethod effects/perform-effects events/control-popup-hide
  [_ _ _ _ _]
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/popup-hide
  [_ _ args app-state]
  (-> app-state
      transitions/clear-flash
      (assoc-in keypaths/popup nil)))

(defmethod effects/perform-effects events/popup-hide
  [_ _ _ _ _]
  (scroll/enable-body-scrolling))

(defmethod effects/perform-effects events/popup-show
  [_ _ _ _ _]
  (scroll/disable-body-scrolling)
  (messages/handle-message events/control-menu-collapse-all))
