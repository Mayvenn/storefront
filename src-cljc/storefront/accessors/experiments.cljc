(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]))

#_(defn bucketing-example
    [data]
    (let [stylist-id    (get-in data keypaths/store-stylist-id)
          store-slug    (get-in data keypaths/store-slug)
          bucket-offset (mod stylist-id 3)]
      (assoc-in data
                keypaths/features
                (cond
                  (= store-slug "shop")  #{"kinky-straight-2-control"}
                  (= store-slug "store") #{"kinky-straight-2-control"}
                  (= bucket-offset 0)    #{"kinky-straight-2"}
                  (= bucket-offset 1)    #{"kinky-straight-2-curly-move"}
                  (= bucket-offset 2)    #{"kinky-straight-2-control"}))))

(defn determine-features [data]
  data)

(defn ^:private str->int [s radix]
  #?(:clj  (java.lang.Integer/parseInt s radix)
     :cljs (js/parseInt s radix)))

(defn ^:private char->int [c]
  #?(:clj  (int c)
     :cljs (.charCodeAt c 0)))

(defn variation-for [data experiment]
  (let [{:keys [enabled? variations]} (get-in data (conj keypaths/experiments-manual experiment))]
    (when (and enabled? (seq variations))
      (let [;; We want to randomly bucket a user into a variation. We can use
            ;; session-id for this. This algorithm turns session-id into session-n
            ;; an integer that: ranges from 0 to 1295, is randomly distributed
            ;; across all the users, and is deterministic for any given user.
            ;; (Because it is cookied, it sticks with the customer for a long time
            ;; - one year.) Caveat: since it is associated with a browser, it will
            ;; have different values on different devices for the same person.
            ;; This is somewhat unavoidable since it has to work for logged-out
            ;; users too.
            session-n  (-> (get-in data keypaths/session-id) (subs 0 2) (str->int 36))
            ;; We also want to avoid always assigning the same set of people to the
            ;; same side of an experiment.
            exp-offset (reduce + (map char->int experiment))

            variation (nth variations (mod (+ exp-offset session-n)
                                           (count variations)))]
        variation))))

(defn feature-for [data experiment]
  (:feature (variation-for data experiment)))

(defn ^:private display-feature? [data feature]
  (contains? (set (get-in data keypaths/features)) feature))

(defn dashboard-with-vouchers? [data]
  (= "aladdin" (get-in data keypaths/user-stylist-experience)))

(defn on-dev?
  "Useful for developing against a feature flag such that it won't break acceptance if it gets accidentally committed."
  [data]
  (= (get-in data keypaths/environment) "development"))

(defn browser-pay?
  [data]
  (display-feature? data "browser-pay"))

(defn promo-link?
  [data]
  (display-feature? data "promo-link"))

(defn color-picker-redesign?
  [data]
  (display-feature? data "color-picker-redesign"))

(defn hide-bookings?
  [data]
  (display-feature? data "hide-bookings"))

(defn hide-star-distribution?
  [data]
  (display-feature? data "hide-star-distribution"))

(defn stylist-blocked?
  [data]
  (display-feature? data "stylist-blocked"))

(defn stylist-results-test?
  [data]
  (display-feature? data "stylist-results-test"))

(defn just-added-control?
  [data]
  (display-feature? data "just-added-control"))

(defn just-added-experience?
  [data]
  (display-feature? data "just-added-experience"))

(defn just-added-only?
  [data]
  (display-feature? data "just-added-only"))

(defn stylist-mismatch?
  [data]
  (display-feature? data "stylist-mismatch"))

(defn add-on-services?
  [data]
  (or (display-feature? data "upsell-addons-opened")
      (display-feature? data "upsell-addons-closed")))

(defn reinstall-services?
  [data]
  (display-feature? data "reinstall-services"))

(defn upsell-addons-opened?
  [data]
  (display-feature? data "upsell-addons-opened"))

(defn remove-closures?
  [data]
  (display-feature? data "remove-closures"))

(defn homepage-rebrand?
  [data]
  (display-feature? data "homepage-rebrand"))

(defn shipping-delay?
  [data]
  (display-feature? data "shipping-delay"))

(defn hide-quadpay?
  [data]
  (boolean
   (and (display-feature? data "hide-quadpay")
        (not (get-in data keypaths/order-cart-payments-quadpay)))))
