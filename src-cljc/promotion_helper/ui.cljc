(ns promotion-helper.ui
  (:require [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(defn ^:private elements
  "Embed a list of organisms in another organism."
  ([organism data elem-key]
   (elements organism data elem-key :default))
  ([organism data elem-key breakpoint]
   (let [elems (get data elem-key)]
     (for [[idx elem] (map-indexed vector elems)]
       (component/build organism
                elem
                (component/component-id elem-key
                                breakpoint
                                idx))))))

(defn promotion-helper-model<-
  ;; COREY a ui state model for the session
  ;; COREY existing structure for menus, collapse?
  [app-state free-mayvenn-service]
  (when (and (experiments/promotion-helper? app-state)
             free-mayvenn-service)
    (assoc free-mayvenn-service
           :promotion-helper/opened?
           (boolean
            (get-in app-state
                    [:ui :promotion-helper :opened?])))))

(defn promotion-helper-ui<-
  [{:promotion-helper/keys [opened?]
    :free-mayvenn-service/keys [failed-criteria-count
                                service-item
                                hair-success hair-missing hair-missing-quantity
                                stylist]
    :as model}]
  (merge
   {:drawer-face
    {:promotion-helper.ui.drawer-face.action/id     "promotion-helper"
     :promotion-helper.ui.drawer-face.action/target [(if opened?
                                                       [:ui :promotion-helper :closed]
                                                       [:ui :promotion-helper :opened])]
     :promotion-helper.ui.drawer-face.circle/value  failed-criteria-count}}
   (when opened?
     {:drawer-contents
      {:promotion-helper.ui.drawer-contents/id "contents"
       :promotion-helper.ui.drawer-contents/conditions
       [{:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Services"
         :promotion-helper.ui.drawer-contents.condition.title/secondary      (:product-name service-item)
         :promotion-helper.ui.drawer-contents.condition.progress/completed   1
         :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}

        (if (empty? hair-missing)
          {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Hair"
           :promotion-helper.ui.drawer-contents.condition.title/secondary      hair-success
           :promotion-helper.ui.drawer-contents.condition.progress/completed   3
           :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}
          (let [missing-description (->> hair-missing
                                         (map (fn [[word _ quantity _]]
                                                (str quantity " " word))))]
            {:promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Hair"
             :promotion-helper.ui.drawer-contents.condition.title/secondary    (->> missing-description
                                                                                    (clojure.string/join " and ")
                                                                                    (str "Add "))
             :promotion-helper.ui.drawer-contents.condition.progress/completed (- 3 hair-missing-quantity)
             :promotion-helper.ui.drawer-contents.condition.progress/remaining hair-missing-quantity
             :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-hair-button" ;; COREY
             :promotion-helper.ui.drawer-contents.condition.action/label       "add"
             :promotion-helper.ui.drawer-contents.condition.action/target      [events/navigate-category
                                                                                {:catalog/category-id "23"
                                                                                 :page/slug           "mayvenn-install"}]}))
        (if stylist
          {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Stylist"
           :promotion-helper.ui.drawer-contents.condition.title/secondary      (str "You have selected "
                                                                                    (:store-nickname stylist)
                                                                                    " as your stylist")
           :promotion-helper.ui.drawer-contents.condition.progress/completed   1
           :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}
          {:promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Stylist"
           :promotion-helper.ui.drawer-contents.condition.title/secondary    "Select a Mayvenn Certified Stylist"
           :promotion-helper.ui.drawer-contents.condition.progress/completed 0
           :promotion-helper.ui.drawer-contents.condition.progress/remaining 1
           :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-stylist-button"
           :promotion-helper.ui.drawer-contents.condition.action/label       "add"
           :promotion-helper.ui.drawer-contents.condition.action/target      [events/navigate-adventure-match-stylist]})]}})))

(defn drawer-face-circle-molecule
  [{:promotion-helper.ui.drawer-face.circle/keys [value]}]
  (component/html
   [:div.circle.bg-red.white.flex.items-center.justify-center.ml2
    {:style {:height "20px" :width "20px"}} value]))

(defcomponent drawer-face-organism
  [data _ _]
  [:div.flex.items-center.justify-center.pl3.pr4.py2.bg-black.white
   (apply utils/fake-href
          (:promotion-helper.ui.drawer-face.action/target data))
   [:div.flex-auto.pr4
    [:div.flex.items-center.justify-left.proxima.button-font-2.bold
     ;; primary
     [:div.shout "Free Mayvenn Service Tracker"]
     (drawer-face-circle-molecule data)]
    ;; secondary
    [:div.button-font-3.mtp4.regular "Swipe up to learn how to get your service for free"]]
   ;; chevron
   [:div.fill-white.flex.items-center.justify-center
    (svg/dropdown-arrow {:height "18px"
                         :width  "18px"})]])

(def drawer-contents-step-teal-checkmark-atom
  (svg/check-mark {:class "fill-teal ml1"
                   :style {:height "14px" :width "18px"}}))

(def drawer-contents-step-gray-checkmark-atom
  (svg/check-mark {:class "fill-gray ml1"
                   :style {:height "14px" :width "18px"}}))

(defn drawer-contents-condition-progress-molecule
  [{:promotion-helper.ui.drawer-contents.condition.progress/keys [completed remaining]}]
  (component/html
   [:div.flex-auto.pl1 {:key "c" :style {:order 2}}
    (for [n (range completed)]
      [:span {:key (str "promotion-helper.ui.drawer-contents.steps.checkmarks.teal." n)}
       drawer-contents-step-teal-checkmark-atom])
    (for [n (range remaining)]
      [:span {:key (str "promotion-helper.ui.drawer-contents.steps.checkmarks.gray." n)}
       drawer-contents-step-gray-checkmark-atom])]))

(defn drawer-contents-condition-title-molecule
  [{:promotion-helper.ui.drawer-contents.condition.title/keys [primary primary-struck secondary]}]
  (component/html
   (list
    (when primary
      [:div.content-2 {:key "a" :style {:order 1}}
       primary])
    (when primary-struck
      [:div.content-2.strike {:key "a" :style {:order 1}}
       primary-struck])
    [:div.content-3.dark-gray.col-12 {:key "b" :style {:order 3}} secondary])))

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
  [:div.black.bg-white.my1.p3.flex
   [:div.col-10.flex.flex-wrap
    (drawer-contents-condition-progress-molecule data)
    (drawer-contents-condition-title-molecule data)]
   [:div.col-2
    (drawer-contents-condition-action-molecule data)]])

(defcomponent drawer-contents-organism
  [data _ _]
  (when (seq data)
    [:div.bg-refresh-gray.p3
     (elements drawer-contents-condition-organism
               data
               :promotion-helper.ui.drawer-contents/conditions)]))

(defcomponent promotion-helper-template
  [{:as data :keys [drawer-face drawer-contents]} owner opts]
  [:div.fixed.z4.bottom-0.left-0.right-0
   ;; COREY use ids
   (component/build drawer-face-organism drawer-face)
   (component/build drawer-contents-organism drawer-contents)])

(defn promotion-helper
  [state]
  (let [servicing-stylist (get-in state adventure.keypaths/adventure-servicing-stylist)
        sku-catalog       (get-in state storefront.keypaths/v2-skus) ;; COREY modelize
        waiter-order      (get-in state storefront.keypaths/order)   ;; COREY modelize
        mayvenn-install   (api.orders/free-mayvenn-service servicing-stylist
                                                           waiter-order
                                                           sku-catalog)
        model             (promotion-helper-model<- state mayvenn-install)]
    (when (seq model)
      (component/build promotion-helper-template
                       (promotion-helper-ui<- model)
                       {}))))
