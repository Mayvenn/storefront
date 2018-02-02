(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]
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

(defn display-feature? [data feature]
  (contains? (set (get-in data keypaths/features)) feature))

(defn stylist-transfers? [data]
  (display-feature? data "stylist-transfers"))
