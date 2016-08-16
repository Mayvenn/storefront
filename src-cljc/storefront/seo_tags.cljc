(ns storefront.seo-tags
  (:require [storefront.assets :as assets]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.accessors.taxons :as taxons]))

(def tag-class "seo-tag")

(defn add-seo-tag-class [tags]
  (map #(update-in % [1] assoc :class tag-class) tags))

(def ^:private constant-tags
  [[:meta {:property "og:site_name" :content "Mayvenn"}]])

(def ^:private default-tags
  [[:title {} "Human hair extensions - virgin Brazilian hair & remy hair extensions | Mayvenn"]
   [:meta {:name "description"
           :content "Quality hair extensions trusted & recommended by 60,000 stylists, and backed by the only 30-day return policy in the industry. Try Mayvenn hair today!"}]
   [:meta {:property "og:title"
           :content "100% Virgin Hair Extensions With a 30 Day Money Back Guarantee and Free Shipping!"}]
   [:meta {:property "og:type"
           :content "website"}]
   [:meta {:property "og:image"
           :content (str "http:" assets/canonical-image)}]
   [:meta {:property "og:description"
           :content "Mayvenn is the recommended and trusted source for quality hair by 60,000 stylists across the country. Mayvenn's 100% virgin human hair is backed by a 30 Day Quality Guarantee & includes FREE shipping!"}]])

(defn category-tags [data]
  (let [{:keys [title og-title description og-description images]} (:seo (taxons/current-taxon data))]
    [[:title {} title]
     [:meta {:name "description" :content description}]
     [:meta {:property "og:title" :content og-title}]
     [:meta {:property "og:type" :content "product"}]
     [:meta {:property "og:image" :content (str "http:" (first images))}]
     [:meta {:property "og:description" :content og-description}]]))

(defn tags-for-page [data]
  (->
   (condp = (get-in data keypaths/navigation-event)
     events/navigate-categories [[:title {} "Weave hairstyles & sew-in styles | Mayvenn"]
                                 [:meta {:property "og:title"
                                         :content "100% Virgin Hair Extensions available in Straight Hair, Loose Wave Hair, Body Wave Hair, Deep Wave Hair, Curly Hair, Closures, Frontals."}]
                                 [:meta {:name "description"
                                         :content "100% Virgin Hair Extensions available in Straight Hair, Loose Wave Hair, Body Wave Hair, Deep Wave Hair, Curly Hair, Closures, Frontals."}]
                                 [:meta {:property "og:type"
                                         :content "website"}]
                                 [:meta {:property "og:description"
                                         :content "Mayvenn has your favorite hair styles. Choose from Straight Hair, Loose Wave Hair, Body Wave Hair, Deep Wave Hair, Curly Hair, Closures, & Frontals. FREE shipping & 30 Day Quality Guarantee included!"}]]

     events/navigate-sign-in    [[:title {} "Sign In | Mayvenn"]
                                 [:meta {:property "og:title"
                                         :content "100% human hair backed by our 30 Day Quality Guarantee. Sign In to your Mayvenn account."}]
                                 [:meta {:name "description"
                                         :content "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to update your profile."}]
                                 [:meta {:property "og:type"
                                         :content "website"}]
                                 [:meta {:property "og:description"
                                         :content "Sign In to your Mayvenn account to see your store credit balance, edit your password, and to edit your email address."}]]

     events/navigate-sign-up    [[:title {} "Sign Up | Mayvenn"]
                                 [:meta {:property "og:title"
                                         :content "100% human hair backed by our 30 Day Quality Guarantee. Sign Up for special offers!"}]
                                 [:meta {:name "description"
                                         :content "Sign up for a Mayvenn account to receive special promotions, exclusive offers, and helpful hair styling tips."}]
                                 [:meta {:property "og:type"
                                         :content "website"}]
                                 [:meta {:property "og:description"
                                         :content "Sign Up for a Mayvenn account and we will be able to send you special promotions for discounted hair and other important messages."}]]

     events/navigate-help       [[:title {} "Contact Us | Mayvenn"]
                                 [:meta {:property "og:title"
                                         :content "Contact Us for any questions, problems, or if you need styling advice!"}]
                                 [:meta {:name "description"
                                         :content "We pride ourselves our top-notch customer service. Need help? Call, text, or email us and we will get back to you as quickly as possible."}]
                                 [:meta {:property "og:type"
                                         :content "website"}]
                                 [:meta {:property "og:description"
                                         :content "We are always here for you and pride ourselves on the best customer service. Call, text, or email us and we will get back to you as quickly as possible."}]]

     events/navigate-guarantee  [[:title {} "Our 30 Day Quality Guarantee | Mayvenn"]
                                 [:meta {:property "og:title"
                                         :content "Our 30 Day Quality Guarantee - Buy Risk Free With Easy Returns and Exchanges!"}]
                                 [:meta {:name "description"
                                         :content "Mayvenn's quality guarantee: wear it, dye it, even flat iron it! If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}]
                                 [:meta {:property "og:type"
                                        :content "website"}]
                                 [:meta {:property "og:description"
                                         :content "Wear it, dye it, even flat iron it. If you do not love your Mayvenn hair we will exchange it within 30 days of purchase."}]]

     events/navigate-category    (category-tags data)

     default-tags)
   (concat constant-tags)
   add-seo-tag-class))
