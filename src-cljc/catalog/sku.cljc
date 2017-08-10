(ns catalog.sku
  (:require [datascript.core :as d]
            [alandipert.intension :as i]))
(comment
  (def skus [{:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "14"
               :hair/base-material "lace"
               :hair/family        "closures"}
              :cost-price  23.5
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       124.0
              :sku         "ILWDBRLC14"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "18"
               :hair/base-material "lace"
               :hair/family        "closures"}
              :cost-price  30.0
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       154.0
              :sku         "ILWDBRLC18"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "14"
               :hair/base-material "lace"
               :hair/family        "frontals"}
              :cost-price  48.0
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       159.0
              :sku         "ILWDBRFLC14"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "18"
               :hair/base-material "lace"
               :hair/family        "frontals"}
              :cost-price  59.33
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       199.0
              :sku         "ILWDBRFLC18"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "14"
               :hair/family        "bundles"}
              :cost-price  21.16
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       89.0
              :sku         "ILWDBR14"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "16"
               :hair/family        "bundles"}
              :cost-price  23.96
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       99.0
              :sku         "ILWDBR16"}
             {:attributes
              {:product/department "hair"
               :hair/grade         "6a"
               :hair/origin        "indian"
               :hair/texture       "loose-wave"
               :hair/color         "dark-blonde-dark-roots"
               :hair/length        "18"
               :hair/family        "bundles"}
              :cost-price  28.82
              :launched-at "2016-01-01T00:00:00.000Z"
              :price       109.0
              :sku         "ILWDBR18"}])

  #_
  (def skus-db (i/make-db skus))



  (d/q '[:find
         ?s
         (min 2 ?price)
         #_(pull ?s [*])
         :where
         [?s :price ?price]
         ;;   :where [?s :price ?price]
         ]
       (-> (d/empty-db)
           (d/db-with (->> skus
                           (map #(merge (:attributes %) %))
                           (mapv #(dissoc % :attributes))))))



  #_
  (count (d/q '[:find (pull ?s [*])
                :where
                [?s :hair/family "bundles"]
                [?s :price ?price]

                ]
              (-> (d/empty-db)
                  (d/db-with (->> skus
                                  (map #(merge (:attributes %)))
                                  (mapv #(dissoc % :attributes)))))))


  #_
  (mapv #(get-in (i/original skus-db) %)
        (d/q '[:find ?s
               :where
               [?s :attributes :hair/family "bundles"]
               [?s :attributes :hair/length "18"]]
             skus-db))


  )
