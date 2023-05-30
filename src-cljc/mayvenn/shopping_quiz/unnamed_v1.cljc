(ns mayvenn.shopping-quiz.unnamed-v1
  "
  Visual Layer: Original version of Shopping Quiz
  "
  (:require #?@(:cljs
                [[storefront.hooks.reviews :as review-hooks]
                 [storefront.platform.reviews :as reviews]])
            api.orders
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.wait :as wait]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.lib.card :as card]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.dividers :as dividers]
            [catalog.images :as catalog-images]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.products :as products]
            [storefront.component :as c]
            [storefront.components.flash :as flash]
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
            [storefront.routes :as routes]
            [clojure.string :as str]))

(def ^:private shopping-quiz-id :unnamed-v1)

;; Visual

(c/defcomponent waiting-template
  [_ _ _]
  [:div.bg-pale-purple.absolute.overlay
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

(c/defcomponent look-2-wrapper [data _ opts]
  [:div.m3
   (c/build card/look-2 (with :quiz.result-v2 data) opts)])

(c/defcomponent results-template
  [{:keys [header quiz-results flash]} _ _]
  [:div.bg-cool-gray
   [:div.col-12.bg-white
    (c/build header/nav-header-component header)]
   (c/build flash/component flash nil)
   [:div.center.ptj2
    {:style {:padding-bottom "160px"}} ;; Footer height...
    [:div.flex.flex-column.px2
     [:div.shout.proxima.title-2 (:quiz.results/primary quiz-results)]
     [:div.m3.canela.title-1 (:quiz.results/secondary quiz-results)]]
    (c/elements look-2-wrapper quiz-results :quiz.results/options)]
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
         (cond-> (fmt bundles (comp first :hair/length) "”" ", ")
           (seq closures)
           (concat [" + " (-> closures first :hair/length first) "” Closure"]))))

(defn quiz-result-option<
  [products-db skus-db images-db idx
   {:as           looks-suggestion
    :product/keys [sku-ids]
    :hair/keys    [origin texture]
    img-id        :img/id
    v2-img-id     :img.v2/id}]
  (let [skus                  (mapv skus-db sku-ids)
        epitome-sku           (->> skus
                                   (sort-by :sku/price)
                                   first)
        epitome-product       (->> epitome-sku
                                   :catalog/sku-id
                                   (products/find-product-by-sku-id products-db))
        service-sku           (get skus-db (:service/sku-id looks-suggestion))
        {bundles  "bundles"
         closures "closures"} (group-by (comp first :hair/family) skus)
        discounted-price      (->> skus
                                   (remove #(= "service" (first (:catalog/department %))))
                                   (map :sku/price)
                                   (apply +))
        retail-price          (+ discounted-price
                                 (:sku/price service-sku))]
    (merge
     {:quiz.result/id            (str "result-option-" idx)
      :quiz.result/index-label   (str "Option " (inc idx))
      :quiz.result/ucare-id      img-id
      :quiz.result/primary       (str origin " " texture)
      :quiz.result/secondary     (formatted-lengths< bundles closures)
      :quiz.result/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
      :quiz.result/cta-label     "Add To Bag"
      :quiz.result/cta-target    [e/biz|looks-suggestions|selected
                                  {:id            shopping-quiz-id
                                   :selected-look looks-suggestion}]
      :quiz.result/tertiary-note "Install Included"}

     (within :quiz.result-v2.image-grid {:gap-px 3})

     (within :quiz.result-v2.image-grid.hero {:image-url     v2-img-id
                                              :badge-url     nil})
     #?(:cljs (within :quiz.result-v2.review (let [review-data (reviews/yotpo-data-attributes epitome-product skus-db)]
                                               {:yotpo-reviews-summary/product-title (some-> review-data :data-name)
                                                :yotpo-reviews-summary/product-id    (some-> review-data :data-product-id)
                                                :yotpo-reviews-summary/data-url      (some-> review-data :data-url)})))
     (within :quiz.result-v2.image-grid.hair-column {:images (map (fn [sku]
                                                                    (let [image (catalog-images/image images-db "cart" sku)]
                                                                      {:image-url (:ucare/id image)
                                                                       :length    (str (first (:hair/length sku)) "\"")}))
                                                                  skus)})
     (within :quiz.result-v2.title {:primary (str origin " " texture " hair + free install service")})
     (within :quiz.result-v2.price {:discounted-price (mf/as-money discounted-price)
                                    :retail-price     (mf/as-money retail-price)})
     (within :quiz.result-v2.line-item-summary {:primary (str (count sku-ids) " products in this look")})
     (within :quiz.result-v2.action {:id     (str "adv-quiz-choose-look-" (name shopping-quiz-id) "-" idx)
                                     :label  "Choose this look"
                                     :target [e/biz|looks-suggestions|selected
                                              {:id            shopping-quiz-id
                                               :selected-look looks-suggestion}]}))))

(defn quiz-results<
  [products-db skus-db images-db answers look-suggestions]
  (merge
   (if (every? #{:unsure} (vals answers))
     {:quiz.results/primary   "Still Undecided?"
      :quiz.results/secondary "These are our most popular styles."}
     {:quiz.results/primary   "Hair & Services"
      :quiz.results/secondary "We think these styles will look great on you."})
   {:quiz.results/options        (map-indexed (partial quiz-result-option< products-db skus-db images-db)
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
                        {:questioning/id shopping-quiz-id
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
                     {:questioning/id shopping-quiz-id
                      :question/idx   question-idx
                      :question/id    question-id
                      :choice/idx     choice-idx
                      :choice/id      choice-id}]
         :selected? answered?})})})

(defn ^:export page
  [state]
  (let [{:order.items/keys [quantity]} (api.orders/current state)
        {:keys [questions answers progression]
         :as   questioning}            (questioning/<- state shopping-quiz-id)
        skus-db                        (get-in state keypaths/v2-skus)
        products-db                    (get-in state keypaths/v2-products)
        images-db                      (get-in state keypaths/v2-images)
        looks-suggestions              (looks-suggestions/<- state shopping-quiz-id)
        header-data                    {:forced-mobile-layout? true
                                        :quantity              (or quantity 0)}
        flash             (when (seq (get-in state keypaths/errors))
                            {:errors {:error-code "generic-error"
                                      :error-message "Sorry, but we don't have this look in stock. Please try a different look."}})]

    (cond
      (utils/requesting? state request-keys/new-order-from-sku-ids)
      (c/build loading-template)

      (or (wait/<- state shopping-quiz-id)
          (utils/requesting? state request-keys/get-products))
      (c/build waiting-template)

      (seq looks-suggestions)
      (c/build results-template
               {:quiz-results      (quiz-results< products-db
                                                  skus-db
                                                  images-db
                                                  answers
                                                  looks-suggestions)
                :header            header-data
                :flash flash})

      :else
      (c/build questioning-template
               {:header      header-data
                :progress    (progress< progression)
                :questions   (questions< questions
                                         answers
                                         progression)
                :see-results (see-results< questioning)}))))
