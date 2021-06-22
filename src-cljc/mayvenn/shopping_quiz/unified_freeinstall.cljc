(ns mayvenn.shopping-quiz.unified-freeinstall
  "
  Visual Layer: Unified-Free Install shopping quiz
  "
  (:require [mayvenn.concept.progression :as progression]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.wait :as wait]
            [mayvenn.visual.lib.card :as card]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.dividers :as dividers]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.actions :as actions]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.components.svg :as svg]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [spice.core :as spice]
            [spice.maps :as maps]))

(def ^:private id :unified-freeinstall)

(c/defcomponent waiting-template
  [_ _ _]
  [:div.max-580.bg-pale-purple.absolute.overlay
   [:div.absolute.overlay.border.border-white.border-framed-white.m4.p5.flex.flex-column.items-center.justify-center
    [:div (svg/mayvenn-logo {:class "spin-y"
                             :style {:width "54px"}})]
    [:div {:style {:height "50%"}}
     [:div.title-2.canela.center
      [:div "Sit back and relax."]
      [:div "There’s no end to what your hair can do."]]]]])

(c/defcomponent escape-hatch-component
  [data _ _]
  [:div.absolute.bottom-0.left-0.right-0.bg-white
   dividers/green
   [:div.flex.flex-column.items-center.py5
    ;; Title/primary here is a nonstandard ui element
    ;; TODO(corey) discover what this is and name it
    [:div.center
     (:title/primary data)]
    [:div.col-5.flex.justify-center.items-center.py1
     (actions/small-secondary (with :action data))]]])

(c/defcomponent summary-template
  [{:keys [header progress selected-look]} _ _]
  [:div.col-12.bg-pale-purple
   [:div.bg-white
    (c/build header/mobile-nav-header-component header)]
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.justify-center.items-center.myj3.pyj3
    [:div.col-8
     (titles/canela {:primary
                     ["Nice choice!"
                      "Now let's find a stylist near you!"]})]
    (c/build card/look-suggestion-1
             selected-look)
    (actions/large-primary
     {:id     "summary-continue"
      :label  "Continue"
      :target [e/redirect
               {:nav-message
                [e/navigate-shopping-quiz-unified-freeinstall-find-your-stylist]}]})]])

(c/defcomponent suggestions-template
  [{:keys [header progress suggestions escape-hatch]} _ _]
  [:div.col-12.bg-cool-gray
   [:div.bg-white
    (c/build header/mobile-nav-header-component header)]
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.mbj3.pbj3
    (titles/canela-huge {:primary "Our picks for you"})
    (c/elements card/look-suggestion-1
                suggestions
                :suggestions)]
   (c/build escape-hatch-component escape-hatch)])

(c/defcomponent questions-template
  [{:keys [header progress questions see-results]} _ _]
  [:div
   (c/build header/mobile-nav-header-component header)
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements question/variation-1
                questions
                :questions)
    [:div.flex.justify-center.items-center
     (actions/large-primary (with :action see-results))]]])

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
    [:div.flex.justify-center.items-center
     (actions/action-molecule
      {:id     "quiz-continue"
       :label  "Continue"
       :target [e/redirect {:nav-message
                            [e/navigate-shopping-quiz-unified-freeinstall-question]}]})]]])

(defn progress<
  [progression]
  (let [extent (apply max progression)]
    {:portions
     [{:bar/units   (* extent 3)
       :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
      {:bar/units (- 12 (* extent 3))}]}))

(defn questions<
  [questions quiz-answers progression]
  {:questions
   (for [[question-idx {question-id    :question/id
                        :question/keys [prompt info choices]}]
         (map-indexed vector questions)
         :when (< question-idx (inc (count progression)))]
     {:title/id        (str "q-" question-idx)
      :title/primary   prompt
      :title/secondary info
      :title/scroll-to (when (> question-idx 0)
                         (str "q-" question-idx))
      :choices
      (for [[choice-idx {choice-id    :choice/id
                         :choice/keys [answer img-url]}]
            (map-indexed vector choices)
            :let [answered? (= choice-id
                               (get quiz-answers question-id))]]
        #:action
        {:icon-url  img-url
         :primary   answer
         :id        (str (name question-id) "-" (name choice-id))
         :target    [e/biz|questioning|answered
                     {:questioning/id id
                      :question/idx   question-idx
                      :question/id    question-id
                      :choice/idx     choice-idx
                      :choice/id      choice-id}]
         :selected? answered?})})})

(defn see-results<
  [questions answers progression]
  (let [unanswered (- (count questions)
                      (count progression))]
    (when (<= unanswered 1)
      {:action/id        "quiz-see-results"
       :action/disabled? (not (zero? unanswered))
       :action/target    [e/redirect {:nav-message
                                      [e/navigate-shopping-quiz-unified-freeinstall-recommendations
                                       {:query-params
                                        (->> answers
                                             (maps/map-values spice/kw-name))}]}]
       :action/label     "See Results"})))

(defn- fmt
  [m k suffix delim]
  (->> m
       (mapv (comp #(str % suffix) k))
       (interpose delim)))

(defn selected-look<
  [{:as           selected-look
    :product/keys [sku-ids]
    :hair/keys    [origin texture]
    img-id        :img/id}]
  (let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
        {bundles  "bundles"
         closures "closures"} (group-by :hair/family skus)]
    {:quiz.result/id            "selected-look"
     :quiz.result/ucare-id      img-id
     :quiz.result/primary       (str origin " " texture)
     :quiz.result/secondary     (apply str
                                       (cond-> (fmt bundles :hair/length "”" ", ")
                                         (seq closures)
                                         (concat [" + "]
                                                 (fmt closures :hair/length "”" ""))))
     :quiz.result/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
     :quiz.result/tertiary-note "Install Included"}))

(defn suggestions<
  [looks-suggestions]
  {:suggestions
   (for [[idx {:as           looks-suggestion
               :product/keys [sku-ids]
               :hair/keys    [origin texture]
               img-id        :img/id}]
         (map-indexed vector looks-suggestions)
         :let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
               {bundles  "bundles"
                closures "closures"} (group-by :hair/family skus)]]
     {:quiz.result/id            (str "result-option-" idx)
      :quiz.result/index-label   (str "Hair + Service Bundle " (inc idx))
      :quiz.result/ucare-id      img-id
      :quiz.result/primary       (str origin " " texture)
      :quiz.result/secondary     (apply str
                                        (cond-> (fmt bundles :hair/length "”" ", ")
                                          (seq closures)
                                          (concat [" + "]
                                                  (fmt closures :hair/length "”" ""))))
      :quiz.result/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
      :quiz.result/tertiary-note "Install Included"
      :action/id                 (str "result-option-" idx)
      :action/label              "Choose this look"
      :action/target             [e/biz|looks-suggestions|selected
                                  {:id            id
                                   :selected-look looks-suggestion
                                   :on/success
                                   [e/navigate-shopping-quiz-unified-freeinstall-summary]}]})})

(def escape-hatch<
  {:title/primary "Wanna explore more options?"
   :action/id     "quiz-result-alternative"
   :action/target [e/navigate-category
                   {:page/slug           "mayvenn-install"
                    :catalog/category-id "23"}]
   :action/label  "Browse Hair"})

(defn ^:export page
  "
  Shopping Quiz: Unified Products+Service v1

  An adventure for helping customers find hair products for a look
  combined with picking a stylist that can do that look.
  "
  [state]
  (let [quiz-progression (progression/<- state id)
        step             (apply max quiz-progression)]
    (case step
      3 nil
      2 (let [looks-suggestions (looks-suggestions/<- state id)
              selected-look     (looks-suggestions/selected<- state id)]
          (if selected-look
            (c/build summary-template
                     {:progress      (progress< quiz-progression)
                      :selected-look (selected-look< selected-look)
                      :escape-hatch  escape-hatch<})
            (c/build suggestions-template
                     {:progress     (progress< quiz-progression)
                      :suggestions  (suggestions< looks-suggestions)
                      :escape-hatch escape-hatch<})))
      1 (let [{:keys [questions answers progression]} (questioning/<- state id)
              wait                                    (wait/<- state id)]
          (if wait
            (c/build waiting-template)
            (c/build questions-template
                     {:progress    (progress< quiz-progression)
                      :questions   (questions< questions answers progression)
                      :see-results (see-results< questions answers progression)})))
      ;; default or 0
      (c/build intro-template {}))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-intro
  [_ _ _ _ _]
  (publish e/biz|progression|reset
           #:progression
           {:id    id
            :value #{0}}))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-question
  [_ _ _ _ state]
  (if (nil? (progression/<- state id))
    (fx/redirect e/navigate-shopping-quiz-unified-freeinstall-intro)
    (do
      (publish e/biz|progression|progressed
               #:progression
               {:id    id
                :value 1})
      (publish e/biz|questioning|reset
               {:questioning/id id})
      (publish e/biz|looks-suggestions|reset
               {:id id}))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-recommendations
  [_ _ {params :query-params} _ _]
  (publish e/biz|progression|progressed
           #:progression
           {:id    id
            :value 2})
  (publish e/biz|questioning|submitted
           {:questioning/id id
            :answers        (maps/map-values keyword params)
            :on/success     [e/biz|looks-suggestions|queried]}))
