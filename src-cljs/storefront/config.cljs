(ns storefront.config)

(def development? (= js/environment "development"))

(def api-base-url (case js/environment
                    "production" "https://api.mayvenn.com"
                    "acceptance" "https://api.diva-acceptance.com"
                    "http://api.mayvenn-dev.com:3005"))

(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")
