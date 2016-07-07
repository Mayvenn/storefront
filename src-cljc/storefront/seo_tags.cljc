(ns storefront.seo-tags
  (:require [storefront.platform.images :as images]))

(def tag-class "seo-tag")

(def ^:private default-tags
  [[:title {} "Shop | Mayvenn"]
   [:meta {:property "og:title"
           :content "Shop Mayvenn"}]
   [:meta {:property "og:type"
           :content "website"}]
   [:meta {:property "og:image"
           :content images/canonical-image}]
   [:meta {:property "og:description"
           :content "Mayvenn sells 100% natural hair extensions backed by a 30-day Quality Guarantee."}]
   [:meta {:property "og:site_name" :content "Mayvenn"}]])

(defn add-seo-tag-class [tags]
  (map #(update-in % [1] assoc :class tag-class) tags))

(defn tags-for-page [data]
  (add-seo-tag-class default-tags))
