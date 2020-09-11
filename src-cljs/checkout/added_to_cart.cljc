(ns checkout.added-to-cart
  (:require #?@(:cljs [[storefront.frontend-trackings :as frontend-trackings]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.history :as history]])
            api.orders
            catalog.services
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            promotion-helper.ui
            [promotion-helper.ui.drawer-contents :as drawer-contents]
            spice.selector
            [storefront.component
             :as
             component
             :refer
             [defcomponent defdynamic-component]]
            [storefront.components.money-formatters :as $]
            [storefront.components.ui :as ui]
            storefront.effects
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            storefront.trackings
            ui.molecules))

(defn ^:private elements
  "Embed a list of organisms in another organism."
  ([organism data elem-key]
   (elements organism data elem-key :default))
  ([organism data elem-key breakpoint]
   (let [elems (get data elem-key)]
     (for [[idx elem] (map-indexed vector elems)]
       (component/build organism elem (component/component-id elem-key breakpoint idx))))))

(defmethod storefront.trackings/perform-track events/cart-interstitial-free-mayvenn-service-tracker-mounted
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

(defdynamic-component promotion-helper-organism
  (did-mount
   [this]
   (let [{:promotion-helper/keys [id]} (component/get-props this)]
     (when id
       (messages/handle-message events/cart-interstitial-free-mayvenn-service-tracker-mounted))))
  (render
   [this]
   (let [{:promotion-helper/keys [id] :as data} (component/get-props this)]
     (component/html
      (if id
        [:div.p4
         [:div.mt2.canela.title-3.center "FREE Mayvenn Service Tracker"]
         (elements drawer-contents/drawer-contents-condition-organism data
                   :promotion-helper.ui.drawer-contents/conditions)]
        [:div])))))

(defmethod storefront.effects/perform-effects events/navigate-added-to-cart
  [_ _ _ _ app-state]
  #?(:cljs
     (let [previous-nav (-> (get-in app-state keypaths/navigation-undo-stack) first :navigation-message)]
       (when (empty? (get-in app-state keypaths/cart-recently-added-skus))
         (apply history/enqueue-redirect (or previous-nav [events/navigate-home]))))))


(defmethod storefront.effects/perform-effects events/control-cart-interstitial-view-cart
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-cart)))

(defmethod storefront.trackings/perform-track events/control-cart-interstitial-view-cart
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

(defcomponent cta-organism
  [{:cta/keys [primary id target label]} _ _]
  (when id
    [:div.px3.center.mt3
     (when primary [:div.mb1 primary])
     (ui/button-medium-primary
      (assoc (apply utils/fake-href target)
             :data-test id)
      label)]))

(defmethod storefront.trackings/perform-track events/cart-interstitial-browse-stylist-mounted
  [_ _ _ _]
  #?(:cljs (stringer/track-event "add_success_browse_stylist_displayed" {})))

(defmethod storefront.effects/perform-effects events/control-cart-interstitial-browse-stylist-cta
  [_ _ _ _ _]
  #?(:cljs (history/enqueue-navigate events/navigate-adventure-match-stylist)))

(defmethod storefront.trackings/perform-track events/control-cart-interstitial-browse-stylist-cta
  [_ _ _ _]
  #?(:cljs (stringer/track-event "add_success_browse_stylist_button_pressed" {})))

(defdynamic-component stylist-helper-organism
  (did-mount [this]
    (let [{:stylist-helper/keys [id]} (component/get-props this)]
      (when id
        (messages/handle-message events/cart-interstitial-browse-stylist-mounted))))
  (render [this]
    (let [{:stylist-helper/keys [id target]} (component/get-props this)]
      (component/html
       (if id
         [:div.p4.stretch
          [:div.canela.title-3.center "No Stylist Selected"]
          [:div.content-3
           "Click below to find your licensed" [:br]
           " Mayvenn certified stylist!"]
          [:div.p2.flex.justify-around.mx-auto.mt3
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

(defcomponent template
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
     [:div.flex-column
      [:div.bg-refresh-gray.flex-grow-0
       [:div.p3
        (added-to-cart-header-molecule header)
        (for [service-item service-items]
          [:div.mt2-on-mb
           (component/build cart-item-v202004/organism {:cart-item service-item}
                            (component/component-id (:react/key service-item)))])
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
              (component/build cart-item-v202004/organism {:cart-item cart-item}
                               (component/component-id (str index "-cart-item-" react-key)))])])

        (when cta
          (component/build cta-organism cta))]]
      [:div.bg-white.center.flex-grow
       (when stylist-helper
         (component/build stylist-helper-organism stylist-helper))
       (when promotion-helper
         (component/build promotion-helper-organism promotion-helper))]])])

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

(def ^:private select
  (partial spice.selector/match-all {:selector/strict? true}))

(def ^:private recent
  {:item/recent? #{true}})

(def ^:private physical
  {:catalog/department #{"hair"}})

(defn free-service-line-item-query
  [{:as           free-service-item
    :catalog/keys [sku-id]
    :item/keys    [unit-price recent-quantity variant-name]}
   addon-skus]
  (when free-service-item
    [(merge {:react/key                             (str "line-item-" sku-id)
             :cart-item-copy/lines                  [{:id    (str "line-item-requirements-" sku-id)
                                                      :value (:promo.mayvenn-install/requirement-copy free-service-item)}
                                                     {:id    (str "line-item-quantity-" sku-id)
                                                      :value (str "qty. " recent-quantity)}]
             :cart-item-floating-box/id             (str "line-item-price-" sku-id)
             :cart-item-floating-box/value          [:span
                                                     [:div.strike (some-> unit-price (* recent-quantity) $/as-money)]
                                                     [:div.s-color "FREE"]]
             :cart-item-service-thumbnail/id        (str "line-item-thumbnail-" sku-id)
             :cart-item-service-thumbnail/image-url (hacky-cart-image free-service-item)
             :cart-item-title/id                    (str "line-item-title-" sku-id)
             :cart-item-title/primary               variant-name}
            (when (seq addon-skus)
              {:cart-item-sub-items/id    (str "free-service-" sku-id "-addon-services")
               :cart-item-sub-items/title "Add-On Services"
               ;; think about sharing this function
               :cart-item-sub-items/items (map (fn [addon-sku]
                                                 {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                  :cart-item-sub-item/price  (some-> addon-sku :sku/price $/as-money)
                                                  :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                               addon-skus)}))]))

(defn ^:private a-la-carte-service-line-items-query
  [service-items]
  (for [service-item service-items
        :let
        [{:catalog/keys [sku-id]
          :item/keys    [unit-price recent-quantity product-name product-title]}
         service-item]]
    {:react/key                             (str "line-item-" sku-id)
     :cart-item-copy/lines                  [{:id    (str "line-item-quantity-" sku-id)
                                              :value (str "qty. " recent-quantity)}]
     :cart-item-floating-box/id             (str "line-item-price-" sku-id)
     :cart-item-floating-box/value          (some-> unit-price (* recent-quantity) $/as-money)
     :cart-item-service-thumbnail/id        (str "line-item-thumbnail-" sku-id)
     :cart-item-service-thumbnail/image-url (hacky-cart-image service-item)
     :cart-item-title/id                    (str "line-item-title-" sku-id)
     :cart-item-title/primary               (or product-title product-name)}))

(defn cart-items<-
  [items]
  (for [item (select (merge recent physical) items)
        :let
        [{:catalog/keys [sku-id]
          :item/keys    [unit-price recent-quantity product-name product-title]}
         item]]
    {:react/key                                (str "line-item-" sku-id)
     :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " recent-quantity)}]
     :cart-item-floating-box/id                (str "line-item-price-" sku-id)
     :cart-item-floating-box/value             ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                           (some-> unit-price (* recent-quantity) $/as-money)
                                                                           [:div.proxima.content-4 " each"]]
     :cart-item-square-thumbnail/id            (str "line-item-thumbnail-" sku-id)
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (some-> item :hair/length first (str "”"))
     :cart-item-square-thumbnail/ucare-id      (hacky-cart-image item)
     :cart-item-title/id                       (str "line-item-title-" sku-id)
     :cart-item-title/primary                  (or product-title product-name)
     :cart-item-title/secondary                (some-> item :hair/color first)}))

(defn service-items<-
  [items]
  (let [recent-free-mayvenn (first (select (merge recent catalog.services/discountable) items))
        recent-addons       (select (merge recent catalog.services/addons) items)
        recent-a-la-carte   (select (merge recent catalog.services/a-la-carte) items)]
    (concat (free-service-line-item-query recent-free-mayvenn
                                          recent-addons)
            (a-la-carte-service-line-items-query recent-a-la-carte))))

(defn header<-
  [items]
  {:added-to-cart.header/primary
   (str (ui/pluralize-with-amount (->> (select recent items)
                                       (mapv :item/recent-quantity)
                                       (reduce + 0))
                                  "Item")
        " Added")})

(defn return-link<-
  [app-state]
  #:return-link{:back          (-> app-state (get-in keypaths/navigation-undo-stack) first)
                :copy          "Continue Shopping"
                :event-message [events/navigate-category {:catalog/category-id "23"
                                                          :page/slug           "mayvenn-install"}]
                :id            "continue-shopping-link"})

(def cta<-
  #:cta{:target [events/control-cart-interstitial-view-cart]
        :label  "Go to Cart"
        :id     "go-to-cart-cta"})

(def celebration-cta<-
  #:cta{:target  [events/control-cart-interstitial-view-cart]
        :label   "Go to Cart"
        :id      "go-to-cart-cta-discounted-service"
        :primary "🎉 Great work! Free service unlocked!"})

(defn promotion-helper<-
  [sku-db
   {:as free-mayvenn-service
    :free-mayvenn-service/keys [failed-criteria-count service-item]}]
  (when (pos? failed-criteria-count)
    (let [service-sku (get sku-db (:sku service-item))]
      (merge
       (promotion-helper.ui/drawer-contents-ui<- service-sku
                                                 free-mayvenn-service)
       {:promotion-helper/id "free-mayvenn-service-tracker"}))))

(def stylist-helper<-
  #:stylist-helper{:id     "browse-stylists"
                   :label  "Browse Stylists"
                   :target [events/control-cart-interstitial-browse-stylist-cta]})

(defn added-to-cart<-
  "Model for which bottom panel to present to the user "
  [items
   {:as                        free-mayvenn-service
    :free-mayvenn-service/keys [failed-criteria-count]}
   stylist]
  (cond
    (and free-mayvenn-service (pos? failed-criteria-count))      :promotion-helper
    (and free-mayvenn-service (zero? failed-criteria-count))     :celebration-continue
    (and (select catalog.services/service items) (nil? stylist)) :stylist-helper
    :else                                                        :continue))

(defn ^:export built-component
  [app-state opts]
  (let [;; data layers
        waiter-order (get-in app-state keypaths/order)
        sku-db       (get-in app-state keypaths/v2-skus)

        ;; business layers
        {:order/keys [items]} (api.orders/->order app-state waiter-order)
        {servicing-stylist
         :services/stylist}   (api.orders/services app-state waiter-order)
        free-mayvenn-service  (api.orders/free-mayvenn-service servicing-stylist
                                                               waiter-order)]
    (component/build template
                     (merge
                      {:return-link   (return-link<- app-state)
                       :spinning?     (empty? (select recent items))
                       :header        (header<- items)
                       :service-items (service-items<- items)
                       :cart-items    (cart-items<- items)}
                      (case (added-to-cart<- items
                                             free-mayvenn-service
                                             servicing-stylist)
                        :continue             {:cta cta<-}
                        :celebration-continue {:cta celebration-cta<-}
                        :promotion-helper     {:promotion-helper
                                               (promotion-helper<- sku-db
                                                                   free-mayvenn-service)}
                        :stylist-helper       {:stylist-helper stylist-helper<-}))
                     opts)))
