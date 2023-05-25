(ns mayvenn.shopping-quiz.crm-persona
  (:require api.orders
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [spice.selector :as selector]
            [storefront.accessors.images :as images]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.ugc :as ugc]))

(def ^:private shopping-quiz-id :crm/persona)

(c/defcomponent result-card
  [data _ _]
  [:div.col-6.flex.flex-column.p2
   [:div
    (ui/img (merge {:width "100%"
                    :max-size 200}
                   (with :image data)))]
   (titles/proxima-content (with :title data))
   (actions/small-primary (with :action data))])

(c/defcomponent results-template
  [data _ _]
  [:div.bg-cool-gray
   [:div.col-12.bg-white
    (c/build header/nav-header-component (with :header data))]
   [:div.flex.flex-wrap.col-12
    (c/elements result-card
                data
                :results)]])

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
                         :on/success     [e/biz|looks-suggestions|queried]}]
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

(defn quiz-results<
  [products-db skus-db images-db results]
  (let [results-ids [{:catalog/product-id "120"
                      :page/slug          "malaysian-body-wave-bundles"}
                     {:catalog/product-id "236"
                      :page/slug          "malaysian-body-wave-bundles"}
                     {:catalog/product-id "236"
                      :page/slug          "malaysian-body-wave-bundles"}
                     {:catalog/product-id "236"
                      :page/slug          "malaysian-body-wave-bundles"}]] 
    {:results (mapv (fn [results-q]
                      (let [result    (get products-db (:catalog/product-id results-q))
                            thumbnail (product-image images-db result)]
                        {:title/primary (:copy/title result)
                         :image/src     (:url thumbnail)
                         :action/id     "p-1"
                         :action/label  "Shop Now"
                         :action/target [e/navigate-product-details result]}))
                    results-ids)}))

(defn ^:export page
  [state]
  (let [#_#_looks-shared-carts-db          (get-in state storefront.keypaths/v1-looks-shared-carts)
        products-db                    (get-in state k/v2-products)
        skus-db                        (get-in state k/v2-skus)
        images-db                      (get-in state k/v2-images)

        {:order.items/keys [quantity]} (api.orders/current state)
        questioning                    (questioning/<- state shopping-quiz-id)
        results                        (looks-suggestions/<- state shopping-quiz-id)

        header-data                    {:forced-mobile-layout? true
                                        :quantity              (or quantity 0)}]
    (cond
      (seq results)
      (c/build results-template
               (merge
                (quiz-results< products-db
                               skus-db
                               images-db
                               results)
                (within :header header-data)))
      
      :else
      (c/build questioning-template
               {:header      header-data
                :progress    (progress< (:progression questioning))
                :questions   (questions< questioning)
                :see-results (see-results< questioning)}))))

;;;; Behavior

(defmethod fx/perform-effects e/navigate-quiz-crm-persona
  [_ _ _ _ state]
  (let [album-keyword :look]
    (ugc/determine-look-album state album-keyword))
  ;; TODO(corey) move to show answers
  (publish e/cache|product|requested "120")
  (publish e/cache|product|requested "236")
  (publish e/biz|looks-suggestions|reset
           {:id shopping-quiz-id})
  (publish e/biz|questioning|reset
           {:questioning/id shopping-quiz-id}))
