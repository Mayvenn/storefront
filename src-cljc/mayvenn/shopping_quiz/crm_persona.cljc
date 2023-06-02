(ns mayvenn.shopping-quiz.crm-persona
  (:require api.orders
            [mayvenn.concept.persona :as persona]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.lib.radio-section :as radio-section]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            #?(:cljs [storefront.api :as api])
            [storefront.accessors.contentful :as contentful]
            [storefront.accessors.images :as images]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            #?(:cljs [storefront.history :as history])
            #?(:cljs [storefront.hooks.stringer :as stringer])
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.ugc :as ugc]
            [storefront.trackings :as tr]
            [storefront.transitions :as t]))

(def ^:private shopping-quiz-id :crm/persona)
(def ^:private model-keypath [:models :quiz-feedback])
(def ^:private answer-keypath [:models :quiz-feedback :answer])
(def ^:private explanation-keypath [:models :quiz-feedback :explanation])
(def ^:private submitted-keypath [:models :quiz-feedback :submitted?])
(def ^:private hide-keypath [:models :quiz-feedback :hide?])

(c/defcomponent result-card
  [data _ _]
  [:div.bg-white.block
   [:div
    {:style {:aspect-ratio "3 / 4"
             :overflow "hidden"}}
    (ui/img (merge {:max-size   200
                    :style {:object-fit "cover"}
                    :class "container-size contents"}
                   (with :image data)))]
   [:div.p2.flex.flex-column.justify-between
    {:style {:height "100px"}}
    (titles/proxima-content (with :title data))
    (actions/small-tertiary (with :action data))]])

(c/defcomponent feedback-radio
  [{:keys [radio-name primary answer slots]} _ _]
  [:div
   [:div.bold primary]
   (into [:div {:required true}]
         (for [{:slot/keys        [id]
                :slot.picker/keys [copy]} slots
               :let                       [radio-id (str radio-name "-" id)]]
           (radio-section/v2
            (merge {:dial.attrs/name          radio-name
                    :dial.attrs/id            radio-id
                    :dial.attrs/data-test     radio-id
                    :dial.attrs/class         "order-0"
                    :label.attrs/class        "col-12 py2"
                    :label.attrs/on-click     (utils/send-event-callback e/biz|quiz-feedback|question-answered
                                                                         {:id id}
                                                                         {:prevent-default?  true
                                                                          :stop-propagation? true})
                    :copy.attrs/class         "order-last flex-auto proxima px2 "
                    :copy.content/text        copy}
                   (when (= id answer)
                     {:state/checked "checked"})))))])

(c/defcomponent feedback-explanation
  [{:keys [primary explanation id target]} _ _]
  [:div.py2
   [:div.bold primary]
   (ui/text-field-large
    {:value       explanation
     :id          id
     :data-test   id
     :autoFocus   false
     :focused     false
     :on-change   (fn [e]
                    (apply publish (conj target {:explanation (.. e -target -value)})))})])

(c/defcomponent quiz-feedback
  [{:keys [id disabled? target label primary] :as data} _ _]
  (if id
    [:form.p2
     (c/build feedback-radio (with :radio data))
     (c/build feedback-explanation (with :explanation data))
     [:div.flex
      (ui/button-small-primary
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (apply utils/route-to target))
       label)]]
    [:div.p2
     [:div.bg-s-color.border.border-s-color
      [:div.bg-lighten-4.p2
       primary]]]))

(c/defcomponent results-template
  [data _ _]
  [:div.bg-refresh-gray
   [:div.col-12.bg-white
    (c/build header/nav-header-component (with :header data))]
   [:div.grid.grid-cols-2.gap-5.p2
    {:style {:align-items "stretch"}}
    (c/elements result-card
                data
                :results)]
   (c/build quiz-feedback (with :feedback data))])

(c/defcomponent questioning-template
  [{:keys [header progress questions see-results]} _ _]
  [:div.col-12
   [:div.top-0.fixed.col-12.bg-white
    (c/build header/nav-header-component header)
    (c/build progress-bar/variation-1 progress)]
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements question/variation-1
                questions
                :questions)
    [:div.flex.justify-center.items-center
     (->> (with :action see-results)
          actions/large-primary)]]])

(defn progress<
  [progression]
  (let [extent (count progression)
        each   (partial * 2)]
    {:portions [{:bar/units   (each extent)
                 :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
                {:bar/units (- 12 (each extent))}]}))

(defn questions<
  [{:keys [questions answers progression]}]
  {:questions (for [[question-idx {question-id    :question/id
                                   :question/keys [prompt info choices]}] (map-indexed vector questions)
                    :when (< question-idx (inc (count progression)))]
                {:title/primary   prompt
                 :title/id        (str "q-" question-idx)
                 :title/secondary info
                 :title/scroll-to (when (> question-idx 0)
                                    (str "q-" question-idx))
                 :choices         (for [[choice-idx {choice-id    :choice/id
                                                     :choice/keys [answer img-url]}] (map-indexed vector choices)
                                        :let [answered? (= choice-id
                                                           (get answers question-id))]]
                                    #:action
                                     {:icon-url  img-url
                                      :primary   answer
                                      :id        (str (name question-id) "-" (name choice-id))
                                      :target    [e/biz|questioning|answered
                                                  {:questioning/id shopping-quiz-id
                                                   :question/idx   question-idx
                                                   :question/id    question-id
                                                   :choice/idx     choice-idx
                                                   :choice/id      choice-id}]
                                      :selected? answered?})})})

(defn see-results<
  [{:questioning/keys [unanswered]
    :keys [answers]}]
  (when (<= unanswered 1)
    {:action/id        "quiz-see-results"
     :action/disabled? (not (zero? unanswered))
     :action/target    [e/biz|questioning|submitted
                        {:questioning/id shopping-quiz-id
                         :answers        answers
                         :on/success     [e/persona|selected
                                          {:on/success-fn
                                           #?(:clj identity
                                              :cljs
                                              #(history/enqueue-navigate
                                                e/navigate-quiz-crm-persona-results
                                                {:query-params {:p %}}))}]}]
     :action/label     "See Results"}))

(def select
  (comp seq
        (partial selector/match-all
                 {:selector/strict? true})))

(defn- product-image
  [images-db product]
  (->> product
       (images/for-skuer images-db)
       (select {:use-case #{"carousel"}
                :image/of #{"model" "product"}})
       (sort-by :order)
       first))

(defn quiz-feedback<
  [state]
  {:feedback/label                   "Submit"
   :feedback/disabled?               (not (get-in state answer-keypath))
   :feedback/target                  [e/control-quiz-results-feedback]
   :feedback/id                      (when (not (get-in state hide-keypath)) "submit")
   :feedback/primary                 "We've received your response. Thank you for your feedback!"
   :feedback.explanation/id          "feedback-explanation"
   :feedback.explanation/primary     "Why or why not?"
   :feedback.explanation/target      [e/biz|quiz-explanation|explained]
   :feedback.explanation/explanation (get-in state explanation-keypath)
   :feedback.radio/answer            (get-in state answer-keypath)
   :feedback.radio/primary           "Did you find the results of this quiz helpful"
   :feedback.radio/radio-name        "results-feedback"
   :feedback.radio/slots             [{:slot/id "yes" :slot.picker/copy "Yes"}
                                      {:slot/id "no" :slot.picker/copy "No"}]})

(defn quiz-results<
  [products-db skus-db images-db persona-id]
  {:results (->> (case persona-id
                   :p1 [{:catalog/product-id "120"}
                        {:catalog/product-id "236"}
                        {:catalog/product-id "9"}
                        {:catalog/product-id "353"}]
                   :p2 [{:catalog/product-id "335"}
                        {:catalog/product-id "354"}
                        {:catalog/product-id "268"}
                        {:catalog/product-id "249"}]
                   :p3 [{:catalog/product-id "352"}
                        {:catalog/product-id "354"}
                        {:catalog/product-id "235"}
                        {:catalog/product-id "313"}]
                   :p4 [{:catalog/product-id "354"}
                        {:catalog/product-id "128"}
                        {:catalog/product-id "252"}
                        {:catalog/product-id "15"}]
                     ;; default is p1
                   [{:catalog/product-id "120"}
                    {:catalog/product-id "236"}
                    {:catalog/product-id "9"}
                    {:catalog/product-id "353"}])
                 (map-indexed (fn [idx result]
                                (when (seq (:catalog/product-id result))
                                  (let [product   (get products-db (:catalog/product-id result))
                                        thumbnail (product-image images-db product)]
                                    {:title/secondary (:copy/title product)
                                     :image/src     (:url thumbnail)
                                     :action/id     (str "result-" (inc idx))
                                     :action/label  "Shop Now"
                                     :action/target [e/navigate-product-details {:catalog/product-id (:catalog/product-id product)
                                                                                 :page/slug          (:page/slug product)}]})))))})

(defn ^:export page
  [state]
  (let [#_#_looks-shared-carts-db          (get-in state storefront.keypaths/v1-looks-shared-carts)
        products-db                    (get-in state k/v2-products)
        skus-db                        (get-in state k/v2-skus)
        images-db                      (get-in state k/v2-images)

        {:order.items/keys [quantity]} (api.orders/current state)
        questioning                    (questioning/<- state shopping-quiz-id)
        persona-id                     (persona/<- state)

        header-data                    {:forced-mobile-layout? true
                                        :quantity              (or quantity 0)}]
    (cond
      persona-id
      (c/build results-template
               (merge
                (quiz-feedback< state)
                (quiz-results< products-db
                               skus-db
                               images-db
                               persona-id)
                (within :header header-data)))
      :else
      (c/build questioning-template
               {:header      header-data
                :progress    (progress< (:progression questioning))
                :questions   (questions< questioning)
                :see-results (see-results< questioning)}))))

;;;; Behavior

(defn preload-products
  []
  ;; TODO(corey) Move to show answers
  ;; P1
  (publish e/cache|product|requested "120")
  (publish e/cache|product|requested "236")
  (publish e/cache|product|requested "9")
  (publish e/cache|product|requested "353")
  ;; P2
  (publish e/cache|product|requested "335")
  (publish e/cache|product|requested "354")
  (publish e/cache|product|requested "268")
  (publish e/cache|product|requested "249")
  ;; P3
  (publish e/cache|product|requested "352")
  (publish e/cache|product|requested "354")
  (publish e/cache|product|requested "235")
  (publish e/cache|product|requested "313")
  ;; P4
  (publish e/cache|product|requested "354")
  (publish e/cache|product|requested "128")
  (publish e/cache|product|requested "252")
  (publish e/cache|product|requested "15"))

(defn preload-looks
  [state]
  #?(:cljs
     (let [cache    (get-in state k/api-cache)
           cms-path [:ugc-collection :aladdin-free-install :looks]
           handler  (fn [cms-data]
                      (when-let [cart-ids (->> (get-in cms-data cms-path)
                                               (take 99)
                                               (mapv contentful/shared-cart-id)
                                               not-empty)]
                        (api/fetch-shared-carts cache cart-ids)))]
       (fx/fetch-cms-keypath state [:ugc-collection :aladdin-free-install] handler))))

(defmethod fx/perform-effects e/navigate-quiz-crm-persona-questions
  [_ _ _ _ state]
  ;; Preloads
  (preload-products) 
  (preload-looks state)
  ;; Reset
  (publish e/persona|reset)
  (publish e/biz|questioning|reset {:questioning/id shopping-quiz-id}))

(defmethod fx/perform-effects e/navigate-quiz-crm-persona-results
  [_ _ {{persona :p} :query-params} _ state]
  ;; Preloads
  (preload-products)
  (preload-looks state)
  ;; Set persona
  (let [persona-id (keyword persona)]
    (when (contains? #{:p1 :p2 :p3 :p4} persona-id)
      (publish e/persona|selected {:persona/id persona-id}))))

(defmethod t/transition-state e/control-quiz-results-feedback
  [_ _event {:keys [explanation] :as _args} state]
  (-> state
      (assoc-in hide-keypath true)))

(defmethod t/transition-state e/biz|quiz-feedback|question-answered
  [_ _event {:keys [id] :as _args} state]
  (-> state
      (assoc-in answer-keypath id)))

(defmethod t/transition-state e/biz|quiz-explanation|explained
  [_ _event {:keys [explanation] :as _args} state]
  (-> state
      (assoc-in explanation-keypath explanation)))

#?(:cljs
   (defmethod tr/perform-track e/control-quiz-results-feedback
     [_ event _ app-state]
     (stringer/track-event "quiz_results_feedback_form"
                           {:quiz-helpful?    (get-in app-state answer-keypath)
                            :quiz-explanation (get-in app-state explanation-keypath)
                            :store-slug       (get-in app-state k/store-slug)
                            :test-variations  (get-in app-state k/features)
                            :store-experience (get-in app-state k/store-experience)})))
