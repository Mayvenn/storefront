(ns storefront.uuid)

;; use cljs.core directly when we upgrade to 0.0-3308 or beyond
(defn random-uuid []
  (letfn [(hex [] (.toString (rand-int 15) 16))]
    (let [rhex (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 14))) 16)]
      (str (hex) (hex) (hex) (hex)
           (hex) (hex) (hex) (hex) "-"
           (hex) (hex) (hex) (hex) "-"
           "4"   (hex) (hex) (hex) "-"
           rhex  (hex) (hex) (hex) "-"
           (hex) (hex) (hex) (hex)
           (hex) (hex) (hex) (hex)
           (hex) (hex) (hex) (hex)))))
