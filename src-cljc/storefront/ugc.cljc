(ns storefront.ugc
  (:require [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]
            [storefront.keypaths :as keypaths]))

(def ^:private shop-by-look-default
  {:title             "Shop by Look"
   :description       (str "Get inspiration for your next hairstyle and "
                           "shop your favorite looks from the #MayvennMade community.")
   :back-copy         "back to shop by look"
   :button-copy       "Shop Look"
   :short-name        "look"
   :seo-title         "Shop by Look | Mayvenn"
   :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"})

;; Looking for adventure? See adventure.select-new-look/album-keyword->prompt-image
(def album-copy
  {:deals                {:title             "Shop Deals"
                          :description       (str "Save more when you bundle up! "
                                                  "We wrapped our most popular textures into "
                                                  "packaged bundle deals so you can shop with ease.")
                          :button-copy       "View this deal"
                          :back-copy         "back to deals"
                          :short-name        "deal"
                          :seo-title         "Shop Deals | Mayvenn"
                          :og-title          (str "Shop Deals - "
                                                  "Find and Buy your favorite Mayvenn bundles!")}
   :look                 shop-by-look-default
   :aladdin-free-install shop-by-look-default ; TODO Rename aladdin-free-install album
   :sleek-and-straight   (assoc shop-by-look-default ; TODO Get proper copy
                                :back-copy          "back"
                                :default-back-event events/navigate-home)
   :waves-and-curly      (assoc shop-by-look-default ; TODO Get proper copy
                                :back-copy          "back"
                                :default-back-event events/navigate-home)
   :email-deals          (assoc shop-by-look-default
                                :description (str "Grab the latest bundle deal! "
                                                  "Below you can shop every bundle deal of the week."))

   :wavy-curly-looks {:title             "Choose from these Wavy and Curly looks"
                      :description       (str "We have an amazing selection of inspo to choose from.")
                      :button-copy       "View this look"
                      :back-copy         "back to looks"
                      :short-name        "look"
                      :seo-title         "Shop Wavy and Curly Looks | Mayvenn"
                      :og-title          (str "Shop Wavy and Curly Looks - "
                                              "Find and Buy your favorite Mayvenn bundles!")}

   :straight-looks {:title             "Choose from these Straight looks"
                    :description       (str "We have an amazing selection of inspo to choose from.")
                    :button-copy       "View this look"
                    :back-copy         "back to looks"
                    :short-name        "look"
                    :seo-title         "Shop Straight Looks | Mayvenn"
                    :og-title          (str "Shop Straight Looks - "
                                            "Find and Buy your favorite Mayvenn bundles!")}

   :straight-bundle-sets {:title             "Our fave Straight Bundle Sets"
                          :description       "The best combinations, period"
                          :button-copy       "View this Bundle Set"
                          :back-copy         "back to bundle sets"
                          :short-name        "bundle set"
                          :seo-title         "Shop Straight Bundle Sets | Mayvenn"
                          :og-title          (str "Shop Straight Bundle Sets - "
                                                  "Find and Buy your favorite Mayvenn bundles!")}

   :wavy-curly-bundle-sets {:title             "Our fave Wavy and Curly Bundle Sets"
                            :description       "The best combinations, period"
                            :button-copy       "View this Bundle Set"
                            :back-copy         "back to bundle sets"
                            :short-name        "bundle set"
                            :seo-title         "Shop Wavy and Curly Bundle Sets | Mayvenn"
                            :og-title          (str "Shop Wavy and Curly Bundle Sets - "
                                                    "Find and Buy your favorite Mayvenn bundles!")}

   :all-bundle-sets {:title             "Our fave Bundle Sets"
                     :description       "The best combinations, period"
                     :button-copy       "View this Bundle Set"
                     :back-copy         "back to bundle sets"
                     :short-name        "bundle set"
                     :seo-title         "Shop Wavy and Curly Bundle Sets | Mayvenn"
                     :og-title          (str "Shop Wavy and Curly Bundle Sets - "
                                             "Find and Buy your favorite Mayvenn bundles!")}})
(def albums
  #{:sleek-and-straight    ;; Aladdin Home - Straight
    :waves-and-curly       ;; Aladdin Home - Wavy
    :free-install-mayvenn  ;; Aladdin & Shop Home - #mayvennfreeinstall
    :deals
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

    ;; adventure
    :shop-by-look-straight
    :shop-by-look-loose-wave
    :shop-by-look-body-wave
    :shop-by-look-deep-wave
    :bundle-sets-straight
    :bundle-sets-yaki-straight
    :bundle-sets-kinky-straight
    :bundle-sets-loose-wave
    :bundle-sets-body-wave
    :bundle-sets-deep-wave
    :bundle-sets-water-wave
    :bundle-sets-curly

    ;; Shop bundle sets
    :all-bundle-sets
    :straight-bundle-sets
    :wavy-curly-bundle-sets})

(defn determine-look-album
  [data target-album-keyword]
  (let [actual-album (cond
                       (not= target-album-keyword :look)
                       target-album-keyword

                       (or (and (= (get-in data keypaths/store-slug) "shop"))
                           (experiments/aladdin-experience? data))
                       :aladdin-free-install

                       :elsewise target-album-keyword)]
    (get albums actual-album :ugc/unknown-album)))
