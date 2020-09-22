(ns storefront.seo-tags
  (:require #?@(:clj [[cheshire.core :as json]
                      [storefront.uri :as uri]
                      [storefront.safe-hiccup :as safe-hiccup]])
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.images :as images]
            [cemerick.url :as cemerick-url]
            [catalog.keypaths :as k]
            [catalog.facets :as facets]
            [storefront.events :as events]
            [homepage.ui-v2020-07 :as homepage.ui]
            [catalog.categories :as categories]
            [catalog.products :as products]
            [spice.selector :as selector]
            [storefront.ugc :as ugc]
            [storefront.utils :as utils]
            [clojure.string :as string]
            [clojure.set :as set]
            [spice.maps :as maps]
            adventure.keypaths
            [stylist-directory.stylists :as stylists]))

(defn- use-case-then-order-key [img]
  [(condp = (:use-case img)
     "seo"      0
     "carousel" 1
     2)
   (:order img)])

(defn ^:private seo-image [images-catalog skuer]
  (->> (selector/match-essentials skuer (images/for-skuer images-catalog skuer))
       (sort-by use-case-then-order-key)))

(def tag-class "seo-tag")

(defn add-seo-tag-class [tags]
  (map #(update-in % [1] assoc :class tag-class) tags))

(def ^:private default-tagmap
  {:title          "Mayvenn - Virgin human hair, bundles, extensions and wigs"
   :description    "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!"
   :og-title       "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!"
   :og-type        "website"
   :og-image       assets/canonical-image
   :og-description "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"})

(defn ->structured-data [data]
  ;; Although it's not difficult to make this work for client side, there is no value to having structured data
  ;; in the fully rendered page as the information is scraped from the server side render.  Additionally,
  ;; the second render was being detected and flagged as duplicate by the Google structured data tool.
  #?(:clj
     [[:script {:type "application/ld+json"}
       (-> (merge {"@context" "https://schema.org"} data)
           json/generate-string
           safe-hiccup/raw)]]
     :cljs []))

(defn ^:private tagmap->tags
  ([{:keys [title description og-title og-type og-image og-description no-index? structured-data]}]
   (cond-> [[:title {} title]
            [:meta {:name "description" :content description}]
            [:meta {:property "og:title" :content og-title}]
            [:meta {:property "og:type" :content og-type}]
            [:meta {:property "og:description" :content og-description}]]
     og-image        (concat [[:meta {:property "og:image" :content og-image}]])
     no-index?       (concat [[:meta {:name "robots" :content "noindex"}]])
     structured-data (concat (->structured-data structured-data)))))

(def ^:private constant-tags
  [[:meta {:property "og:site_name" :content "Mayvenn"}]])

(defn product-details-tags [data]
  (let [product        (products/current-product data)
        images-catalog (get-in data keypaths/v2-images)
        sku            (get-in data k/detailed-product-selected-sku)
        image-url      (some->> sku
                                (seo-image images-catalog)
                                first
                                :url
                                (str "http:"))]

    {:title           (:page/title product)
     :description     (:page.meta/description product)
     :og-title        (:opengraph/title product)
     :og-type         "product"
     :og-image        image-url
     :og-description  (:opengraph/description product)
     :structured-data {"@type"      "Product"
                       :name        (:sku/title sku)
                       :image       image-url
                       :sku         (:catalog/sku-id sku)
                       :description (:opengraph/description product)
                       :offers      {"@type"        "Offer"
                                     :price         (str (:sku/price sku))
                                     :priceCurrency "USD"
                                     :availability  (if (:inventory/in-stock? sku)
                                                      "http://schema.org/InStock"
                                                      "http://schema.org/OutOfStock")}}}))

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
        canonical-category-id (:category-id (accessors.categories/canonical-category-data
                                             categories
                                             (accessors.categories/id->category
                                              (get-in data catalog.keypaths/category-id)
                                              categories)
                                             (get-in data keypaths/navigation-uri)
                                             #?(:cljs (experiments/remove-closures? data)
                                                :clj true)))
        category              (accessors.categories/id->category canonical-category-id categories)
        allowed-query-params  (category->allowed-query-params category)
        facets                (facets/by-slug data)
        selected-options      (->  data
                                   (get-in keypaths/navigation-uri)
                                   :query
                                   (select-keys allowed-query-params)
                                   accessors.categories/sort-query-params)
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
    {:title          page-title
     :description    page-meta-description
     :og-title       (:opengraph/title category)
     :og-type        "product"
     :og-image       (str "http:" (:category/image-url category))
     :og-description (:opengraph/description category)
     :no-index?      (not indexable?)}))

(defn ^:private derive-canonical-uri-query-params-category-pages
  [uri data]
  (let [categories             (get-in data keypaths/categories)
        {:keys [category-slug
                category-id
                selections]}   (accessors.categories/canonical-category-data
                                categories
                                (-> data
                                    (get-in catalog.keypaths/category-id)
                                    (accessors.categories/id->category categories))
                                (get-in data keypaths/navigation-uri)
                                #?(:cljs (experiments/remove-closures? data)
                                   :clj true))
        category               (accessors.categories/id->category category-id categories)
        permitted-query-params (category->allowed-query-params category)
        query                  (-> uri
                                   :query
                                   (merge selections)
                                   (select-keys permitted-query-params)
                                   accessors.categories/sort-query-params
                                   not-empty)]
    (-> uri
        (assoc :path (str "/categories/" category-id "-" category-slug))
        (assoc :query (storefront.uri/map->query query)))))

;; Figure out if this helps us determine if a category page is its own canonical for sitemap
(defn ^:private derive-canonical-uri-query-params
  [uri data]
  (if (= events/navigate-category  (get-in data keypaths/navigation-event))
    (derive-canonical-uri-query-params-category-pages uri data)
    (assoc uri :query nil)))

(defn canonical-uri
  [data]
  (some-> (get-in data keypaths/navigation-uri)
          cemerick-url/map->URL
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
       events/navigate-sign-in
       {:title          "Sign In | Mayvenn"
        :og-title       "100% human hair backed by our 30 Day Quality Guarantee. Sign In to your Mayvenn account."
        :description    "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to update your profile."
        :og-type        "website"
        :og-description "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to edit your email address."}

       events/navigate-sign-up
       {:title          "Sign Up | Mayvenn"
        :og-title       "100% human hair backed by our 30 Day Quality Guarantee. Sign Up for special offers!"
        :description    "Sign up for a Mayvenn account to receive special promotions, exclusive offers, and helpful hair styling tips."
        :og-type        "website"
        :og-description "Sign Up for a Mayvenn account and we will be able to send you special promotions for discounted hair and other important messages."}

       events/navigate-content-help
       {:title          "Contact Us | Mayvenn"
        :og-title       "Contact Us for any questions, problems, or if you need styling advice!"
        :description    "We pride ourselves our top-notch customer service. Need help? Call, text, or email us and we will get back to you as quickly as possible."
        :og-type        "website"
        :og-description "We are always here for you and pride ourselves on the best customer service. Call, text, or email us and we will get back to you as quickly as possible."}

       events/navigate-content-guarantee
       {:title          "Our 30 Day Quality Guarantee | Mayvenn"
        :og-title       "Our 30 Day Quality Guarantee - Buy Risk Free With Easy Returns and Exchanges!"
        :description    "Mayvenn's quality guarantee: wear it, dye it, even flat iron it! If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."
        :og-type        "website"
        :og-image       og-image-url
        :og-description "Wear it, dye it, even flat iron it. If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}

       events/navigate-content-about-us
       {:title           "About Us - 100% virgin human hair company | Mayvenn "
        :og-title        "The Mayvenn Story - About Us"
        :description     "Mayvenn is a hair company providing top-quality 100% virgin human hair for consumers and stylists. Learn more about us!"
        :og-type         "website"
        :og-image        og-image-url
        :og-description  "Mayvenn's story starts with a Toyota Corolla filled with bundles of hair to now having over 50,000 stylists selling Mayvenn hair and increasing their incomes. Learn more about us!"
        :structured-data {:url     "https://shop.mayvenn.com/about-us"
                          "@type"  "Corporation"
                          :name    "Mayvenn Hair"
                          :logo    "https://d6w7wdcyyr51t.cloudfront.net/cdn/images/header_logo.e8e0ffc6.svg"
                          :sameAs  ["https://www.facebook.com/MayvennHair"
                                    "http://instagram.com/mayvennhair"
                                    "https://twitter.com/MayvennHair"
                                    "http://www.pinterest.com/mayvennhair/"]
                          :founder {"@context" "http://schema.org"
                                    "@type"    "Person"
                                    :name      "Diishan Imira"}}}

       events/navigate-shop-by-look
       (let [album-keyword (get-in data keypaths/selected-album-keyword)]
         {:title          (-> ugc/album-copy album-keyword :seo-title)
          :og-title       (-> ugc/album-copy album-keyword :og-title)
          :description    "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."
          :og-type        "website"
          :og-image       og-image-url
          :og-description "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."})

       events/navigate-category
       (category-tags data)

       events/navigate-product-details
       (product-details-tags data)

       events/navigate-home
       {:title           "Sew-In Weave Bundles, Human Hair Wigs, and Free Install Salon Services | Mayvenn",
        :description     "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!",
        :og-title        "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!",
        :og-type         "website",
        :og-image        "http://ucarecdn.com/401c6886-077a-4445-85ec-f6b7023d5d1e/-/format/auto/canonical_image",
        :og-description  "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"
        :structured-data (when (= "shop" (get-in data keypaths/store-slug))
                           {"@type"     "FAQPage"
                            :mainEntity (mapv (fn
                                                [{:faq/keys [title content]}]
                                                {"@type"         "Question"
                                                 :name           title
                                                 :acceptedAnswer {"@type" "Answer"
                                                                  :text   content}})
                                              homepage.ui/faq-sections-data)})}

       events/navigate-info-certified-stylists
       {:title          "Certified Stylists: Top-Rated Hair Weave Stylists | Mayvenn",
        :description    "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!",
        :og-title       "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!",
        :og-type        "website",
        :og-image       "http://ucarecdn.com/401c6886-077a-4445-85ec-f6b7023d5d1e/-/format/auto/canonical_image",
        :og-description "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"}

       events/navigate-adventure-match-stylist
       {:title          "Browse Stylists: Find Stylists in Your Area | Mayvenn",
        :description    "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!",
        :og-title       "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!",
        :og-type        "website",
        :og-image       "http://ucarecdn.com/401c6886-077a-4445-85ec-f6b7023d5d1e/-/format/auto/canonical_image",
        :og-description "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"}

       events/navigate-content-our-hair
       (merge default-tagmap
              {:title "Our Hair: Virgin and Dyed Virgin Human Hair | Mayvenn"})

       events/navigate-adventure-stylist-profile
       (let [{{salon-name :name
               :keys      [city state]} :salon} (stylists/by-id data (get-in data adventure.keypaths/stylist-profile-id))]
         (merge default-tagmap
                {:title (str salon-name " " city ", " state " | Mayvenn")
                 :description (str salon-name " in " city ", " state " offers sew-in installs, leave outs & more. Check out their full salon menu & book today.")}))

       ;; else
       default-tagmap)
     tagmap->tags
     (concat constant-tags (canonical-link-tag data))
     add-seo-tag-class)))
