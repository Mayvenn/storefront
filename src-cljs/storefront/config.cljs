(ns storefront.config)

(def environment js/environment)
(def production? (= js/environment "production"))
(def development? (= js/environment "development"))
(def acceptance? (= js/environment "acceptance"))

(def allowed-version-drift
  "The number of versions that storefront is allowed to fall behind before requiring a refresh"
  0)

(def api-base-url js/apiUrl)

(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")

(def review-tag-url (case js/environment
                      "production" "//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js"
                      "//staticw2.yotpo.com/2UyuTzecYoIe4JECHCuqP6ClAOzjnodSFMc7GEuT/widget.js"))

(def talkable-script
  (case js/environment
    "production" "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn.min.js"
    "https://d2jjzw81hqbuqv.cloudfront.net/integration/clients/mayvenn-staging.min.js"))

(def optimizely-app-id (case js/environment
                         "production" 592210561
                         3156430062))

(def canonical-image (if development?
                       js/canonicalImage
                       (str "https:" js/canonicalImage)))

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

(def sonar-facebook-app-id 165304437179658)

(def places-api-key
  (case js/environment
    "production" "AIzaSyBaMJMMqq4gygvlO9J_BO_mZ9t86XEZ4EA"
    "acceptance" "AIzaSyA25Ehwf5yqYjAVfzYeAT5VEfqsKZjVbKY"
    "AIzaSyBF1WsIRs4wIRTEsnNi8Klynxtxqz5RoIA"))
