(ns design-system.organisms
  (:require [storefront.component :as component]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [clojure.pprint :as pprint]))

(defn demo-component
  [{:organism/keys [label component query]} _ _]
  (component/create
   [:div.py3.border.border-black.bg-light-gray
    [:div.h3.px6.py2.bold (str label)]
    [:div (component/build component query nil)]
    [:div.p6
     [:div "query"]
     [:pre.h8 (with-out-str (pprint/pprint query))]]]))

(defn demo
  [organisms & [{:keys [popup-visible?]}]]
  (for [{:organism/keys [label popup? component query] :as organism} organisms]
    [:div {:key (str "organism-" label)}
     (if popup?
       (if popup-visible?
         (component/build component query nil)
         (ui/teal-button (utils/fake-href events/control-design-system-popup-show)
                         "Show popup wow, you can really dance"))
       (component/build demo-component organism nil))]))

(def dismiss events/control-design-system-popup-dismiss)

(defmethod transitions/transition-state events/control-design-system-popup-show
  [_ event args app-state]
  (assoc-in app-state [:design-system :organisms :popup-visible?] true))

(defmethod transitions/transition-state events/control-design-system-popup-dismiss
  [_ event args app-state]
  (assoc-in app-state [:design-system :organisms :popup-visible?] nil))
