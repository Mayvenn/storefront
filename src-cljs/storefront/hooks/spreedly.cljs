(ns storefront.hooks.spreedly
  (:require [clojure.string :as string]
            [storefront.browser.tags :refer [insert-tag-with-src]]
            [storefront.config :as config]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message]]))

(defn insert []
  (when (not (.hasOwnProperty js/window "Spreedly"))
    (insert-tag-with-src "https://core.spreedly.com/iframe/iframe-v1.min.js" "spreedly")))

(defn create-frame [{:keys [number-id cvv-id]} ready-event]
  (when (.hasOwnProperty js/window "SpreedlyPaymentFrame")
    (let [frame (js/SpreedlyPaymentFrame.)]
      (.on frame "ready"
           (fn []
             (let [css-style "font-size: 18px; font-family: 'Proxima Nova', Arial,, sans-serif; font-weight: 300;"]
               (.setPlaceholder frame "number" "Card Number")
               (.setPlaceholder frame "cvv" "CVV")
               (.setStyle frame "number" css-style)
               (.setStyle frame "cvv" css-style))
             (handle-message ready-event {:frame frame})))
      (.on frame "paymentMethod" (fn [token payment]
                                   (handle-message events/spreedly-frame-tokenized {:token   token
                                                                                    :payment (js->clj payment :keywordize-keys true)})))
      (.on frame "errors" (fn [errors]
                            (handle-message events/flash-show-failure {:message (string/join ", " (map :message (js->clj errors :keywordize-keys true)))})))
      (.init frame config/spreedly-key (clj->js {:numberEl (str number-id)
                                                 :cvvEl    (str cvv-id)})))))

(defn tokenize [spreedly-frame {:keys [first-name last-name exp-month exp-year zip]}]
  (when spreedly-frame
    (.tokenizeCreditCard spreedly-frame (clj->js {:first_name first-name
                                                  :last_name  last-name
                                                  :month      exp-month
                                                  :year       exp-year
                                                  :zip        zip}))))
