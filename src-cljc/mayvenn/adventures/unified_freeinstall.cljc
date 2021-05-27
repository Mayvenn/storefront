(ns mayvenn.adventures.unified-freeinstall
  "
  Visual Layer: Unified-Free Install adventure

  Product is also calling this 'shopping quiz' because that
  is the feature-slot in the customers' experience.
  "
  (:require [mayvenn.adventures.core :as adventures]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.actions :as actions]
            [storefront.events :as e]
            [storefront.component :as c]))

(def ^:private adventure-id
  :unified-freeinstall)

(def ^:private adventure-steps #{0 1})

(def ^:private adventure-initial-model
  #:adventure{:step 0})

;; TODO Fix other quiz adventure
;;      - The domain scope spread too far
;;      - This visual layer will need quizzing
;; TODO Plan for bundle modularization
;; TODO Plan for adventuring domain
;; TODO Get product to agree to our terminology on:
;;      quiz, adventure, shopping, etc

(c/defcomponent questions-template
  [_ _ _]
  [:div.bg-pale-purple.stretch.ptj3
   [:div.col-10.mx-auto
    [:div "quiz"]]])

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
      {:id     "quiz-continue"
       :label  "Continue"
       :target [e/flow|adventure|advanced
                #:adventure
                {:id   adventure-id
                 :step 1}]})]]])

(defn ^:export page
  "
  Shopping Quiz: Unified Products+Service v1

  An adventure for helping customers find hair products for a look
  combined with picking a stylist that can do that look.
  "
  [state]
  (let [{:adventure/keys [step]} (adventures/<- state
                                                adventure-initial-model
                                                adventure-id)]
    (case step
      1 (c/build questions-template {})
      ;; default or 0
      (c/build intro-template {}))))
