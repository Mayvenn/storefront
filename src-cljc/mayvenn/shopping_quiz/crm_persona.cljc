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
            [storefront.transitions :as t]
            [storefront.routes :as routes]))

(def ^:private shopping-quiz-id :crm/persona)
(def ^:private answer-keypath [:models :quiz-feedback :answer])
(def ^:private explanation-keypath [:models :quiz-feedback :explanation])
(def ^:private hide-keypath [:models :quiz-feedback :hide?])

(defn underline-button
  [{:keys [id label target]}]
  (when id
    (if target
      [:a.p-color.title-3.shout.bold.underline.proxima
       (merge {:data-test id}
              (apply utils/route-to target))
       label]
      [:div.p-color.title-3.shout.bold.underline.proxima
       {:data-test id}
       label])))

(c/defcomponent result-card
  [{:keys [nav-target tracking-target] :as data} _ _]
  (if nav-target
    [:a.bg-white.block.pointer.black
     {:href (apply routes/path-for nav-target)
      :on-click (apply utils/send-event-callback tracking-target)}
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
      (underline-button (with :action data))]]
    [:div
     ;; CONTENT SHIFT
     [:div.bg-warm-gray.flex.flex-column.justify-center
      {:style {:aspect-ratio "3 / 4"
               :heigh "100vh"
               :overflow "hidden"}}
      ui/spinner]
     [:div.bg-white.flex.flex-column.justify-center
      {:style {:height "100px"}}
      ui/spinner]]))

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
  [:div.py4
   (if id
     [:form
      (c/build feedback-radio (with :radio data))
      (c/build feedback-explanation (with :explanation data))
      [:div.flex
       (ui/button-small-primary
        (merge {:data-test id}
               (when disabled? {:disabled? disabled?})
               (apply utils/route-to target))
        label)]]
     [:div
      [:div.bg-s-color.border.border-s-color
       [:div.bg-lighten-4.p2
        primary]]])])

(c/defcomponent results-profile-content
  [{:keys [primary secondary tertiary quaternary]} _ _]
  [:div
   [:div.proxima.title-1 primary]
   [:div.py4
    [:div.proxima.title-2 secondary]
    [:div.pt2 tertiary]]
   [:div.proxima.title-2.pb2 quaternary]])

(defn need-more-inspo
  [{:keys [primary] :as data}]
  [:div.pt4
   [:div.proxima.title-1 primary]
   (underline-button data)])

(c/defcomponent results-template
  [data _ _]
  [:div.bg-refresh-gray
   [:div.col-12.bg-white
    (c/build header/component (with :header data))]
   [:div.p4.flex.flex-column.col-4-on-dt.mx-auto
    (c/build results-profile-content (with :results.content data))
    [:div.grid.grid-cols-2.gap-5
     {:style {:align-items "stretch"}}
     (c/elements result-card
                 data
                 :results)]
    (need-more-inspo (with :action data))
    (c/build quiz-feedback (with :feedback data))]])

(c/defcomponent questioning-template
  [{:keys [header progress questions see-results]} _ _]
  [:div.col-12
   [:div.top-0.left-0.fixed.col-12.bg-white
    (header/built-component header nil)
    (c/build progress-bar/variation-1 progress)]
   [:div.flex.flex-column.mbj3.pbj3.col-4-on-dt.mx-auto
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

(defn quiz-results-content<
  [persona]
  (within :results.content
          (case (:persona/id persona)
            :p1 {:primary    "Your Personalized Hair Profile"
                 :secondary  "Signature Style Maven"
                 :tertiary   "You know what you want, and why you want it - and your hairstyle is no different. You’re looking for your everyday, go-to look. You need something that is functional, super cute, and serves a look without breaking the bank. Your signature style awaits."
                 :quaternary "We think you'll love these looks:"}
            :p2 {:primary    "Your Personalized Hair Profile"
                 :secondary  "Keeping it Classic"
                 :tertiary   "Classic, timeless, chic - you’re all of the above. Making sure your look is on point is important to you, so it’s only fair that all the quality and info you deserve is ready and available. Whether you’re sticking to your tried and true texture or ready to branch out, rest assured that we’ve got you covered."
                 :quaternary "We think you'll love these looks:"}
            :p3 {:primary    "Your Personalized Hair Profile"
                 :secondary  "Alter Your Ego"
                 :tertiary   "You’re her, and her…and her, too. The bottom line is this - you can do it all! You’re ready for a switch-up at any given moment, and your look needs to understand the assignment. Whether it’s the latest color trend or a brand-new product moment, you’re ready for the spotlight."
                 :quaternary "We think you'll love these looks:"}
            :p4 {:primary    "Your Personalized Hair Profile"
                 :secondary  "Give Me Inspiration"
                 :tertiary   "No matter the occasion, you deserve to feel like your best self. Whether you’re planning a vacay, date night, or saying “I do”, we’ve got a look for you. Need a little extra inspiration? Never fear - we’ll show you all the best ways to wear your next favorite style IRL."
                 :quaternary "We think you'll love these looks:"}
            ;; default is p1
            {:primary    "Your Personalized Hair Profile"
             :secondary  "Signature Style Maven"
             :tertiary   "You know what you want, and why you want it - and your hairstyle is no different. You're looking for your everyday, go-to look. You need something that is functional, super cute, and serves a look without breaking the bank. Your signature style awaits."
             :quaternary "We think you'll love these looks:"})))

(defn quiz-results<
  [persona]
  {:results (->> persona
                 :results
                 (map-indexed (fn [idx {:keys [content/id catalog/product-id] :as result}]
                                (cond
                                  (seq product-id)
                                  (let [{:keys [copy/title catalog/sku-id page/slug url]} result]
                                    {:title/secondary title
                                     :nav-target      [e/navigate-product-details {:catalog/product-id product-id
                                                                                   :page/slug          slug
                                                                                   :query-params       {:SKU sku-id}}]
                                     :tracking-target [e/control-quiz-shop-now-product {:catalog/product-id product-id
                                                                                        :page/slug          slug
                                                                                        :query-params       {:SKU sku-id}
                                                                                        :persona            persona}]
                                     :image/src       url
                                     :action/id       (str "result-" (inc idx))
                                     :action/label    "Shop Now"})
                                  (seq id)
                                  (let [{:keys [title photo-url]} result]
                                    {:title/secondary title
                                     :nav-target      [e/navigate-shop-by-look-details {:look-id       id
                                                                                        :album-keyword :look}]
                                     :tracking-target [e/control-quiz-shop-now-look {:look-id       id
                                                                                     :album-keyword :look
                                                                                     :persona       persona}]
                                     :image/src       photo-url
                                     :action/id       (str "result-" (inc idx))
                                     :action/label    "Shop Now"})))))})

(defn ^:export page
  [state]
  (let [questioning (questioning/<- state shopping-quiz-id)
        persona     (persona/<- state)
        header-data state]
    (cond
      persona
      (c/build results-template
               (merge
                (within :action {:id      "need-more-inspiration"
                                 :primary "Need more inspiration?"
                                 :label   "Shop all ready to wear wigs"
                                 :target  [e/navigate-category {:page/slug           "ready-wear-wigs"
                                                                :catalog/category-id "25"}]})
                (quiz-results-content< persona)
                (quiz-feedback< state)
                (quiz-results< persona)
                (within :header header-data)))
      :else
      (c/build questioning-template
               {:header      header-data
                :progress    (progress< (:progression questioning))
                :questions   (questions< questioning)
                :see-results (see-results< questioning)}))))

;;;; Behavior


(defmethod fx/perform-effects e/navigate-quiz-crm-persona-questions
  [_ _ _ _ state]
  ;; Reset
  (publish e/persona|reset)
  (publish e/biz|questioning|reset {:questioning/id shopping-quiz-id}))

(defmethod fx/perform-effects e/navigate-quiz-crm-persona-results
  [_ _ {{persona :p} :query-params} _ state]
  ;; Set persona
  (let [persona-id      (keyword persona)
        cache           (get-in state k/api-cache)]
    (when (contains? #{:p1 :p2 :p3 :p4} persona-id)
      (let [handler (fn [result]
                      (when-let [cart-ids (->> (get-in result [:ugc-collection :aladdin-free-install :looks])
                                               (take 99)
                                               (mapv contentful/shared-cart-id)
                                               not-empty)]
                        #?(:cljs (api/fetch-shared-carts cache cart-ids))))]
        (fx/fetch-cms-keypath state [:ugc-collection :aladdin-free-install] handler)
        (publish e/persona|selected {:persona/id persona-id})))))

(defmethod fx/perform-effects e/control-quiz-shop-now-product
  [_ _ args _ state]
  #?(:cljs (history/enqueue-navigate e/navigate-product-details args)))

(defmethod tr/perform-track e/control-quiz-shop-now-product
  [_ event {:keys [catalog/product-id page/slug query-params persona]} app-state]
  (->> {:product_id   product-id
        :slug         slug
        :query_params query-params
        :persona      persona}
       #?(:cljs (stringer/track-event "quiz_result_selection"))))

(defmethod fx/perform-effects e/control-quiz-shop-now-look
  [_ _ args _ state]
  #?(:cljs (history/enqueue-navigate e/navigate-shop-by-look-details args)))


(defmethod tr/perform-track e/control-quiz-shop-now-look
  [_ event {:keys [look-id album-keyword persona]} app-state]
  (->> {:look-id       look-id
        :album-keyword album-keyword
        :persona       persona}
       #?(:cljs (stringer/track-event "quiz_result_selection"))))

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
                           {:quiz_helpful?    (get-in app-state answer-keypath)
                            :quiz_explanation (get-in app-state explanation-keypath)})))
