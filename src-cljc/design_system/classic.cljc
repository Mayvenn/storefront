(ns design-system.classic
  (:require [storefront.component :as component]
            [design-system.organisms :as organisms]
            [storefront.events :as events]
            [popup.organisms :as popup]
            [storefront.platform.component-utils :as utils]))


(def nowhere events/navigate-design-system-adventure)

(def organisms
  [{:organism/label     :popup
    :organism/component popup/organism
    :organism/popup?    true
    :organism/query
    {:modal-close/event             organisms/dismiss
     :pre-title/content             [:h7 "Pre-title"]
     :monstrous-title/copy          ["Monstrous" "Title"]
     :subtitle/copy                 "Subtitle"
     :description/copy              ["Description"]
     :single-field-form/callback    (utils/fake-href organisms/dismiss)
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
                                     :data-test    "email-input-submit"}}}])

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Classic Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section.p4
      (organisms/demo organisms (:organisms data))]]]))

(defn built-component
  [{:keys [design-system]} opts]
  (component/build component design-system nil))
