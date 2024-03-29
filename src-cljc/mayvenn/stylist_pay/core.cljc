(ns mayvenn.stylist-pay.core
  (:require api.stylist
            [storefront.component :as c]
            [storefront.components.footer-links :as footer-links]
            [storefront.components.ui :as ui]
            [storefront.config :as config]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            #?@(:cljs
                [[storefront.hooks.stripe :as stripe]])
            [storefront.effects :as fx]
            [storefront.events :as e]
            [mayvenn.concept.stylist-payment :as stylist-payment]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.tools :refer [with]]))

(c/defdynamic-component ^:private new-card-1
  (did-mount
   [_]
   (publish e/stripe-component-mounted
            {:card-element
             #?(:clj nil
                :cljs
                (stripe/card-element "#card-element"))}))
  (will-unmount
   [_]
   (publish e/stripe-component-will-unmount))
  (render
   [this]
   (let [{:keys [keypath focused errors value]}
         (c/get-props this)]
     (c/html
      [:div
       [:div.content-3 "Cardholder's name"]
       (ui/text-field {:errors    (get errors ["cardholder-name"])
                       :data-test "new-card-1-cardholder-name"
                       :keypath   keypath
                       :focused   focused
                       :name      "name"
                       :required  true
                       :value     value})
       (let [card-errors (keep #(get-in errors [% :long-message])
                               [["card-number"]
                                ["card-expiration"]
                                ["security-code"]
                                ["card-error"]])]
         [:div
          [:div.content-3 "Credit card details"]
          [:div#card-element.border.rounded.p2
           {:style {:height "47px"}
            :class (if (seq card-errors)
                     "border-error error"
                     "border-gray")}]
          (when (seq card-errors)
            [:div.h6.my1.error.center.medium
             {:data-test "new-card-1-card-error"}
             (first card-errors)])])]))))

(c/defcomponent field-1
  [{:keys [primary placeholder id keypath required]
    type_ :type
    min_  :min
    max_  :max} _ _]
  [:div
   [:div.content-3 primary]
   (ui/text-field
    (cond->
        {:label     placeholder
         :keypath   keypath
         :data-test id}
        (boolean? required) (assoc :required required)
        (string? type_)     (assoc :type type_)
        (int? max_)         (assoc :max max_)
        (int? min_)         (assoc :min min_)))])

(c/defcomponent checkbox-1
  [{:keys [primary id keypath value]} _ _]
  [:div
   [:div.col-12.my1
    [:label.py1
     [:div.flex
      [:div.flex-1
       (ui/check-box
        {:type      "checkbox"
         :id        id
         :data-test id
         :value     value
         :keypath   keypath})]
      [:div.content-3.flex-auto
       primary]]]]])

;;;

(c/defcomponent header
  [{:keys [target]} _ _]
  [:div
   (ui/clickable-logo
    (merge
     {:data-test "header-logo"
      :height    "28px"}
     (when (seq target)
       {:event (first target)})))])

(c/defcomponent footer-1
  [_ _ _]
  [:div.py2.px3.flex.flex-column
   [:div.title-2.proxima.shout.mx-auto.mb1 "Need Help?"]
   [:div.content-3.proxima.mx-auto
    (ui/link :link/phone :a.inherit-color {} config/support-phone-number)
    " | 8am-5pm PST M-F"]
   [:div.left
    (c/build footer-links/component {:minimal? true} nil)]])

;;;

(defn flipped-title
  [{:keys [id icon primary secondary tertiary]}]
  (c/html
   [:div.flex.flex-column.items-center
    [:div.proxima.title-3.flex.flex-auto.shout
     secondary]
    [:div.canela.title-1
     primary]
    [:div.proxima.content-3.dark-gray.col-10.center
     tertiary]]))

(defn stylist-title
  [{:keys [img-url primary secondary]}]
  (c/html
   [:div.flex.items-center
    [:div.pr4
     (ui/circle-picture
      {:width "72px"}
      (ui/square-image {:resizable-url img-url}
                       72))]
    [:div
     [:div.proxima.content-2 primary]
     [:div.proxima.content-3 secondary]]]))

;;;

(c/defcomponent spinner
  [data _ _]
  [:div.my4
   (c/build header (with :header data))
   [:div.col-12.bg-cool-gray.myj1.pyj2.stretch
    (flipped-title (with :title data))
    (ui/large-spinner
     {:style {:height "6em"}})]])

(c/defcomponent template
  [data _ _]
  [:div.my4
   (c/build header (with :header data))
   [:div.col-12.bg-cool-gray.myj1.pyj2
    (flipped-title (with :title data))]
   [:div.mx4
    (stylist-title (with :stylist data))
    [:div.py4
     [:div.proxima-3.proxima.shout.bold.pb2
      "Payment Information"]
     (c/build field-1 (with :amount data))
     (c/build field-1 (with :note data))]
    [:div
     [:div.content-3.proxima.shout.bold.pb2
      "Credit Card Information"]
     (c/build new-card-1 (with :new-card data))
     [:div.pt2
      (c/build field-1 (with :email data))
      (c/build field-1 (with :phone data))
      (c/build checkbox-1 (with :opt-in data))]]
    [:div.mt4
     (actions/medium-primary (with :action data))]]
   [:div.col-12.bg-cool-gray.myj1.pyj2
    (c/build footer-1 data)]])

(defn summary-line
  [{:keys [primary secondary]}]
  (c/html
   [:div.flex.flex-column.justify-between.items-center
    [:div.bold primary]
    [:div secondary]]))

(c/defcomponent receipt-template
  [data _ _]
  [:div.my4
   (c/build header (with :header data))
   [:div.col-12.bg-cool-gray.myj1.pyj2.px2.stretch
    [:div.myj3
     (flipped-title (with :title data))]
    [:div.myj4.px4.center
     (summary-line (with :summary data))]]])

(c/defcomponent goodbye-template 
  [data _ _]
  [:div.my4
   (c/build header (with :header data))
   [:div.col-12.bg-cool-gray.myj1.pyj2.px2.stretch
    [:div.myj3
     (flipped-title (with :title data))]]])

(defn ^:export page
  [state]
  (let [stripe?         (get-in state k/loaded-stripe)
        store           (get-in state k/store)
        stylist-payment (stylist-payment/<- state :current)]
    (cond
      (or
       (nil? (:state stylist-payment))
       (= "requested" (:state stylist-payment))
       (not stripe?))
      (c/build spinner {:title/primary   "Mayvenn Stylist Pay"
                        :title/secondary "Beta"})

      (= "sent" (:state stylist-payment))
      (let [{:stylist-payment/keys [amount payment-id]} stylist-payment]
        (c/build receipt-template
                 {:header/target     [e/navigate-home]
                  :title/primary     "Payment sent"
                  :title/tertiary    (str "Payment ID: " payment-id)
                  :summary/primary   "Congratulations!"
                  :summary/secondary (str
                                      "Your payment was successfully sent to " (:store-name store) ". "
                                      "You will receive your 15% off coupon at the end of the month.")}))

      :else
      (c/build goodbye-template
               {:header/target      [e/navigate-home]
                :title/primary      "Mayvenn Stylist Pay"
                :title/secondary    "Discontinued"
                :title/tertiary     "Mayvenn Stylist Pay was discontinued at the end of 2022."}))))

(defmethod fx/perform-effects
  e/navigate-mayvenn-stylist-pay
  [_ _ _ _ state]
  (let [store-id (get-in state k/store-stylist-id)]
    (publish e/stylist-payment|reset
             {:stylist/id store-id}))
  #?(:cljs (stripe/insert)))
