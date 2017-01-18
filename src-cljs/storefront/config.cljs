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
                               "production" "https://stylist.mayvenn.com/index.php/new-community-transition-notice/"
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
    "production" {:api-key    "PUTXr6XBGuAhWqoIP4ir"
                  :mosaic     {:albumId 952508}}
    {:api-key    "iiQ27jLOrmKgTfIcRIk"
     :mosaic     {:albumId 965034}}))

(def manual-experiments
  (case js/environment
    "production"
    ;; TODO: To enable experiment on production, uncomment experiment convert
    ;; id, variations, and deploy.
    ;; DO NOT define the variations until you want to deploy - it will corrupt
    ;; Convert and keep too many users out of the experiment
    {"address-login" {:convert-id nil #_"100013592"
                      :variations [#_{:name "original" :convert-id "100079506"}
                                   #_{:name "variation" :convert-id "100079507" :feature "address-login"}]}}

    {"address-login" {:convert-id "100013593"
                      :variations [{:name "original" :convert-id "100079508"}
                                   {:name "variation" :convert-id "100079509" :feature "address-login"}]}}))
