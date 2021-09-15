(ns promotion-helper.ui
  (:require api.orders
            [clojure.string :as string]
            [promotion-helper.behavior :as behavior]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.stylists :as stylists]
            [storefront.events :as e]
            storefront.keypaths))

(def install-navigation-message
  [e/navigate-category {:catalog/category-id "23"
                        :page/slug           "mayvenn-install"}])
(def wig-navigation-message
  [e/navigate-category {:catalog/category-id "13"
                        :page/slug           "wigs-install"
                        :query-params        {:family (str "360-wigs"
                                                           categories/query-param-separator
                                                           "lace-front-wigs")}}])

(defn drawer-contents-ui<-
  [{:promo.mayvenn-install/keys [failed-criteria-count
                                 hair-success-quantity
                                 hair-missing
                                 hair-missing-quantity]
    :item.service/keys          [stylist]
    :item/keys                  [product-name]
    :copy/keys                  [whats-included]
    :catalog/keys               [sku-id]}]
  (let [[add-more? failure-nav] (if (= "SRV-WGC-000" sku-id)
                                  [false wig-navigation-message]
                                  [true install-navigation-message])]
    (merge
     {:promotion-helper.ui.drawer-contents/id "promotion-helper-contents"
      :promotion-helper.ui.drawer-contents/conditions
      [{:promotion-helper.ui.drawer-contents.condition.title/id             "service"
        :promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Services"
        :promotion-helper.ui.drawer-contents.condition.title/secondary      product-name
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
                   :promotion-helper.ui.drawer-contents.condition.action/target      [behavior/followed {:target    failure-nav
                                                                                                         :condition "add-hair"}]}))

         (not (seq hair-missing))
         (merge {:promotion-helper.ui.drawer-contents.condition.title/primary-struck "Add Your Hair"
                 :promotion-helper.ui.drawer-contents.condition.title/secondary      (if add-more?
                                                                                       "Add more bundles for a fuller look"
                                                                                       (str "You're all set! " whats-included))
                 :promotion-helper.ui.drawer-contents.condition.progress/completed   hair-success-quantity})

         (and (not (seq hair-missing))
              add-more?)
         (merge {:promotion-helper.ui.drawer-contents.condition.secondary.action/id     "condition-add-hair-button" ;; COREY
                 :promotion-helper.ui.drawer-contents.condition.secondary.action/label  "add"
                 :promotion-helper.ui.drawer-contents.condition.secondary.action/target [behavior/followed {:target    failure-nav
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
          [behavior/followed {:target    [e/navigate-adventure-find-your-stylist]
                              :condition "add-stylist"}]})]}
     (when (zero? failed-criteria-count)
       {:promotion-helper.ui.drawer-contents.footer/id         "promotion-helper-conditions-fulfilled-footer"
        :promotion-helper.ui.drawer-contents.footer/primary    "🎉 Great work! Free service unlocked!"
        :promotion-helper.ui.drawer-contents.footer/cta-label  "View Bag"
        :promotion-helper.ui.drawer-contents.footer/cta-target [behavior/followed {:target    [e/navigate-cart]
                                                                                   :condition "view-cart"}]}))))
