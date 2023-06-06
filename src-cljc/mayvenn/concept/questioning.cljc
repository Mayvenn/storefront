(ns mayvenn.concept.questioning
  "
  Biz layer: Questionings

  Question and Answers, basically, with a worse name.

  Questions are prompts in db, combined into question-sets
  - They have multiple choices

  Answers are the user's answers to the questions

  Progression in answering is also tracked.
  "
  (:require #?@(:cljs
                [[storefront.hooks.stringer :as stringer]
                 storefront.frontend-trackings])
            [clojure.string :refer [join]]
            [mayvenn.concept.progression :as progression]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.transitions :as t]
            [storefront.trackings :as trk]))

(def db-unnamed-v1
  "Questions focused on selecting between some predefined looks (vec of skus)"
  [{:question/id     :texture
    :question/prompt ["Let's talk about texture."
                      "What do you want your final look to be?"]
    :question/choices
    [{:choice/id      :straight
      :choice/answer  "Straight"
      :choice/img-url "/images/categories/straight-icon.svg"}
     {:choice/id      :loose-wave
      :choice/answer  "Loose wave"
      :choice/img-url "/images/categories/loose-wave-icon.svg"}
     {:choice/id      :body-wave
      :choice/answer  "Body wave"
      :choice/img-url "/images/categories/body-wave-icon.svg"}
     {:choice/id     :unsure
      :choice/answer "I'm not sure yet" }]}
   {:question/id     :length
    :question/prompt ["What about length?"]
    :question/choices
    [{:choice/id     :short
      :choice/answer "Short 10\" to 14\""}
     {:choice/id     :medium
      :choice/answer "Medium 14\" to 18\""}
     {:choice/id     :long
      :choice/answer "Long 18\" to 22\""}
     {:choice/id     :extra-long
      :choice/answer "Extra Long 22\" to 26\""}
     {:choice/id     :unsure
      :choice/answer "I'm not sure yet"}]}
   {:question/id     :leave-out
    :question/prompt ["Would you like to leave any of your natural hair out?"]
    :question/info   ["Leave-out covers the tracks of a sew-in and blends your natural hair with the extensions."]
    :question/choices
    [{:choice/id     :yes
      :choice/answer "Yes"}
     {:choice/id     :no
      :choice/answer "No"}
     {:choice/id     :unsure
      :choice/answer "I'm not sure yet"}]}])

(def crm-persona
  "Questions focused on understanding customer needs and segments"
  [[:customer/goals "What are your top hair goals? (Pick up to two)"
    [[:customer.goals/enhance-natural "Enhance my natural hair (add length and volume)"]
     [:customer.goals/protect-natural "Protect my natural hair"]
     [:customer.goals/save-money "Save money"]
     [:customer.goals/easy-maintenance "Low maintenance"]
     [:customer.goals/easy-install "Easy to install"]
     [:unsure "Not Sure"]]]
   [:customer/styles "What type of styles are you most interested in? (Pick one)"
    [[:customer.styles/everyday-look "My everyday look"]
     [:customer.styles/special-occasion "I'm shopping for a special occasion"]
     [:customer.styles/vacation "A vacation look"]
     [:customer.styles/work "A style for work"]
     [:customer.styles/switch-it-up "I want to switch it up"]
     [:unsure "I'm not sure. Surprise me!"]]]
   [:id3 "How much of your natural hair do you want to leave out?"
    [[:id3a "All"]
     [:id3b "Some"]
     [:id3c "None or Not Sure"]]]
   [:id4 "Do you want the option to remove your style at night?"
    [[:id4a "Leave It In"]
     [:id4b "Remove"]
     [:id4c "I don't know"]]]
   [:id5 "Do you prefer a low maintenance style, or are you flexible?"
    [[:id5a "Low Maintenance"]
     [:id5b "I don't know"]
     [:id5c "Flexible"]]]
   [:id6 "Finally, would you like to be able to put your hair up?"
    [[:id6a "Yes"]
     [:id6b "I don't know"]
     [:id6c "No"]]]])

(defn- inflate-quiz
  [quiz-data]
  (letfn [(inflate-choices [choices]
            (mapv (fn [[id answer]]
                    {:choice/id     id
                     :choice/answer answer})
                  choices))
          (inflate-question [[id prompt choices]]
            {:question/id     id
             :question/prompt [prompt]
             :question/choices (inflate-choices choices)})]
    (mapv inflate-question quiz-data)))

(defn <-
  [state id]
  (when-let [questions (case id
                         :unified-freeinstall db-unnamed-v1
                         :unnamed-v1          db-unnamed-v1
                         :crm/persona         (inflate-quiz crm-persona))]
    (let [progression (progression/<- state
                                      (keyword "questionings" id))]
      {:questioning/id         id
       :questioning/unanswered (- (count questions) (count progression))
       :questions              questions
       :answers                (get-in state
                                       (conj k/models-questionings id))
       :progression            progression})))

;; Behavior

;;;; Reset

(defmethod t/transition-state e/biz|questioning|reset
  [_ _ {:questioning/keys [id]} state]
  (-> state
      (assoc-in (conj k/models-questionings id)
                nil)))

(defmethod fx/perform-effects e/biz|questioning|reset
  [_ _ {:questioning/keys [id]} _ _]
  (publish e/biz|progression|reset
           {:progression/id    (keyword "questionings" id)
            :progression/value #{}}))

(defmethod t/transition-state e/biz|questioning|answered
  [_ _ {questioning-id :questioning/id
        question-id    :question/id
        choice-id      :choice/id} state]
  (assoc-in state
            (conj k/models-questionings
                  questioning-id
                  question-id)
            choice-id))

;;;; Answered

(defmethod fx/perform-effects e/biz|questioning|answered
  [_ _ {:questioning/keys [id] :question/keys [idx]} _ _]
  (publish e/biz|progression|progressed
           {:progression/id    (keyword "questionings" id)
            :progression/value idx}))

(defmethod trk/perform-track e/biz|questioning|answered
  [_ _ {question-idx   :question/idx
        questioning-id :questioning/id
        choice-idx     :choice/idx} state]
  (let [{:keys [progression questions]}   (<- state questioning-id)
        {:question/keys [prompt choices]} (get questions question-idx)
        {:choice/keys [answer]}           (get choices choice-idx)]
    (->> {:question_copy          (join " " prompt)
          :questioning_id         questioning-id
          :answer_option_selected answer
          :question_position      question-idx
          :answer_position        choice-idx
          :number_answered        (count progression)
          :total_questions        (count questions)}
         #?(:cljs
            (stringer/track-event "quiz_question_answered")))))

;;;; Submitted

(defmethod fx/perform-effects e/biz|questioning|submitted
  [_ _ {:questioning/keys [id]
        :keys [answers]
        :on/keys [success]} _ state]
  (when (and id success)
    (let [[e args] success]
      (publish e (merge {}
                        args
                        (<- state id)
                        {:answers answers})))))

#?(:cljs
   (defmethod trk/perform-track e/biz|questioning|submitted
     [_ _ {:questioning/keys [id]
           :keys             [answers]} state]
     (stringer/track-event "quiz_question_submitted" (merge
                                                      (<- state id)
                                                      {:answers answers}))))
