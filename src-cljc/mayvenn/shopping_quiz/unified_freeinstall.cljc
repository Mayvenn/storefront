(ns mayvenn.shopping-quiz.unified-freeinstall
  "
  Visual Layer: Unified-Free Install shopping quiz
  "
  (:require [mayvenn.concept.progression :as progression]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
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
            [storefront.keypaths :as k]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]))

(def ^:private id :unified-freeinstall)

(c/defcomponent escape-hatch-component
  [data _ _]
  [:div.absolute.bottom-0.left-0.right-0.bg-white
   dividers/green
   [:div.flex.flex-column.items-center.py5
    ;; Title/primary here is a nonstandard ui element
    ;; TODO(corey) discover what this is and name it
    [:div.center
     (:title/primary data)]
    [:div.col-6.flex.justify-center.items-center
     (actions/small-secondary (with :action data))]]])

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
       :target [e/biz|progression|progressed
                #:progression
                {:id    id
                 :value 1}]})]]])

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
  [questions progression]
  (prn (>= (count progression) (dec (count questions)))
       (count progression)
       (dec (count questions))
       (not= (count questions)
             (count progression)))
  (when (>= (count progression) (dec (count questions)))
    {:action/id        "quiz-see-results"
     :action/disabled? (not= (count questions)
                             (count progression))
     :action/target    [e/biz|questioning|submitted
                        {:questioning/id id
                         :on/success     [:looks-suggestions :queried]}]
     :action/label     "See Results"}))

(defn- fmt
  [m k suffix delim]
  (->> m
       (mapv (comp #(str % suffix) k))
       (interpose delim)))

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
      :quiz.result/index-label   (str "Option " (inc idx))
      :quiz.result/ucare-id      img-id
      :quiz.result/primary       (str origin " " texture)
      :quiz.result/secondary     (apply str
                                        (cond-> (fmt bundles :hair/length "”" ", ")
                                          (seq closures)
                                          (conj
                                           " + " (fmt closures :hair/length "”" ""))))
      :quiz.result/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
      :quiz.result/tertiary-note "Install Included"
      :action/id                 (str "result-option-" idx)
      :action/label              "Add To Bag"
      :action/target             [e/biz|looks-suggestions|selected
                                  (select-keys looks-suggestion
                                               [:product/sku-ids
                                                :service/sku-id])]})})

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
      1 (let [{:keys [questions answers progression]}
              (questioning/<- state id)

              look-suggestions (get-in state (conj k/models-looks-suggestions id))]
          (if (seq look-suggestions)
            (c/build suggestions-template
                     {:progress     (progress< quiz-progression)
                      :suggestions  (suggestions< look-suggestions)
                      :escape-hatch escape-hatch<})
            (c/build questions-template
                     {:progress    (progress< quiz-progression)
                      :questions   (questions< questions answers progression)
                      :see-results (see-results< questions progression)})))
      ;; default or 0
      (c/build intro-template {}))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall
  [_ _ _ _ _]
  (publish e/biz|progression|reset
           #:progression
           {:id    id
            :value #{0}})
  (publish e/biz|looks-suggestions|reset
           {:id id})
  (publish e/biz|questioning|reset
           {:questioning/id id}))

