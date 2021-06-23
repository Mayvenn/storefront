(ns mayvenn.shopping-quiz.unified-freeinstall
  "
  Visual Layer: Unified-Free Install shopping quiz
  "
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            [clojure.string :refer [starts-with?]]
            [api.catalog :refer [select ?service]]
            api.orders
            [mayvenn.concept.progression :as progression]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.wait :as wait]
            [mayvenn.visual.lib.card :as card]
            [mayvenn.visual.lib.escape-hatch :as escape-hatch]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.tools :refer [with]]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.ui.actions :as actions]
            [stylist-matching.core :refer [stylist-matching<-]]
            [stylist-matching.ui.stylist-search :as stylist-search]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.components.money-formatters :as mf]
            [storefront.platform.messages
             :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [spice.core :as spice]
            [spice.maps :as maps]))

(def ^:private id :unified-freeinstall)

(defn progress<
  [progression]
  (let [extent (apply max progression)]
    {:portions
     [{:bar/units   (* extent 3)
       :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
      {:bar/units (- 12 (* extent 3))}]}))

(defn- fmt
  "TODO: burn into the model for suggestion look"
  [m k suffix delim]
  (->> m
       (mapv (comp #(str % suffix) k))
       (interpose delim)))

;; Template: 3/Find your stylist

(c/defcomponent find-your-stylist-template
  [data _ _]
  [:div.center.flex.flex-column
   [:div.bg-white
    (c/build header/mobile-nav-header-component)]

   #_
   (header/adventure-header header)
   #_
   (component/build flash/component flash nil)
   [:div.px2.mt8.pt4
    (c/build stylist-search/organism data)]
   #_
   (if (seq spinner)
     (component/build spinner/organism spinner nil)
     [:div.px2.mt8.pt4
      (component/build stylist-search/organism stylist-search nil)])])

(defn find-your-stylist<
  [{:as stylist-matching :google/keys [input location]}]
  {:stylist-search.title/id                        "find-your-stylist-stylist-search-title"
   :stylist-search.title/primary                   "Where do you want to get your hair done?"
   :stylist-search.location-search-box/id          "stylist-match-address"
   :stylist-search.location-search-box/placeholder "Enter city or street address"
   :stylist-search.location-search-box/value       (str input)
   :stylist-search.location-search-box/clear?      (seq location)
   :stylist-search.button/id                       "stylist-match-address-submit"
   :stylist-search.button/disabled?                (or (empty? location)
                                                       (empty? input))
   :stylist-search.button/target                   [e/control-adventure-location-submit]
   :stylist-search.button/label                    "Search"})

;; Template: 2/Summary

(c/defcomponent summary-template
  [{:keys [header progress summary]} _ _]
  [:div.col-12.bg-pale-purple
   [:div.bg-white
    (c/build header/mobile-nav-header-component header)]
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.justify-center.items-center.myj3.pyj3
    [:div.col-8
     (titles/canela (with :title summary))]
    (c/build card/look-suggestion-1
             (with :suggestion summary))
    (actions/large-primary (with :action summary))]])

(defn summary<
  [{:product/keys [sku-ids]
    :hair/keys    [origin texture]
    img-id        :img/id}]
  (let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
        {bundles  "bundles"
         closures "closures"} (group-by :hair/family skus)]
    {:title/primary            ["Nice choice!"
                                 "Now let's find a stylist near you!"]
     :suggestion/id            "selected-look"
     :suggestion/ucare-id      img-id
     :suggestion/primary       (str origin " " texture)
     :suggestion/secondary     (apply str
                                      (cond-> (fmt bundles :hair/length "”" ", ")
                                        (seq closures)
                                        (concat [" + "]
                                                (fmt closures :hair/length "”" ""))))
     :suggestion/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
     :suggestion/tertiary-note "Install Included"
     :action/id                "summary-continue"
     :action/label             "Continue"
     :action/target            [e/redirect
                                {:nav-message
                                 [e/navigate-shopping-quiz-unified-freeinstall-find-your-stylist]}]}))

;; Template: 2/Suggestions

(c/defcomponent suggestions-template
  [{:keys [header progress suggestions]} _ _]
  [:div.col-12.bg-cool-gray
   [:div.bg-white
    (c/build header/mobile-nav-header-component header)]
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.mbj3.pbj3
    (titles/canela-huge {:primary "Our picks for you"})
    (c/elements card/look-suggestion-1
                suggestions
                :suggestions)]
   (c/build escape-hatch/variation-1
            (with :escape-hatch suggestions))])

(defn suggestions<
  [looks-suggestions]
  {:escape-hatch.title/primary "Wanna explore more options?"
   :escape-hatch.action/id     "quiz-result-alternative"
   :escape-hatch.action/target [e/navigate-category
                                {:page/slug           "mayvenn-install"
                                 :catalog/category-id "23"}]
   :escape-hatch.action/label  "Browse Hair"
   :suggestions
   (for [[idx {:as           looks-suggestion
               :product/keys [sku-ids]
               :hair/keys    [origin texture]
               img-id        :img/id}]
         (map-indexed vector looks-suggestions)
         :let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
               {bundles  "bundles"
                closures "closures"} (group-by :hair/family skus)]]
     {:id            (str "result-option-" idx)
      :index-label   (str "Hair + Service Bundle " (inc idx))
      :ucare-id      img-id
      :primary       (str origin " " texture)
      :secondary     (apply str
                            (cond-> (fmt bundles :hair/length "”" ", ")
                              (seq closures)
                              (concat [" + "]
                                      (fmt closures :hair/length "”" ""))))
      :tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
      :tertiary-note "Install Included"
      :action/id     (str "result-option-" idx)
      :action/label  "Choose this look"
      :action/target [e/biz|looks-suggestions|selected
                      {:id            id
                       :selected-look looks-suggestion
                       :on/success
                       [e/navigate-shopping-quiz-unified-freeinstall-summary]}]})})

;; Template: 1/Questions

(c/defcomponent questions-template
  [{:keys [header progress questions]} _ _]
  [:div
   (c/build header/mobile-nav-header-component header)
   (c/build progress-bar/variation-1 progress)
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements question/variation-1
                questions
                :questions)
    [:div.flex.justify-center.items-center
     (actions/large-primary (with :action questions))]]])

(defn questions<
  [questions answers progression]
  (merge
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
        :action/label     "See Results"}))
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
          :selected? answered?})})}))

;; Template: 1/Waiting

(c/defcomponent waiting-template
  [data _ _]
  [:div.bg-pale-purple.absolute.overlay
   [:div.absolute.overlay.border.border-white.border-framed-white.m4.p5.flex.flex-column.items-center.justify-center
    (titles/canela (with :title data))]])

(def waiting<
  {:title/icon    [:svg/mayvenn-logo
                   {:class "spin-y"
                    :style {:width "54px"}}]
   :title/primary ["Sit back and relax."
                   "There’s no end to what your hair can do."]})

;; Template: 0/Intro

(c/defcomponent intro-template
  [data _ _]
  [:div.flex.flex-column.stretch.bg-pale-purple
   [:div.bg-white.self-stretch
    (c/build header/mobile-nav-header-component (:header data))]
   [:div.flex.flex-column.items-center.justify-center.flex-auto
    [:div.col-10
     (titles/canela-huge (with :title data))]
    [:div.col-6
     (actions/medium-primary (with :action data))]]])

(def intro<
  {:title/icon      [:svg/heart {:style {:height "41px" :width "37px"}
                                 :class "fill-p-color"}]
   :title/primary   ["Hair + Service" "One Price"]
   :title/secondary (str "This short quiz (2-3 minutes) will help "
                         "you find the look and a stylist to complete "
                         "your install in your area")
   :action/id       "quiz-continue"
   :action/label    "Continue"
   :action/target   [e/redirect
                     {:nav-message
                      [e/navigate-shopping-quiz-unified-freeinstall-question]}]})

;;;

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
      3 (let [stylist-matching (stylist-matching<- state)]
          (c/build find-your-stylist-template
                   (find-your-stylist< stylist-matching)))
      2 (let [looks-suggestions (looks-suggestions/<- state id)
              selected-look     (looks-suggestions/selected<- state id)]
          (if selected-look
            (c/build summary-template
                     {:progress (progress< quiz-progression)
                      :summary  (summary< selected-look)})
            (c/build suggestions-template
                     {:progress    (progress< quiz-progression)
                      :suggestions (suggestions< looks-suggestions)})))
      1 (let [{:keys [questions answers progression]} (questioning/<- state id)
              wait                                    (wait/<- state id)]
          (if wait
            (c/build waiting-template
                     waiting<)
            (c/build questions-template
                     {:progress  (progress< quiz-progression)
                      :questions (questions< questions answers progression)})))
      ;; default or 0
      (c/build intro-template intro<))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-intro
  [_ _ _ _ _]
  #?(:cljs (google-maps/insert))
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

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-summary
  [_ _ _ _ state]
  (if (api.orders/current state)
    (publish e/biz|progression|progressed
             #:progression
             {:id    id
              :value 2})
    (fx/redirect e/navigate-shopping-quiz-unified-freeinstall-recommendations)))

(def ^:private sv2-codes->srvs
  {"LBI" "SRV-LBI-000"
   "CBI" "SRV-CBI-000"
   "FBI" "SRV-FBI-000"
   "3BI" "SRV-3BI-000"
   \3    "SRV-3CU-000"
   \C    "SRV-DPCU-000"
   \D    "SRV-TKDU-000"
   \F    "SRV-FCU-000"
   \L    "SRV-CCU-000"
   \T    "SRV-TRMU-000"})

(defn ^:private services->srv-sku-ids
  [srv-sku-ids {:keys [catalog/sku-id]}]
  (concat srv-sku-ids
          (if (starts-with? sku-id "SV2")
            (let [[_ base addons] (re-find #"SV2-(\w+)-(\w+)" sku-id)]
              (->> addons
                   (concat [base])
                   (map sv2-codes->srvs)
                   (remove nil?)))
            [sku-id])))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-find-your-stylist
  [_ _ _ _ state]
  (publish e/biz|progression|progressed
           #:progression
           {:id    id
            :value 3})
  #?(:cljs (google-maps/insert))
  (messages/handle-message e/flow|stylist-matching|initialized)
  (when-let [preferred-services (->> (api.orders/current state)
                                     :order/items
                                     (select ?service)
                                     (reduce services->srv-sku-ids [])
                                     not-empty)]
    (messages/handle-message e/flow|stylist-matching|param-services-constrained
                             {:services preferred-services})))
