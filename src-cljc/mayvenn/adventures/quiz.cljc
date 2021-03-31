(ns mayvenn.adventures.quiz
  "Visual Layer: Quiz in an adventure setting"
  (:require [storefront.assets :as assets]
            [storefront.component :as c]
            [storefront.components.ui :as ui]))

(c/defcomponent progress-portion-bar-molecule
  [{:progress.portion.bar/keys [units img-url]} _ _]
  [:div {:class (str "col-" units)}
   [:div
    {:style {:background-image    "url('//ucarecdn.com/937451d3-070b-4f2c-b839-4f5b621ef661/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "6px"}}]])

(c/defcomponent progress-organism
  [data _ _]
  [:div.flex
   {:style {:max-height "6px"}}
   (c/elements progress-portion-bar-molecule
               data
               :progress/portions)])

(defn quiz-question-title-molecule
  [{:quiz.question.title/keys [primary]}]
  [:div.canela.title-2
   (interpose [:br] primary)])

(c/defcomponent quiz-question-choice-button-molecule
  [{:quiz.question.choice.button/keys [primary icon-url]} _ _]
  [:div
   (ui/button-choice-unselected
    {:class "my2"}
    [:div.flex.items-center.py2
     {:style {:height "0px"}}
     (when icon-url
       [:img.mr3 {:src   (assets/path icon-url)
              :style {:width "42px"}}])
     [:div.content-2.proxima
      primary]])])

(c/defcomponent quiz-question-organism
  [data _ _]
  [:div.mtj3.mx6
   (quiz-question-title-molecule data)
   [:div.py2
    (c/elements quiz-question-choice-button-molecule
                data
                :quiz.question/choices)]])

(c/defcomponent template
  [{:keys [progress quiz-questions]} _ _]
  [:div
   (c/build progress-organism progress)
   (c/elements quiz-question-organism
                    quiz-questions
                    :quiz/questions)])

(defn quiz-questions<
  []
  {:quiz/questions
   [{:quiz.question.title/primary ["Let's talk about texture."
                                   "Do you want your final look to be:"]
     :quiz.question/choices       [{:quiz.question.choice.button/icon-url "/images/categories/straight-icon.svg"
                                    :quiz.question.choice.button/primary  "Straight"}
                                   {:quiz.question.choice.button/icon-url "/images/categories/water-wave-icon.svg"
                                    :quiz.question.choice.button/primary  "Wavy"}
                                   {:quiz.question.choice.button/icon-url nil
                                    :quiz.question.choice.button/primary  "I'm not sure yet"}]}]})

(defn progress<
  []
  {:progress/portions [{:progress.portion.bar/units   3
                        :progress.portion.bar/img-url "/images/categories/straight-icon.svg"}
                       {:progress.portion.bar/units 9}]})

(defn ^:export page
  [state]
  (->> {:quiz-questions (quiz-questions<)
        :progress       (progress<)}
       (c/build template)))
