(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
            [storefront.config :as config]
            [spice.date :as date]))

#_ (defn bucketing-example
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
  #_(bucketing-example data)
  data)

(defn str->int [s radix]
  #?(:clj  (java.lang.Integer/parseInt s radix)
     :cljs (js/parseInt s radix)))

(defn char->int [c]
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

(defn v2-experience? [data]
  (contains? #{"aladdin" "phoenix"}
             (get-in data keypaths/store-experience)))

(defn aladdin-experience?
  "Use v2-experience? unless you absolutely need just aladdin"
  [data]
  (= "aladdin" (get-in data keypaths/store-experience)))

(defn v2-homepage? [data]
  (->> (get-in data keypaths/store-features)
       (some #{"aladdin-homepage" "phoenix-homepage"})
       boolean))

;; Promo bar experiments
;; +------------------------------+--------------+----------------+--------+
;; | experiment                   | control page | pdp & category | sticky |
;; |------------------------------+--------------+----------------+--------|
;; | default                      | ✓            | -              | -      |
;; | category-pdp-promo-bar?      | ✓            | ✓              | -      |
;; | sticky-promo-bar?            | ✓            | -              | ✓      |
;; | sticky-promo-bar-everywhere? | ✓            | ✓              | ✓      |
;; +------------------------------+--------------+----------------+--------+

;; Control set of pages that have the promo-bar + category & pdp page
(defn category-pdp-promo-bar? [data]
  (display-feature? data "category-pdp-promo-bar"))

;; Control set of pages that have the promo-bar + sticky bar
(defn sticky-promo-bar? [data]
  (display-feature? data "sticky-promo-bar"))

;; Control set of pages that have the promo-bar + category & pdp page + sticky bar
(defn sticky-promo-bar-everywhere? [data]
  (display-feature? data "sticky-promo-bar-everywhere"))

(defn email-capture-35-percent-got-bundles? [data]
  (display-feature? data "email-capture-35-percent-got-bundles"))

(defn guaranteed-delivery? [data]
  (display-feature? data "guaranteed-delivery"))

(defn dashboard-with-vouchers? [data]
  (->> (get-in data keypaths/store-features)
       (some #{"aladdin-dashboard" "phoenix-dashboard" "dashboard-with-vouchers"})
       boolean))

(defn look-detail-price?
  [data]
  (display-feature? data "look-detail-price"))

(defn install?
  [data]
  (->> (get-in data keypaths/store-features)
       (some #{"install"})
       boolean))

(defn adv-email-capture? [data]
  (display-feature? data "adv-email-capture"))

(defn vouchers?
  [data]
  (or (display-feature? data "vouchers")
      (install? data)))

(defn phone-capture? [data]
  (display-feature? data "phone-capture"))

(defn adventure-shop-individual-bundles?
  [data]
  (display-feature? data
                    "adventure-shop-individual-bundles"))

(defn no-prices-on-picker?
  [data]
  (display-feature? data "no-prices-on-picker"))

(defn browser-pay?
  [data]
  (display-feature? data "browser-pay"))

(defn freeinstall-pdp-looks?
  "Used for UGC of PDP page to enable/disable 'view this look' button for freeinstall"
  [data]
  (display-feature? data "freeinstall-pdp-looks?"))

(defn shop-to-freeinstall?
  [data]
  (display-feature? data "shop-to-freeinstall"))
