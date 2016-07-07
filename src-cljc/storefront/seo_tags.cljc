(ns storefront.seo-tags
  (:require [storefront.platform.images :as images]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]))

(def tag-class "seo-tag")

(def ^:private default-tags
  [[:title {} "Shop | Mayvenn"]
   [:meta {:name "description"
           :content "Mayvenn sells 100% natural hair extensions backed by a 30-day Quality Guarantee."}]
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
  (add-seo-tag-class
   (condp = (get-in data keypaths/navigation-event)
     events/navigate-guarantee [[:title {} "Guarantee | Mayvenn"]
                                [:meta {:property "og:title"
                                        :content "Mayvenn's Guarantee"}]
                                [:meta {:name "description"
                                        :content "Mayvenn ensures satisfaction by guaranteeing your purchase"}]
                                [:meta {:property "og:type"
                                        :content "website"}]
                                [:meta {:property "og:description"
                                        :content "Mayvenn ensures satisfaction by guaranteeing your purchase"}]
                                [:meta {:property "og:site_name" :content "Mayvenn"}]]

     default-tags)))
