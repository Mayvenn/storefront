(ns mayvenn.adventures.quiz
  "Visual Layer: Quiz in an adventure setting"
  (:require #?@(:cljs
                [[storefront.browser.scroll :as scroll]])
            [storefront.assets :as assets]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.transitions :as t]))

;; TODO(corey) parameterize for color
(def checkmark-circle-atom
  [:div.circle.bg-p-color.flex.items-center.justify-center
   {:style {:height "20px" :width "20px"}}
   (svg/check-mark {:height "12px" :width "16px" :class "fill-white"})])

;; TODO(corey) extract (bg-image url)
(c/defcomponent progress-portion-bar-molecule
  [{:progress.portion.bar/keys [units img-url]} _ _]
  [:div {:class (str "col-" units)}
   [:div.bg-cool-gray
    {:style {:background-image    (str "url('" img-url "')")
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

(c/defdynamic-component quiz-question-title-molecule
  (did-mount [this]
             #?(:cljs
                (let [idx (c/get-opts this)]
                  (scroll/scroll-selector-to-top (str "[data-ref=question-" idx "]")))))
  (render [this]
          (let [{:quiz.question.title/keys [primary id]} (c/get-props this)]
            (c/html
             [:div.canela.title-2
              {:data-ref (str "question-" id)}
              (interpose [:br] primary)]))))

(c/defcomponent quiz-question-choice-button-molecule
  [{:quiz.question.choice.button/keys [primary icon-url target selected?]} _ _]
  [:div
   ((if selected?
      ui/button-choice-selected
      ui/button-choice-unselected)
    (merge
     {:class "my2"}
     (when target
       {:on-click (apply utils/send-event-callback target)}))
    [:div.flex.items-center.py2
     {:style {:height "0px"}}
     (when icon-url
       [:img.mr3 {:src   (assets/path icon-url)
                  :style {:width "42px"}}])
     [:div.content-2.proxima.flex-auto.left-align
      primary]
     (when selected?
       [:div checkmark-circle-atom])])])

(c/defcomponent quiz-question-organism
  [data _ _]
  (c/html
   [:div.myj3.mx6
    (c/build quiz-question-title-molecule data {:opts (:quiz.question.title/id data)})
    [:div.py2
     (c/elements quiz-question-choice-button-molecule
                 data
                 :quiz.question/choices)]]))

(c/defcomponent template
  [{:keys [progress quiz-questions]} _ _]
  [:div
   (c/build progress-organism progress)
   (c/html [:div.absolute.top-0
            (c/elements quiz-question-organism
                        quiz-questions
                        :quiz/questions)])])

(def questions
  {[1 :q/texture ["Let's talk about texture."
                  "Do you want your final look to be:"]]
   [[:q/straight "Straight" "/images/categories/straight-icon.svg"]
    [:q/wavy "Wavy" "/images/categories/water-wave-icon.svg"]
    [:q/unsure "I'm not sure yet"]]

   [2 :q/length ["What about length?"
                 "Do you want your final look to be:"]]
   [[:q/short "Short 10\" to 14\""]
    [:q/medium "Medium 14\" to 18\""]
    [:q/long "Long 18\" to 22\""]
    [:q/extra-long "Extra Long 22\" to 26\""]
    [:q/unsure "I'm not sure yet"]]})

(defn quiz-questions<
  [quiz-answers progression]
  {:quiz/questions
   (for [[[idx key question-title] choices] questions
         :when                              (<= idx progression)]
     {:quiz.question.title/primary question-title
      :quiz.question.title/id      idx
      :quiz.question/choices
      (for [[value choice-title img-url] choices
            :let                         [answered? (= value (get quiz-answers key))]]
        #:quiz.question.choice.button
        {:icon-url  img-url
         :primary   (str choice-title)
         :target    [e/flow|quiz|answered [:quiz/shopping idx key value]]
         :selected? answered?})})})

(defn progress<
  [progression]
  {:progress/portions
   [{:progress.portion.bar/units   (* progression 3)
     :progress.portion.bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
    {:progress.portion.bar/units (- 12 (* progression 3))}]})

(defn ^:export page
  [state]
  (let [quiz-id      :quiz/shopping
        progression  (get-in state (conj k/models-progressions quiz-id))
        quiz-answers (get-in state (conj k/models-quizzes quiz-id))]
    (->> {:quiz-questions (quiz-questions< quiz-answers progression)
          :progress       (progress< progression)}
         (c/build template))))

;;---------- -behavior

(defmethod fx/perform-effects e/navigate-adventures-quiz
  [_ _ _ _ _]
  (publish e/flow|progression|reset [:quiz/shopping]))

(defmethod t/transition-state e/flow|quiz|answered
  [_ _ [quiz-id _ question answer] state]
  (cond-> state
    (every? some? [quiz-id question answer])
    (assoc-in (conj k/models-quizzes quiz-id question) answer)))

(defmethod fx/perform-effects e/flow|quiz|answered
  [_ _ [quiz-id idx _ _] _ _]
  (publish e/flow|progression|progressed [quiz-id (inc idx)]))

(defmethod t/transition-state e/flow|progression|reset
  [_ _ [quiz-id] state]
  (cond-> state
    (some? quiz-id)
    (assoc-in (conj k/models-progressions quiz-id) 1)))

(defmethod t/transition-state e/flow|progression|progressed
  [_ _ [quiz-id idx] state]
  (cond-> state
    (every? some? [quiz-id idx])
    (assoc-in (conj k/models-progressions quiz-id) idx)))

(comment
  {:visual/quiz ["init" "answered"]
   :visual/progression ["init" "progressed"]
   :biz/look-selector ["selected"] ;; TODO probably could use a generic/abstract selector domain
   :biz/look ["carted"]}) ;; TODO cart buiding and progression to checkout is poorly model, but we knew that
