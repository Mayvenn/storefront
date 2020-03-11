(ns storefront.seo-tags
  (:require [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.categories :as accessors.categories]
            [cemerick.url :as cemerick-url]
            [catalog.keypaths :as k]
            [catalog.facets :as facets]
            [storefront.events :as events]
            [catalog.category :as category]
            [catalog.categories :as categories]
            [catalog.products :as products]
            [spice.selector :as selector]
            [storefront.ugc :as ugc]
            [storefront.utils :as utils]
            [storefront.uri :as uri]
            [clojure.string :as string]
            #?@(:clj [[cheshire.core :as json]
                      [storefront.safe-hiccup :as safe-hiccup]])
            [clojure.set :as set]
            [spice.maps :as maps]))

(defn- use-case-then-order-key [img]
  [(condp = (:use-case img)
     "seo"      0
     "carousel" 1
     2)
   (:order img)])

(defn ^:private seo-image [skuer]
  (->> (selector/match-essentials skuer (:selector/images skuer))
       (sort-by use-case-then-order-key)))

(def tag-class "seo-tag")

(defn add-seo-tag-class [tags]
  (map #(update-in % [1] assoc :class tag-class) tags))

(def ^:private constant-tags
  [[:meta {:property "og:site_name" :content "Mayvenn"}]])

(def ^:private default-tags
  [[:title {} "Mayvenn - Virgin human hair, bundles, extensions and wigs"]
   [:meta {:name "description"
           :content "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!"}]
   [:meta {:property "og:title"
           :content "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!"}]
   [:meta {:property "og:type"
           :content "website"}]
   [:meta {:property "og:image"
           :content assets/canonical-image}]
   [:meta {:property "og:description"
           :content "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"}]])

(defn product-details-tags [data]
  (let [product (products/current-product data)
        sku     (get-in data k/detailed-product-selected-sku)
        image   (when sku (first (seo-image sku)))] ;; This when-clause is because of direct-to-detail products.
    [[:title {} (:page/title product)]
     [:meta {:name "description" :content (:page.meta/description product)}]
     [:meta {:property "og:title" :content (:opengraph/title product)}]
     [:meta {:property "og:type" :content "product"}]
     [:meta {:property "og:image" :content (str "http:" (:url image))}]
     [:meta {:property "og:description" :content (:opengraph/description product)}]]))

(defn ^:private facet-option->option-name
  ;; For origin and color, the sku/name is more appropriate than the option name
  ;; #169613608
  [facets [facet-slug option-slug :as selection]]
  (let [name-key (if (#{"origin" "color"} facet-slug) :sku/name :option/name)]
    (get-in facets [(keyword "hair" facet-slug) :facet/options option-slug name-key])))

(defn ^:private category->allowed-query-params
  [{:keys [selector/electives]}]
  (->> electives
       (select-keys (set/map-invert accessors.categories/query-params->facet-slugs))
       vals
       (map name)
       set))

(defn category-tags [data]
  (let [categories            (get-in data keypaths/categories)
        canonical-category-id (accessors.categories/canonical-category-id data)
        category              (accessors.categories/id->category canonical-category-id categories)
        allowed-query-params  (category->allowed-query-params category)
        facets                (facets/by-slug data)
        uri-query             (-> data (get-in keypaths/navigation-uri) :query)
        selected-options      (cond-> uri-query
                                (string? uri-query) cemerick-url/query->map
                                :always             (select-keys allowed-query-params)
                                :always             category/sort-query-params)
        indexable?            (and
                               (not-any? #(string/includes? % accessors.categories/query-param-separator)
                                         (vals selected-options))
                               (<= (count selected-options) 3))
        can-use-seo-template? (and (:page/title-template category)
                                   (:page.meta/description-template category))
        selected-facet-string (when (and indexable? (seq selected-options))
                                (->> selected-options
                                     (mapv (partial facet-option->option-name facets))
                                     (string/join " ")))

        {seo-title :seo/title
         :keys     [page/title-template
                    page.meta/description-template]} category

        page-title            (if (and can-use-seo-template? selected-facet-string)
                                (categories/render-template title-template (assoc category :computed/selected-facet-string selected-facet-string))
                                (:page/title category))
        page-meta-description (if (and can-use-seo-template? selected-facet-string)
                                (categories/render-template description-template (assoc category :computed/selected-facet-string selected-facet-string))
                                (:page.meta/description category))]
    (cond-> [[:title {} page-title]
             [:meta {:name "description" :content page-meta-description}]
             [:meta {:property "og:title" :content (:opengraph/title category)}]
             [:meta {:property "og:type" :content "product"}]
             [:meta {:property "og:image" :content (str "http:" (:category/image-url category))}]
             [:meta {:property "og:description" :content (:opengraph/description category)}]]
      (not indexable?)
      (conj [:meta {:name "robots" :content "noindex"}]))))

(defn ^:private filter-and-sort-seo-query-params-for-category-page
  [query category]
  (let [allowed-query-params (category->allowed-query-params category)]
    #?(:clj (-> query ;; string in clj
                cemerick-url/query->map
                (select-keys allowed-query-params)
                category/sort-query-params
                uri/map->query
                not-empty)
       :cljs (-> query ;; map in cljs
                 (select-keys allowed-query-params)
                 category/sort-query-params
                 not-empty))))

(defn ^:private derive-canonical-uri-query-params
  [uri data]
  (let [nav-event             (get-in data keypaths/navigation-event)
        canonical-category-id (accessors.categories/canonical-category-id data)
        categories            (get-in data keypaths/categories)
        {:keys [page/slug]
         :as category}   (accessors.categories/id->category canonical-category-id categories)]
    (if (= events/navigate-category nav-event)
      (-> uri
          (assoc :path (str "/categories/" canonical-category-id "-" slug))
          (utils/?update :query filter-and-sort-seo-query-params-for-category-page category))
      (assoc uri :query nil))))

(defn canonical-uri
  [data]
  (some-> (get-in data keypaths/navigation-uri)
          (derive-canonical-uri-query-params data)
          (update :host string/replace #"^[^.]+" "shop")
          (assoc :scheme (get-in data keypaths/scheme))
          str))

(defn canonical-link-tag [data]
  (when-let [canonical-href (canonical-uri data)]
    [[:link {:rel "canonical" :href canonical-href}]]))

(defn tags-for-page [data]
  (let [og-image-url assets/canonical-image]
    (->
     (condp = (get-in data keypaths/navigation-event)
       events/navigate-sign-in [[:title {} "Sign In | Mayvenn"]
                                [:meta {:property "og:title"
                                        :content  "100% human hair backed by our 30 Day Quality Guarantee. Sign In to your Mayvenn account."}]
                                [:meta {:name    "description"
                                        :content "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to update your profile."}]
                                [:meta {:property "og:type"
                                        :content  "website"}]
                                [:meta {:property "og:description"
                                        :content  "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to edit your email address."}]]

       events/navigate-sign-up [[:title {} "Sign Up | Mayvenn"]
                                [:meta {:property "og:title"
                                        :content  "100% human hair backed by our 30 Day Quality Guarantee. Sign Up for special offers!"}]
                                [:meta {:name    "description"
                                        :content "Sign up for a Mayvenn account to receive special promotions, exclusive offers, and helpful hair styling tips."}]
                                [:meta {:property "og:type"
                                        :content  "website"}]
                                [:meta {:property "og:description"
                                        :content  "Sign Up for a Mayvenn account and we will be able to send you special promotions for discounted hair and other important messages."}]]

       events/navigate-content-help [[:title {} "Contact Us | Mayvenn"]
                                     [:meta {:property "og:title"
                                             :content  "Contact Us for any questions, problems, or if you need styling advice!"}]
                                     [:meta {:name    "description"
                                             :content "We pride ourselves our top-notch customer service. Need help? Call, text, or email us and we will get back to you as quickly as possible."}]
                                     [:meta {:property "og:type"
                                             :content  "website"}]
                                     [:meta {:property "og:description"
                                             :content  "We are always here for you and pride ourselves on the best customer service. Call, text, or email us and we will get back to you as quickly as possible."}]]

       events/navigate-content-guarantee [[:title {} "Our 30 Day Quality Guarantee | Mayvenn"]
                                          [:meta {:property "og:title"
                                                  :content  "Our 30 Day Quality Guarantee - Buy Risk Free With Easy Returns and Exchanges!"}]
                                          [:meta {:name    "description"
                                                  :content "Mayvenn's quality guarantee: wear it, dye it, even flat iron it! If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}]
                                          [:meta {:property "og:type"
                                                  :content  "website"}]
                                          [:meta {:property "og:image"
                                                  :content  og-image-url}]
                                          [:meta {:property "og:description"
                                                  :content  "Wear it, dye it, even flat iron it. If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}]]

       events/navigate-content-about-us [[:title {} "About Us - 100% virgin human hair company | Mayvenn "]
                                         [:meta {:property "og:title"
                                                 :content  "The Mayvenn Story - About Us"}]
                                         [:meta {:name    "description"
                                                 :content "Mayvenn is a hair company providing top-quality 100% virgin human hair for consumers and stylists. Learn more about us!"}]
                                         [:meta {:property "og:type"
                                                 :content  "website"}]
                                         [:meta {:property "og:image"
                                                 :content  og-image-url}]
                                         [:meta {:property "og:description"
                                                 :content  "Mayvenn's story starts with a Toyota Corolla filled with bundles of hair to now having over 50,000 stylists selling Mayvenn hair and increasing their incomes. Learn more about us!"}]
                                         [:script {:type "application/ld+json"}
                                          (#?(:clj (comp safe-hiccup/raw json/generate-string)
                                              :cljs (comp js/JSON.stringify clj->js))
                                           {:url             "https://shop.mayvenn.com/about-us"
                                            (symbol "@type") "Corporation"
                                            :name            "Mayvenn Hair"
                                            :logo            "https://d6w7wdcyyr51t.cloudfront.net/cdn/images/header_logo.e8e0ffc6.svg"
                                            :sameAs          ["https://www.facebook.com/MayvennHair"
                                                              "http://instagram.com/mayvennhair"
                                                              "https://twitter.com/MayvennHair"
                                                              "http://www.pinterest.com/mayvennhair/"]
                                            :founder         {(symbol "@type") "Person"
                                                              :name            "Diishan Imira"}})]]

       events/navigate-shop-by-look (let [album-keyword (get-in data keypaths/selected-album-keyword)]
                                      [[:title {} (-> ugc/album-copy album-keyword :seo-title)]
                                       [:meta {:property "og:title"
                                               :content  (-> ugc/album-copy album-keyword :og-title)}]
                                       [:meta {:name    "description"
                                               :content "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}]
                                       [:meta {:property "og:type"
                                               :content  "website"}]
                                       [:meta {:property "og:image"
                                               :content  og-image-url}]
                                       [:meta {:property "og:description"
                                               :content  "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}]])

       events/navigate-category        (category-tags data)
       events/navigate-product-details (product-details-tags data)
       default-tags)
     (concat constant-tags (canonical-link-tag data))
     add-seo-tag-class)))
