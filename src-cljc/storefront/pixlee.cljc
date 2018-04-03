(ns storefront.pixlee)

(def ^:private pixlee-copy
  {:deals        {:title             "shop deals"
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
   :look         {:title             "shop by look"
                  :description       (str "Get inspiration for your next hairstyle and "
                                          "shop your favorite looks from the #MayvennMade community.")
                  :button-copy       "View this look"
                  :back-copy         "back to shop by look"
                  :above-button-copy nil
                  :short-name        "look"
                  :seo-title         "Shop by Look | Mayvenn"
                  :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}
   :free-install {:title             "shop by look"
                  :description       (str "Get inspiration for your next hairstyle and "
                                          "shop your favorite looks from the #MayvennMade community.")
                  :button-copy       "View this look"
                  :back-copy         "back to shop by look"
                  :above-button-copy nil
                  :short-name        "look"
                  :seo-title         "Shop by Look | Mayvenn"
                  :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}
   :email-deals  {:title             "shop by look"
                  :description       (str "Grab the latest bundle deal! "
                                          "Below you can shop every bundle deal of the week.")
                  :button-copy       "View this look"
                  :back-copy         "back to shop by look"
                  :above-button-copy nil
                  :short-name        "look"
                  :seo-title         "Shop by Look | Mayvenn"
                  :og-title          "Shop by Look - Find and Buy your favorite Mayvenn bundles!"}})

(defn pixlee-config [environment]
  (case environment
    "production" {:api-key "PUTXr6XBGuAhWqoIP4ir"
                  :copy    pixlee-copy
                  :albums  {:look            952508
                            :free-install    3082797
                            :deals           3091418
                            :email-deals     3130480
                            "straight"       1104027
                            "loose-wave"     1104028
                            "body-wave"      1104029
                            "deep-wave"      1104030
                            "curly"          1104031
                            "closures"       1104032
                            "frontals"       1104033
                            "kinky-straight" 1700440
                            "water-wave"     1814288
                            "yaki-straight"  1814286
                            "dyed"           2750237
                            "wigs"           1880465}}
    {:api-key "iiQ27jLOrmKgTfIcRIk"
     :copy    pixlee-copy
     :albums  {:look            965034
               :free-install    3082796
               :deals           3091419
               :email-deals     3130242
               "straight"       1327330
               "loose-wave"     1327331
               "body-wave"      1327332
               "deep-wave"      1327333
               "curly"          1331955
               "closures"       1331956
               "frontals"       1331957
               "kinky-straight" 1801984
               "water-wave"     1912641
               "yaki-straight"  1912642
               "dyed"           2918644
               "wigs"           2918645}}))
