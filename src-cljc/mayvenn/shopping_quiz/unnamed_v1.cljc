(ns mayvenn.shopping-quiz.unnamed-v1
  "
  Visual Layer: Original version of Shopping Quiz
  "
  (:require api.orders
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.wait :as wait]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.image-grids :as image-grids]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.dividers :as dividers]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :as mf]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.request-keys :as request-keys]
            [ui.molecules]
            [clojure.set :as set]))

(def ^:private id :unnamed-v1)

;; Visual

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

(c/defcomponent loading-template
  [_ _ _]
  [:div.flex.items-center.justify-between.col-12
   [:div.mx-auto
    (ui/large-spinner {:style {:height "80px"
                               :width  "80px"}})]])

(c/defcomponent questioning-template
  [{:keys [header progress questions see-results]} _ _]
  [:div.col-12
   [:div.max-580.top-0.fixed.col-12.bg-white
    (c/build header/mobile-nav-header-component header)
    (c/build progress-bar/variation-1 progress)]
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements question/variation-1
                questions
                :questions)
    [:div.flex.justify-center.items-center
     (->> (with :action see-results)
          actions/large-primary)]]])

(c/defcomponent quiz-results-star-rating-organism [{:keys [id star-count review-count]} _ _]
  [:div.flex.items-center
   (ui.molecules/stars-rating-molecule {:rating/id id
                                        :rating/value star-count})
   [:p.content-3.ml1
    (str "(" review-count ")")]])

(c/defcomponent shopping-quiz-v2-price-molecule [{:keys [discounted-price retail-price]} _ _]
  [:div.flex
   [:p.content-3.mr1 (mf/as-money discounted-price)]
   [:p.content-3.strike (mf/as-money retail-price)]])

(c/defcomponent quiz-results-shopping-quiz-v2-organism ; Using "Look" style result card
  [data _ _]
  [:div.bg-white.m3
   [:div.left-align.p3
    [:div.mb3
     (c/build image-grids/hero-with-little-hair-column-molecule
              (with :quiz.result-v2.image-grid data))]
    (titles/proxima-left (with :quiz.result-v2.title data))
    [:div.py1
     (c/build quiz-results-star-rating-organism (with :quiz.result-v2.rating data))]
    (c/build shopping-quiz-v2-price-molecule (with :quiz.result-v2.price data))
    (titles/proxima-tiny-left (with :quiz.result-v2.line-item-summary data))
    [:div.flex.justify-center
     (actions/wide-medium-primary (with :quiz.result-v2.action data))]]])

(c/defcomponent quiz-results-organism
  [{:quiz.result/keys [id index-label ucare-id primary secondary tertiary tertiary-note cta-label cta-target]} _ _]
  [:div.left-align.px3.mt5.mb3
   [:div.shout.proxima.title-3.mb1 index-label]
   [:div.bg-white
    [:div.flex.p3
     [:div.mr4
      (ui/img {:src ucare-id :width "80px"})]
     [:div.flex.flex-column
      [:div primary]
      [:div secondary]
      [:div.content-1 tertiary [:span.ml2.s-color.content-2 tertiary-note]]
      (ui/button-small-primary (merge {:data-test id
                                       :class     "mt2 col-8"}
                                      (apply utils/fake-href cta-target)) cta-label)]]]])

(c/defcomponent results-template
  [{:keys [header quiz-results shopping-quiz-v2?]} _ _]
  [:div.bg-cool-gray
   [:div.max-580.col-12.bg-white
    (c/build header/mobile-nav-header-component header)]
   [:div.center.ptj2
    {:style {:padding-bottom "160px"}} ;; Footer height...
    [:div.flex.flex-column.px2
     [:div.shout.proxima.title-2 (:quiz.results/primary quiz-results)]
     [:div.m3.canela.title-1 (:quiz.results/secondary quiz-results)]]
    (if shopping-quiz-v2?
      (c/elements quiz-results-shopping-quiz-v2-organism quiz-results :quiz.results/options)
      (c/elements quiz-results-organism quiz-results :quiz.results/options))]
   [:div.absolute.bottom-0.left-0.right-0
    dividers/green
    (let [{:quiz.alternative/keys [primary cta-label cta-target id]} quiz-results]
      [:div.bg-white.py5.flex.flex-column.center.items-center
       primary
       (ui/button-small-secondary (merge {:data-test id :class "mt2 mx-auto"}
                                         (apply utils/route-to cta-target)) cta-label)])]])

;;;; Queries: models -> ui

(defn progress<
  [progression]
  (let [extent (inc (count progression))]
    {:portions
     [{:bar/units   (* extent 3)
       :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
      {:bar/units (- 12 (* extent 3))}]}))

(defn- fmt
  [m k suffix delim]
  (->> m
       (mapv (comp #(str % suffix) k))
       (interpose delim)))

(defn ^:private formatted-lengths<
  [bundles closures]
  (apply str
         (cond-> (fmt bundles :hair/length "”" ", ")
           (seq closures)
           (concat [" + " (-> closures first :hair/length) "” Closure"]))))

(defn quiz-result-option<
  [idx {:as           looks-suggestion
        :product/keys [sku-ids]
        :hair/keys    [origin texture]
        img-id        :img/id}]
  (let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
        {bundles  "bundles"
         closures "closures"} (group-by :hair/family skus)]
    (merge
     {:quiz.result/id            (str "result-option-" idx)
      :quiz.result/index-label   (str "Option " (inc idx))
      :quiz.result/ucare-id      img-id
      :quiz.result/primary       (str origin " " texture)
      :quiz.result/secondary     (formatted-lengths< bundles closures)
      :quiz.result/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
      :quiz.result/cta-label     "Add To Bag"
      :quiz.result/cta-target    [e/biz|looks-suggestions|selected
                                  {:id            id
                                   :selected-look looks-suggestion}]
      :quiz.result/tertiary-note "Install Included"}

     (within :quiz.result-v2.images {:primary     img-id
                                     :secondaries [""]})

     (within :quiz.result-v2.image-grid {:height-in-num-px 240
                                         :gap-in-num-px 3})
     (within :quiz.result-v2.image-grid.hero {:image-url img-id
                                              :badge-url nil
                                              :gap-in-num-px 3})
     (within :quiz.result-v2.image-grid.hair-column {:images (map (fn [sku-id]
                                                                    {:image-url "http://placekitten.com/200/200"
                                                                     :length sku-id})
                                                                     sku-ids)})
     (within :quiz.result-v2.title {:primary (str origin " " texture " hair + free install service")})
     (within :quiz.result-v2.rating {:id           (str "adv-quiz-result-option-rating-" id)
                                     :star-count   4
                                     :review-count 32}) ;; TODO Use proper value
     (within :quiz.result-v2.price {:discounted-price 342.38
                                    :retail-price     462.38})
     (within :quiz.result-v2.line-item-summary {:primary (str (count sku-ids) " products in this look")}) ;; TODO Use proper value
     (within :quiz.result-v2.action {:id     (str "adv-quiz-choose-look-" id)
                                     :label  "Choose this look"
                                     :target [e/biz|looks-suggestions|selected
                                              {:id            id
                                               :selected-look looks-suggestion}]}))))

(defn quiz-results<
  [answers look-suggestions]
  (merge
   (if (every? #{:unsure} (vals answers))
     {:quiz.results/primary   "Still Undecided?"
      :quiz.results/secondary "These are our most popular styles."}
     {:quiz.results/primary   "Hair & Services"
      :quiz.results/secondary "We think these styles will look great on you."})
   {:quiz.results/options        (map-indexed quiz-result-option<
                                              look-suggestions)
    :quiz.alternative/primary    "Wanna explore more options?"
    :quiz.alternative/id         "quiz-result-alternative"
    :quiz.alternative/cta-target [e/navigate-category
                                  {:page/slug           "mayvenn-install"
                                   :catalog/category-id "23"}]
    :quiz.alternative/cta-label  "Browse Hair"}))

(defn see-results<
  [{:questioning/keys [unanswered]
    :keys [answers]}]
  (when (<= unanswered 1)
    {:action/id        "quiz-see-results"
     :action/disabled? (not (zero? unanswered))
     :action/target    [e/biz|questioning|submitted
                        {:questioning/id id
                         :answers        answers
                         :on/success     [e/biz|looks-suggestions|queried]}]
     :action/label     "See Results"}))

(defn questions<
  [questions answers progression]
  {:questions
   (for [[question-idx {question-id    :question/id
                        :question/keys [prompt info choices]}]
         (map-indexed vector questions)
         :when (< question-idx (inc (count progression)))]
     {:title/primary   prompt
      :title/id        (str "q-" question-idx)
      :title/secondary info
      :title/scroll-to (when (> question-idx 0)
                         (str "q-" question-idx))
      :choices
      (for [[choice-idx {choice-id    :choice/id
                         :choice/keys [answer img-url]}]
            (map-indexed vector choices)
            :let [answered? (= choice-id
                               (get answers question-id))]]
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

(defn ^:export page
  [state]
  (let [{:order.items/keys [quantity]} (api.orders/current state)
        {:keys [questions answers progression]
         :as   questioning}            (questioning/<- state id)
        looks-suggestions              (looks-suggestions/<- state id)
        header-data                    {:forced-mobile-layout? true
                                        :quantity              (or quantity 0)}
        shopping-quiz-v2?              (experiments/shopping-quiz-v2? state)]

    (cond
      (utils/requesting? state request-keys/new-order-from-sku-ids)
      (c/build loading-template)

      (or (wait/<- state id)
          (utils/requesting? state request-keys/get-products))
      (c/build waiting-template)

      (seq looks-suggestions)
      (->> {:quiz-results      (quiz-results< answers looks-suggestions)
            :header            header-data
            :shopping-quiz-v2? shopping-quiz-v2?}
           (c/build results-template))

      :else
      (->> {:header      header-data
            :progress    (progress< progression)
            :questions   (questions< questions answers progression)
            :see-results (see-results< questioning)}
           (c/build questioning-template)))))

;;;; Behavior

(defmethod fx/perform-effects e/navigate-adventure-quiz
  [_ _ _ _ _]
  (publish e/biz|looks-suggestions|reset
           {:id id})
  (publish e/biz|questioning|reset
           {:questioning/id id}))
