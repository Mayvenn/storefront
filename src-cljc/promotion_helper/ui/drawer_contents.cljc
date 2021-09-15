(ns promotion-helper.ui.drawer-contents
  (:require [storefront.component :as c :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

(def drawer-contents-step-s-color-checkmark-atom
  (svg/check-mark {:class "fill-s-color ml1"
                   :style {:height "14px" :width "18px"}}))

(def drawer-contents-step-gray-checkmark-atom
  (svg/check-mark {:class "fill-gray ml1"
                   :style {:height "14px" :width "18px"}}))

(defn drawer-contents-condition-action-molecule
  [{:promotion-helper.ui.drawer-contents.condition.action/keys [id label target]}]
  [:div
   (when id
     (ui/button-small-primary
      {:on-click  (apply utils/send-event-callback target)
       :href      (apply routes/path-for (:target (second target)))
       :data-test id}
      label))])

(defn drawer-contents-condition-secondary-action-molecule
  [{:promotion-helper.ui.drawer-contents.condition.secondary.action/keys [id label target]}]
  [:div
   (when id
     (ui/button-small-secondary
      (assoc (apply utils/route-to target)
             :data-test id)
      label))])

(defn drawer-contents-condition-progress-molecule
  [{:promotion-helper.ui.drawer-contents.condition.progress/keys [id completed remaining]}]
  (c/html
   [:div.pl1
    (for [n    (range completed)
          :let [dt (str "drawer-contents.checkmarks.s-color.completed-" id "-" n)]]
      [:span {:key       dt
              :data-test dt}
       drawer-contents-step-s-color-checkmark-atom])
    (for [n    (range remaining)
          :let [dt (str "drawer-contents.checkmarks.gray.remaining-" id "-" n)]]
      [:span {:key       dt
              :data-test dt}
       drawer-contents-step-gray-checkmark-atom])]))

(defn drawer-contents-condition-title-molecule
  [{:promotion-helper.ui.drawer-contents.condition.title/keys [id primary primary-struck secondary]
    :as data}]
  (c/html
   [:div.flex.flex-column.left-align
    [:div.flex
     [:div
      (when primary
        [:div.content-2 {:data-test (str "primary-" id)}
         primary])
      (when primary-struck
        [:div.content-2.strike {:data-test (str "primary-struck-" id)}
         primary-struck])]
     (drawer-contents-condition-progress-molecule data)]
    [:div.content-3.dark-gray {:data-test (str "secondary-" id)}
     secondary]]))

(defcomponent drawer-contents-condition-organism
  [data _ _]
  [:div.black.bg-white.my2.p3.flex.items-center.border.border-refresh-gray
   [:div.col-10
    (drawer-contents-condition-title-molecule data)]
   [:div.col-2
    (drawer-contents-condition-action-molecule data)
    (drawer-contents-condition-secondary-action-molecule data)]])
