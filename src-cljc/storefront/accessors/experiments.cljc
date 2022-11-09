(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
            [spice.date :as date]
            [storefront.accessors.sites :as sites]))

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
  (get-in data (conj keypaths/features (keyword feature))))

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

(defn edit-gallery?
  [data]
  (or
   (display-feature? data "past-appointments")
   (display-feature? data "edit-gallery")))

(defn past-appointments?
  [data]
  (display-feature? data "past-appointments"))

(defn easy-booking?
  [data]
  (display-feature? data "easy-booking"))

(defn top-stylist-v2?
  [data]
  (display-feature? data "top-stylist-v2"))

(defn instagram-stylist-profile?
  [data]
  (display-feature? data "instagram-stylist-profile"))

(defn inventory-count-shipping-halt?
  [data]
  (display-feature? data "inventory-count-shipping-halt"))

(defn looks-tags?
  ; NOTE: can be done without the experiment: if no tags are tagged on the contentful looks, the filters will not show up
  [data]
  (display-feature? data "looks-tags"))

(defn accordion-v2?
  [data]
  (display-feature? data "accordion-v2"))

(defn bf-2022-sale?
  [data]
  (display-feature? data "bf-2022-sale"))

(defn pdp-accordion-picker?
  [data]
  (display-feature? data "pdp-accordion-picker"))

(defn carousel-redesign?
  [data]
  (display-feature? data "carousel-redesign"))

(defn bundle-sets-in-sub-menu?
  [data]
  (display-feature? data "bundle-sets-in-sub-menu"))

(defn pdp-faq-in-accordion?
  [data]
  (display-feature? data "pdp-faq-in-accordion"))

(defn footer-v22?
  [data]
  (display-feature? data "footer-v22"))

(defn pdp-content-slots?
  [data]
  (display-feature? data "pdp-content-slots"))
