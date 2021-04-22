(ns storefront.config)

(def environment js/environment)

(def welcome-subdomain "welcome")

;; TODO: make this load from server side rendering (since config.clj has this)
;; human-readable phone number to call support. Use a helper function to
;; format it properly for "tel:" (see ui/phone-url)
;;
;; Don't forget to update static html pages & config.clj if you change this.
(def support-phone-number "1 (855) 287-6868")

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

(def enable-design-system?
  (not= "production" js/environment))

(def api-base-url js/apiUrl)

(def review-tag-url (case js/environment
                      "production" "//staticw2.yotpo.com/ZmvkoIuVo61VsbHVPaqDPZpkfGm6Ce2kjVmSqFw9/widget.js"
                      "//staticw2.yotpo.com/2UyuTzecYoIe4JECHCuqP6ClAOzjnodSFMc7GEuT/widget.js"))

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

(def facebook-pixel-id (case js/environment
                         "production" "721931104522825"
                         "139664856621138"))

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

(def manual-experiments
  (case js/environment
    "production"
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}
    {"address-login" {:enabled?   false
                      :variations [{:feature "address-login-control"}
                                   {:feature "address-login"}]}}))

(def kustomer-api-key
  (case js/environment
    "production" "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjVkMjRlMjFkZTZhY2E2MDA4ZmY0MTkxZSIsInVzZXIiOiI1ZDI0ZTIxZGQ2ZWExMTAwMTI3ODY5YWIiLCJvcmciOiI1Y2Y2YzNlMmVkODg5YzAwMTI0ZmRkMTMiLCJvcmdOYW1lIjoibWF5dmVubiIsInVzZXJUeXBlIjoibWFjaGluZSIsInJvbGVzIjpbIm9yZy50cmFja2luZyJdLCJhdWQiOiJ1cm46Y29uc3VtZXIiLCJpc3MiOiJ1cm46YXBpIiwic3ViIjoiNWQyNGUyMWRkNmVhMTEwMDEyNzg2OWFiIn0.6DBXhYV5ypGVbMhZGHRTHlHGMZZ9nBh9f3szQh38Lmo"
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjVkMWE1OGVmZWE2NTI3MDA5NjAxYjYyYiIsInVzZXIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYiLCJvcmciOiI1Y2Y2YzcxNTYyNGEwNzAwMTNhZDQ0YzgiLCJvcmdOYW1lIjoibWF5dmVubi1zYW5kYm94IiwidXNlclR5cGUiOiJtYWNoaW5lIiwicm9sZXMiOlsib3JnLnRyYWNraW5nIl0sImF1ZCI6InVybjpjb25zdW1lciIsImlzcyI6InVybjphcGkiLCJzdWIiOiI1ZDFhNThlZTRkNzdhMDAwMWE0MTkwNGYifQ.N_3t02QEEURClNfyBodzPmFSmv60CwdIROXArlVtudE"))

(def kustomer-brand-id
  (case js/environment
    "production" "5daf7912124be4f2960cafca"
    "5daf7912124be4f2960cafcc"))
