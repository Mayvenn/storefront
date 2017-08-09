(ns storefront.accessors.old-bundle-builder
  (:require [storefront.platform.numbers :as numbers]
            [clojure.set :as set]))

;; TERMINOLOGY
;; step: :color
;; option: "Natural #1B"
;; selection: {:color "Natural #1B"}

;; Be careful though. Sometimes
;; option: {:name      "Natural #1B"
;;          :long-name "Natural Black (#1B)"
;;          :image     "//:..."}

;; flow: [:color :origin :length]

(defn ^:private only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn ^:private update-vals [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn selected-variant [bundle-builder]
  (only (:selected-variants bundle-builder)))

(defn expanded-selections
  "Calculate the selections where all available variants share a
  common value. This lets us copy {:style \"straight\"} to the closures page,
  even though it was not a selection on the straight page"
  [{:keys [selected-variants]}]
  (->> selected-variants
       (map :variant_attrs)
       (map #(update-vals % (comp set vector)))
       (reduce (partial merge-with set/union))
       (map (juxt first (comp only second)))
       (filter second)
       (into {})))

(defn last-step [{:keys [flow selections]}]
  (let [finished-step? (set (keys selections))]
    (last (take-while finished-step? flow))))

(defn next-step [{:keys [flow selections]}]
  (let [finished-step? (set (keys selections))]
    (first (drop-while finished-step? flow))))

(defn min-price [variants]
  (when (seq variants)
    (->> variants
         (map :price)
         (apply min))))

(defn options-by-price-and-position
  "Ensure cheapest options are first, breaking ties by preserving the order
  specified by cellar."
  [options]
  (->> options
       (map-indexed vector)
       (sort-by (fn [[idx {:keys [price-delta]}]] [price-delta idx]))
       (map second)))

(defn ^:private option-with-selection [selected-option option]
  (assoc option :checked? (= selected-option option)))

(defn ^:private option-by-name [options option-name]
  (some #(when (= option-name (:name %)) %) options))

(defn ^:private options-for-step [step step-options step-variants prior-selections]
  (let [step-min-price   (min-price step-variants)
        option->variants (group-by step step-variants)]
    (->> step-options
         (keep (fn [{:keys [name] :as option}]
                 ;; The :material step has "silk" and "lace". But, the variants
                 ;; may not include any "silk" variants (e.g., when {:color
                 ;; "blonde"} has been chosen). In that case, we don't show the
                 ;; "silk" option.
                 (when-let [variants (seq (option->variants name))]
                   (assoc option
                          :price-delta (- (min-price variants) step-min-price)
                          :sold-out? (every? :sold-out? variants)
                          :selections (assoc prior-selections step name)
                          :variants variants))))
         options-by-price-and-position)))

(defn reset-selections [{:keys [flow initial-variants step->options] :as bundle-builder} proposed-selections]
  (let [[confirmed-selections selected-variants steps]
        (reduce (fn [[confirmed-selections selected-variants steps] step]
                  (let [options          (options-for-step step (step->options step) selected-variants confirmed-selections)
                        in-stock-options (remove :sold-out? options)
                        confirmed-option (or (option-by-name in-stock-options (get proposed-selections step))
                                             (only in-stock-options))
                        steps            (conj steps {:step-name       step
                                                      :selected-option confirmed-option
                                                      :later-step?     (> (count steps) (count confirmed-selections))
                                                      :options         (map (partial option-with-selection confirmed-option) options)})]
                    (if confirmed-option
                      [(:selections confirmed-option)
                       (:variants confirmed-option)
                       steps]
                      [confirmed-selections
                       selected-variants
                       steps])))
                [{} initial-variants []]
                flow)]
    (assoc bundle-builder
           :steps steps
           :selections confirmed-selections
           :selected-variants selected-variants)))

(defn rollback [{:keys [selections] :as bundle-builder}]
  (if-let [last-step (last-step bundle-builder)]
    (reset-selections bundle-builder (dissoc selections last-step))
    bundle-builder))

(defn ^:private ordered-steps [{:keys [result-facets]}]
  (map (comp keyword :step) result-facets))

(defn ^:private ordered-options-by-step [{:keys [result-facets]}]
  (->> result-facets
       (map (juxt (comp keyword :step) :options))
       (into {})))

(defn ^:private build-variants [product]
  (map (fn [variant]
         (-> variant
             (merge (:variant_attrs variant))
             (assoc :price (numbers/parse-float (:price variant))
                    :sold-out? (not (:can_supply? variant)))))
       (:variants product)))

(defn initialize [named-search products-by-id]
  (let [initial-variants (->> (map products-by-id (:product-ids named-search))
                              (remove nil?)
                              (mapcat build-variants))
        initial-state    {:flow             (ordered-steps named-search)
                          :initial-variants initial-variants
                          :step->options    (ordered-options-by-step named-search)}]
    (reset-selections initial-state {})))
