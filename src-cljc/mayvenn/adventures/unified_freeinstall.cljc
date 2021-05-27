(ns mayvenn.adventures.unified-freeinstall
  "Visual Layer: Unified-Free Install adventure

  Product is also calling this 'shopping quiz'
  We should argue that this is confusing."
  (:require mayvenn.adventures.core
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.actions :as actions]
            [storefront.component :as c]))

;; TODO Fix other quiz adventure
;;      - The domain scope spread too far
;;      - This visual layer will need quizzing
;; TODO Plan for bundle modularization
;; TODO Plan for adventuring domain
;; TODO Get product to agree to our terminology on:
;;      quiz, adventure, shopping, etc


(c/defcomponent intro-template
  [_ _ _]
  [:div.bg-pale-purple.stretch.ptj3
   [:div.col-10.mx-auto
    (titles/canela-huge {:icon      [:svg/heart {:style {:height "41px"
                                                         :width  "37px"}
                                                 :class "fill-p-color"}]
                         :primary   ["Hair + Service"
                                     "One Price"]
                         :secondary "This short quiz (2-3 minutes) will help you find the look and a stylist to complete your install in your area"})
    [:div.flex.justify-center
     (actions/action-molecule
      {:id    "quiz-continue"
       :label "Continue"
       :target []})]]])

(defn ^:export page
  [state]
  (c/build intro-template {}))
