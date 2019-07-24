(ns design-system.adventure
  (:require [adventure.organisms.call-out-center :as call-out-center]
            [adventure.stylist-matching.stylist-profile :as stylist-profile]
            [storefront.component :as component]
            [design-system.organisms :as organisms]
            [storefront.events :as events]))

(def nowhere events/navigate-design-system-adventure)

(def organisms
  [{:organism/label     :call-out-center
    :organism/component call-out-center/organism
    :organism/query     {:call-out-center/bg-class    "bg-lavender"
                         :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
                         :call-out-center/title       "Call Out Centered Title"
                         :call-out-center/subtitle    "Subtitle"
                         :cta/id                      "call-out-center"
                         :cta/target                  nowhere
                         :cta/label                   "Call To Action"
                         :react/key                   :call-out-center}}
   {:organism/label     :stylist-profile
    :organism/component stylist-profile/component
    :organism/query     {;; :gallery-modal-data {:ucare-img-urls                 nil #_ ["https://ucarecdn.com/41d4fb30-fa4c-45d9-907a-ce83964b7caf/"] ;; empty hides the modal
                         ;;                      :initially-selected-image-index 0
                         ;;                      :close-button                   {:target-message events/control-adventure-stylist-gallery-close}}

                         :transposed-title/id        "stylist-name"
                         :transposed-title/primary   "Stylist Name"
                         :transposed-title/secondary "Salon Name"
                         :rating/value               4.5
                         :phone-link/target          [nowhere {:stylist-id   0
                                                               :phone-number "555-333-4444"}]
                         :phone-link/phone-number    "(555) - 333 - 4444"
                         :cta/id                     "select-stylist"
                         :cta/label                  "Select Stylist"
                         :cta/target                 [nowhere {:arg 1}]
                         :circle-portrait/ucare-id   "6a221a42-9a1f-4443-8ecc-595af233ab42"
                         :carousel/items             [{:key            "gallery-image-1"
                                                       :ucare-img-url  "https://ucarecdn.com/41d4fb30-fa4c-45d9-907a-ce83964b7caf/"
                                                       :target-message [nowhere
                                                                        {:ucare-img-urls                 ["https://ucarecdn.com/41d4fb30-fa4c-45d9-907a-ce83964b7caf/"]
                                                                         :initially-selected-image-index 0}]}]
                         :details [{:section-details/title "Detail A"
                                    :section-details/content "x, y, z"}
                                   {:section-details/title "Specialties"
                                    :section-details/content [:div.mt1.col-12.col.regular
                                                              [:div.col-4.col
                                                               (stylist-profile/checks-or-x "A" false)
                                                               (stylist-profile/checks-or-x "B" true)]
                                                              [:div.col-4.col
                                                               (stylist-profile/checks-or-x "C" true)
                                                               (stylist-profile/checks-or-x "D" false)]]}]

                         :profile-card-data          {:card-data {:image-url         "https://ucarecdn.com/6a221a42-9a1f-4443-8ecc-595af233ab42/"
                                                                  :detail-line       "details details"
                                                                  :detail-attributes ["comma" "separated" "list"]
                                                                  :specialties       {:specialty-sew-in-leave-out   true
                                                                                      :specialty-sew-in-closure     true
                                                                                      :specialty-sew-in-frontal     false
                                                                                      :specialty-sew-in-360-frontal false}}}}}])

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Adventure Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section
      (organisms/demo organisms)]]]))

(defn built-component
  [data opts]
  (component/build component data nil))
