(ns storefront.ugc
  (:require [storefront.events :as events]
            [storefront.accessors.experiments :as experiments]))

;; Looking for adventure? See adventure.select-new-look/album-keyword->prompt-image
(def album-copy
  {:deals {:title             "shop deals"
           :description       (str "Save more when you bundle up! "
                                   "We wrapped our most popular textures into "
                                   "packaged bundle deals so you can shop with ease.")
           :button-copy       "View this deal"
           :back-copy         "back to deals"
           :above-button-copy "*Discounts applied at check out"
           :short-name        "deal"
           :seo-title         "Shop Deals | Mayvenn"
           :og-title          (str "Shop Deals - "
                                   "Find and Buy your favorite Mayvenn bundles!")}
   :look  {:title             "shop by look"
           :description       (str "Get inspiration for your next hairstyle and "
                                   "shop your favorite looks from the #MayvennMade community.")
           :back-copy         "back to shop by look"
           :button-copy       "Shop Look"
           :above-button-copy nil
           :short-name        "look"
           :seo-title         "Shop by Look | Mayvenn"
           :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}

   ;;TODO Rename aladdin-free-install album
   :aladdin-free-install {:title             "shop by look"
                          :description       (str "Get inspiration for your next hairstyle and "
                                                  "shop your favorite looks from the #MayvennMade community.")
                          :back-copy         "back to shop by look"
                          :button-copy       "Shop Look"
                          :above-button-copy nil
                          :short-name        "look"
                          :seo-title         "Shop by Look | Mayvenn"
                          :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}


   ;;TODO Get proper copy
   :sleek-and-straight {:title              "shop by look"
                        :description        (str "Get inspiration for your next hairstyle and "
                                                 "shop your favorite looks from the #MayvennMade community.")
                        :button-copy        "Shop Look"
                        :back-copy          "back"
                        :default-back-event events/navigate-home
                        :above-button-copy  nil
                        :short-name         "look"
                        :seo-title          "Shop by Look | Mayvenn"
                        :og-title           "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}

   ;;TODO Get proper copy
   :waves-and-curly {:title              "shop by look"
                     :description        (str "Get inspiration for your next hairstyle and "
                                              "shop your favorite looks from the #MayvennMade community.")
                     :button-copy        "Shop Look"
                     :back-copy          "back"
                     :default-back-event events/navigate-home
                     :above-button-copy  nil
                     :short-name         "look"
                     :seo-title          "Shop by Look | Mayvenn"
                     :og-title           "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}

   :email-deals {:title             "shop by look"
                 :description       (str "Grab the latest bundle deal! "
                                         "Below you can shop every bundle deal of the week.")
                 :button-copy       "Shop Look"
                 :back-copy         "back to shop by look"
                 :above-button-copy nil
                 :short-name        "look"
                 :seo-title         "Shop by Look | Mayvenn"
                 :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}})
(def albums
  #{:sleek-and-straight    ;; Aladdin Home - Straight
    :waves-and-curly       ;; Aladdin Home - Wavy
    :free-install-mayvenn  ;; Aladdin Home - #mayvennfreeinstall
    :deals
    :email-deals
    :aladdin-free-install  ;; rename to free-install "Aladdin shop by look"
    :look                   ;; Shop by Look on classic/non-aladdin

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
    :bundle-sets-curly})

(defn determine-look-album
  [data target-album-keyword]
  (let [actual-album (cond
                       (not= target-album-keyword :look)
                       target-album-keyword

                       (experiments/aladdin-experience? data)
                       :aladdin-free-install

                       :elsewise target-album-keyword)]
    (get albums actual-album :ugc/unknown-album)))
