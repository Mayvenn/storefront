(ns storefront.config)

(def environment js/environment)

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

(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")

(def review-tag-url (case js/environment
                      "production" "//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js"
                      "//staticw2.yotpo.com/2UyuTzecYoIe4JECHCuqP6ClAOzjnodSFMc7GEuT/widget.js"))

(def telligent-community-url (case js/environment
                               "production" "https://community.mayvenn.com"
                               "http://telligentps.mayvenn.com/"))

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

(def stripe-publishable-key (case js/environment
                              "production" "pk_live_S8NS2f14rDQz9USq5Gu9qBnR"
                              "pk_test_cc749q2i3rIK5Kvhbtesy1Iu"))

(def uploadcare-public-key
  (case js/environment
    "production" "6a68fb24fd9c50c7ed8b"
    "a0c937dbb5a75e3c7937"))

(def sift-api-key
  (case js/environment
    "production" "269464bc68"
    "bbe6fafe11"))

(def facebook-app-id (case js/environment
                       "production" 1536288310021691
                       "acceptance" 1537350883248767
                       1539908182993037))

(def places-api-key
  (case js/environment
    "production" "AIzaSyBaMJMMqq4gygvlO9J_BO_mZ9t86XEZ4EA"
    "acceptance" "AIzaSyA25Ehwf5yqYjAVfzYeAT5VEfqsKZjVbKY"
    "AIzaSyBF1WsIRs4wIRTEsnNi8Klynxtxqz5RoIA"))

(def pixlee
  (case js/environment
    "production" {:api-key "PUTXr6XBGuAhWqoIP4ir"
                  :albums  {:mosaic          952508
                            "straight"       1104027
                            "loose-wave"     1104028
                            "body-wave"      1104029
                            "deep-wave"      1104030
                            "curly"          1104031
                            "closures"       1104032
                            "frontals"       1104033
                            "kinky-straight" 1700440}}
    {:api-key "iiQ27jLOrmKgTfIcRIk"
     :albums  {:mosaic      965034
               "straight"   1327330
               "loose-wave" 1327331
               "body-wave"  1327332
               "deep-wave"  1327333
               "curly"      1331955
               "closures"   1331956
               "frontals"   1331957}}))

(def manual-experiments
  (case js/environment
    "production"
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}))
