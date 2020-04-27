(ns checkout.ui.begin-checkout
  (:require
   [storefront.component :as c]
   [storefront.components.ui :as ui]
   [storefront.platform.component-utils :as utils]))

(c/defcomponent begin-checkout-button-organism
  [{:begin-checkout.button/keys [id style disabled label target]}
   _
   _]
  [:div
   ((if (= :p-color style)
      ui/button-large-primary
      ui/button-large-paypal)
    {:spinning? false
     :disabled? disabled
     :on-click  (utils/send-event-callback target)
     :data-test id}
    label)])

(defn ^:private elements
  "Embed a list of organisms in another organism."
  ([organism data elem-key]
   (elements organism data elem-key :default))
  ([organism data elem-key breakpoint]
   (let [elems (->> (keyword (name elem-key)
                             "elements")
                    (get data))]
     (for [[idx elem] (map-indexed vector elems)]
       (c/build organism
                elem
                (c/component-id elem-key
                                breakpoint
                                idx))))))

(def ^:private horizontal-or-atom
  [:div.h5.black.py1.flex.items-center
   [:div.flex-grow-1.border-bottom.border-gray]
   [:div.mx2 "or"]
   [:div.flex-grow-1.border-bottom.border-gray]])

;; TODO BROWSER PAY
(c/defcomponent organism
  [data _ _]
  [:div
   (interpose
    horizontal-or-atom
    (elements begin-checkout-button-organism
              data
              :begin-checkout.button))])
