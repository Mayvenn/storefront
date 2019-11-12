(ns design-system.organisms
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [clojure.pprint :as pprint]))

(defcomponent demo-component
  [{:organism/keys [label component query]} _ _]
  [:div.py3.border.border-black.bg-light-gray
   [:div.h3.px6.py2.bold (str label)]
   ;[:div.border.border-black (component/build @component query)]
   [:div.p6
    [:div "query"]
    [:pre.h8 (with-out-str (pprint/pprint query))]]])

(defcomponent popup-button-component
  [_ _ _]
  [:div.bg-white
   (ui/teal-button (utils/fake-href events/control-design-system-popup-show)
                   "Show popup")])

(defn demo
  [organisms & [{:keys [popup-visible?]}]]
  (for [{:organism/keys [label popup?] :as organism} organisms
        :let [component (cond-> organism
                          (and popup? (not popup-visible?))
                          (assoc :organism/component popup-button-component))]]
    [:div
     {:key (str "organism-" label)}
     (component/build demo-component component nil)]))

(def dismiss events/control-design-system-popup-dismiss)

(defmethod transitions/transition-state events/control-design-system-popup-show
  [_ event args app-state]
  (assoc-in app-state [:design-system :organisms :popup-visible?] true))

(defmethod transitions/transition-state events/control-design-system-popup-dismiss
  [_ event args app-state]
  (assoc-in app-state [:design-system :organisms :popup-visible?] nil))
