(ns homepage.ui.email-capture
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent organism
  [{:email-capture.text-field/keys [keypath errors email focused] :as data} _ _]
  [:div.flex-on-tb-dt
   {:style {:background "#D5C2B4"}}
   (ui/img {:src        "//ucarecdn.com/cbf1bb83-0ebc-4691-9c61-de6af79e6f8d/"
            :style      {:object-fit "cover"}
            :smart-crop "600x400"
            :class      "block col-12 col-6-on-tb-dt"})
   [:div.p6
    {:style {:background "linear-gradient(90deg, #D5C2B4, #E9E1DC)"}}
    [:div.title-2.canela
     "Stay In The Know"]
    [:div
     "Get the tea on the latest promos, product launches, and exclusive hair content."]
    [:form.col-8-on-tb-dt.center.px1.my4
     {:on-submit
      (apply utils/send-event-callback (:email-capture.submit/target data))}
     [:div.px3
      [:div.mx-auto.mb3
       (ui/text-field {:errors    (get errors ["email"])
                       :keypath   keypath
                       :focused   focused
                       :label     "Enter your Email"
                       :name      "homepage-email"
                       :required  true
                       :type      "email"
                       :value     email
                       :class     "col-12 bg-white"
                       :data-test "homepage-email"})]
      [:div.mb5 (ui/submit-button-medium "Become a Mayvenn" {:data-test "homepage-email-cta"})]]]]])
