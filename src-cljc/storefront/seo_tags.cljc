(ns storefront.seo-tags
  (:require [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [catalog.keypaths :as k]
            [storefront.events :as events]
            [catalog.categories :as categories]
            [catalog.products :as products]
            [catalog.selector :as selector]
            [lambdaisland.uri :as uri]
            [clojure.string :as string]
            [spice.core :as spice]))

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
           :content (str "http:" assets/canonical-image)}]
   [:meta {:property "og:description"
           :content "Mayvenn is the recommended and trusted source for quality hair by 100,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"}]])

(defn category-tags [data]
  (let [category (categories/current-category data)]
    [[:title {} (:page/title category)]
     [:meta {:name "description" :content (:page.meta/description category)}]
     [:meta {:property "og:title" :content (:opengraph/title category)}]
     [:meta {:property "og:type" :content "product"}]
     [:meta {:property "og:image" :content (str "http:" (:category/image-url category))}]
     [:meta {:property "og:description" :content (:opengraph/description category)}]]))

(defn product-details-tags [data]
  (let [product (products/current-product data)
        sku     (get-in data k/detailed-product-selected-sku)
        image   (when sku (first (selector/seo-image sku)))] ;; This when-clause is because of direct-to-detail products.
    [[:title {} (:page/title product)]
     [:meta {:name "description" :content (:page.meta/description product)}]
     [:meta {:property "og:title" :content (:opengraph/title product)}]
     [:meta {:property "og:type" :content "product"}]
     [:meta {:property "og:image" :content (str "http:" (:url image))}]
     [:meta {:property "og:description" :content (:opengraph/description product)}]]))

(defn canonical-uri
  [{:as data :keys [store]}]
  (when-not (contains? #{"shop" "welcome"} (:store_slug store))
    (some-> (get-in data keypaths/navigation-uri)
            (update :host string/replace #"^[^.]+" "shop")
            str)))

(defn canonical-link-tag [data]
  (when-let [canonical-href (canonical-uri data)]
    [[:link {:rel "canonical" :href canonical-href}]]))

(defn tags-for-page [data]
  (let [og-image-url (str "http:" assets/canonical-image)]
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
                                                  :content og-image-url}]
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
                                                 :content og-image-url}]
                                         [:meta {:property "og:description"
                                                 :content  "Mayvenn's story starts with a Toyota Corolla filled with bundles of hair to now having over 50,000 stylists selling Mayvenn hair and increasing their incomes. Learn more about us!"}]]

       events/navigate-shop-by-look [[:title {} "Shop By Look | Mayvenn"]
                                     [:meta {:property "og:title"
                                             :content  "Shop By Look - Find and Buy your favorite Mayvenn hairstyle!"}]
                                     [:meta {:name    "description"
                                             :content "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}]
                                     [:meta {:property "og:type"
                                             :content  "website"}]
                                     [:meta {:property "og:image"
                                             :content og-image-url}]
                                     [:meta {:property "og:description"
                                             :content  "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}]]
       events/navigate-shop-bundle-deals [[:title {} "Shop Bundle Deals | Mayvenn"]
                                          [:meta {:property "og:title"
                                                  :content  "Shop Bundle Deals - Find and Buy your favorite Mayvenn bundles!"}]]

       events/navigate-category            (category-tags data)
       events/navigate-product-details     (product-details-tags data)

       default-tags)
     (concat constant-tags (canonical-link-tag data))
     add-seo-tag-class)))
