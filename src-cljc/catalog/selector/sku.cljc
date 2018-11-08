(ns catalog.selector.sku
  (:require [clojure.spec.alpha :as s]
            [spice.selector :as selector]
            [lambdaisland.uri :as uri]))

;; Options for selection of skus for a particular product

(s/def ::keyword
  (s/conformer
   (fn [value]
     (cond (string? value)  (keyword value)
           (keyword? value) value
           :otherwise       ::s/invalid))))

(s/def ::ucare-url
  (s/conformer
   (s/nilable
    (fn [value]
      (or
       (and value
            (= "ucarecdn.com"
               (:host (uri/parse value))))
       ::s/invalid)))))

(s/def :option/name string?)
(s/def :option/slug string?)
(s/def :option/order int?)
(s/def :option/sku-swatch ::ucare-url)
(s/def :option/rectangular-swatch ::ucare-url)

(s/def ::stocked? boolean?)
(s/def ::image ::ucare-url)
(s/def ::price number?)
(s/def ::price-delta double?)

(s/def ::selector-option
  (s/keys
   :req [:option/slug
         :option/name]
   :opt [:option/rectangular-swatch
         :option/sku-swatch
         :option/order]
   :req-un [::image ::price]))

(defn ^:private conform!
  [spec value]
  (let [result (s/conform spec value)]
    (if (= ::s/invalid result)
      (do
        #?(:cljs (js/console.log (s/explain-str spec value)))
        (throw (ex-info "Failing spec!" {:explaination (s/explain-str spec value)
                                         :value value
                                         :spec spec})))
      result)))

(defn find-swatch-sku-image [sku]
  (first (selector/match-all {:selector/strict? true}
                             {:use-case #{"cart"}}
                             (:selector/images sku))))

(defn product-options
  [facets {:as product :keys [selector/electives]} product-skus]
  (not-empty
   (reduce (fn [acc-options [facet-slug {:as facet :keys [facet/options]}]]
             (->> product-skus
                  (group-by facet-slug)
                  (map (fn [[option-slug option-skus]]
                         (let [cheapest-sku (apply min-key :sku/price option-skus)
                               option       (merge (dissoc (get options (first option-slug)) :sku/name)
                                                   {:price    (:sku/price cheapest-sku)
                                                    :stocked? (when (seq option-skus)
                                                                (some :inventory/in-stock? option-skus))
                                                    :option/sku-swatch (:url (find-swatch-sku-image cheapest-sku))
                                                    :image    (get-in options [option-slug :option/image])})]
                           (conform! ::selector-option option))))
                  (sort-by :filter/order)
                  (assoc acc-options facet-slug)))
           {}
           (select-keys facets electives))))


;;     ;; Options generation
;;
;;     
;;     (defn selected-options [options selections] options')



;;     ;; Navigation on PDP w/ sku-id param
;;
;;     (defn find-sku [product sku-id] sku)
;;     (defn sku->selections [product sku] selections)


;;     ;; Navigation on PDP w/o sku-id param
;;
;;     (defn filter-valid-selections [selections product] selections')
;;     (defn cheapest-sku [product] cheapest-sku) ; 'epitome'
;;     (defn selections->sku [product selections] sku)
;;
;;     ;; comp'ing cheapest-sku and sku->selections gives you default options
;;     (defn default-selections [product] selections) ; derivable from above primitives
