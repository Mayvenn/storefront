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
  ;; FIXME: need real convert-ids for both prod and non-prod; these are all fake
  (let [production? (= "production" environment)
        experiments {"address-login" {:convert-id (if production? "" "100011894")
                                      :variations [{:name       "original"
                                                    :convert-id (if production? "" "100073286")}
                                                   {:name       "variation"
                                                    :convert-id (if production? "" "100073287")
                                                    :feature    "address-login"}]}}]
    (get experiments experiment-name)))

(defn display-feature? [data feature]
  ((set (get-in data keypaths/features)) feature))

(defn enable-feature [data feature]
  (update-in data keypaths/features conj feature))

(defn kinky-straight? [data]
  (display-feature? data "kinky-straight-2"))

(defn swap-curly-loose-wave? [data]
  (display-feature? data "kinky-straight-2-curly-move"))

(defn view-look? [data]
  (display-feature? data "view-look"))

(defn address-login? [data]
  (display-feature? data "address-login"))

(defn ensure-bucketed-for [data environment experiment-name]
  ;; NOTE: called in a transition, so MUST return data, and SHOULD be pure
  ;; (except for the randomness of choosing a bucket, which should be
  ;; idempotent)
  ;; TODO: Is it sufficient to store this in app-state? Or should it also be
  ;; cookied? Or could we have convert tell us whether they've been bucketed?
  (if (contains? (get-in data keypaths/experiments-manually-bucketed) experiment-name)
    data
    (let [{:keys [variations] :as experiment}  (manual-experiments environment experiment-name)
          {:keys [feature] :as rand-variation} (rand-nth variations)]
      (cond-> data
        true    (update-in keypaths/experiments-manually-bucketed conj experiment-name)
        true    (update-in keypaths/experiments-buckets-to-notify conj [experiment rand-variation])
        ;; The feature is enabled even before convert is notified, so that a
        ;; decision can be made and acted on in the same event.
        feature (enable-feature feature)))))
