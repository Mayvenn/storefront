(ns checkout.added-to-cart
  (:require #?@(:cljs [[storefront.browser.tags :as tags]
                       [storefront.frontend-trackings :as frontend-trackings]
                       [storefront.history :as history]
                       [storefront.hooks.stringer :as stringer]])
            [api.catalog :refer [select ?discountable ?physical ?recent ?service]]
            api.orders
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            promotion-helper.ui
            [promotion-helper.ui.drawer-contents :as drawer-contents]
            [storefront.component :as c]
            [storefront.components.money-formatters :as $]
            [storefront.components.ui :as ui]
            storefront.effects
            [storefront.events :as e]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            storefront.trackings
            ui.molecules))

(defmethod storefront.trackings/perform-track e/cart-interstitial-free-mayvenn-service-tracker-mounted
  [_ _ _ app-state]
  #?(:cljs
     (let [{waiter-order :waiter/order} (api.orders/current app-state)]
       (stringer/track-event
        "add_success_helper_displayed"
        {:current_servicing_stylist_id (:servicing-stylist-id waiter-order)
         :cart_items                   (frontend-trackings/cart-items-model<-
                                        waiter-order
                                        (get-in app-state keypaths/v2-images)
                                        (get-in app-state keypaths/v2-skus))}))))

(c/defdynamic-component promotion-helper-organism
  (did-mount
   [this]
   (let [{:promotion-helper/keys [id]} (c/get-props this)]
     (when id
       (messages/handle-message e/cart-interstitial-free-mayvenn-service-tracker-mounted))))
  (render
   [this]
   (let [{:promotion-helper/keys [id] :as data} (c/get-props this)]
     (c/html
      (if id
        [:div.px4.mtj1
         {:data-test id}
         [:div.canela.title-3.center "FREE Mayvenn Service Tracker"]
         (c/elements drawer-contents/drawer-contents-condition-organism data
                     :promotion-helper.ui.drawer-contents/conditions)]
        [:div])))))

(defmethod storefront.effects/perform-effects e/navigate-added-to-cart
  [_ _ _ _ state]
  #?(:cljs (tags/add-classname ".kustomer-app-icon" "hide"))
  (let [previous-nav   (-> (get-in state keypaths/navigation-undo-stack)
                           first
                           :navigation-message)
        recent-sku-ids (get-in state keypaths/cart-recently-added-skus)]
    #?(:cljs (when (empty? recent-sku-ids)
               (apply history/enqueue-redirect (or previous-nav
                                                   [e/navigate-home]))))))

(defmethod storefront.effects/perform-effects e/control-cart-interstitial-view-cart
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate e/navigate-cart)))

(defmethod storefront.trackings/perform-track e/control-cart-interstitial-view-cart
  [_ _ _ app-state]
  #?(:cljs
     (let [{waiter-order :waiter/order} (api.orders/current app-state)]
       (stringer/track-event
        "add_success_view_cart_button_pressed"
        {:current_servicing_stylist_id (:servicing-stylist-id waiter-order)
         :cart_items                   (frontend-trackings/cart-items-model<-
                                        waiter-order
                                        (get-in app-state keypaths/v2-images)
                                        (get-in app-state keypaths/v2-skus))}))))

(c/defcomponent cta-organism
  [{:cta/keys [primary id target label]} _ _]
  (when id
    [:div.px3.center.mt3
     (when primary [:div.mb1 primary])
     (ui/button-medium-primary
      (assoc (apply utils/fake-href target)
             :data-test id)
      label)]))

(defmethod storefront.trackings/perform-track e/cart-interstitial-browse-stylist-mounted
  [_ _ _ _]
  #?(:cljs (stringer/track-event "add_success_browse_stylist_displayed" {})))

(defmethod storefront.effects/perform-effects e/control-cart-interstitial-browse-stylist-cta
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate e/navigate-adventure-find-your-stylist)))

(defmethod storefront.trackings/perform-track e/control-cart-interstitial-browse-stylist-cta
  [_ _ _ _]
  #?(:cljs (stringer/track-event "add_success_browse_stylist_button_pressed" {})))

(c/defdynamic-component stylist-helper-organism
  (did-mount
   [this]
   (let [{:stylist-helper/keys [id]} (c/get-props this)]
     (when id
       (messages/handle-message e/cart-interstitial-browse-stylist-mounted))))
  (render
   [this]
   (let [{:stylist-helper/keys [id target]} (c/get-props this)]
     (c/html
      (if id
        [:div.stretch.mtj1
         [:div.canela.title-3.center "No Stylist Selected"]
         [:div.content-3
          "Click below to find your licensed" [:br]
          " Mayvenn certified stylist!"]
         [:div.flex.justify-around.mx-auto.mt2
          (ui/button-small-primary
           (assoc (apply utils/fake-href target)
                  :data-test id)
           "Browse Stylists")]]
        [:div])))))

(defn added-to-cart-header-molecule
  [{:added-to-cart.header/keys [primary]}]
  [:div.canela.title-2.center.my4
   {:data-test "cart-interstitial-title"}
   primary])

(c/defcomponent template
  [{:as   queried-data
    :keys [service-items
           cart-items
           spinning?
           header
           cta
           promotion-helper
           stylist-helper
           return-link]}
   owner _]
  [:div.container
   [:div.p2 (ui.molecules/return-link return-link)]
   (if spinning?
     [:div ui/spinner]
     [:div.flex.flex-column.bg-refresh-gray.stretch
      [:div.px2.pt3.pb6
       (added-to-cart-header-molecule header)
       ;; TODO decomplect template positioning from item iteration
       (for [service-item service-items]
         [:div.mt2-on-mb
          {:key (:react/key service-item)}
          (c/build cart-item-v202004/organism
                   {:cart-item service-item}
                   (c/component-id (:react/key service-item)))])
       (when (seq cart-items)
         [:div.mt3
          {:data-test "cart-interstitial-line-items"}
          (for [[index cart-item] (map-indexed vector cart-items)
                :let              [react-key (:react/key cart-item)]
                :when             react-key]
            [:div
             {:key (str index "-cart-item-" react-key)}
             (when-not (zero? index)
               [:div.flex.bg-white
                [:div.ml2 {:style {:width "75px"}}]
                [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto.mr2]])
             (c/build cart-item-v202004/organism
                      {:cart-item cart-item}
                      (c/component-id (str index "-cart-item-" react-key)))])])
       (when cta
         [:div.pt2
          (c/build cta-organism cta)])]
      (when (or stylist-helper promotion-helper)
        [:div.bg-white.center.flex-grow-1
         (c/build stylist-helper-organism stylist-helper)
         (c/build promotion-helper-organism promotion-helper)])])])

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

(defn cart-items<-
  [items]
  (for [item (select (merge ?physical ?recent) items)
        :let
        [{:catalog/keys [sku-id]
          :hacky/keys   [cart-title]
          :item/keys    [unit-price recent-quantity product-name product-title]}
         item]]
    {:react/key                                (str "line-item-" sku-id)
     :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " recent-quantity)}]
     :cart-item-floating-box/id                (str "line-item-price-" sku-id)
     :cart-item-floating-box/contents          [{:text  (some-> unit-price $/as-money)
                                                 :attrs {:data-test (str "line-item-price-ea-" sku-id)}}
                                                {:text  " each"
                                                 :attrs {:class "proxima content-4"}}]
     :cart-item-square-thumbnail/id            (str "line-item-thumbnail-" sku-id)
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (some-> item :hair/length first (str "”"))
     :cart-item-square-thumbnail/ucare-id      (hacky-cart-image item)
     :cart-item-title/id                       (str "line-item-title-" sku-id)
     :cart-item-title/primary                  (or cart-title product-name)
     :cart-item-title/secondary                (ui/sku-card-secondary-text item)}))

(defn service-items<-
  [items]
  (when-let [{:as                free-service-item
              :catalog/keys      [sku-id]
              :item/keys         [unit-price recent-quantity variant-name]
              :join/keys         [addon-facets]
              :hacky/keys        [promo-mayvenn-install-requirement-copy]
              :product/keys      [essential-title essential-price]
              :item.service/keys [addons]}
             (->> items
                  (select (merge ?recent ?discountable))
                  first)]
    [(merge {:react/key                             (str "line-item-" sku-id)
             :cart-item-copy/lines                  [{:id    (str "line-item-requirements-" sku-id)
                                                      :value (or
                                                              (:promo.mayvenn-install/requirement-copy free-service-item)
                                                              promo-mayvenn-install-requirement-copy)}
                                                     {:id    (str "line-item-quantity-" sku-id)
                                                      :value (str "qty. " recent-quantity)}]
             :cart-item-floating-box/id             (str "line-item-price-" sku-id)
             :cart-item-floating-box/contents       [{:text  (some-> (or
                                                                      essential-price
                                                                      unit-price) $/as-money)
                                                      :attrs {:class "strike"}}
                                                     {:text "FREE" :attrs {:class "s-color"}}]
             :cart-item-service-thumbnail/id        (str "line-item-thumbnail-" sku-id)
             :cart-item-service-thumbnail/image-url (hacky-cart-image free-service-item)
             :cart-item-title/id                    (str "line-item-title-" sku-id)
             :cart-item-title/primary               (or
                                                     essential-title
                                                     variant-name)}
            (when (seq addons)
              {:cart-item-sub-items/id    (str "free-service-" sku-id "-addon-services")
               :cart-item-sub-items/title "Add-On Services"
               ;; think about sharing this function
               :cart-item-sub-items/items (map (fn [addon-sku]
                                                 {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                  :cart-item-sub-item/price  (some-> addon-sku :sku/price $/as-money)
                                                  :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                               addons)})
            (when (seq addon-facets)
              {:cart-item.addons/id    "addon-services"
               :cart-item.addons/title "Add-On Services"
               :cart-item.addons/elements
               (->> addon-facets
                    (mapv (fn [facet]
                            {:cart-item.addon/title (:facet/name facet)
                             :cart-item.addon/price (some-> facet :service/price $/as-money)
                             :cart-item.addon/id    (:service/sku-part facet)})))}))]))

(defn header<-
  [items]
  {:added-to-cart.header/primary
   (str (ui/pluralize-with-amount (->> (select ?recent items)
                                       (mapv :item/recent-quantity)
                                       (reduce + 0))
                                  "Item")
        " Added")})

(defn return-link<-
  [app-state]
  #:return-link{:back          (-> app-state (get-in keypaths/navigation-undo-stack) first)
                :copy          "Continue Shopping"
                :event-message [e/navigate-category {:catalog/category-id "23"
                                                     :page/slug           "mayvenn-install"}]
                :id            "continue-shopping"})

(def cta<-
  #:cta{:target [e/control-cart-interstitial-view-cart]
        :label  "Go to Bag"
        :id     "go-to-cart-cta"})

(def celebration-cta<-
  #:cta{:target  [e/control-cart-interstitial-view-cart]
        :label   "Go to Bag"
        :id      "go-to-cart-cta-discounted-service"
        :primary "🎉 Great work! Free service unlocked!"})

(defn promotion-helper<-
  [{:as free-mayvenn-service
    :promo.mayvenn-install/keys [failed-criteria-count]}]
  (when (pos? failed-criteria-count)
    (merge
     (promotion-helper.ui/drawer-contents-ui<- free-mayvenn-service)
     {:promotion-helper/id "free-mayvenn-service-tracker"})))

(def stylist-helper<-
  #:stylist-helper{:id     "browse-stylists"
                   :label  "Browse Stylists"
                   :target [e/control-cart-interstitial-browse-stylist-cta]})

(defn added-to-cart<-
  "Model for which bottom panel to present to the user"
  [items]
  (let [{:as                         free-mayvenn-service
         :promo.mayvenn-install/keys [failed-criteria-count]}
        (first (select ?discountable items))

        services (select ?service items)]
    (cond
      (and free-mayvenn-service (pos? failed-criteria-count))  :promotion-helper
      (and free-mayvenn-service (zero? failed-criteria-count)) :celebration-continue
      (and services
           (nil? (:item.service/stylist (first services))))    :stylist-helper
      :else                                                    :continue)))

(defn ^:export built-component
  [app-state opts]
  (let [{:order/keys [items]} (api.orders/current app-state)
        free-mayvenn-service  (first (select ?discountable items))]
    (c/build template
             (merge
              {:return-link   (return-link<- app-state)
               :spinning?     (empty? (select ?recent items))
               :header        (header<- items)
               :service-items (service-items<- items)
               :cart-items    (cart-items<- items)}
              (case (added-to-cart<- items)
                :continue             {:cta cta<-}
                :celebration-continue {:cta celebration-cta<-}
                :promotion-helper     {:promotion-helper (promotion-helper<- free-mayvenn-service)}
                :stylist-helper       {:stylist-helper stylist-helper<-}))
             opts)))
