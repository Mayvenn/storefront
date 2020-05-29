(ns storefront.seo-tags
  (:require #?@(:clj [[cheshire.core :as json]
                      [storefront.uri :as uri]
                      [storefront.safe-hiccup :as safe-hiccup]])
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.categories :as accessors.categories]
            [cemerick.url :as cemerick-url]
            [catalog.keypaths :as k]
            [catalog.facets :as facets]
            [storefront.events :as events]
            [homepage.v2020-04 :as homepage]
            [catalog.categories :as categories]
            [catalog.products :as products]
            [spice.selector :as selector]
            [storefront.ugc :as ugc]
            [storefront.utils :as utils]
            [clojure.string :as string]
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

(defn ->structured-data [data]
  ;; Although it's not difficult to make this work for client side, there is no value to having structured data
  ;; in the fully rendered page as the information is scraped from the server side render.  Additionally,
  ;; the second render was being detected and flagged as duplicate by the Google structured data tool.
  #?(:clj
     [:script {:type "application/ld+json"}
      (-> (merge {"@context" "https://schema.org"} data)
          json/generate-string
          safe-hiccup/raw)]))

(defn product-details-tags [data]
  (let [product   (products/current-product data)
        sku       (get-in data k/detailed-product-selected-sku)
        image-url (some->> sku
                           seo-image
                           first
                           :url
                           (str "http:"))]
    [[:title {} (:page/title product)]
     [:meta {:name "description" :content (:page.meta/description product)}]
     [:meta {:property "og:title" :content (:opengraph/title product)}]
     [:meta {:property "og:type" :content "product"}]
     [:meta {:property "og:image" :content image-url}]
     [:meta {:property "og:description" :content (:opengraph/description product)}]
     (->structured-data {"@type"      "Product"
                         :name        (:sku/title sku)
                         :image       image-url
                         :sku         (:catalog/sku-id sku)
                         :description (:opengraph/description product)
                         :offers      {"@type"        "Offer"
                                       :price         (str (:sku/price sku))
                                       :priceCurrency "USD"
                                       :availability  (if (:inventory/in-stock? sku)
                                                       "http://schema.org/InStock"
                                                       "http://schema.org/OutOfStock")}})]))

(defn ^:private facet-option->option-name
  ;; For origin and color, the sku/name is more appropriate than the option name
  ;; #169613608
  [facets [facet-slug option-slug :as selection]]
  (let [name-key (if (#{:hair/color :hair/origin} facet-slug) :sku/name :option/name)]
    (get-in facets [facet-slug :facet/options option-slug name-key])))

(defn ^:private category->allowed-query-params
  [{:keys [selector/electives]}]
  (->> electives
       (select-keys (set/map-invert accessors.categories/query-params->facet-slugs))
       vals
       (map name)
       set))

(defn category-tags [data]
  (let [categories            (get-in data keypaths/categories)
        canonical-category-id (accessors.categories/canonical-category-id
                               (get-in data catalog.keypaths/category-id)
                               categories
                               (get-in data keypaths/navigation-uri))
        category              (accessors.categories/id->category canonical-category-id categories)
        allowed-query-params  (category->allowed-query-params category)
        facets                (facets/by-slug data)
        uri-query             (-> data (get-in keypaths/navigation-uri) :query)
        selected-options      (cond-> uri-query
                                (string? uri-query) cemerick-url/query->map
                                :always             (select-keys allowed-query-params)
                                :always             accessors.categories/sort-query-params)
        indexable?            (and
                               (not-any? #(string/includes? % accessors.categories/query-param-separator)
                                         (vals selected-options))
                               (<= (count selected-options) 3))
        can-use-seo-template? (and (:page/title-template category)
                                   (:page.meta/description-template category))
        selected-facet-string (when (and indexable? (seq selected-options))
                                (->> selected-options
                                     (maps/map-keys (comp accessors.categories/query-params->facet-slugs keyword str))
                                     (mapv (partial facet-option->option-name facets))
                                     (string/join " ")))

        {:keys [page/title-template
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
                accessors.categories/sort-query-params
                uri/map->query
                not-empty)
       :cljs (-> query ;; map in cljs
                 (select-keys allowed-query-params)
                 accessors.categories/sort-query-params
                 not-empty))))

;; Figure out if this helps us determine if a category page is its own canonical for sitemap
(defn ^:private derive-canonical-uri-query-params
  [uri data]
  (let [nav-event             (get-in data keypaths/navigation-event)
        categories            (get-in data keypaths/categories)
        canonical-category-id (accessors.categories/canonical-category-id
                               (get-in data catalog.keypaths/category-id)
                               categories
                               (get-in data keypaths/navigation-uri))
        {:keys [page/slug]
         :as   category}      (accessors.categories/id->category canonical-category-id categories)]
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

(defn homepage-tags
  [data]
  (when (= "shop" (get-in data keypaths/store-slug))
    [(->structured-data {"@type"     "FAQPage"
                         :mainEntity (mapv (fn
                                             [{:faq/keys [title paragraphs]}]
                                             {"@type"         "Question"
                                              :name           title
                                              :acceptedAnswer {"@type" "Answer"
                                                               :text   (string/join " " paragraphs)}})
                                           homepage/faq-sections-data)})]))

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
                                         (->structured-data {:url     "https://shop.mayvenn.com/about-us"
                                                             "@type"  "Corporation"
                                                             :name    "Mayvenn Hair"
                                                             :logo    "https://d6w7wdcyyr51t.cloudfront.net/cdn/images/header_logo.e8e0ffc6.svg"
                                                             :sameAs  ["https://www.facebook.com/MayvennHair"
                                                                         "http://instagram.com/mayvennhair"
                                                                         "https://twitter.com/MayvennHair"
                                                                         "http://www.pinterest.com/mayvennhair/"]
                                                             :founder {"@context" "http://schema.org"
                                                                       "@type"    "Person"
                                                                       :name      "Diishan Imira"}})]

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

       events/navigate-home (homepage-tags data)

       default-tags)
     (concat constant-tags (canonical-link-tag data))
     add-seo-tag-class)))
