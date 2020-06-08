(ns promotion-helper.ui
  (:require [catalog.categories :as categories]
            [storefront.accessors.auth :as auth]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.numbers :as numbers]))

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
  [app-state order]
  (when (experiments/promotion-helper? app-state)
    #{:shown?               (:mayvenn-install/entered? order)
                       :opened?              true
                       :conditions/remaining 2}))

(defn promotion-helper-ui<-
  [model]
  (merge
   {:drawer-face
    {:promotion-helper.ui.drawer-face/id           "promotion-helper"
     :promotion-helper.ui.drawer-face.circle/value (:conditions/remaining model)}}

   (when (:promotion-helper/opened? model)
     {:drawer-contents
      {:promotion-helper.ui.drawer-contents/id "contents"
       :promotion-helper.ui.drawer-contents/steps
       [
        {:promotion-helper.ui.drawer-contents.steps/primary   "Add your services"
         :promotion-helper.ui.drawer-contents.steps/secondary "Frontal Install"}
        {:promotion-helper.ui.drawer-contents.steps/primary   "Add your hair"
         :promotion-helper.ui.drawer-contents.steps/secondary "Add bundles and a frontal closure"}
        {:promotion-helper.ui.drawer-contents.steps/primary   "Add your Stylist"
         :promotion-helper.ui.drawer-contents.steps/secondary nil}]}})))

(defn drawer-face-circle-molecule
  [{:promotion-helper.ui.drawer-face.circle/keys [value]}]
  (component/html
   [:div.circle.bg-red.white.flex.items-center.justify-center.ml2
    {:style {:height "20px" :width "20px"}} value]))

(defcomponent drawer-face-organism
  [data _ _]
  [:div.flex.items-center.justify-center.pl3.pr4.py2.bg-black.white
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

(defcomponent drawer-contents-step-organism
  [data _ _]
  [:div.black.bg-white.my1.p3
   [:div.content-2 (:promotion-helper.ui.drawer-contents.steps/primary data)]
   [:div.content-3.dark-gray (:promotion-helper.ui.drawer-contents.steps/secondary data)]])

(defcomponent drawer-contents-organism
  [data _ _]
  (when (seq data)
    [:div.bg-refresh-gray.p3
     (elements drawer-contents-step-organism
               data
               :promotion-helper.ui.drawer-contents/steps)]))

(defcomponent promotion-helper-template
  [{:as data :keys [drawer-face drawer-contents]} owner opts]
  [:div.fixed.z4.bottom-0.left-0.right-0 #_ {:data-test id}
   ;; COREY use ids
   (component/build drawer-face-organism drawer-face)
   (component/build drawer-contents-organism drawer-contents)])

(defn promotion-helper
  [state]
  (let [order-model (api.orders/current state)
        model       (promotion-helper-model<- state order-model)]
    (prn model)
    (when (:promotion-helper/shown? model)
      (prn "shown?")
      (component/build promotion-helper-template
                       (promotion-helper-ui<- model)
                       {}))))
