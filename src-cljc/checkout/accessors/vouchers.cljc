(ns checkout.accessors.vouchers)

(defn ^:private family->ordering [family]
  (get {"bundles"      4
        "closures"     3
        "frontals"     2
        "360-frontals" 1} family 10))

(def ^:private product-family->service-type
  {"bundles"      :leave-out
   "closures"     :closure
   "frontals"     :frontal
   "360-frontals" :three-sixty})

(defn product-items->highest-value-service [product-items]
  (->> product-items
       (map (comp :hair/family :variant-attrs))
       (sort-by family->ordering)
       first
       product-family->service-type))

(defn campaign-configuration
  [environment]
  ;; WARN: sync this with vaqum, diva, voucherify, and el-jefe
  (case environment
    "production" [{:service/type             :leave-out
                   :service/diva-type        :install-sew-in-leave-out
                   :voucherify/campaign-name "Free Install - Leave Out"}
                  {:service/type             :closure
                   :service/diva-type        :install-sew-in-closure
                   :voucherify/campaign-name "Free Install - Closure"}
                  {:service/type             :frontal
                   :service/diva-type        :install-sew-in-frontal
                   :voucherify/campaign-name "Free Install - Frontal"}
                  {:service/type             :three-sixty
                   :service/diva-type        :install-sew-in-360-frontal
                   :voucherify/campaign-name "Free Install - 360"}]
    [{:service/type             :leave-out
      :service/diva-type        :install-sew-in-leave-out
      :voucherify/campaign-name "Free Install - Leave Out"}
     {:service/type             :closure
      :service/diva-type        :install-sew-in-closure
      :voucherify/campaign-name "Free Install - Closure"}
     {:service/type             :frontal
      :service/diva-type        :install-sew-in-frontal
      :voucherify/campaign-name "Free Install - Frontal"}
     {:service/type             :three-sixty
      :service/diva-type        :install-sew-in-360-frontal
      :voucherify/campaign-name "Free Install - 360"}]))

