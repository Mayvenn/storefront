(ns homepage.ui.email-capture
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(c/defcomponent organism
  [{:keys            [id]
    :text-field/keys [keypath errors email focused submitted-text] :as data} _ _]
  (when id
    [:div.homepage-email-cap
     {:key id}
     [:div.gradient-bar.p6
      {:style {:background "linear-gradient(90deg, #D8D8D8, white)"}}]
     (ui/img {:src   "//ucarecdn.com/cbf1bb83-0ebc-4691-9c61-de6af79e6f8d/"
              :style {:object-fit      "cover"
                      :object-position "50% 35%"}
              :alt   ""
              :class "block container-size"})
     [:div.prompt
      [:div.title-2.canela.mb4
       "Stay in the Know"]
      [:div.mb4
       "Get the tea on the latest promos, product launches, and exclusive hair content."]
      [:form
       {:on-submit (apply utils/send-event-callback (:submit/target data))}
       (ui/text-field {:errors    (get errors ["email"])
                       :keypath   keypath
                       :focused   focused
                       :label     "Enter your Email"
                       :name      "homepage-email"
                       :required  true
                       :type      "email"
                       :value     email
                       :class     "col-12 bg-white"
                       :data-test "homepage-email"})
       (when submitted-text
         [:div submitted-text])
       [:div.pt2 (ui/submit-button-medium "Make me an expert" {:data-test "homepage-email-cta"})]]]]))
