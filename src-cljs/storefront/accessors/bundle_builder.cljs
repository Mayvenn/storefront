(ns storefront.accessors.bundle-builder
  (:require [storefront.accessors.products :as products]))

(defn last-step [flow selections]
  (let [finished-step? (set (keys selections))]
    (last (take-while finished-step? flow))))

(defn next-step [flow selections]
  (let [finished-step? (set (keys selections))]
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
                     option-variants  (products/filter-variants-by-selections option-selection step-variants)]
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
  [flow step->option-names all-selections variants]
  (for [[step-name prior-steps] (map vector flow (reductions conj [] flow))
        ;; The variants that represent a step are tricky. Even if a user has
        ;; selected Lace, the Deep Wave variants include Lace and Silk, because
        ;; the Style step comes before the Material step. To manage this, this
        ;; code keeps track of which steps precede every other step.
        :let                    [prior-selections (select-keys all-selections prior-steps)
                                 step-variants    (products/filter-variants-by-selections prior-selections variants)]]
    {:step-name     step-name
     :later-step?   (> (count prior-steps) (count all-selections))
     :options       (options-for-step (step->option-names step-name)
                                      {:prior-selections     prior-selections
                                       :selected-option-name (get all-selections step-name nil)
                                       :step-name            step-name
                                       :step-variants        step-variants
                                       :step-min-price       (min-price step-variants)})}))

(defn selection-flow [{:keys [name]}]
  (case name
    "frontals" '(:style :material :origin :length)
    "closures" '(:style :material :origin :length)
    "blonde" '(:color :origin :length)
    '(:origin :length)))

(def included-product? (complement :stylist_only?))
(def included-taxon? (complement :stylist_only?))
