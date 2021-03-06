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

(defn <-
  [state id]
  (when-let [questions (case id
                         :unified-freeinstall db-unnamed-v1
                         :unnamed-v1          db-unnamed-v1)]
    (let [progression (progression/<- state
                          (keyword "questionings" id))]
      {:questioning/id id
       :questioning/unanswered (- (count questions)
                                  (count progression))
       :questions      questions
       :answers        (get-in state
                               (conj k/models-questionings id))
       :progression    progression})))

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
  [_ _ {question-idx :question/idx
        choice-idx   :choice/idx} state]
  (let [questions                         (:questions (<- state :unnamed-v1))
        {:question/keys [prompt choices]} (get questions question-idx)
        {:choice/keys [answer]}           (get choices choice-idx)]
    (->> {:question_copy          (join " " prompt)
          :answer_option_selected answer
          :question_position      question-idx
          :answer_position        choice-idx}
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
