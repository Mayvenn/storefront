(ns checkout.added-to-cart
  (:require #?@(:cljs [[storefront.frontend-trackings :as frontend-trackings]
                       [storefront.hooks.stringer :as stringer]
                       [storefront.history :as history]])
            api.orders
            catalog.images
            catalog.services
            [checkout.shop.cart-v202004 :as cart]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [promotion-helper.ui.drawer-contents :as drawer-contents]
            promotion-helper.ui
            spice.selector
            storefront.trackings
            storefront.effects
            [storefront.accessors.images :as images]
            [storefront.accessors.line-items :as line-items]
            [storefront.accessors.orders :as orders]
            [storefront.component :as component :refer [defcomponent defdynamic-component]]
            [storefront.components.money-formatters :as $]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            ui.molecules
            [storefront.platform.messages :as messages]))

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

(defdynamic-component promotion-helper-component
  (did-mount [this]
    (let [{:promotion-helper/keys [id]} (component/get-props this)]
      (when id
        (messages/handle-message events/cart-interstitial-free-mayvenn-service-tracker-mounted))))
  (render [this]
    (let [{:promotion-helper/keys [id] :as data} (component/get-props this)]
      (component/html
        (if id
          [:div.p4.stretch
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

(defcomponent cta
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

(defdynamic-component stylist-helper
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

(defcomponent template
  [{:as   queried-data
    :keys [title
           service-line-items
           cart-items
           spinning?]}
   owner _]
  [:div.container
   [:div.p2 (ui.molecules/return-link queried-data)]
   (if spinning?
     [:div ui/spinner]
     [:div.bg-refresh-gray.stretch
      [:div.p3
       [:div.canela.title-2.center.my4 {:data-test "cart-interstitial-title"} title]
       (for [service-line-item service-line-items]
         [:div.mt2-on-mb
          (component/build cart-item-v202004/organism {:cart-item service-line-item}
                           (component/component-id (:react/key service-line-item)))])
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

       (component/build cta queried-data nil)]])])

(defn free-service-line-item-query
  [sku-db images-db free-service-line-item addon-skus]
  (let [sku-id (:sku free-service-line-item)]
    (when-let [service-sku (get sku-db sku-id)]
      [(merge {:react/key                             (str "line-item-" sku-id)
               :cart-item-title/id                    (str "line-item-title-" sku-id)
               :cart-item-floating-box/id             (str "line-item-price-" sku-id)
               :cart-item-copy/lines                  [{:id    (str "line-item-requirements-" sku-id)
                                                        :value (:promo.mayvenn-install/requirement-copy service-sku)}
                                                       {:id    (str "line-item-quantity-" sku-id)
                                                        :value (str "qty. " (:quantity free-service-line-item))}]
               :cart-item-floating-box/value          [:span
                                                       [:div.strike (some-> free-service-line-item line-items/service-line-item-price $/as-money)]
                                                       [:div.s-color "FREE"]]
               :cart-item-service-thumbnail/id        (str "line-item-thumbnail-" sku-id)
               :cart-item-service-thumbnail/image-url (->> service-sku (images/skuer->image images-db "cart") :url)
               :cart-item-title/primary               (:variant-name free-service-line-item)}
              (when (seq addon-skus)
                {:cart-item-sub-items/id    (str "free-service-" sku-id "-addon-services")
                 :cart-item-sub-items/title "Add-On Services"
                 ;; think about sharing this function
                 :cart-item-sub-items/items (map (fn [addon-sku]
                                                   {:cart-item-sub-item/title  (:sku/title addon-sku)
                                                    :cart-item-sub-item/price  (some-> addon-sku :sku/price $/as-money)
                                                    :cart-item-sub-item/sku-id (:catalog/sku-id addon-sku)})
                                                 addon-skus)}))])))

(defn cart-items-query
  [sku-db images-db line-items]
  (for [{sku-id :sku :as line-item} line-items
        :let
        [sku   (get sku-db sku-id)
         price (or (:sku/price line-item)
                   (:unit-price line-item))]]
    {:react/key                                (str sku-id "-" (:quantity line-item))
     :cart-item-title/id                       (str "line-item-title-" sku-id)
     :cart-item-title/primary                  (or (:product-title line-item)
                                                   (:product-name line-item))
     :cart-item-copy/lines                     [{:id    (str "line-item-quantity-" sku-id)
                                                 :value (str "qty. " (:quantity line-item))}]
     :cart-item-title/secondary                (:color-name line-item)
     :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku-id)
     :cart-item-floating-box/value             ^:ignore-interpret-warning [:div {:data-test (str "line-item-price-ea-" sku-id)}
                                                                           ($/as-money price)
                                                                           [:div.proxima.content-4 " each"]]
     :cart-item-square-thumbnail/id            sku-id
     :cart-item-square-thumbnail/sku-id        sku-id
     :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> sku :hair/length first)]
                                                 (str length-circle-value "”"))
     :cart-item-square-thumbnail/ucare-id      (->> sku (catalog.images/image images-db "cart") :ucare/id)}))

(defn ^:private a-la-carte-service-line-items-query
  [sku-db images-db line-items]
  (for [{sku-id :sku :as service-line-item} line-items
        :let
        [sku   (get sku-db sku-id)
         price (or (:sku/price service-line-item)
                   (:unit-price service-line-item))]]
    {:react/key                             sku-id
     :cart-item-title/primary               (or (:product-title service-line-item)
                                                (:product-name service-line-item))
     :cart-item-title/id                    (str "line-item-" sku-id)
     :cart-item-copy/lines                  [{:id    (str "line-item-quantity-" sku-id)
                                              :value (str "qty. " (:quantity service-line-item))}]
     :cart-item-floating-box/id             (str "line-item-" sku-id "-price")
     :cart-item-floating-box/value          (some-> price $/as-money)
     :cart-item-service-thumbnail/id        sku-id
     :cart-item-service-thumbnail/image-url (->> sku (catalog.images/image images-db "cart") :ucare/id)}))

(defn query
  [data]
  (let [order                              (get-in data keypaths/order)
        sku-db                             (get-in data keypaths/v2-skus)
        images-db                          (get-in data keypaths/v2-images)
        recently-added-sku-ids->quantities (get-in data storefront.keypaths/cart-recently-added-skus)
        products                           (get-in data keypaths/v2-products)
        facets                             (get-in data keypaths/v2-facets)
        a-la-carte-service-in-cart?        (->> order
                                                orders/service-line-items
                                                (filter line-items/standalone-service?)
                                                seq
                                                boolean)
        recent-line-items                  (->> order
                                                orders/product-and-service-items
                                                (filter (fn [{:keys [sku]}] (contains? recently-added-sku-ids->quantities sku)))
                                                (map (fn [line-item] (assoc line-item :quantity (recently-added-sku-ids->quantities (:sku line-item)))))
                                                ;; NOTE: enables selecting later on
                                                (map #(merge (:variant-attrs %) %)))

        recent-physical-line-items            (->> recent-line-items
                                                   (filter line-items/product?)
                                                   (map (partial cart/add-product-title-and-color-to-line-item products facets)))
        recent-free-mayvenn-service-line-item (->> recent-line-items
                                                   (spice.selector/match-all {:selector/strict? true}
                                                                             catalog.services/discountable)
                                                   first)
        recent-a-la-carte-service-line-items (->> recent-line-items
                                                  (spice.selector/match-all {:selector/strict? true}
                                                                                    catalog.services/a-la-carte))
        recent-addon-service-skus            (->> recent-line-items
                                                  (spice.selector/match-all {:selector/strict? true}
                                                                                    catalog.services/addons)
                                                          (map (fn [addon-service] (get sku-db (:sku addon-service)))))

        {servicing-stylist :services/stylist} (api.orders/services data order)
        free-mayvenn-service                  (api.orders/free-mayvenn-service servicing-stylist order)]
    (merge
     {:title                     (str (ui/pluralize-with-amount (->> recent-physical-line-items
                                                                     (cons recent-free-mayvenn-service-line-item)
                                                                     (concat recent-a-la-carte-service-line-items)
                                                                     (mapv :quantity)
                                                                     (reduce + 0)) "Item")
                                      " Added")
      :service-line-items        (concat
                                  (free-service-line-item-query sku-db images-db recent-free-mayvenn-service-line-item recent-addon-service-skus)
                                  (a-la-carte-service-line-items-query sku-db images-db recent-a-la-carte-service-line-items))
      :cart-items                (cart-items-query sku-db images-db recent-physical-line-items)
      :spinning?                 (empty? recent-line-items)
      :return-link/back          (first (get-in data keypaths/navigation-undo-stack))
      :return-link/copy          "Continue Shopping"
      :return-link/event-message [events/navigate-category {:catalog/category-id "23",
                                                            :page/slug           "mayvenn-install"}]
      :return-link/id            "continue-shopping-link"}
     (cond
       free-mayvenn-service (if (and (:free-mayvenn-service/discounted? free-mayvenn-service)
                                     servicing-stylist)
                              {:cta/target  [events/control-cart-interstitial-view-cart]
                               :cta/label   "Go to Cart"
                               :cta/id      "go-to-cart-cta-discounted-service"
                               :cta/primary "🎉 Great work! Free service unlocked!"}

                              (merge
                               (promotion-helper.ui/drawer-contents-ui<-
                                (->> free-mayvenn-service :free-mayvenn-service/service-item :sku (get sku-db))
                                (api.orders/free-mayvenn-service servicing-stylist order))
                               {:promotion-helper/id "free-mayvenn-service-tracker"}))

       (and a-la-carte-service-in-cart? (nil? servicing-stylist))
       {:stylist-helper/id     "browse-stylists"
        :stylist-helper/label  "Browse Stylists"
        :stylist-helper/target [events/control-cart-interstitial-browse-stylist-cta]}

       :else
       {:cta/target [events/control-cart-interstitial-view-cart]
        :cta/label  "Go to Cart"
        :cta/id     "go-to-cart-cta"}))))

(defn ^:export built-component
  [data opts]
  (component/build template (query data) opts))
