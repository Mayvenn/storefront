(ns storefront.accessors.bundle-builder
  (:require [storefront.platform.numbers :as numbers]
            [clojure.set :as set]))

(defn ^:private only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn ^:private update-vals [m f & args]
  (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn ^:private filter-variants-by-selections [selections variants yaki-and-waterwave?]
  (->> variants
       (filter (fn [variant]
                 (every? (fn [[step-name option-name]]
                           (= (step-name variant) option-name))
                         selections)))
       (filter (fn [{:keys [style]}]
                 (or yaki-and-waterwave?
                     (and (not= style "Yaki Straight")
                          (not= style "Water Wave")))))))

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

(defn ^:private by-price-and-position
  "Ensure cheapest options are first, breaking ties by preserving the order
  specified by cellar."
  [options]
  (->> options
       (map-indexed vector)
       (sort-by (fn [[idx {:keys [min-price]}]] [min-price idx]))
       (map second)))

(defn ^:private options-for-step [options
                                  {:keys [selected-option
                                          step-name
                                          step-variants]}
                                  yaki-and-waterwave?]
  (for [{:keys [name] :as option} options
        :let                      [option-selection {step-name name}
                                   option-variants  (filter-variants-by-selections option-selection step-variants yaki-and-waterwave?)]
        ;; There are no Silk Blonde closures, so hide Silk when Blonde has been selected.
        :when                     (seq option-variants)]
    (merge option
           {:min-price   (min-price option-variants)
            :checked?    (= selected-option option)
            :sold-out?   (every? :sold-out? option-variants)
            :selection   option-selection})))

(defn steps
  "We are going to build the steps of the bundle builder.

  The options are hardest to generate because they have to take into
  consideration the position in which the step appears in the flow, the list of
  variants in the step, and the selections from prior steps."
  [{:keys [flow selections initial-variants step->options]} yaki-and-waterwave?]
  (for [[step-name prior-steps] (map vector flow (reductions conj [] flow))
        ;; The variants that represent a step are tricky. Even if a user has
        ;; selected Lace, the Deep Wave variants include Lace and Silk, because
        ;; the Style step comes before the Material step. To manage this, this
        ;; code keeps track of which steps precede every other step.
        :let                    [prior-selections     (select-keys selections prior-steps)
                                 step-variants        (filter-variants-by-selections prior-selections initial-variants yaki-and-waterwave?)
                                 options              (step->options step-name)
                                 selected-option      (->> options
                                                           (filter #(= (get selections step-name)
                                                                       (:name %)))
                                                           only)]]
    {:step-name       step-name
     :selected-option selected-option
     :options         (by-price-and-position
                       (options-for-step options
                                         {:selected-option selected-option
                                          :step-name       step-name
                                          :step-variants   step-variants}
                                         yaki-and-waterwave?))}))

(defn ^:private any-variant-has-selection? [step option variants]
  (some (fn [variant]
          (= option (get variant step)))
        variants))

(defn ^:private first-option-for-step [step step-options variants]
  (let [option->min-price (update-vals (group-by step variants) min-price)]
    (->> step-options
         (keep (fn [{:keys [name]}]
                 (when-let [min-price (option->min-price name)]
                   {:name      name
                    :min-price min-price})))
         by-price-and-position
         first
         :name)))

(defn make-selections [{:keys [flow step->options selections initial-variants] :as bundle-builder} new-selections yaki-and-waterwave?]
  (let [proposed-selections (merge selections new-selections)

        [confirmed-selections selected-variants]
        (reduce (fn [[confirmed-selections selected-variants] step]
                  (let [proposed-option          (get proposed-selections step)
                        in-stock-variants        (remove :sold-out? selected-variants)
                        confirmed-option         (if (and proposed-option
                                                          (any-variant-has-selection? step proposed-option in-stock-variants))
                                                   proposed-option
                                                   (first-option-for-step step (step->options step) in-stock-variants))
                        new-confirmed-selections (assoc confirmed-selections step confirmed-option)
                        new-selected-variants    (filter-variants-by-selections new-confirmed-selections selected-variants yaki-and-waterwave?)]
                    [new-confirmed-selections new-selected-variants]))
                ;; Start with no confirmed-selections and all the variants
                [{} initial-variants]
                flow)]
    (assoc bundle-builder
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

(defn initialize [named-search products-by-id yaki-and-waterwave?]
  (let [initial-variants (->> (map products-by-id (:product-ids named-search))
                              (remove nil?)
                              (mapcat build-variants))
        initial-state    {:flow             (ordered-steps named-search)
                          :initial-variants initial-variants
                          :step->options    (ordered-options-by-step named-search)}]
    (make-selections initial-state {} yaki-and-waterwave?)))
