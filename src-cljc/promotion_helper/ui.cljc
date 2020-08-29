(ns promotion-helper.ui
  (:require api.orders
            adventure.keypaths
            [clojure.string :as string]
            [promotion-helper.behavior :as behavior]
            [promotion-helper.keypaths :as k]
            [promotion-helper.ui.drawer-contents :as drawer-contents]
            [promotion-helper.ui.drawer-face :as drawer-face]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.accessors.nav :as nav]
            [storefront.accessors.sites :as sites]
            storefront.keypaths))

(c/defcomponent promotion-helper-template
  [{:keys [drawer-face drawer-contents]} owner opts]
  (let [face     (c/build drawer-face/organism drawer-face)
        contents (c/build drawer-contents/organism drawer-contents)]
    [:div
     [:div.fixed.bottom-0.left-0.right-0.z1.hide-on-dt.stacking-context
      face
      contents]
     [:div.fixed.z1.hide-on-mb-tb.col-4.bottom-0.mx-auto.lit-strong.stacking-context
      {:style {:left      "50%"
               :transform "translate(-50%, 0)"}}
      face contents]]))

(defn promotion-helper-ui<-
  [{:promotion-helper/keys [opened? hair-success]}
   {:free-mayvenn-service/keys [failed-criteria-count
                                service-item
                                hair-success-quantity
                                hair-missing
                                hair-missing-quantity
                                failure-navigation-event
                                add-more?
                                stylist]}]
  (merge
   {:drawer-face
    (merge
     {:promotion-helper.ui.drawer-face.action/id      "promotion-helper"
      :promotion-helper.ui.drawer-face.action/target  [(if opened? behavior/closed behavior/opened)]
      :promotion-helper.ui.drawer-face.action/opened? opened?}
     (if (pos? failed-criteria-count)
       {:promotion-helper.ui.drawer-face.circle/color "bg-red white"
        :promotion-helper.ui.drawer-face.circle/value failed-criteria-count
        :promotion-helper.ui.drawer-face.circle/id    "failed-criteria-count"}
       {:promotion-helper.ui.drawer-face.circle/color "bg-white"
        :promotion-helper.ui.drawer-face.circle/id    "success-criteria-count"
        :promotion-helper.ui.drawer-face.circle/value [:svg/check-mark {:class "fill-teal"
                                                                        :style {:height "12px" :width "14px"}}]}))}
   (when opened?
     {:drawer-contents
      (merge
       {:promotion-helper.ui.drawer-contents/id "promotion-helper-contents"
        :promotion-helper.ui.drawer-contents/conditions
        [{:promotion-helper.ui.drawer-contents.condition.title/id             "service"
          :promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Services"
          :promotion-helper.ui.drawer-contents.condition.title/secondary      (:product-name service-item)
          :promotion-helper.ui.drawer-contents.condition.progress/completed   1
          :promotion-helper.ui.drawer-contents.condition.progress/id          "service"
          :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}

         (cond->
             {:promotion-helper.ui.drawer-contents.condition.title/id           "hair"
              :promotion-helper.ui.drawer-contents.condition.progress/id        "hair"
              :promotion-helper.ui.drawer-contents.condition.progress/remaining hair-missing-quantity}


           (seq hair-missing)
           (merge (let [missing-description (->> hair-missing
                                                 (map (fn [{:keys [word missing-quantity]}]
                                                        (apply str (if (= 1 missing-quantity)
                                                                     ["a " word]
                                                                     [missing-quantity " " word "s"])))))]
                    {:promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Hair"
                     :promotion-helper.ui.drawer-contents.condition.title/secondary    (str "Add " (string/join " and " missing-description))
                     :promotion-helper.ui.drawer-contents.condition.progress/completed (- hair-success-quantity hair-missing-quantity)
                     :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-hair-button" ;; COREY
                     :promotion-helper.ui.drawer-contents.condition.action/label       "add"
                     :promotion-helper.ui.drawer-contents.condition.action/target      [behavior/followed {:target    failure-navigation-event
                                                                                                           :condition "add-hair"}]}))

           (not (seq hair-missing))
           (merge {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Hair"
                   :promotion-helper.ui.drawer-contents.condition.title/secondary      hair-success
                   :promotion-helper.ui.drawer-contents.condition.progress/completed   hair-success-quantity})

           (and (not (seq hair-missing))
                add-more?)
           (merge {:promotion-helper.ui.drawer-contents.condition.secondary.action/id     "condition-add-hair-button" ;; COREY
                   :promotion-helper.ui.drawer-contents.condition.secondary.action/label  "add"
                   :promotion-helper.ui.drawer-contents.condition.secondary.action/target [behavior/followed {:target    failure-navigation-event
                                                                                                              :condition "add-hair"}]}))
         (if stylist
           {:promotion-helper.ui.drawer-contents.condition.title/id             "stylist"
            :promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Stylist"
            :promotion-helper.ui.drawer-contents.condition.title/secondary      (str "You have selected "
                                                                                     (stylists/->display-name stylist)
                                                                                     " as your stylist")
            :promotion-helper.ui.drawer-contents.condition.progress/completed   1
            :promotion-helper.ui.drawer-contents.condition.progress/id          "stylist"
            :promotion-helper.ui.drawer-contents.condition.progress/remaining   0}
           {:promotion-helper.ui.drawer-contents.condition.title/id           "stylist"
            :promotion-helper.ui.drawer-contents.condition.title/primary      "Add Your Stylist"
            :promotion-helper.ui.drawer-contents.condition.title/secondary    "Select a Mayvenn Certified Stylist"
            :promotion-helper.ui.drawer-contents.condition.progress/completed 0
            :promotion-helper.ui.drawer-contents.condition.progress/remaining 1
            :promotion-helper.ui.drawer-contents.condition.progress/id        "stylist"
            :promotion-helper.ui.drawer-contents.condition.action/id          "condition-add-stylist-button"
            :promotion-helper.ui.drawer-contents.condition.action/label       "add"
            :promotion-helper.ui.drawer-contents.condition.action/target
            [behavior/followed {:target    [e/navigate-adventure-match-stylist]
                                :condition "add-stylist"}]})]}
       (when (zero? failed-criteria-count)
         {:promotion-helper.ui.drawer-contents.footer/id         "promotion-helper-conditions-fulfilled-footer"
          :promotion-helper.ui.drawer-contents.footer/primary    "🎉 Great work! Free service unlocked!"
          :promotion-helper.ui.drawer-contents.footer/cta-label  "View Cart"
          :promotion-helper.ui.drawer-contents.footer/cta-target [behavior/followed {:target    [e/navigate-cart]
                                                                                     :condition "view-cart"}]}))})))

(defn promotion-helper-model<-
  "Model depends on existence of a mayvenn service that can be gratis"
  [app-state free-mayvenn-service]
  (when free-mayvenn-service
    (let [nav-event                        (get-in app-state storefront.keypaths/navigation-event)
          sku-id                           (-> free-mayvenn-service :free-mayvenn-service/service-item :sku)
          skus                             (get-in app-state storefront.keypaths/v2-skus)
          service-sku                      (get skus sku-id)
          on-cart-with-criteria-fulfilled? (and (= e/navigate-cart nav-event)
                                                (= 0 (:free-mayvenn-service/failed-criteria-count free-mayvenn-service)))]
      {:promotion-helper/exists?      (and (nav/promotion-helper-can-exist-on-page? nav-event)
                                           (= :shop (sites/determine-site app-state))
                                           (not on-cart-with-criteria-fulfilled?))
       :promotion-helper/hair-success (if (:free-mayvenn-service/add-more? free-mayvenn-service)
                                        "Add more bundles for a fuller look"
                                        (str "You're all set! " (:copy/whats-included service-sku)))
       :promotion-helper/opened?      (->> k/ui-promotion-helper-opened
                                             (get-in app-state)
                                             boolean)})))

;; COREY
;; Concepts that exist, but not modeled well:
;;   servicing stylist, sku-catalog, and orders
;; Additionally page models, ui states, FFs need to be considered completely

(defn promotion-helper
  [state]
  (let [servicing-stylist      (get-in state adventure.keypaths/adventure-servicing-stylist)
        waiter-order           (get-in state storefront.keypaths/order)
        free-mayvenn-service   (api.orders/free-mayvenn-service servicing-stylist waiter-order)
        promotion-helper-model (promotion-helper-model<- state free-mayvenn-service)]
    (when (:promotion-helper/exists? promotion-helper-model)
      (c/build promotion-helper-template
                       (promotion-helper-ui<- promotion-helper-model
                                              free-mayvenn-service)
                       {}))))
