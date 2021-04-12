(ns mayvenn.adventures.quiz
  "Visual Layer: Quiz in an adventure setting"
  (:require #?@(:cljs
                [[storefront.browser.scroll :as scroll]])
            api.orders

            [storefront.assets :as assets]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.transitions :as t]))

;; state

(def questions
  [{:quiz/question-id     :texture
    :quiz/question-prompt ["Let's talk about texture."
                           "Do you want your final look to be:"]
    :quiz/choices
    [{:quiz/choice-id     :straight
      :quiz/choice-answer "Straight"
      :quiz/img-url       "/images/categories/straight-icon.svg"}
     {:quiz/choice-id     :wavy
      :quiz/choice-answer "Wavy"
      :quiz/img-url       "/images/categories/water-wave-icon.svg"}
     {:quiz/choice-id     :unsure
      :quiz/choice-answer "I'm not sure yet" }]}
   {:quiz/question-id     :length
    :quiz/question-prompt ["What about length?"
                           "Do you want your final look to be:"]
    :quiz/choices
    [{:quiz/choice-id     :short
      :quiz/choice-answer "Short 10\" to 14\""}
     {:quiz/choice-id     :medium
      :quiz/choice-answer "Medium 14\" to 18\""}
     {:quiz/choice-id     :long
      :quiz/choice-answer "Long 18\" to 22\""}
     {:quiz/choice-id     :extra-long
      :quiz/choice-answer "Extra Long 22\" to 26\""}
     {:quiz/choice-id     :unsure
      :quiz/choice-answer "I'm not sure yet"}]}
   {:quiz/question-id     :leave-out
    :quiz/question-prompt ["Would you like to leave any of your natural hair out?"]
    :quiz/question-info   ["Leave-out covers the tracks of a sew-in and blends your natural hair with the extensions."]
    :quiz/choices
    [{:quiz/choice-id     :yes
      :quiz/choice-answer "Yes"}
     {:quiz/choice-id     :no
      :quiz/choice-answer "No"}
     {:quiz/choice-id     :unsure
      :quiz/choice-answer "I'm not sure yet"}]}])

(def initial-answers nil)

(def initial-progression #{}) ;; progression is a specialized rollup useful for extent

;; visual

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
  (did-mount
   [this]
   #?(:cljs
      (some->> (c/get-props this)
               :quiz.question.title/scroll-to
               (#(str "[data-scroll=" % "]"))
               scroll/scroll-selector-to-top)))
  (render
   [this]
   (let [{:quiz.question.title/keys [id primary secondary scroll-to]} (c/get-props this)]
     (c/html
      [:div
       {:key id}
       [:div.canela.title-2.mtj3.ptj3
        (when scroll-to
          {:data-scroll scroll-to})
        (interpose [:br] primary)]
       [:div.content-2.dark-gray.my3
        secondary]]))))

(c/defcomponent quiz-question-choice-button-molecule
  [{:quiz.question.choice.button/keys [primary icon-url target selected?]} _ _]
  [:div
   ;; TODO(corey) rationale for if
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
  [:div.mx6.pbj3
   (c/build quiz-question-title-molecule data)
   [:div.my2
    (c/elements quiz-question-choice-button-molecule
                data
                :quiz.question/choices)]])

(c/defcomponent quiz-see-results-organism
  [{:quiz.see-results.button/keys [id target label disabled?]} _ _]
  [:div
   (when id
     [:div.col-10.my2.mx-auto
      (ui/button-large-primary
       (merge (apply utils/route-to target)
              {:data-test id
               :disabled? disabled?})
       label)])])

(def spinner-template
  (c/html
   [:div.max-580.bg-pale-purple.absolute.overlay
    [:div.absolute.overlay.border.border-white.border-framed-white.m4.p5.flex.flex-column.items-center.justify-center
     [:div (svg/mayvenn-logo {:class "spin-y"
                              :style {:width "54px"}})]
     [:div {:style {:height "50%"}}
      [:div.title-2.canela.center
       [:div "Sit back and relax."]
       [:div "There’s no end to what your hair can do."]]]]]))

(c/defcomponent template
  [{:keys [header progress quiz-questions quiz-see-results]} _ _]
  [:div.col-12
   [:div.max-580.top-0.fixed.col-12.bg-white
    (c/build header/mobile-nav-header-component header)
    (c/build progress-organism progress)]
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements quiz-question-organism
                quiz-questions
                :quiz/questions)
    (c/build quiz-see-results-organism quiz-see-results)]])

(defn quiz-see-results<
  [progression]
  (when (>= (count progression) (dec (count questions)))
    {:quiz.see-results.button/id        "quiz.see-results"
     :quiz.see-results.button/disabled? (not= (count questions)
                                              (count progression))
     :quiz.see-results.button/target    [e/flow|quiz|submitted {:quiz/id :quiz/shopping}]
     :quiz.see-results.button/label     "See Results"}))

(defn quiz-questions<
  [quiz-answers progression]
  {:quiz/questions
   (for [[idx {:quiz/keys [question-id question-prompt question-info choices]}]
         (map-indexed vector questions)
         :let [question-idx (inc idx)] ;; question-idx is indexed from 1
         :when (<= idx (count progression))]
     {:quiz.question.title/primary   question-prompt
      :quiz.question.title/secondary question-info
      :quiz.question.title/scroll-to (when (> question-idx 1)
                                       (str "q-" question-idx))
      :quiz.question/choices
      (for [{:quiz/keys [choice-id choice-answer img-url]} choices
            :let                                           [answered? (= choice-id
                                                                         (get quiz-answers question-id))]]
        #:quiz.question.choice.button
        {:icon-url  img-url
         :primary   choice-answer
         :target    [e/flow|quiz|answered
                     {:quiz/id           :quiz/shopping
                      :quiz/question-idx question-idx
                      :quiz/question-id  question-id
                      :quiz/choice-id    choice-id}]
         :selected? answered?})})})

(defn progress<
  [progression]
  (let [extent (inc (count progression))]
    {:progress/portions
     [{:progress.portion.bar/units   (* extent 3)
       :progress.portion.bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
      {:progress.portion.bar/units (- 12 (* extent 3))}]}))

(c/defcomponent quiz-results-organism
  [{:quiz.result/keys [id index-label ucare-id primary secondary tertiary tertiary-note cta-label _cta-target]} _ _]
  [:div.left-align.px3
   [:div.shout.proxima.title-3.mb1 index-label]
   [:div.bg-white
    [:div.flex.p3
     [:div.mr4
      (ui/img {:src ucare-id :width "80px"})]
     [:div.flex.flex-column
      [:div primary]
      [:div secondary]
      [:div.content-1 tertiary [:span.ml2.s-color.content-2 tertiary-note]]
      (ui/button-small-primary {:data-test id
                                :class     "mt2 col-8"} cta-label)]]]])

(def ^:private green-divider-atom
  (c/html
   [:div
    {:style {:background-image  "url('//ucarecdn.com/7e91271e-874c-4303-bc8a-00c8babb0d77/-/resize/x24/')"
             :background-position "center center"
             :background-repeat   "repeat-x"
             :height              "24px"}}]))

(c/defcomponent results-template
  [{:keys [header quiz-results]} _ _]
  [:div.bg-cool-gray
   [:div.max-580.col-12.bg-white
    (c/build header/mobile-nav-header-component header)]

   [:div.center.pyj2
    [:div.flex.flex-column.px2
     [:div.shout.proxima.title-2 (:quiz.results/primary quiz-results)]
     [:div.m3.canela.title-1 (:quiz.results/secondary quiz-results)]]
    (c/elements quiz-results-organism quiz-results :quiz/results)]
   green-divider-atom
   (let [{:quiz.alternative/keys [primary cta-label cta-target id]} quiz-results]
     [:div.bg-white.py5.flex.flex-column.center.items-center
      primary
      (ui/button-small-secondary (merge {:data-test id :class "mt2 mx-auto"}
                                        (apply utils/route-to cta-target)) cta-label)])])

(def quiz-results<
  [{:quiz.result/id            "result-option-0"
    :quiz.result/index-label   "Option 1"
    :quiz.result/ucare-id      "f7568a58-d240-4856-9d7d-21096bafda1c"
    :quiz.result/primary       "Brazilian Wavy"
    :quiz.result/secondary     "20”, 22” + 24” Frontal"
    :quiz.result/tertiary      "$400.99"
    :quiz.result/tertiary-note "Install Included"
    :quiz.result/cta-label     "Add To Bag"}
   {:quiz.result/id            "result-option-1"
    :quiz.result/index-label   "Option 2"
    :quiz.result/ucare-id      "f7568a58-d240-4856-9d7d-21096bafda1c"
    :quiz.result/primary       "Brazilian Wavy"
    :quiz.result/secondary     "20”, 22”, 22”"
    :quiz.result/tertiary      "$400.99"
    :quiz.result/tertiary-note "Install Included"
    :quiz.result/cta-label     "Add To Bag"}])

(defn ^:export page
  [state]
  (let [{:order.items/keys [quantity]} (api.orders/current state)

        quiz-id      :quiz/shopping
        progression  (get-in state (conj k/models-progressions quiz-id))
        quiz-answers (get-in state (conj k/models-quizzes quiz-id))
        quiz-results (get-in state (conj k/models-quizzes-results quiz-id))]
    (cond
      (get-in state (conj k/models-wait quiz-id)) spinner-template

      (seq quiz-results) (->> {:quiz-results {:quiz/results                quiz-results
                                              :quiz.results/primary        "Hair & Services"
                                              :quiz.results/secondary      "We think these styles will look great on you."
                                              :quiz.alternative/primary    "Wanna explore more options?"
                                              :quiz.alternative/id         "quiz-result-alternative"
                                              :quiz.alternative/cta-target [e/navigate-category {:page/slug  "mayvenn-install"
                                                                                                 :catalog/category-id "23"}]
                                              :quiz.alternative/cta-label  "Browse Hair"}
                               :header       {:forced-mobile-layout? true
                                              :quantity              (or quantity 0)}}
                              (c/build results-template))
      :else              (->> {:header           {:forced-mobile-layout? true
                                                  :quantity              (or quantity 0)}
                               :progress         (progress< progression)
                               :quiz-questions   (quiz-questions< quiz-answers progression)
                               :quiz-see-results (quiz-see-results< progression)}
                              (c/build template)))))

;;---------- -behavior

(defmethod fx/perform-effects e/navigate-adventure-quiz
  [_ _ _ _ _]
  (publish e/flow|quiz|reset {:quiz/id :quiz/shopping}))

;; TODO(corey) should be biz
;; flow|quiz

(defmethod t/transition-state e/flow|quiz|reset
  [_ _ {quiz-id :quiz/id} state]
  (-> state
      (assoc-in (conj k/models-quizzes quiz-id) initial-answers)
      (assoc-in (conj k/models-quizzes-results quiz-id) nil)))

(defmethod fx/perform-effects e/flow|quiz|submitted
  [_ _ {quiz-id :quiz/id} _ _]
  (publish e/flow|wait|begun {:wait/id quiz-id})
  (publish e/flow|quiz|results|resulted {:quiz/id quiz-id
                                         :quiz/results quiz-results<}))


(defmethod t/transition-state e/flow|quiz|results|resulted
  [_ _ {:quiz/keys [id results]} state]
  (-> state
      (assoc-in (conj k/models-quizzes-results id)
                results)))

(defmethod t/transition-state e/flow|wait|begun
  [_ _ {wait-id :wait/id} state]
  (assoc-in state (conj k/models-wait wait-id) true))

(defmethod fx/perform-effects e/flow|wait|begun
  [_ _ {wait-id :wait/id} _ _]
  (messages/handle-later e/flow|wait|elapsed {:wait/id wait-id
                                              :wait/ms 3000}
                         3000))

(defmethod t/transition-state e/flow|wait|elapsed
  [_ _ {wait-id :wait/id} state]
  (assoc-in state (conj k/models-wait wait-id) false))

(defmethod fx/perform-effects e/flow|quiz|reset
  [_ _ {quiz-id :quiz/id} _ _]
  (publish e/flow|progression|reset
           {:progression/id    quiz-id
            :progression/value initial-progression}))

(defmethod t/transition-state e/flow|quiz|answered
  [_ _ {quiz-id     :quiz/id
        question-id :quiz/question-id
        choice-id   :quiz/choice-id} state]
  (assoc-in state
            (conj k/models-quizzes quiz-id question-id)
            choice-id))

(defmethod fx/perform-effects e/flow|quiz|answered
  [_ _ {quiz-id      :quiz/id
        question-idx :quiz/question-idx} _ _]
  (publish e/flow|progression|progressed
           {:progression/id    quiz-id
            :progression/value (inc question-idx)}))

;; flow|progression

(defmethod t/transition-state e/flow|progression|reset
  [_ _ {:progression/keys [id value]} state]
  (assoc-in state (conj k/models-progressions id) value))

(defmethod t/transition-state e/flow|progression|progressed
  [_ _ {:progression/keys [id value]} state]
  (update-in state (conj k/models-progressions id) conj value))

(comment
  {:visual/quiz ["init" "answered"]
   :visual/progression ["init" "progressed"]
   :biz/look-selector ["selected"] ;; TODO probably could use a generic/abstract selector domain
   :biz/look ["carted"]}) ;; TODO cart buiding and progression to checkout is poorly model, but we knew that
