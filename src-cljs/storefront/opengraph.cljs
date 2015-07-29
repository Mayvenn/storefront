(ns storefront.opengraph
  (:require [storefront.script-tags :as tags]
            [storefront.config :as config]))

(def ^:private graph-class "opengraph-tag")

(def ^:private common-attributes
  [{:property "og:description"
    :content "Mayvenn sells 100% natural hair extensions backed by a 30-day Quality Guarantee."}
   {:property "og:site_name" :content "Mayvenn"}])

(def ^:private site-attributes
  (concat common-attributes
          [{:property "og:title" :content "Shop Mayvenn"}
           {:property "og:type" :content "website"}
           {:property "og:image" :content config/canonical-image}]))

(defn- product-attributes [{:keys [image name]}]
  (concat common-attributes
          [{:property "og:title" :content name}
           {:property "og:type" :content "product"}]
          (if image [{:property "og:image" :content image}] [])))

(defn reset-meta-tags [attributes]
  (tags/remove-tags graph-class)
  (doseq [tag-properties attributes]
    (tags/insert-in-head (tags/meta-tag
                          tag-properties
                          graph-class))))

(def set-site-tags (partial reset-meta-tags site-attributes))
(def set-product-tags (comp reset-meta-tags product-attributes))
