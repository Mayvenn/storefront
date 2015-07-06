(ns storefront.config)

(def environment js/environment)
(def development? (= js/environment "development"))

(def api-base-url js/apiUrl)
                    
                    
                    

(def send-sonar-base-url "https://www.sendsonar.com/api/v1")
(def send-sonar-publishable-key "d7d8f2d0-9f91-4507-bc82-137586d41ab8")

(def honeybadger-api-key "b0a4a070")

(def optimizely-app-id (case js/environment
                         "production" 1234341
                         3156430062))
