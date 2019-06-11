(ns storefront.config)

(def environment js/environment)

(def welcome-subdomain "welcome")

(def freeinstall-subdomain "freeinstall")

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

(def enable-loader?
  true
  #_(not= "development" js/environment))

(def telligent-community-secured?
  (= "production" js/environment))

(def api-base-url js/apiUrl)

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

(def spreedly-key
  (case js/environment
    "production" "4wcpGjpNjX7C3tEYUN9LJHZSdU2"
    "cGBXs4oevFmesd7gWv0DY95KRE"))

(def convert-goals
  (case js/environment
    "production"
    {"view-categories"     "100016254"
     "view-category"       "100016255"
     "place-order"         "100016055"
     "revenue"             "100016054"
     "checkout"            "100017136"
     "paypal-checkout"     "100017137"}
    {"view-categories"     "100016257"
     "view-category"       "100016256"
     "place-order"         "100016047"
     "revenue"             "100016046"
     "checkout"            "100017132"
     "paypal-checkout"     "100017133"}))

(def google-analytics-property (case js/environment
                                 "production" "UA-36226630-1"
                                 "UA-36226630-2"))

(def facebook-pixel-id (case js/environment
                         "production" "721931104522825"
                         "139664856621138"))

(def twitter-pixel-id (case js/environment
                        "production" "o1tn1"
                        "TEST"))

(def stripe-publishable-key (case js/environment
                              "production" "pk_live_S8NS2f14rDQz9USq5Gu9qBnR"
                              "pk_test_cc749q2i3rIK5Kvhbtesy1Iu"))

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

(def pixlee
  (case js/environment
    "production" {:api-key                "PUTXr6XBGuAhWqoIP4ir"
                  :mayvenn-made-widget-id 1048394}
    {:api-key                "iiQ27jLOrmKgTfIcRIk"
     :mayvenn-made-widget-id 1057225}))

(def manual-experiments
  (case js/environment
    "production"
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}))

(def voucherify
  (assoc (case js/environment
           "production" {:client-app-id    "a23e1aef-c484-4c02-9de0-efdc34d95f9c"
                         :client-app-token "abe193c2-0147-4f5b-bd85-3a3ce159e150"}
           {:client-app-id    "8797ea1c-509d-4569-959a-8f27c2432195"
            :client-app-token "bc631e80-99dd-4e30-b2b1-a366c842550a"})
         :base-url "https://api.voucherify.io/client/v1"))
