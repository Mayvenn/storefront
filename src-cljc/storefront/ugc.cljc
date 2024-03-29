(ns storefront.ugc
  (:require [storefront.events :as events]
            [storefront.keypaths :as keypaths]))

(def ^:private shop-by-look-default
  {:title             "Shop by Look"
   :description       (str "Get inspiration for your next hairstyle and "
                           "shop your favorite looks from the #MayvennMade community.")
   :button-copy       "Shop Look"
   :short-name        "look"
   :seo-title         "Shop by Look | Mayvenn"
   :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"})

;; Looking for adventure? See adventure.select-new-look/album-keyword->prompt-image
(def album-copy
  {:look                 shop-by-look-default
   :aladdin-free-install shop-by-look-default ; TODO Rename aladdin-free-install album
   :sleek-and-straight   (merge shop-by-look-default ; TODO Get proper copy
                                {:default-back-event events/navigate-home
                                 :seo-title          "Shop by Sleek and Straight Looks | Mayvenn"})
   :waves-and-curly      (merge shop-by-look-default ; TODO Get proper copy
                                {:default-back-event events/navigate-home
                                 :seo-title          "Shop by Wavy and Curly Looks | Mayvenn"})
   :email-deals          (assoc shop-by-look-default
                                :description (str "Grab the latest bundle deal! "
                                                  "Below you can shop every bundle deal of the week."))
   :wavy-curly-looks     {:title       "Choose from these Wavy and Curly looks"
                          :description (str "We have an amazing selection of inspo to choose from.")
                          :button-copy "View this look"
                          :short-name  "look"
                          :seo-title   "Shop Wavy and Curly Looks | Mayvenn"
                          :og-title    (str "Shop Wavy and Curly Looks - "
                                            "Find and Buy your favorite Mayvenn bundles!")}
   :straight-looks       {:title       "Choose from these Straight looks"
                          :description (str "We have an amazing selection of inspo to choose from.")
                          :button-copy "View this look"
                          :short-name  "look"
                          :seo-title   "Shop Straight Looks | Mayvenn"
                          :og-title    (str "Shop Straight Looks - "
                                            "Find and Buy your favorite Mayvenn bundles!")}})

(def redirect-to-look-filter
  {:straight [[:texture "straight"]]
   :loose-wave [[:texture "loose-wave"]]
   :body-wave [[:texture "body-wave"]]
   :deep-wave [[:texture "deep-wave"]]
   :curly [[:texture "curly"]]
   :closures [[:texture "closures"]]
   :frontals [[:texture "frontals"]]
   :kinky-straight [[:texture "kinky-straight"]]
   :water-wave [[:texture "water-wave"]]
   :yaki-straight [[:texture "yaki-straight"]]
   :dyed [[:color "brown"] [:color "blonde"]]
   :wigs []

   ;; shop product detail page
   :straight-shop [[:texture "straight"]]
   :loose-wave-shop [[:texture "loose-wave"]]
   :body-wave-shop [[:texture "body-wave"]]
   :deep-wave-shop [[:texture "deep-wave"]]
   :curly-shop [[:texture "curly"]]
   :closures-shop [[:texture "closures"]]
   :frontals-shop [[:texture "frontals"]]
   :kinky-straight-shop [[:texture "kinky-straight"]]
   :water-wave-shop [[:texture "water-wave"]]
   :yaki-straight-shop [[:texture "yaki-straight"]]
   :dyed-shop [[:color "brown"] [:color "blonde"]]
   :wigs-shop []})

;; TODO audit this, looks out of date - April 2020, ditto Sept 2020
;; These 'aladdin' albums might be shop or around services
(def albums
  #{:sleek-and-straight    ;; Aladdin Home - Straight
    :waves-and-curly       ;; Aladdin Home - Wavy
    :free-install-mayvenn  ;; Aladdin & Shop Home - #MAYVENNHAIR
    :email-deals
    :aladdin-free-install  ;; rename to free-install "Aladdin shop by look"
    :look                  ;; Shop by Look on classic/non-aladdin

    ;; aladdin/shop looks
    :wavy-curly-looks
    :straight-looks

    ;; product detail page
    :straight
    :loose-wave
    :body-wave
    :deep-wave
    :curly
    :closures
    :frontals
    :kinky-straight
    :water-wave
    :yaki-straight
    :dyed
    :wigs

    ;; shop product detail page
    :straight-shop
    :loose-wave-shop
    :body-wave-shop
    :deep-wave-shop
    :curly-shop
    :closures-shop
    :frontals-shop
    :kinky-straight-shop
    :water-wave-shop
    :yaki-straight-shop
    :dyed-shop
    :wigs-shop

    ;; adventure
    :shop-by-look-straight
    :shop-by-look-loose-wave
    :shop-by-look-body-wave
    :shop-by-look-deep-wave

    ;; Quiz Results
    :quiz-results})

(defn determine-look-album
  [data target-album-keyword]
  (let [actual-album (cond
                       (not= target-album-keyword :look)
                       target-album-keyword

                       (or (= "shop" (get-in data keypaths/store-slug))
                           (= "retail-location" (get-in data keypaths/store-experience)))
                       :aladdin-free-install

                       :elsewise target-album-keyword)]
    (get albums actual-album :ugc/unknown-album)))

(defn product->album-keyword
  [shop? product]
  (let [album-key-fn (if shop?
                       :contentful/shop-album
                       :contentful/classic-album)]
    (when-let [album (or (album-key-fn product)
                         (:legacy/named-search-slug product))]
      (keyword album))))

(defn redirect-album-to-look-with-filter [album-keyword]
  (get redirect-to-look-filter album-keyword))
