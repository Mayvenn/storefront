(ns storefront.seo-tags
  (:require #?@(:cljs [[storefront.accessors.experiments :as experiments]]
                :clj [[cheshire.core :as json]
                      [storefront.uri :as uri]
                      [storefront.safe-hiccup :as safe-hiccup]])
            api.stylist
            [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.categories :as accessors.categories]
            [storefront.accessors.products :as accessors.products]
            [storefront.accessors.images :as images]
            [cemerick.url :as cemerick-url]
            [catalog.keypaths :as k]
            [catalog.facets :as facets]
            [storefront.events :as events]
            [catalog.categories :as categories]
            [catalog.products :as products]
            [spice.selector :as selector]
            [storefront.ugc :as ugc]
            [clojure.string :as string]
            [clojure.set :as set]
            [spice.maps :as maps]
            adventure.keypaths
            [storefront.accessors.sites :as sites]))

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
  #?(:clj (for [datum data]
            [:script {:type "application/ld+json"}
             (-> (merge {"@context" "https://schema.org"} datum)
                 json/generate-string
                 safe-hiccup/raw)])
     :cljs (for [datum data]
             [:script {:type "application/ld+json"}
              (->> (merge {"@context" "https://schema.org"} datum)
                   clj->js
                   (.stringify js/JSON))])))

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

(defn ^:private faq->structured-data [faq]
  (when faq
    (let [{:keys [question-answers]} faq]
      {"@type"     "FAQPage"
       :mainEntity (for [{:keys [question answer]} question-answers]
                     {"@type"         "Question"
                      :name           (:text question)
                      :acceptedAnswer {"@type" "Answer"
                                       :text   (->> answer
                                                    (mapcat :paragraph)
                                                    (map (fn [{:keys [text url]}]
                                                           (if url
                                                             (str "<a href=\"" url "\">" text "</a>")
                                                             text)))
                                                    (apply str))}})})))

(defn product-details-tags [data]
  (let [product        (products/current-product data)
        images-catalog (get-in data keypaths/v2-images)
        sku            (get-in data k/detailed-product-selected-sku)
        image-url      (some->> sku
                                (seo-image images-catalog)
                                first
                                :url
                                (str "http:"))
        shop?          (= :shop (sites/determine-site data))
        faq            (get-in data (conj keypaths/cms-faq (accessors.products/product->faq-id product)))]
    {:title           (:page/title product)
     :description     (:page.meta/description product)
     :og-title        (:opengraph/title product)
     :og-type         "product"
     :og-image        image-url
     :og-description  (:opengraph/description product)
     :structured-data (cond->
                          [{"@type"      "Product"
                            :name        (:sku/title sku)
                            :image       image-url
                            :sku         (:catalog/sku-id sku)
                            :description (:opengraph/description product)
                            :offers      {"@type"        "Offer"
                                          :price         (str (:sku/price sku))
                                          :priceCurrency "USD"
                                          :availability  (if (:inventory/in-stock? sku)
                                                           "http://schema.org/InStock"
                                                           "http://schema.org/OutOfStock")
                                          :shippingDetails {"@type"              "OfferShippingDetails"
                                                            :shippingDestination {"@type"         "DefinedRegion"
                                                                                  :addressCountry "US"}
                                                            :deliveryTime        {"@type"       "ShippingDeliveryTime"
                                                                                  :businessDays {"@type"    "OpeningHoursSpecification"
                                                                                                 :dayOfWeek ["https://schema.org/Monday"
                                                                                                             "https://schema.org/Tuesday"
                                                                                                             "https://schema.org/Wednesday"
                                                                                                             "https://schema.org/Thursday"
                                                                                                             "https://schema.org/Friday"]}
                                                                                  :cutOffTime   "18:00:00-05:00"
                                                                                  :handlingTime {"@type"   "QuantitativeValue"
                                                                                                 :minValue "0"
                                                                                                 :maxValue "3"}
                                                                                  :transitTime  {"@type"   "QuantitativeValue"
                                                                                                 :minValue "4"
                                                                                                 :maxValue "6"}}
                                                            :shippingRate         {"@type"   "MonetaryAmount"
                                                                                   :currency "USD"
                                                                                   :value    "0"}}}}]
                        (and shop? faq)
                        (conj (faq->structured-data faq)))}))

(defn ^:private facet-option->option-name
  ;; For origin and color, the sku/name is more appropriate than the option name
  ;; #169613608
  [facets [facet-slug option-slug :as selection]]
  (let [name-key (if (#{:hair/color :hair/origin} facet-slug) :sku/name :option/name)]
  (if (= facet-slug :hair/color.shorthand)
    (string/capitalize option-slug)
    (get-in facets [facet-slug :facet/options option-slug name-key]))))

(defn ^:private category->allowed-query-params
  [{:keys [selector/electives]}]
  (let [allowed-query-params (->> electives
                                  (select-keys facets/slug>query-param)
                                  vals
                                  (map name)
                                  set)]
  (if (some #(= :hair/color %) electives)
    (conj allowed-query-params "color-shorthand")
    allowed-query-params)))

(defn category-tags [data]
  (let [shop?                 (= "shop" (get-in data keypaths/store-slug))
        categories            (get-in data keypaths/categories)
        canonical-category-id (:category-id (accessors.categories/canonical-category-data
                                             categories
                                             (accessors.categories/id->category
                                              (get-in data catalog.keypaths/category-id)
                                              categories)
                                             (get-in data keypaths/navigation-uri)))
        category              (accessors.categories/id->category canonical-category-id categories)
        faq                   (get-in data (conj keypaths/cms-faq (:contentful/faq-id category)))
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
                                     (maps/map-keys (comp facets/query-param>slug keyword str))
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
    (merge {:title           page-title
            :description     page-meta-description
            :og-title        (:opengraph/title category)
            :og-type         "product"
            :og-image        (str "http:" (:category/image-url category))
            :og-description  (:opengraph/description category)
            :no-index?       (not indexable?)}
           (when shop? {:structured-data [(faq->structured-data faq)]}))))

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
                                (get-in data keypaths/navigation-uri))
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

(defn location-structured-data
  [data location]
  (when-let [location-cms-data (get-in data (conj keypaths/cms-retail-location (keyword location)))]
    (let [telephone  (-> location-cms-data :phone-number)]
      {"@type"    "LocalBusiness"
       :name      (str "Mayvenn Beauty Lounge - " (-> location-cms-data :name))
       :telephone telephone
       :address   {"@type"          "PostalAddress"
                   :postalCode      (-> location-cms-data :address-zipcode)
                   :streetAddress   (str (-> location-cms-data :address-1)
                                         (when-let [address-2 (-> location-cms-data :address-2)]
                                           (str ", " address-2)))
                   :addressCountry  "USA"
                   :addressRegion   (-> location-cms-data :state)
                   :addressLocality (-> location-cms-data :address-city)
                   :telephone       telephone}
       :image     (->> location-cms-data :hero :file :url (str "http:"))
       :geo       {"@type"    "GeoCoordinates"
                   :latitude  (-> location-cms-data :location :lat)
                   :longitude (-> location-cms-data :location :lon)}})))

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
        :structured-data [{:url     "https://shop.mayvenn.com/about-us"
                           "@type"  "Corporation"
                           :name    "Mayvenn Hair"
                           :logo    "https://d6w7wdcyyr51t.cloudfront.net/cdn/images/header_logo.e8e0ffc6.svg"
                           :sameAs  ["https://www.facebook.com/MayvennHair"
                                     "http://instagram.com/mayvennhair"
                                     "https://twitter.com/MayvennHair"
                                     "http://www.pinterest.com/mayvennhair/"]
                           :founder {"@context" "http://schema.org"
                                     "@type"    "Person"
                                     :name      "Diishan Imira"}}]}

       events/navigate-content-return-and-exchange-policy
       {:title          "Return and Exchange Policy | Mayvenn"
        :og-title       "Return and Exchange Policy - Buy Risk Free With Easy Returns and Exchanges!"
        :description    "Wear it, dye it, even cut it. If you’re not in love with your hair, we’ll exchange it within 30 days of purchase."
        :og-type        "website"
        :og-image       og-image-url
        :og-description "Wear it, dye it, even cut it. If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}

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
       {:title           "Sew-In Weave Bundles, Closures, Frontals, and Human Hair Wigs | Mayvenn",
        :description     "Quality virgin human hair & extensions trusted & recommended by 100,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!",
        :og-title        "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!",
        :og-type         "website",
        :og-image        assets/canonical-image
        :og-description  "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"
        :structured-data (when (= "shop" (get-in data keypaths/store-slug))
                           [(faq->structured-data (get-in data (conj keypaths/cms-faq :free-mayvenn-services)))])}

       events/navigate-content-our-hair
       (merge default-tagmap
              {:title "Our Hair: Virgin and Colored Virgin Human Hair | Mayvenn"})

       events/navigate-retail-walmart
       (merge default-tagmap
              {:title       "Mayvenn Beauty Lounge - Texas"
               :description "Visit our Texas' stores for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look"})

       events/navigate-retail-walmart-houston
       (merge default-tagmap
              {:title           "Mayvenn Beauty Lounge - Houston"
               :description     "Visit our Houston store for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look."
               :structured-data [(merge (location-structured-data data :houston)
                                        {:openingHoursSpecification
                                         [{"@type" "OpeningHoursSpecification",
                                           :closes "18:00:00",
                                           :dayOfWeek "https://schema.org/Sunday",
                                           :opens "12:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Saturday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Thursday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Tuesday",
                                           :opens "13:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Friday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Monday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Wednesday",
                                           :opens "11:00:00"}]})]})

       events/navigate-retail-walmart-katy
       (merge default-tagmap
              {:title           "Mayvenn Beauty Lounge - Katy"
               :description     "Visit our Katy store for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look."
               :structured-data [(merge (location-structured-data data :katy)
                                        {:openingHoursSpecification
                                         [{"@type" "OpeningHoursSpecification",
                                           :closes "18:00:00",
                                           :dayOfWeek "https://schema.org/Sunday",
                                           :opens "12:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Saturday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Thursday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Tuesday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Friday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Monday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Wednesday",
                                           :opens "11:00:00"}]})]})

       events/navigate-retail-walmart-dallas
       (merge default-tagmap
              {:title           "Mayvenn Beauty Lounge - Dallas"
               :description     "Visit our Dallas store for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look."
               :structured-data [(merge (location-structured-data data :dallas)
                                        {:openingHoursSpecification
                                         [{"@type" "OpeningHoursSpecification",
                                           :closes "18:00:00",
                                           :dayOfWeek "https://schema.org/Sunday",
                                           :opens "12:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Saturday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Thursday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Tuesday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Friday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Monday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Wednesday",
                                           :opens "11:00:00"}]})]})

       events/navigate-retail-walmart-grand-prairie
       (merge default-tagmap
              {:title           "Mayvenn Beauty Lounge - Grand Prairie"
               :description     "Visit our Grand Prairie store for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look."
               :structured-data [(merge (location-structured-data data :grand-prairie)
                                        {:openingHoursSpecification
                                         [{"@type" "OpeningHoursSpecification",
                                           :closes "18:00:00",
                                           :dayOfWeek "https://schema.org/Sunday",
                                           :opens "12:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Saturday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Thursday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Tuesday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Friday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Monday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:30:00",
                                           :dayOfWeek "https://schema.org/Wednesday",
                                           :opens "11:00:00"}]})]})

       events/navigate-retail-walmart-mansfield
       (merge default-tagmap
              {:title           "Mayvenn Beauty Lounge - Mansfield"
               :description     "Visit our Mansfield store for a large selection of 100% virgin human hair wigs, bundles and seamless hair extensions to create your perfect look."
               :structured-data [(merge (location-structured-data data :mansfield)
                                        {:openingHoursSpecification
                                         [{"@type" "OpeningHoursSpecification",
                                           :closes "18:00:00",
                                           :dayOfWeek "https://schema.org/Sunday",
                                           :opens "12:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Saturday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Thursday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Tuesday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Friday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Monday",
                                           :opens "11:00:00"}
                                          {"@type" "OpeningHoursSpecification",
                                           :closes "19:00:00",
                                           :dayOfWeek "https://schema.org/Wednesday",
                                           :opens "11:00:00"}]})]})

       events/navigate-adventure-stylist-profile
       (let [{:stylist/keys [salon] :stylist.address/keys [city state]}
             (api.stylist/by-id data (get-in data adventure.keypaths/stylist-profile-id))]
         (merge default-tagmap
                {:title       (str salon " " city ", " state " | Mayvenn")
                 :description (str salon " in " city ", " state " offers sew-in installs, leave outs & more. "
                                   "Check out their full salon menu & book today.")}))

       ;; else
       default-tagmap)
     tagmap->tags
     (concat constant-tags (canonical-link-tag data))
     add-seo-tag-class)))
