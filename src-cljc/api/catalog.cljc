(ns api.catalog
  (:require
   [spice.selector :refer [match-all]]))

(def select
  (comp seq
        (partial match-all
                 {:selector/strict? true})))

(defn select-sorted
  [query sort-fn coll]
  (->> coll
       (select query)
       (sort-by sort-fn)
       seq))

;; ------------ Identities

;; ------ Department: Service

(def ?service
  {:catalog/department #{"service"}})

(def ?base
  (merge ?service
         {:service/type #{"base"}}))

(def ?addons
  (merge ?service
         {:service/type #{"addon"}}))

(def ?discountable
  (merge ?base
         {:promo.mayvenn-install/discountable #{true}}))

(def ?discountable-install
  (merge ?discountable
         {:service/category #{"install"}}))

(def ?a-la-carte
  (merge ?base
         {:promo.mayvenn-install/discountable #{false}}))

;; ------ Department: Hair

(def ?hair
  {:catalog/department #{"hair"}})

;; NOTE: this differs from wig-rule for free install
(def ?wig
  (merge ?hair
         {:hair/family #{"ready-wigs" "360-wigs" "lace-front-wigs" "closure-wigs" "headband-wigs"}}))

(def ?physical
  {:catalog/department #{"hair" "stylist-exclusives"}})

;; ------ Images

(def ?model-image
  {:image/of #{"model"}})

(def ?cart-product-image
  {:use-case #{"cart"}
   :image/of #{"product"}})

;; ------- Items

(def ?recent
  {:item/recent? #{true}})

;; ------- Migrations

(def ?new-world-service
  {:service/world #{"SV2"}})
