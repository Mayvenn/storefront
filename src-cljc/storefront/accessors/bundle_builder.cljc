(ns storefront.accessors.bundle-builder
  (:require [storefront.platform.numbers :as numbers]
            [clojure.set :as set]))

(defn ^:private only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn ^:private update-vals [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn ^:private min-price [variants]
  (when (seq variants)
    (->> variants
         (map :price)
         (apply min))))

;; TERMINOLOGY
;; step: :color
;; option: "Natural #1B"
;; selection: {:color "Natural #1B"}

;; Be careful though. Sometimes
;; option: {:name      "Natural #1B"
;;          :long-name "Natural Black (#1B)"
;;          :image     "//:..."}

;; flow: [:color :origin :length]

(defn ^:private option-with-selection [selected-option option]
  (assoc option :checked? (= selected-option option)))

(defn ^:private option-by-name [options option-name]
  (some #(when (= option-name (:name %)) %) options))

(defn ^:private options-for-step [step step-options step-variants]
  (let [option->variants (group-by step step-variants)]
    (->> step-options
         (keep (fn [{:keys [name] :as option}]
                 ;; The :material step has "silk" and "lace". But, the variants
                 ;; may not include any "silk" variants (e.g., when {:color
                 ;; "blonde"} has been chosen). In that case, we don't show the
                 ;; "silk" option.
                 (when-let [variants (seq (option->variants name))]
                   (assoc option
                          :min-price (min-price variants)
                          :sold-out? (every? :sold-out? variants)
                          :selection {step name}
                          :variants variants)))))))

(defn make-selections [{:keys [flow step->options selections initial-variants] :as bundle-builder} new-selections]
  (let [proposed-selections (merge selections new-selections)

        ;; Walk through every step, confirming that the selection for that step
        ;; makes sense, taking into account prior steps.
        [confirmed-selections selected-variants steps]
        (reduce (fn [[confirmed-selections selected-variants steps] step]
                  (let [options                  (options-for-step step (step->options step) selected-variants)
                        in-stock-options         (remove :sold-out? options)
                        confirmed-option         (or (option-by-name in-stock-options (get proposed-selections step))
                                                     (first in-stock-options))]
                    [(merge confirmed-selections (:selection confirmed-option))
                     (:variants confirmed-option)
                     (conj steps {:step-name       step
                                  :selected-option confirmed-option
                                  :options         (map (partial option-with-selection confirmed-option) options)})]))
                [{} initial-variants []]
                flow)]
    (assoc bundle-builder
           :steps steps
           :selections confirmed-selections
           :selected-variant (only selected-variants))))

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
    (make-selections initial-state {})))
