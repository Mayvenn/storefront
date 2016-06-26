(ns storefront.accessors.bundle-builder
  (:require [storefront.platform.numbers :as numbers]))

(defn only [coll]
  (when (= 1 (count coll))
    (first coll)))

(defn ^:private update-keys [m f & args]
  (reduce (fn [r [k v]] (assoc r (apply f k args) v)) {} m))

(defn selected-variant [bundle-builder]
  (only (:selected-variants bundle-builder)))

(defn ^:private selected-products [{:keys [selected-variants]} products]
  (->> selected-variants (map :product_id) set (map products)))

(defn selected-product [bundle-builder products]
  (only (selected-products bundle-builder products)))

(defn ^:private filter-variants-by-selections [selections variants]
  (filter (fn [variant]
            (every? (fn [[step-name option-name]]
                      (= (step-name variant) option-name))
                    selections))
          variants))

(defn last-step [{:keys [flow selected-options]}]
  (let [finished-step? (set (keys selected-options))]
    (last (take-while finished-step? flow))))

(defn next-step [{:keys [flow selected-options]}]
  (let [finished-step? (set (keys selected-options))]
    (first (drop-while finished-step? flow))))

(defn min-price [variants]
  (when (seq variants)
    (->> variants
         (map :price)
         (apply min))))

(defn options-for-step [option-names
                        {:keys [prior-selections
                                selected-option-name
                                step-name
                                step-variants
                                step-min-price]}]
  (for [option-name option-names
        :let        [option-selection {step-name option-name}
                     option-variants  (filter-variants-by-selections option-selection step-variants)]
        ;; There are no Silk Blonde closures, so hide Silk when Blonde has been
        ;; selected, even though Silk Straight closures exist.
        :when       (seq option-variants)]
    {:option-name option-name
     :price-delta (- (min-price option-variants) step-min-price)
     :checked?    (= selected-option-name option-name)
     :sold-out?   (every? :sold-out? option-variants)
     :selections  (merge prior-selections option-selection)}))

(defn steps
  "We are going to build the steps of the bundle builder. A 'step' is a name and
  vector of options, e.g., Material: Lace or Silk. An 'option' is a single one
  of these, Silk. The pair Material: Silk is a 'selection'.

  The options are hardest to generate because they have to take into
  consideration the position in which the step appears in the flow, the list of
  variants in the step, and the seletions the user has chosen so far."
  [{:keys [flow selected-options initial-variants step->option-names]}]
  (for [[step-name prior-steps] (map vector flow (reductions conj [] flow))
        ;; The variants that represent a step are tricky. Even if a user has
        ;; selected Lace, the Deep Wave variants include Lace and Silk, because
        ;; the Style step comes before the Material step. To manage this, this
        ;; code keeps track of which steps precede every other step.
        :let                    [prior-selections (select-keys selected-options prior-steps)
                                 step-variants    (filter-variants-by-selections prior-selections initial-variants)]]
    {:step-name     step-name
     :later-step?   (> (count prior-steps) (count selected-options))
     :options       (options-for-step (step->option-names step-name)
                                      {:prior-selections     prior-selections
                                       :selected-option-name (get selected-options step-name nil)
                                       :step-name            step-name
                                       :step-variants        step-variants
                                       :step-min-price       (min-price step-variants)})}))

(defn ^:private ordered-steps [{:keys [result-facets]}]
  (map (comp keyword first) result-facets))

(defn ^:private ordered-options-by-step [{:keys [result-facets]}]
  (update-keys result-facets keyword))

(defn ^:private build-variants [product]
  (map (fn [variant]
         (-> variant
             (merge (:variant_attrs variant))
             (assoc :price (numbers/parse-float (:price variant))
                    :sold-out? (not (:can_supply? variant)))))
       (:variants product)))

(def included-product? (complement :stylist_only?))
(def included-taxon? (complement :stylist_only?))

(declare select-option)
(defn ^:private auto-advance [{:keys [flow selected-options selected-variants auto-advance?] :as bundle-builder}]
  (or (when auto-advance?
        (when-let [next-step (next-step bundle-builder)]
          (when-let [next-option (->> selected-variants
                                      (remove :sold-out?)
                                      (map next-step)
                                      set
                                      only)]
            (select-option bundle-builder next-step next-option))))
      bundle-builder))

(defn reset-options [{:keys [initial-variants] :as bundle-builder} selected-options]
  (let [selected-variants (filter-variants-by-selections selected-options initial-variants)
        revised-state (assoc bundle-builder
                             :selected-options selected-options
                             :selected-variants selected-variants)]
    (auto-advance revised-state)))

(defn select-option [{:keys [selected-options] :as bundle-builder} option value]
  (reset-options bundle-builder (assoc selected-options option value)))

(defn rollback [{:keys [selected-options] :as bundle-builder}]
  (if-let [last-step (last-step bundle-builder)]
    (reset-options bundle-builder (dissoc selected-options last-step))
    bundle-builder))

(defn initialize [taxon products color-option?]
  (let [initial-variants (->> (map products (:product-ids taxon))
                              (remove nil?)
                              (mapcat build-variants))
        initial-state    {:flow               (ordered-steps taxon)
                          :auto-advance?      color-option?
                          :initial-variants   initial-variants
                          :step->option-names (ordered-options-by-step taxon)}]
    (reset-options initial-state {})))
