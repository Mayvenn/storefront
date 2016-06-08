(ns storefront.components.product)

(defn number->words [n]
  (let [mapping ["Zero" "One" "Two" "Three" "Four" "Five" "Six" "Seven" "Eight" "Nine" "Ten" "Eleven" "Twelve" "Thirteen" "Fourteen" "Fifteen"]]
    (get mapping n (str "(x " n ")"))))

(defn display-bagged-variant [{:keys [quantity product variant]}]
  [:div.item-added
   [:strong "Added to Cart: "]
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   (:name product)])

(defn redesigned-display-bagged-variant [idx {:keys [quantity product variant]}]
  [:.h6.line-height-3.my1.p1.caps.gray.bg-dark-white.medium.center
   {:key idx}
   "Added to bag: "
   (number->words quantity)
   " "
   (some-> variant :variant_attrs :length)
   " "
   (:name product)])
