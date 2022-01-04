(ns mayvenn.stylist-pay.core
  (:require api.stylist
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]
            #?@(:cljs
                [[storefront.hooks.stripe :as stripe]])
            [storefront.transitions :as t]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [mayvenn.concept.stylist-payment :as stylist-payment]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
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
       (ui/text-field {:errors    (get errors ["cardholder-name"])
                       :data-test "new-card-1-cardholder-name"
                       :keypath   keypath
                       :focused   focused
                       :label     "Cardholder's Name"
                       :name      "name"
                       :required  true
                       :value value})
       (let [card-errors (keep #(get-in errors [% :long-message])
                               [["card-number"]
                                ["card-expiration"]
                                ["security-code"]
                                ["card-error"]])]
         [:div
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
   [:div.h3 primary]
   (ui/text-field
    (cond->
        {:label     placeholder
         :keypath   keypath
         :data-test id}
        (boolean? required) (assoc :required required)
        (string? type_)     (assoc :type type_)
        (int? max_)         (assoc :max max_)
        (int? min_)         (assoc :min min_)))])

(c/defcomponent spinner
  [_ _ _]
  [:div.mt8
   (titles/canela-huge {:primary "Mayvenn Stylist Pay"})
   (ui/large-spinner {:style {:height "6em"}})])

(c/defcomponent template
  [data _ _]
  [:div.my4.mx2
   (titles/canela-huge (with :title data))
   (c/build field-1 (with :amount data))
   (c/build field-1 (with :note data))
   [:div
    [:div.h3 "Your information"]
    (c/build new-card-1 (with :new-card data))]
   [:div.mt4
    (actions/medium-primary (with :action data))]])

(defn summary-line
  [{:keys [primary secondary]}]
  (c/html
   [:div.col-12.flex.justify-between.items-center
    [:div primary]
    [:div secondary]]))

(c/defcomponent receipt-template
  [data _ _]
  [:div.my4.mx2
   (titles/canela-huge (with :title data))
   [:div (summary-line (with :amount data))]
   [:div (summary-line (with :note data))]
   [:div (summary-line (with :card data))]
   [:div
    (actions/medium-primary (with :action data))]])

(defn ^:export page
  [state]
  (let [stripe?         (get-in state k/loaded-stripe)
        store-name      (get-in state (conj k/store :store-name))
        stylist-payment (stylist-payment/<- state :current)]
    (cond
      (or
       (nil? (:state stylist-payment))
       (not stripe?))
      (c/build spinner {})

      (= "sent" (:state stylist-payment))
      (let [{:stylist-payment/keys [amount note]} stylist-payment]
        (c/build receipt-template
                 {:title/primary    store-name
                  :title/secondary  "Payment Test"
                  :amount/primary   "Amount"
                  :amount/secondary (str "$" (/ amount 100))
                  :note/primary     "Note"
                  :note/secondary   note
                  :card/primary     "Card Used"
                  :card/secondary   (:stripe/description stylist-payment)
                  :action/id        "restart"
                  :action/label     "Make another Payment"
                  :action/disabled? false
                  :action/target    [e/stylist-payment|reset]}))

      :else
      (c/build template
               {:title/primary      store-name
                :title/secondary    "Mayvenn Stylist Pay"
                :amount/primary     "Amount"
                :amount/placeholder "e.g. 100"
                :amount/keypath     (conj stylist-payment/k-current
                                          :stylist-payment/amount)
                :amount/type        "number"
                :amount/min         "1"
                :amount/max         "500"
                :note/primary       "Note"
                :note/placeholder   "Describe what you are purchasing"
                :note/keypath       (conj stylist-payment/k-current
                                          :stylist-payment/note)
                :action/id          "pay-action"
                :action/label       "Pay"
                :action/disabled?   false
                :action/target      [e/stylist-payment|prepared]
                :new-card/keypath   (conj stylist-payment/k-current
                                          :cardholder/name)
                :new-card/value     (:cardholder/name stylist-payment)
                :new-card/focused   (get-in state k/ui-focus)
                :new-card/errors    (:field-errors (get-in state k/errors))}))))

(defmethod fx/perform-effects
  e/navigate-mayvenn-stylist-pay
  [_ _ _ _ state]
  (let [store-id (get-in state k/store-stylist-id)]
    (publish e/stylist-payment|reset
             {:stylist/id store-id}))
  #?(:cljs (stripe/insert)))
