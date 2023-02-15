(ns catalog.selector.sku
  (:require [clojure.spec.alpha :as s]
            clojure.set
            [storefront.accessors.images :as images]
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

(defn find-swatch-sku-image [images-catalog sku]
  (first (selector/match-all {:selector/strict? true}
                             {:use-case #{"cart"}}
                             (images/for-skuer images-catalog sku))))


(defn product-options
  [facets {:as product :keys [selector/electives]} product-skus images-catalog]
  (not-empty
   (reduce (fn [acc-options [facet-slug {:as facet :keys [facet/options]}]]
             (->> product-skus
                  (group-by (comp set facet-slug))
                  (map (fn [[option-slug option-skus]]
                         (let [cheapest-sku     (apply min-key :sku/price option-skus)
                               no-price-family? (not (empty? (clojure.set/intersection #{"closures" "360-frontals" "frontals"} (:hair/family product))))
                               option           (merge (dissoc (get options (first option-slug)) :sku/name)
                                                       {:price             (if no-price-family?
                                                                             nil
                                                                             (:sku/price cheapest-sku))
                                                        :stocked?          (when (seq option-skus)
                                                                             (some :inventory/in-stock? option-skus))
                                                        :option/sku-swatch (:url (find-swatch-sku-image images-catalog cheapest-sku))
                                                        :image             (get-in options [option-slug :option/image])})]
                           (conform! ::selector-option option))))
                  (sort-by :filter/order)
                  (assoc acc-options facet-slug)))
           {}
           (select-keys facets (set (cond-> electives
                ;; If the product has a length, return a length. Same for color.
                ;; This is to show colors and lengths for products with essential colors/lengths
                                      (seq (:hair/length product)) (conj :hair/length)
                                      (seq (:hair/color product)) (conj :hair/color)))))))
