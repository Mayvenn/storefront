(ns storefront.accessors.experiments
  (:require [storefront.keypaths :as keypaths]))

(defn determine-features [data]
  data
  ;; NOTE: Ryan wants kinky-straight disabled under further review
  #_(let [stylist-id    (get-in data keypaths/store-stylist-id)
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

(defn manual-experiments [environment experiment-name]
  ;; TODO: For Deploy of address-login: ensure we have convert-ids for production
  (let [production? (= "production" environment)
        experiments {"address-login" {:convert-id (if production? "" "100011894")
                                      :variations [{:name       "original"
                                                    :convert-id (if production? "" "100073286")}
                                                   {:name       "variation"
                                                    :convert-id (if production? "" "100073287")
                                                    :feature    "address-login"}]}}]
    (get experiments experiment-name)))

(defn str->int [s radix]
  #?(:clj  (java.lang.Integer/parseInt s radix)
     :cljs (js/parseInt s radix)))

(defn experiment-and-variation-for [data environment experiment-name]
  (let [{:keys [variations] :as experiment} (manual-experiments environment experiment-name)

        ;; We want to randomly bucket a user into a variation. We can use
        ;; session-id for this. This algorithm turns session-id into session-n
        ;; an integer that: ranges from 0 to 1295, is randomly distributed
        ;; across all the users, and is deterministic for any given user.
        ;; (Because it is cookied, it sticks with the customer for a long time
        ;; - one year.) Caveat: since it is associated with a browser, it will
        ;; have different values on different devices for the same person.
        ;; This is somewhat unavoidable since it has to work for logged-out
        ;; users too.
        session-n  (-> (get-in data keypaths/session-id) (subs 0 2) (str->int 36))
        exp-offset (reduce + (map int experiment-name))

        variation (if (= "production" environment)
                    ;; TODO: For Deploy of address-login: remove this branch
                    ;; Always Off
                    (first variations)
                    (nth variations (mod (+ exp-offset session-n)
                                         (count variations))) ;; Random
                    ;; HEAT helpers, can be removed after address-login is feature complete
                    ;; Always Off
                    #_(first variations)
                    ;; Always On
                    #_(last variations))]
    [experiment variation]))

(defn feature-for [data environment experiment-name]
  (let [[_ {:keys [feature]}] (experiment-and-variation-for data environment experiment-name)]
    feature))

(defn display-feature? [data feature]
  (contains? (set (get-in data keypaths/features)) feature))

(defn enable-feature [data feature]
  (update-in data keypaths/features conj feature))

(defn kinky-straight? [data]
  (display-feature? data "kinky-straight-2"))

(defn swap-curly-loose-wave? [data]
  (display-feature? data "kinky-straight-2-curly-move"))

(defn view-look? [data]
  (display-feature? data "view-look"))

(defn address-login? [data environment]
  (or (display-feature? data "address-login")
      (= "address-login" (feature-for data environment "address-login"))))
