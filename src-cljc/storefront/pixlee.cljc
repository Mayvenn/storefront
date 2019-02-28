(ns storefront.pixlee
  (:require [storefront.events :as events]))

;; Looking for adventure? See adventure.select-new-look/album-keyword->prompt-image
(def ^:private pixlee-copy
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

(defn pixlee-config [environment]
  (case environment
    "production" {:api-key                "PUTXr6XBGuAhWqoIP4ir"
                  :copy                   pixlee-copy
                  :mayvenn-made-widget-id 1048394

                  :albums {:sleek-and-straight         3892338 ;; Aladdin Home - Straight
                           :waves-and-curly            3892339 ;; Aladdin Home - Wavy
                           :free-install-mayvenn       3896653 ;; Aladdin Home - #mayvennfreeinstall
                           :deals                      2585224
                           :email-deals                3130480
                           :aladdin-free-install       3923024 ;; rename to free-install "Aladdin shop by look"
                           :look                       952508  ;; Shop by Look on classic/non-aladdin

                           ;; product detail page
                           :straight                   1104027
                           :loose-wave                 1104028
                           :body-wave                  1104029
                           :deep-wave                  1104030
                           :curly                      1104031
                           :closures                   1104032
                           :frontals                   1104033
                           :kinky-straight             1700440
                           :water-wave                 1814288
                           :yaki-straight              1814286
                           :dyed                       2750237
                           :wigs                       1880465

                           ;; adventure
                           :shop-by-look-straight      4374434
                           :shop-by-look-loose-wave    4374435
                           :shop-by-look-body-wave     4374437
                           :shop-by-look-deep-wave     4374436
                           :bundle-sets-straight       4374438
                           :bundle-sets-yaki-straight  4374445
                           :bundle-sets-kinky-straight 4374446
                           :bundle-sets-loose-wave     4374439
                           :bundle-sets-body-wave      4374441
                           :bundle-sets-deep-wave      4374442
                           :bundle-sets-water-wave     4374443
                           :bundle-sets-curly          4374444}}

    {:api-key                "iiQ27jLOrmKgTfIcRIk"
     :copy                   pixlee-copy
     :mayvenn-made-widget-id 1057225
     :albums                 {:sleek-and-straight         3892340
                              :waves-and-curly            3892341
                              :free-install-mayvenn       3896655
                              :deals                      3091419
                              :email-deals                3130242
                              :aladdin-free-install       3923023
                              :look                       965034
                              :straight                   1327330
                              :loose-wave                 1327331
                              :body-wave                  1327332
                              :deep-wave                  1327333
                              :curly                      1331955
                              :closures                   1331956
                              :frontals                   1331957
                              :kinky-straight             1801984
                              :water-wave                 1912641
                              :yaki-straight              1912642
                              :dyed                       2918644
                              :wigs                       2918645
                              :shop-by-look-straight      4374415
                              :shop-by-look-loose-wave    4374417
                              :shop-by-look-body-wave     4374418
                              :shop-by-look-deep-wave     4374422
                              :bundle-sets-straight       4374398
                              :bundle-sets-yaki-straight  4374399
                              :bundle-sets-kinky-straight 4374400
                              :bundle-sets-loose-wave     4374408
                              :bundle-sets-body-wave      4374407
                              :bundle-sets-deep-wave      4374411
                              :bundle-sets-water-wave     4374413
                              :bundle-sets-curly          4374414}}))
