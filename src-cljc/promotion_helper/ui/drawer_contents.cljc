(ns promotion-helper.ui.drawer-contents
  (:require [storefront.component :as c :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn ^:private elements
  "Embed a list of organisms in another organism."
  ([organism data elem-key]
   (elements organism data elem-key :default))
  ([organism data elem-key breakpoint]
   (let [elems (get data elem-key)]
     (for [[idx elem] (map-indexed vector elems)]
       (c/build organism
                elem
                (c/component-id elem-key
                                breakpoint
                                idx))))))

(def drawer-contents-step-teal-checkmark-atom
  (svg/check-mark {:class "fill-teal ml1"
                   :style {:height "14px" :width "18px"}}))

(def drawer-contents-step-gray-checkmark-atom
  (svg/check-mark {:class "fill-gray ml1"
                   :style {:height "14px" :width "18px"}}))

(defn drawer-contents-condition-progress-molecule
  [{:promotion-helper.ui.drawer-contents.condition.progress/keys [id completed remaining]}]
  (c/html
   [:div.flex-auto.pl1 {:key "c" :style {:order 2}}
    (for [n (range completed)
          :let [dt (str "drawer-contents.checkmarks.s-color.completed-" id "-" n)]]
      [:span {:key dt
              :data-test dt}
       drawer-contents-step-teal-checkmark-atom])
    (for [n (range remaining)
          :let [dt (str "drawer-contents.checkmarks.gray.remaining-" id "-" n)]]
      [:span {:key dt
              :data-test dt}
       drawer-contents-step-gray-checkmark-atom])]))

(defn drawer-contents-condition-title-molecule
  [{:promotion-helper.ui.drawer-contents.condition.title/keys [id primary primary-struck secondary]}]
  (c/html
   (list
    (when primary
      [:div.content-2
       {:key "a"
        :style {:order 1}
        :data-test (str "primary-" id)}
       primary])
    (when primary-struck
      [:div.content-2.strike
       {:key "a" :style {:order 1}
        :data-test (str "primary-struck-" id)}
       primary-struck])
    [:div.content-3.dark-gray.col-12
     {:key "b" :style {:order 3}
      :data-test (str "secondary-" id)}
     secondary])))

(defn drawer-contents-condition-action-molecule
  [{:promotion-helper.ui.drawer-contents.condition.action/keys [id label target]}]
  [:div
   (when id
     (ui/button-small-primary
      (assoc (apply utils/route-to target)
             :data-test id)
      label))])

(defcomponent drawer-contents-condition-organism
  [data _ _]
  [:div.black.bg-white.my1.p3.flex.items-center
   [:div.col-10.flex.flex-wrap
    (drawer-contents-condition-progress-molecule data)
    (drawer-contents-condition-title-molecule data)]
   [:div.col-2
    (drawer-contents-condition-action-molecule data)]])

(defn drawer-contents-footer
  [{:promotion-helper.ui.drawer-contents.footer/keys [id primary cta-label cta-target]}]
  (c/html
   [:div
    (when id
      [:div.flex.items-center.mt3.justify-between
       [:div.content-3 primary]
       (ui/button-small-primary (assoc (apply utils/route-to cta-target)
                                       :data-test id)  cta-label)])]))

(defcomponent organism
  [data _ _]
  (when (seq data)
    [:div.bg-refresh-gray.p3
     {:data-test (:promotion-helper.ui.drawer-contents/id data)}
     (elements drawer-contents-condition-organism data :promotion-helper.ui.drawer-contents/conditions)
     (drawer-contents-footer data)]))
