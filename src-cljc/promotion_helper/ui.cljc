(ns promotion-helper.ui
  (:require [promotion-helper.ui.drawer-contents :as drawer-contents]
            [promotion-helper.ui.drawer-face :as drawer-face]
            [promotion-helper.keypaths :as k]
            [promotion-helper.behavior :as behavior]
            [storefront.accessors.experiments :as experiments]
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
                                hair-success hair-missing hair-missing-quantity
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
        (svg/check-mark {:class "fill-teal ml1"
                         :style {:height "12px" :width "14px"}})}))}
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
             :promotion-helper.ui.drawer-contents.condition.action/target      [e/navigate-category
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
           :promotion-helper.ui.drawer-contents.condition.action/target      [e/navigate-adventure-match-stylist]})]}})))

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
        sku-catalog            (get-in state storefront.keypaths/v2-skus)
        waiter-order           (get-in state storefront.keypaths/order)
        free-mayvenn-service   (api.orders/free-mayvenn-service servicing-stylist
                                                                waiter-order
                                                                sku-catalog)
        promotion-helper-model (promotion-helper-model<- state
                                                         free-mayvenn-service)]
    (when (and (experiments/promotion-helper? state)
               promotion-helper-model)
      (c/build promotion-helper-template
                       (promotion-helper-ui<- promotion-helper-model
                                              free-mayvenn-service)
                       {}))))
