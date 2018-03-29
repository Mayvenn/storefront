(ns storefront.config)

(def environment js/environment)

(def welcome-subdomain "welcome")

(def feature-block-look-ids
  ;;NOTE edit the clj config too!
  ;;NOTE @Ryan, please only change the top map
  (if (= environment "production")
    {:left  191567494
     :right 191567299}
    {:left  144863121
     :right 144863121}))

(def report-errors?
  (not= "development" js/environment))

(def secure?
  (not= "development" js/environment))

(def enable-console-print?
  (= "development" js/environment))

(def enable-style-guide?
  (not= "production" js/environment))

(def telligent-community-secured?
  (= "production" js/environment))

(def allowed-version-drift
  "The number of versions that storefront is allowed to fall behind before requiring a refresh"
  0)

(def api-base-url js/apiUrl)

(def mayvenn-leads-call-number "1-866-424-7201")
(def mayvenn-leads-a1-call-number "1-510-867-3439")
(def mayvenn-leads-sms-number  "34649")

(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")

(def review-tag-url (case js/environment
                      "production" "//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js"
                      "//staticw2.yotpo.com/2UyuTzecYoIe4JECHCuqP6ClAOzjnodSFMc7GEuT/widget.js"))

(def telligent-community-url (case js/environment
                               "production" "https://community.mayvenn.com"
                               "https://community.diva-acceptance.com"))

(def talkable-script
  (case js/environment
    "production" "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn.min.js"
    "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn-staging.min.js"))

(def convert-project-id (case js/environment
                          "production" "10003995-10005092"
                          "10003995-10005089"))

(def convert-goals
  (case js/environment
    "production"
    {"view-categories"     "100016254"
     "view-category"       "100016255"
     "place-order"         "100016055"
     "revenue"             "100016054"
     "apple-pay-checkout"  "100017135"
     "checkout"            "100017136"
     "paypal-checkout"     "100017137"
     "apple-pay-available" "100017138"}
    {"view-categories"     "100016257"
     "view-category"       "100016256"
     "place-order"         "100016047"
     "revenue"             "100016046"
     "apple-pay-checkout"  "100017131"
     "checkout"            "100017132"
     "paypal-checkout"     "100017133"
     "apple-pay-available" "100017134"}))

(def google-analytics-property (case js/environment
                                 "production" "UA-36226630-1"
                                 "UA-36226630-2"))

(def facebook-pixel-id (case js/environment
                         "production" "721931104522825"
                         "139664856621138"))

(def stripe-publishable-key (case js/environment
                              "production" "pk_live_S8NS2f14rDQz9USq5Gu9qBnR"
                              "pk_test_cc749q2i3rIK5Kvhbtesy1Iu"))

(def affirm-public-api-key (case js/environment
                             "production" "BBIXI0YNZMRCG3CX"
                             "5LLKDFV3DLO0FUGE"))
(def affirm-script-uri (case js/environment
                         "production" "https://cdn1.affirm.com/js/v2/affirm.js"
                         "https://cdn1-sandbox.affirm.com/js/v2/affirm.js"))

(def pinterest-tag-id
  (if (not= "production" js/environment)
    2612961581995
    2617847432239))

(def uploadcare-public-key
  (case js/environment
    "production" "6a68fb24fd9c50c7ed8b"
    "a0c937dbb5a75e3c7937"))

(def facebook-app-id (case js/environment
                       "production" 1536288310021691
                       "acceptance" 1537350883248767
                       1539908182993037))

(def places-api-key
  (case js/environment
    "production" "AIzaSyBaMJMMqq4gygvlO9J_BO_mZ9t86XEZ4EA"
    "acceptance" "AIzaSyA25Ehwf5yqYjAVfzYeAT5VEfqsKZjVbKY"
    "AIzaSyBF1WsIRs4wIRTEsnNi8Klynxtxqz5RoIA"))

(def ^:private pixlee-copy
  {:deals  {:back-copy       "back to deals"
            :short-name      "deal"
            :button-copy     "View this deal"
            :seo-title       "Shop Deals | Mayvenn"
            :seo-description "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."
            :og-title        "Shop Deals - Find and Buy your favorite Mayvenn bundles!"
            :og-description  "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}
   :mosaic {:short-name      "look"
            :button-copy     "View this look"
            :back-copy       "back to shop by look"
            :seo-title       "Shop by Look | Mayvenn"
            :seo-description "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."
            :og-title        "Shop By Look - Find and Buy your favorite Mayvenn hairstyle!"
            :og-description  "Find your favorite Mayvenn hairstyle on social media and shop the exact look directly from our website."}})

(def pixlee
  (case js/environment
    "production" {:api-key "PUTXr6XBGuAhWqoIP4ir"
                  :copy    pixlee-copy
                  :albums  {:mosaic          952508
                            :free-install    3082797
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
                            "wigs"           1880465
                            :deals           3091418}}
    {:api-key "iiQ27jLOrmKgTfIcRIk"
     :copy    pixlee-copy
     :albums  {:mosaic          965034
               :free-install    3082796
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
               "wigs"           2918645
               :deals           3091419}}))

(def manual-experiments
  (case js/environment
    "production"
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}))
