(ns promotion-helper.ui
  (:require [clojure.string :as string]
            [promotion-helper.behavior :as behavior]
            [promotion-helper.keypaths :as k]
            [promotion-helper.ui.drawer-contents :as drawer-contents]
            [promotion-helper.ui.drawer-face :as drawer-face]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.events :as e]))

(c/defcomponent promotion-helper-template
  [{:as data :keys [drawer-face drawer-contents]} owner opts]
  [:div.fixed.z4.bottom-0.left-0.right-0
   (c/build drawer-face/organism drawer-face)
   (c/build drawer-contents/organism drawer-contents)])

(defn promotion-helper-ui<-
  [{:promotion-helper/keys [opened?]}
   {:free-mayvenn-service/keys [failed-criteria-count
                                service-item
                                hair-success
                                hair-success-quantity
                                hair-missing
                                hair-missing-quantity
                                failure-navigation-event
                                stylist]}]
  (merge
   {:drawer-face
    (merge
     {:promotion-helper.ui.drawer-face.action/id     "promotion-helper"
      :promotion-helper.ui.drawer-face.action/target [(if opened?
                                                        behavior/ui-promotion-helper-closed
                                                        behavior/ui-promotion-helper-opened)]}
     (if (pos? failed-criteria-count)
       {:promotion-helper.ui.drawer-face.circle/color "bg-red white"
        :promotion-helper.ui.drawer-face.circle/value failed-criteria-count}
       {:promotion-helper.ui.drawer-face.circle/color "bg-white"
        :promotion-helper.ui.drawer-face.circle/value
        (svg/check-mark {:class "fill-teal"
                         :style {:height "12px" :width "14px"}})}))}
   (when opened?
     {:drawer-contents
      (merge
      {:promotion-helper.ui.drawer-contents/id "contents"
       :promotion-helper.ui.drawer-contents/conditions
       [{:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Services"
         :promotion-helper.ui.drawer-contents.condition.title/secondary      (:product-name service-item)
         :promotion-helper.ui.drawer-contents.condition.progress/completed   1
         :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}

         (if (seq hair-missing)
          (let [missing-description (->> hair-missing
                                         (map (fn [{:keys [word missing-quantity]}]
                                                (apply str (if (= 1 missing-quantity)
                                                             ["a " word]
                                                             [missing-quantity " " word "s"])))))]
            {:promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Hair"
              :promotion-helper.ui.drawer-contents.condition.title/secondary    (str "Add " (string/join " and " missing-description))
             :promotion-helper.ui.drawer-contents.condition.progress/completed (- hair-success-quantity hair-missing-quantity)
             :promotion-helper.ui.drawer-contents.condition.progress/remaining hair-missing-quantity
             :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-hair-button" ;; COREY
             :promotion-helper.ui.drawer-contents.condition.action/label       "add"
              :promotion-helper.ui.drawer-contents.condition.action/target      failure-navigation-event})
           {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Hair"
            :promotion-helper.ui.drawer-contents.condition.title/secondary      hair-success
            :promotion-helper.ui.drawer-contents.condition.progress/completed   hair-success-quantity
            :promotion-helper.ui.drawer-contents.condition.progress/remaining   0})
        (if stylist
          {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Stylist"
           :promotion-helper.ui.drawer-contents.condition.title/secondary      (str "You have selected "
                                                                                    (stylists/->display-name stylist)
                                                                                    " as your stylist")
           :promotion-helper.ui.drawer-contents.condition.progress/completed   1
           :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}
          {:promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Stylist"
           :promotion-helper.ui.drawer-contents.condition.title/secondary    "Select a Mayvenn Certified Stylist"
           :promotion-helper.ui.drawer-contents.condition.progress/completed 0
           :promotion-helper.ui.drawer-contents.condition.progress/remaining 1
           :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-stylist-button"
           :promotion-helper.ui.drawer-contents.condition.action/label       "add"
            :promotion-helper.ui.drawer-contents.condition.action/target      [e/navigate-adventure-match-stylist]})]}
       (when (zero? failed-criteria-count)
         {:promotion-helper.ui.drawer-contents.footer/id         "promotion-helper-conditions-fulfilled-footer"
          :promotion-helper.ui.drawer-contents.footer/primary    "🎉 Great work! Free service unlocked!"
          :promotion-helper.ui.drawer-contents.footer/cta-label  "View Cart"
          :promotion-helper.ui.drawer-contents.footer/cta-target [e/navigate-cart]}))})))

(defn promotion-helper-model<-
  "Model depends on existence of a mayvenn service that can be gratis"
  [app-state free-mayvenn-service]
  (when free-mayvenn-service
    {:promotion-helper/opened? (->> k/ui-promotion-helper-opened
                                    (get-in app-state)
                                    boolean)}))

;; COREY
;; Concepts that exist, but not modeled well:
;;   servicing stylist, sku-catalog, and orders
;; Additionally page models, ui states, ffs need to be considered completely

(defn promotion-helper
  [state]
  (let [servicing-stylist      (get-in state adventure.keypaths/adventure-servicing-stylist)
        waiter-order           (get-in state storefront.keypaths/order)
        free-mayvenn-service   (api.orders/free-mayvenn-service servicing-stylist waiter-order)
        promotion-helper-model (promotion-helper-model<- state
                                                         free-mayvenn-service)]
    (when (and (experiments/promotion-helper? state)
               promotion-helper-model)
      (c/build promotion-helper-template
                       (promotion-helper-ui<- promotion-helper-model
                                              free-mayvenn-service)
                       {}))))
