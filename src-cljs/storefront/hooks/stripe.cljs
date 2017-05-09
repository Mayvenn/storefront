(ns storefront.hooks.stripe
  (:require [storefront.browser.tags :refer [insert-tag-with-callback
                                             src-tag]]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.request-keys :as request-keys]
            [storefront.events :as events]
            [storefront.config :as config]
            [clojure.string :as string]
            [clojure.set :refer [rename-keys]]))

(defn insert []
  (when-not (.hasOwnProperty js/window "Stripe")
    (insert-tag-with-callback
     (src-tag "https://js.stripe.com/v2/stripe.js" "stripe-v2")
     (fn []
       (handle-message events/inserted-stripe {:version :v2})
       (js/Stripe.setPublishableKey config/stripe-publishable-key)))
    (insert-tag-with-callback
     (src-tag "https://js.stripe.com/v3/" "stripe-v3")
     (fn []
       (handle-message events/inserted-stripe {:version :v3})
       (let [stripe (js/Stripe. config/stripe-publishable-key)]
         (set! (.-stripe js/window) stripe))))))

(def fonts (clj->js
            {:fonts
             [{:family "roboto-300"
               :src    "url('https://d6w7wdcyyr51t.cloudfront.net/cdn/fonts/Roboto-Light-webfont.2dbb9930.woff') format('woff')"
               :style  "normal"
               :weight "300"}]}))

(def styles (clj->js
             {:style
              {:base {:color          "#000000"
                      :fontFamily     "roboto-300"
                      :fontSize       "14px"
                      :lineHeight     "1"
                      "::placeholder" {:color "#ccc"}}}}))

(defn card-element [selector]
  (doto (-> js/stripe
            (.elements fonts)
            (.create "card" styles))
    (.mount selector)))

(defn stripe-response-handler [place-order? api-id create-token-result]
  (let [result (js->clj create-token-result :keywordize-keys true)]
    (handle-message events/api-end api-id)
    (if (:token result)
      (handle-message events/stripe-success-create-token
                      (assoc result :place-order? place-order?))
      (handle-message events/stripe-failure-create-token
                      result))))

(defn create-token [card-element cardholder-name address & [args]]
  (when (.hasOwnProperty js/window "Stripe")
    (let [api-id      {:request-key request-keys/stripe-create-token
                       :request-id  (str (random-uuid))}
          card-holder (clj->js {:name            cardholder-name
                                :address_line1   (:address1 address)
                                :address_line2   (str (:address2 address))
                                :address_city    (:city address)
                                :address_state   (:state address)
                                :address_zip     (:zipcode address)
                                :address_country "us"
                                :currency        "usd"})]
      (handle-message events/api-start api-id)
      (-> js/stripe
          (.createToken card-element card-holder)
          (.then (partial stripe-response-handler (:place-order? args) api-id))))))
