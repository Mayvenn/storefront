(ns homepage.ui.email-capture
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent organism
  [{:email-capture.text-field/keys [keypath errors email focused] :as data} _ _]
  [:div.flex-on-tb-dt
   (ui/img {:src   "//ucarecdn.com/a6fc2289-5688-44d0-94b5-5961867dab13/"
            :style {:object-fit "cover"}
            :class "block col-6-on-tb-dt"
            })
   #_[:div
    "The image"]
   [:div.p6
    {:style {:background "linear-gradient(90deg, #D5C2B4, #E9E1DC)"}}
    [:div.title-2.canela
     "Become a Mayvenn."]
    [:div
     "Get the scoop on launches, promos, and occasional spiced cup of tea. Unsubscribe at any time."]
    [:form.col-8-on-tb-dt.center.px1.my4
     {:on-submit
      (apply utils/send-event-callback (:email-capture.submit/target data))}
     [:div.px3
      [:div.mx-auto.mb3
       (spice.core/spy data)
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
      [:div.mb5 (ui/submit-button-medium "Make me an expert" {:data-test "homepage-email-cta"})]]]]])
