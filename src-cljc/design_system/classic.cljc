(ns design-system.classic
  (:require [storefront.component :as component]
            [design-system.organisms :as organisms]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.events :as events]
            [popup.organisms :as popup]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn query
  [data]
  {:organism/label     :popup
   :organism/component popup/organism
   :organism/show?     (get-in data keypaths/popup)
   :organism/query
   {:modal-close/event             events/control-design-system-popup-dismiss
    :pre-title/content             [:h7 "Pre-title"]
    :monstrous-title/copy          ["Monstrous" "Title"]
    :subtitle/copy                 "Subtitle"
    :description/copy              ["Description"]
    :single-field-form/callback    (utils/fake-href events/control-design-system-popup-dismiss)
    :single-field-form/field-data  {:errors    nil
                                    :keypath   nil
                                    :focused   false
                                    :label     "Placeholder"
                                    :name      "textfield"
                                    :type      "textfield"
                                    :value     ""
                                    :data-test "textfield-input"}
    :single-field-form/button-data {:title        "Submit"
                                    :color-kw     :color/teal
                                    :height-class :large
                                    :data-test    "email-input-submit"}}})



(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Classic Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section.p4
      (ui/teal-button (utils/fake-href events/control-design-system-popup-show) "Show popup")
      (when (:organism/show? data)
        (component/build (:organism/component data)
                         (:organism/query data)
                         nil))]]]))

(defn built-component
  [app-state opts]
  (component/build component (query app-state) nil))

(defmethod transitions/transition-state events/control-design-system-popup-show [_ event args app-state]
  (assoc-in app-state keypaths/popup :design-system))

(defmethod transitions/transition-state events/control-design-system-popup-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))
