(ns order-details.core
  (:require #?@(:cljs [[storefront.api :as api]
                       [storefront.hooks.google-maps :as google-maps]
                       [storefront.history :as history]])
            [api.catalog :as catalog]
            [catalog.images :as catalog-images]
            [checkout.ui.cart-item-v202004 :as cart-item-v202004]
            [clojure.string :as string]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.date :as date]
            [storefront.accessors.line-items :as line-items]
            [storefront.component :as c]
            [storefront.components.checkout-delivery :as checkout-delivery]
            [storefront.components.formatters :as f]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.events :as e]
            [storefront.effects :as effects]
            [storefront.platform.messages :as messages]
            [storefront.transitions :as transitions]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [mayvenn.concept.follow :as follow]
            [mayvenn.concept.hard-session :as hard-session]
            [email-verification.core :as email-verification]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.auth :as auth]
            [storefront.effects :as storefront.effects]
            [storefront.request-keys :as request-keys]
            [storefront.components.flash :as flash]
            [stylist-matching.search.accessors.filters :as stylist-filters]
            [adventure.stylist-matching.maps :as stylist-maps]))

(defn titled-content
  ([title content] (titled-content nil title content))
  ([dt title content]
   [:div.my6 (when dt {:data-test dt})
    [:div.title-2.shout.proxima title]
    [:div.content-1.proxima content]]))

(defn titled-subcontent
  ([title content] (titled-subcontent nil title content))
  ([dt title content]
   [:div.my3 (when dt {:data-test dt})
    [:div.title-3.shout.proxima title]
    [:div.content-1.proxima content]]))


(defn address-copy [{:keys [address1 city state zipcode address2]}]
  (str address1 ", " (when (not-empty address2) address2 ",") city ", " state ", " zipcode))

(defn your-looks-title-template
  [{:your-looks-title/keys [id copy]}]
  (when id
    [:div.title-2.canela.center.mt10.mb6 {:key id} copy]))

;; TODO: break this apart
(defn order-details-template
  [{:order-details/keys [id
                         shipments
                         order-number
                         placed-at
                         shipping-address
                         total
                         pending-cart-items]}]
  (when id
       [:div.my6.max-960.mx-auto
        {:key id}
        [:div.title-1.canela "My Recent Order"]
        (titled-content (str "order-" order-number) "Order Number" order-number)
        (when placed-at
          (titled-content "placed-at" "Placed On" placed-at))
        (titled-content "Shipping Address" (address-copy shipping-address))
        (for [{:keys [fulfillments number title] :as shipment} shipments]
          (titled-content title
                          (into [:div]
                                (for [{:keys [title url carrier tracking-number cart-items type] :as fulf} fulfillments]
                                  (titled-subcontent title
                                                     [:div
                                                      (when url [:a.content-2
                                                                 (utils/fake-href e/external-redirect-url {:url url})
                                                                 tracking-number])
                                                      (interpose
                                                       [:div.flex
                                                        [:div {:style {:width "75px"}}]
                                                        [:div.flex-grow-1.border-bottom.border-cool-gray.ml-auto]]
                                                       (for [[index cart-item] (map-indexed vector cart-items)
                                                             :let              [react-key (:react/key cart-item)]
                                                             :when             react-key]
                                                         [:div.mxn2
                                                          {:key (str index "-cart-item-" react-key)}
                                                          (c/build cart-item-v202004/organism {:cart-item cart-item}
                                                                   (c/component-id (str index "-cart-item-" react-key)))]))])))))
        (titled-content "Payment" [:div {:data-test "payment-total"}
                                   (titled-subcontent "Total" (mf/as-money total))])]))

(defn no-orders-details-template
  [{:no-orders-details/keys [id]}]
  (when id
    [[:div.center.mb4.content-3 "You have no recent orders"]
     (ui/button-medium-primary (utils/route-to e/navigate-category {:page/slug "mayvenn-install" :catalog/category-id "23"}) "Browse Products")]))

;; TODO: does appointment status header need to be there when there's no appt set?
(defn appointment-details-template
  [{:appointment-details/keys [id spinning? date time]}]
  (when id
    [:div.my6.max-960.mx-auto
     {:key id}
     (titled-content "Appointment Status"
                     (titled-subcontent "Appointment Info"
                                        [:div
                                         [:div  "Date: " [:span {:data-test "appointment-date"} date]]
                                         [:div "Time: " [:span {:data-test "appointment-time"} time]]]))]))

(defn stylist-details-template
  [{:stylist-details/keys [id spinning? nickname phone salon-name salon-address map]}]
  (cond
    spinning?
    [:div
     {:style {:min-height "400px"}}
     ui/spinner]

    id
    [:div.my6.max-960.mx-auto
     {:key id
      :data-test id}
     (titled-content "Stylist Info"
                     [:div
                      [:div nickname]
                      [:div [:a {:href (ui/phone-url phone)}
                             phone]]
                      (c/build stylist-maps/component-v2 map)])]))

(defn vouchers-details-template
  [{:vouchers-details/keys [id spinning? vouchers]}]
  (cond
    spinning?
    [:div
     {:style {:min-height "400px"}}
     ui/spinner]

    id
    (into [:div.my6.max-960.mx-auto
           {:key id
            :data-test id}]
          (map (fn [{:vouchers-details/keys [qr-code-url voucher-code services expiration-date redemption-date status]}]
                 (titled-content "Voucher"
                                 [:div
                                  (when qr-code-url
                                    [:div.flex.flex-column.items-center.my6
                                     (ui/img {:src   qr-code-url
                                              :style {:max-width "150px"}})
                                     [:div {:data-test "voucher-code"} voucher-code]])
                                  (titled-subcontent "Status" status)
                                  (if redemption-date
                                    (titled-subcontent "voucher-redemption-date" "Redemption date" redemption-date)
                                    (titled-subcontent "voucher-expiration-date" "Expiration date" expiration-date))
                                  (titled-subcontent "What's included"
                                                     [:div {:data-test "voucher-whats-included"}
                                                      (map (fn [{:keys [included-services]}]
                                                             [:ul
                                                              (map (fn [service-line]
                                                                     [:li service-line])
                                                                   included-services)])
                                                           services)
                                                      [:div.content-3.mt2
                                                       "*Shampoo, Condition, Braid down, and Basic styling included."]])]))
               vouchers)) ))

(c/defcomponent template
  [data _ _]
  [:div.py2.px8.max-960.mx-auto
   {:key "your-look"}
   (let [copy (:verif-status-message/copy data)]
     (when copy (flash/success-box {} copy)))
   (your-looks-title-template data)
   (order-details-template data)
   (no-orders-details-template data)
   (appointment-details-template data)
   (stylist-details-template data)
   (vouchers-details-template data)
   #_[:p.mt8 "If you need to edit or cancel your order, please contact our customer service at "
    (ui/link :link/email :a {} "help@mayvenn.com")
    " or "
    (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
    "."]
   (c/build email-verification/template data)])

(defn generate-tracking-url [carrier tracking-number]
  (when tracking-number
    (some-> carrier
            clojure.string/lower-case
            (case "ups"   "https://www.ups.com/track?loc=en_US&tracknum="
                  "fedex" "https://www.fedex.com/fedextrack/?trknbr="
                  "usps"  "https://tools.usps.com/go/TrackConfirmAction?qtc_tLabels1="
                  "dhl"   "https://www.dhl.com/en/express/tracking.html?AWB="
                  "headex" "https://foo-bar.com/tracking?tracking-number="
                  nil)
            (str tracking-number))))

;; TODO Switch to using spice.date/date?
(defn date? [value]
  (try
    (some? (date/to-iso value))
    (catch #?(:clj Throwable
              :cljs :default) _
      false)))

(defn long-date [dt]
  (when (date? dt)
    (str (f/day->day-abbr dt) ", " #?(:cljs (f/long-date dt)))))

;; TODO: reintroduce a shipping estimate
#_(defn ->shipping-days-estimate [drop-shipping?
                                shipping-sku
                                placed-at]
  (let [{:keys [weekday hour]} #?(:cljs
                                  (->> (.formatToParts
                                        (js/Intl.DateTimeFormat
                                         "en-US" #js
                                                  {:timeZone "America/New_York"
                                                   :weekday  "short"
                                                   :hour     "numeric"
                                                   :hour12   false}) placed-at)
                                       js->clj
                                       (mapv js->clj)
                                       (mapv (fn [{:strs [type value]}]
                                               {(keyword type) value}))
                                       (reduce merge {}))
                                  :clj nil)
        weekday?               (contains? #{"Mon" "Tue" "Wed" "Thu" "Fri"} weekday)
        parsed-hour            (spice/parse-int hour)
        {:keys [saturday-delivery?
                max-delivery]} (checkout-delivery/shipping-method-rules shipping-sku drop-shipping?)
        in-window?             (and weekday?
                                    hour
                                    (< parsed-hour 13)
                                    (or (not (= "Fri" weekday))
                                        (< parsed-hour 10)))]
    (checkout-delivery/number-of-days-to-ship
     weekday
     in-window?
     saturday-delivery?
     max-delivery)))

(defn fulfillment-items-query
  [app-state line-item-ids shipments]
  (let [images (get-in app-state keypaths/v2-images)
        skus   (get-in app-state keypaths/v2-skus)]
    (for [line-item-id line-item-ids
          :let         [all-line-items (mapcat :line-items shipments)
                        line-item (first (filter #(= line-item-id (:line-item-id %))
                                                 all-line-items))]]
      (when-not (line-items/shipping-method? line-item)
        {:react/key                                (str line-item-id "-" (:sku line-item))
         :cart-item-title/id                       (str line-item-id "-" (:sku line-item))
         :cart-item-title/primary                  (or (:product-title line-item)
                                                       (:product-name line-item))
         :cart-item-copy/lines                     [{:id    (str "quantity-" line-item-id "-" (:sku line-item))
                                                     :value (str "qty. " (:quantity line-item))}]
         :cart-item-title/secondary                (ui/sku-card-secondary-text line-item)
         :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" line-item-id)
         :cart-item-floating-box/contents          [{:text  (mf/as-money (:unit-price line-item))
                                                     :attrs {:data-test (str "line-item-price-ea-" (:sku line-item))}}
                                                    {:text " each" :attrs {:class "proxima content-4"}}]
         :cart-item-square-thumbnail/id            line-item-id
         :cart-item-square-thumbnail/sku-id        line-item-id
         :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> (:sku line-item) :hair/length first)]
                                                     (str length-circle-value "”"))
         :cart-item-square-thumbnail/ucare-id      (->> (get skus (:sku line-item)) (catalog-images/image images "cart") :ucare/id)}))))

(defn pending-fulfillment-query
  [app-state pending-line-items]
  (let [images (get-in app-state keypaths/v2-images)
        skus   (get-in app-state keypaths/v2-skus)]
    (for [{:keys [sku] :as line-item} pending-line-items]
      (when-not (line-items/shipping-method? line-item)
        {:react/key                                (str "pending-" sku)
         :cart-item-title/id                       (str "pending-" sku)
         :cart-item-title/primary                  (or (:product-title line-item)
                                                       (:product-name line-item))
         :cart-item-copy/lines                     [{:id    (str "quantity-" sku)
                                                     :value (str "qty. " (:quantity line-item))}]
         :cart-item-title/secondary                (ui/sku-card-secondary-text line-item)
         :cart-item-floating-box/id                (str "line-item-price-ea-with-label-" sku)
         :cart-item-floating-box/contents          [{:text  (mf/as-money (:unit-price line-item))
                                                     :attrs {:data-test (str "line-item-price-ea-" sku)}}
                                                    {:text " each" :attrs {:class "proxima content-4"}}]
         :cart-item-square-thumbnail/id            sku
         :cart-item-square-thumbnail/sku-id        sku
         :cart-item-square-thumbnail/sticker-label (when-let [length-circle-value (-> (:sku line-item) :hair/length first)]
                                                     (str length-circle-value "”"))
         :cart-item-square-thumbnail/ucare-id      (->> (get skus sku) (catalog-images/image images "cart") :ucare/id)}))))

(defn appointment-details-query
  [app-state]
  (when-let [{:keys [appointment-date canceled-at]} (get-in app-state [:models :appointment :date])]
    (when (and false ; Disabled due to Kustomer ingestion irregularities. GROT when resolved.
               appointment-date
               (not canceled-at))
      {:appointment-details/id "appointment-details"
       :appointment-details/date (f/short-date appointment-date)
       :appointment-details/time (f/time-12-hour appointment-date)})))

(defn stylist-details-query
  [app-state]
  (cond
    (utils/requesting? app-state request-keys/fetch-stylist)
    {:stylist-details/spinning? true}

    (get-in app-state [:models :order-details :stylist])
    (let [{:keys [salon
                  store-nickname
                  phone
                  store-slug
                  address]}   (get-in app-state [:models :order-details :stylist])
          {:keys [latitude
                  longitude]} salon]
      {:stylist-details/id       (str "stylist-details-" store-slug)
       :stylist-details/nickname store-nickname
       :stylist-details/phone    (f/phone-number (:phone address))
       :stylist-details/map      {:salon     salon
                                  :latitude  latitude
                                  :longitude longitude
                                  :loaded?   (and (get-in app-state storefront.keypaths/loaded-google-maps)
                                                  (some? latitude)
                                                  (some? longitude))}})))

;; Voucher states: pending, active, redeemed, expired, canceled, new

(defn vouchers-details-query [app-state]
  (cond
    (utils/requesting? app-state request-keys/fetch-vouchers-for-order)
    {:vouchers-details/spinning? true}

    (seq (get-in app-state [:models :order-details :vouchers]))
    {:vouchers-details/id       "vouchers-details"
     :vouchers-details/vouchers (map (fn [{:keys [services qr-code-url voucher-code
                                                  expiration-date redemption-date
                                                  disabled-reason fulfilled? redeemed? identified?]}]
                                       (let [past-expiration-date? (date/after? (date/now) expiration-date)
                                             status                (cond disabled-reason       "Canceled"
                                                                         redeemed?             "Redeemed"
                                                                         past-expiration-date? "Expired"
                                                                         fulfilled?            "Issued"
                                                                         identified?           "Pending"
                                                                         :else                 "Unknown")]
                                         (merge #:vouchers-details
                                                {:qr-code-url     (when (= "Issued" status) qr-code-url)
                                                 :voucher-code    voucher-code
                                                 :expiration-date (f/short-date expiration-date)
                                                 :services        (map (fn [{:keys [service-awards/offered-service-slugs]}]
                                                                         {:included-services (map (fn [service-slug]
                                                                                                    (let [specialty-key (str "specialty-"service-slug)
                                                                                                          addon?        (stylist-filters/service-menu-key->addon? specialty-key)]
                                                                                                      (str
                                                                                                       (stylist-filters/service-menu-key->title specialty-key)
                                                                                                       (if addon? " (add-on)" "*"))))
                                                                                                  offered-service-slugs)})
                                                                       services)
                                                 :whats-included  (mapcat :service-awards/offered-service-slugs services)
                                                 :status          status}
                                                (when redemption-date
                                                  #:vouchers-details{:redemption-date (f/short-date expiration-date)}))))
                                     (get-in app-state [:models :order-details :vouchers]))}))

(defn query [app-state]
  (let [user-verified?       (boolean (get-in app-state k/user-verified-at))
        order-number         (or (:order-number (last (get-in app-state k/navigation-message)))
                                 (-> app-state (get-in k/order-history) first :number))
        images-catalog       (get-in app-state keypaths/v2-images)
        skus                 (get-in app-state keypaths/v2-skus)
        {:as   order
         :keys [fulfillments
                placed-at
                shipments
                shipping-address
                total]}      (->> (get-in app-state k/order-history)
                                  (filter (fn [o] (= order-number (:number o))))
                                  first)
        verif-status-message (-> app-state (get-in keypaths/navigation-message) second :query-params :stsm)]
    (cond-> {}
      (not user-verified?)
      (merge {:your-looks-title/id   "verify-email-title"
              :your-looks-title/copy "Verify Your Email"})

      (and user-verified?
           (not order))
      (merge {:no-orders-details/id  "no-orders-details"
              :your-looks-title/id   "no-order-title"
              :your-looks-title/copy "Your Recent Order"})

      (and user-verified?
           order)
      (merge #:order-details
             {:id               "order-details"
              :order-number     order-number
              :placed-at        (long-date placed-at)
              :shipping-address shipping-address
              :total            total
              :skus             skus
              :images-catalog   images-catalog
              :shipments        (for [{:keys [number
                                              created-at
                                              state
                                              line-items]
                                       :as   shipment} shipments
                                      :let             [shipment-fulfillments  (filter #(= number (:shipment-number %)) fulfillments)
                                                        physical-fulfillments  (filter #(= "physical" (:type %)) shipment-fulfillments)
                                                        voucher-fulfillments   (filter #(= "voucher" (:type %)) shipment-fulfillments)
                                                        unfulfilled-line-items (filter #(and (nil? (:line-item-id %))
                                                                                             (= "spree" (:source %))) line-items)]
                                      :when            (not= "pending" state)]
                                  {:number       number
                                   :title        (cond
                                                   (= 1 (count shipments)) "Order"
                                                   (= "S1" number)         "Original Order"
                                                   (= 2 (count shipments)) "Replacement Order"
                                                   :else                   (str "Replacement Order #" (-> number (subs 1) spice/parse-int dec)))
                                   :fulfillments (concat
                                                  (for [{:keys [carrier
                                                                carrier-service
                                                                tracking-number
                                                                line-item-ids]} physical-fulfillments]
                                                    {:title           (when (and carrier carrier-service)
                                                                        (str carrier " - " carrier-service))
                                                     :url             (generate-tracking-url carrier tracking-number)
                                                     :carrier         carrier
                                                     :tracking-number tracking-number
                                                     :cart-items      (fulfillment-items-query app-state line-item-ids shipments)})
                                                  (for [{:keys [line-item-ids]} voucher-fulfillments]
                                                    {:title      "Service"
                                                     :cart-items (fulfillment-items-query app-state line-item-ids shipments)})
                                                  (when-let [line-items (seq unfulfilled-line-items)]
                                                    [{:title      "Pending"
                                                      :cart-items (pending-fulfillment-query app-state line-items)}]))})}
             (appointment-details-query app-state)
             (stylist-details-query app-state)
             (vouchers-details-query app-state)

             {:verif-status-message/copy (when (= "verif-success" verif-status-message) "Your email was successfully verified.")}))))

(defn ^:export page
  [app-state]
  (c/build template (merge (query app-state)
                           (email-verification/query app-state))))

(defmethod effects/perform-effects e/navigate-yourlooks-order-details
  ;; evt = email validation token
  [_ event {:keys [order-number query-params] :as args} _ app-state]
  (let [user-verified-at (get-in app-state k/user-verified-at)
        evt              (:evt query-params)
        sign-in-data     (hard-session/signed-in app-state)]
    (cond
      (not (hard-session/allow? sign-in-data event))
      (effects/redirect e/navigate-sign-in)

      (and (not user-verified-at)
           evt
           (not (utils/requesting? app-state request-keys/email-verification-verify)))
      (messages/handle-message e/biz|email-verification|verified {:evt evt})

      user-verified-at
      #?(:cljs (do (google-maps/insert)
                   (if order-number
                     (api/get-order {:number     order-number
                                     :user-id    (get-in app-state k/user-id)
                                     :user-token (get-in app-state k/user-token)}
                                    {:handler       #(messages/handle-message e/flow--orderdetails--resulted {:orders [%]})
                                     :error-handler #(messages/handle-message e/flash-show-failure
                                                                              {:message (str "Unable to retrieve order " order-number ". Please contact support.")})})
                     (api/get-orders {:limit      1
                                      :user-id    (get-in app-state k/user-id)
                                      :user-token (get-in app-state k/user-token)}
                                     #(messages/handle-message e/flow--orderdetails--resulted {:orders (:results %)})
                                     #(messages/handle-message e/flash-show-failure
                                                               {:message (str "Unable to retrieve order. Please contact support.")}))))
         :clj nil))))

(defmethod transitions/transition-state e/flow--orderdetails--resulted
  [_ _ {:keys [orders]} app-state]
  (let [old-orders (maps/index-by :number (get-in app-state k/order-history))
        new-orders (maps/index-by :number orders)
        order-history (->> (merge old-orders new-orders)
                           vals
                           (sort-by :placed-at >))]
    (-> app-state
        (assoc-in k/order-history order-history)
        (assoc-in [:models :appointment :date] (:appointment (first order-history))))))

(defmethod effects/perform-effects e/flow--orderdetails--resulted
  [_ _ args _ app-state]
  (let [most-recent-open-order (first (get-in app-state k/order-history))
        api-cache              (get-in app-state keypaths/api-cache)]
    (messages/handle-message e/ensure-sku-ids {:sku-ids (->> most-recent-open-order
                                                             :shipments
                                                             (mapcat :line-items)
                                                             (filter line-items/product-or-service?)
                                                             (map :sku))})
    #?(:cljs
       (api/fetch-vouchers-for-order (get-in app-state k/user-id)
                                     (get-in app-state k/user-token)
                                     (:number most-recent-open-order)
                                     {:success-handler #(messages/handle-message e/flow--orderdetails--vouchers-resulted %)
                                      :error-handler   identity}))
    (when-let [ssid (:servicing-stylist-id most-recent-open-order)]
      #?(:cljs
         (api/fetch-stylist api-cache
                            (get-in app-state k/user-id)
                            (get-in app-state k/user-token)
                            ssid
                            {:success-handler #(messages/handle-message e/flow--orderdetails--stylist-resulted %)})))))

(defmethod transitions/transition-state e/flow--orderdetails--vouchers-resulted
  [_ _ {:keys [vouchers]} app-state]
  (assoc-in app-state [:models :order-details :vouchers] vouchers))

(defmethod transitions/transition-state e/flow--orderdetails--stylist-resulted
  [_ _ stylist app-state]
  (assoc-in app-state [:models :order-details :stylist] stylist))
