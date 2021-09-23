(ns mayvenn.shopping-quiz.unified-freeinstall
  "
  Visual Layer: Unified-Free Install shopping quiz
  "
  (:require
   [api.catalog :refer [?service select]]
   api.current
   api.orders
   [appointment-booking.core :as booking.core]
   [clojure.string :refer [split starts-with?]]
   [catalog.images :as catalog-images]
   [mayvenn.concept.looks-suggestions :as looks-suggestions]
   [mayvenn.concept.progression :as progression]
   [mayvenn.concept.questioning :as questioning]
   [mayvenn.concept.booking :as booking]
   [mayvenn.concept.wait :as wait]
   [mayvenn.live-help.core :as live-help]
   [mayvenn.visual.lib.card :as card]
   [mayvenn.visual.lib.escape-hatch :as escape-hatch]
   [mayvenn.visual.lib.progress-bar :as progress-bar]
   [mayvenn.visual.lib.question :as question]
   [mayvenn.visual.tools :refer [with within]]
   [mayvenn.visual.ui.actions :as actions]
   [mayvenn.visual.ui.titles :as titles]
   #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
              [storefront.history :as history]
              [storefront.hooks.exception-handler :as exception-handler]
              [storefront.hooks.quadpay :as quadpay]
              [storefront.hooks.stringer :as stringer]
              [storefront.hooks.reviews :as review-hooks]
              [storefront.browser.cookie-jar :as cookie-jar]
              [stylist-matching.search.filters-modal :as filter-menu]
              [storefront.platform.reviews :as reviews]])
   [spice.core :as spice]
   [spice.maps :as maps]
   [storefront.accessors.experiments :as experiments]
   [storefront.accessors.orders :as orders]
   [storefront.accessors.products :as products]
   [storefront.accessors.sites :as accessors.sites]
   [storefront.component :as c]
   [storefront.components.money-formatters :as mf]
   [storefront.components.formatters :as formatters]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.effects :as fx]
   [storefront.events :as e]
   [storefront.keypaths :as k]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages
    :refer [handle-message]
    :rename {handle-message publish}]
   [storefront.platform.component-utils :as utils]
   [storefront.request-keys :as request-keys]
   [storefront.trackings :as trackings]
   [stylist-matching.core :refer [stylist-matching<- query-params<- service-delimiter]]
   [stylist-matching.keypaths :as matching.k]
   [stylist-matching.stylist-results :as stylist-results]
   [stylist-matching.ui.stylist-search :as stylist-search]
   [storefront.routes :as routes]))

(def ^:private shopping-quiz-id :unified-freeinstall)

(defn header<
  [undo-history step]
  {:header/back    (not-empty (first undo-history))
   :header/target  [e/navigate-home]
   :header/primary (str "Hair Quiz (" step "/3)")})

(defn progress<
  [progression]
  (let [extent (apply max progression)]
    {:progress/portions
     [{:bar/units   (* extent 4)
       :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
      {:bar/units (- 12 (* extent 4))}]}))

(defn- fmt
  "TODO: burn into the model for suggestion look"
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

(defn mobile-nav-header [attrs left center right]
  (let [size {:width "80px" :height "55px"}]
    (c/html
     [:div.flex.items-center
      ^:attrs attrs
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline left]
      [:div.flex-auto.py3 ^:inline center]
      [:div.mx-auto.flex.items-center.justify-around {:style size} ^:inline right]])))

(defn quiz-header
  [{:keys [target back primary]}]
  (mobile-nav-header
   {:class "border-bottom border-gray bg-white black"
    :style {:height "70px"}}
   (c/html
    (if target
      [:div
       {:data-test "header-back"}
       [:a.block.black.p2.flex.justify-center.items-center
        (apply utils/route-back-or-to back target)
        (svg/left-arrow {:width  "20"
                         :height "20"})]]
      [:div {:key "center"}]))
   (c/html [:div.content-1.proxima.center primary])
   (c/html [:div {:key "right"}])))

;; Template: 3/Appointment Booking
(c/defcomponent appointment-booking-template
  [data _ _]
  [:div
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with "progress" data))
   (c/build booking.core/body data)])

;; Template: 3/Match Success
(c/defcomponent matched-success-template
  [data _ _]
  [:div.flex.flex-column.flex-auto.bg-pale-purple
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   (titles/canela-huge (with :title data))

   [:div.flex.flex-column.m3.g2
    (c/elements card/cart-item-1
                data
                :card/cart-items)]
   [:div.center.px4.my2
    [:div.mb4
     [:div.flex.justify-center.items-center.align-middle
      (svg/symbolic->html (:summary/icon data))
      (titles/proxima (with :summary-subtotal data))]
     [:div.strike (titles/proxima-tiny (with :summary-slash data))]
     (titles/proxima-large (with :summary-total data))]
    [:div.flex.justify-center.items-center
     (actions/large-primary (with :checkout data))]
    [:div.h5.black.py1.flex.items-center
     [:div.flex-grow-1.border-bottom.border-gray]
     [:div.mx2 (titles/proxima-tiny (with :or data))]
     [:div.flex-grow-1.border-bottom.border-gray]]
    [:div.flex.justify-center.items-center
     (actions/large-paypal (with :paypal data))]
    #?(:cljs
       [:div.my4
        (c/build quadpay/component
                 (with :quadpay data)
                 nil)])]
   (c/build escape-hatch/variation-1 (with :escape-hatch data))])

(defn ^:private hacky-stylist-image
  [stylist]
  (some->> stylist
           :stylist/portrait
           :resizable-url
           ui/ucare-img-id))

(defn ^:private hacky-cart-image
  [item]
  (some->> item
           :selector/images
           (filter #(= "cart" (:use-case %)))
           first
           :url
           ui/ucare-img-id))

(defn matched-success<
  [quiz-progression items waiter-order current-stylist undo-history quadpay-loaded? paypal-redirect?]
  (let [order-total (some-> waiter-order orders/products-subtotal)
        step        (apply max quiz-progression)]
    (merge
     (progress< quiz-progression)
     (header< undo-history step)
     {:title/primary              "You're All Set"
      :summary/icon               [:svg/discount-tag {:class  "mxnp6 fill-s-color pr1"
                                                      :height "2em" :width "2em"}]
      :summary-subtotal/primary   "Hair + Install"
      :summary-slash/primary      (some-> waiter-order :total mf/as-money)
      :or/primary                 "or"
      :escape-hatch.title/primary "Wanna explore more options?"
      :escape-hatch.action/id     "quiz-result-alternative"
      :escape-hatch.action/target [e/navigate-category
                                   {:page/slug           "mayvenn-install"
                                    :catalog/category-id "23"}]
      :escape-hatch.action/label  "Browse Hair"
      :summary-total/primary      (mf/as-money order-total)

      :checkout/label  "Checkout"
      :checkout/target [e/control-checkout-cart-submit]
      :checkout/id     "start-checkout-button"

      :paypal/target    [e/control-checkout-cart-paypal-setup]
      :paypal/spinning? paypal-redirect?
      :paypal/disabled? nil #_updating?
      :paypal/id        "paypal-checkout"

      :quadpay.quadpay/show?       quadpay-loaded?
      :quadpay.quadpay/order-total order-total
      :quadpay.quadpay/directive   :just-select
      :card/cart-items
      (conj
       (into []
             (map-indexed
              (fn [idx
                   {:keys [catalog/sku-id item/quantity legacy/product-name sku/title
                           join/facets sku/price hair/length]
                    :as   item}]
                (merge
                 {:id                      (str idx "-cart-item-" sku-id "-" quantity)
                  :idx                     idx
                  :title/id                (str "line-item-title-" sku-id)
                  :title/primary           (or product-name title)
                  :title/secondary         (some-> facets :hair/color :option/name)
                  :title/tertiary          [(str "qty. " quantity)]
                  :price-title/id          (str "line-item-price-ea-with-label-" sku-id)
                  :price-title/primary     (mf/as-money price)
                  :price-title/secondary   " each"
                  :thumbnail/id            sku-id
                  :thumbnail/sticker-label (some-> length
                                                   first
                                                   (str "”"))
                  :thumbnail/ucare-id      (hacky-cart-image item)})))
             items)
       (let [{:stylist/keys [id name]} current-stylist
             appointment-time-slot     (:appointment-time-slot waiter-order)
             idx                       (count items)]
         (merge
          {:id                            (str idx "-cart-item-stylist-" id)
           :idx                           idx
           :title/id                      "line-item-title-stylist"
           :title/primary                 name
           :title/secondary               "Your Certified Mayvenn Stylist"
           :thumbnail/id                  id
           :thumbnail/ucare-id            (hacky-stylist-image current-stylist)
           :stylist.rating/id             id
           :stylist.rating/value          (:stylist.rating/score current-stylist)}
          (within :booking.appointment-time-slot appointment-time-slot))))})))

;; Template: 3/Stylist Results
(def ^:private scrim-atom
  [:div.absolute.overlay.bg-darken-4])

(c/defcomponent stylist-results-template
  [{:keys [stylist-search-inputs results scrim? spinning?] :as data} _ _]
  [:div.center.flex.flex-column.flex-auto
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   (c/build stylist-results/search-inputs-organism
            stylist-search-inputs)
   (if spinning?
     [:div.mt6 ui/spinner]
     [:div.relative.stretch
      (c/build stylist-results/results-template results)
      (when scrim? scrim-atom)])])

(defn stylist-results<
  [quiz-progression
   matching
   skus-db
   undo-history
   google-loaded?
   convert-loaded?
   requesting?
   just-added-control?
   just-added-only?
   just-added-experience?
   stylist-results-test?
   address-field-errors]
  (merge
   (progress< quiz-progression)
   (header< undo-history (apply max quiz-progression))
   {:stylist-search-inputs (stylist-results/stylist-search-inputs<-
                            matching
                            google-loaded?
                            skus-db
                            address-field-errors)
    :spinning?             (or (empty? (:status matching))
                               requesting?
                               (and (not convert-loaded?)
                                    stylist-results-test?
                                    (or (not just-added-control?)
                                        (not just-added-only?)
                                        (not just-added-experience?))))

    :scrim?          (contains? (:status matching)
                                :results.presearch/name)
    :results         (stylist-results/results< matching
                                               just-added-only?
                                               just-added-experience?
                                               stylist-results-test?)}))

;; Template: 3/Find your stylist

(c/defcomponent find-your-stylist-template
  [data _ _]
  [:div.center.flex.flex-column
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   [:div.px2.mt8.pt4
    (c/build stylist-search/organism data)]])

(defn find-your-stylist<
  [quiz-progression {:google/keys [input location]} undo-history]
  (merge (progress< quiz-progression)
         (header< undo-history (apply max quiz-progression))
         {:stylist-search.title/id                        "find-your-stylist-stylist-search-title"
          :stylist-search.title/primary                   "Where do you want to get your hair done?"
          :stylist-search.location-search-box/id          "stylist-match-address"
          :stylist-search.location-search-box/placeholder "Enter address, city, or zip code"
          :stylist-search.location-search-box/value       (str input)
          :stylist-search.location-search-box/clear?      (seq location)
          :stylist-search.button/id                       "stylist-match-address-submit"
          :stylist-search.button/disabled?                (or (empty? location)
                                                              (empty? input))
          :stylist-search.button/label                    "Search"

          :stylist-search.button/target
          [e/biz|follow|defined
           {:follow/start    [e/control-adventure-location-submit]
            :follow/after-id e/flow|stylist-matching|resulted
            :follow/then     [e/top-stylist-navigation-decided
                              {:decision
                               {:top-stylist     e/navigate-shopping-quiz-unified-freeinstall-top-stylist
                                :stylist-results e/navigate-shopping-quiz-unified-freeinstall-stylist-results}}]}]}))

(defmethod fx/perform-effects e/top-stylist-navigation-decided
  [_ _ {:keys [decision] :follow/keys [args]} _ state]
  (->> [(:stylist-results decision)
        {:query-params (->> (stylist-matching<- state)
                            (query-params<- {}))}]
       (if (and
            (experiments/top-stylist? state)
            (->> (:results args)
                 (some :top-stylist)
                 boolean))
         [(:top-stylist decision)])
       #?(:cljs (apply history/enqueue-navigate))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-top-stylist
  [_ _ _args _ _state]
  (publish e/biz|follow|defined
           {:follow/after-id e/flow|stylist-matching|matched
            :follow/then     [e/post-stylist-matched-navigation-decided
                              {:decision
                               {:booking e/navigate-shopping-quiz-unified-freeinstall-appointment-booking
                                :success e/navigate-shopping-quiz-unified-freeinstall-match-success}}]}))


;; Template: 2/Summary

(c/defcomponent summary-template
  [data _ _]
  [:div.col-12.bg-pale-purple.stretch
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   [:div.flex.flex-column.justify-center.items-center.myj3
    [:div.col-8.my2
     (titles/canela (with :title data))]
    [:div.mb6.col-10.col-8-on-tb
     (c/build card/look-suggestion-1
              (with :suggestion data))]
    (actions/large-primary (with :action data))]])

(defn summary<
  [quiz-progression
   {:product/keys [sku-ids]
    :hair/keys    [origin texture]
    img-id        :img/id}
   undo-history]
  (let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
        {bundles  "bundles"
         closures "closures"} (group-by :hair/family skus)]
    (merge (progress< quiz-progression)
           (header< undo-history (apply max quiz-progression))
           {:title/primary            ["Nice choice!"
                                       "Now let's find a stylist near you!"]
            :suggestion/id            "selected-look"
            :suggestion/ucare-id      img-id
            :suggestion/primary       (str origin " " texture)
            :suggestion/secondary     (formatted-lengths< bundles closures)
            :suggestion/tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
            :suggestion/tertiary-note "Install Included"
            :action/id                "summary-continue"
            :action/label             "Continue"
            :action/target            [e/go-to-navigate
                                       {:target [e/navigate-shopping-quiz-unified-freeinstall-find-your-stylist]}]})))

;; Template: 2/Suggestions

(c/defcomponent suggestions-template-v2
  [data _ _]
  [:div.col-12.bg-cool-gray.stretch
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   [:div.flex.flex-column.mbj2
    (titles/canela-huge {:primary "Our picks for you"})
    (c/elements card/look-suggestion-2
                data
                :suggestions-v2)]
   (c/build escape-hatch/variation-1
            (with :escape-hatch data))])

(c/defcomponent suggestions-template
  [data _ _]
  [:div.col-12.bg-cool-gray.stretch
   [:div.bg-white
    (quiz-header (with :header data))]
   (c/build progress-bar/variation-1 (with :progress data))
   [:div.flex.flex-column.mbj2
    (titles/canela-huge {:primary "Our picks for you"})
    (c/elements card/look-suggestion-1
                data
                :suggestions)]
   (c/build escape-hatch/variation-1
            (with :escape-hatch data))])

(defn suggestions<
  [products-db skus-db images-db quiz-progression looks-suggestions undo-history]
  (merge
   (progress< quiz-progression)
   (header< undo-history (apply max quiz-progression))
   {:escape-hatch.title/primary "Wanna explore more options?"
    :escape-hatch.action/id     "quiz-result-alternative"
    :escape-hatch.action/target [e/navigate-category
                                 {:page/slug           "mayvenn-install"
                                  :catalog/category-id "23"}]
    :escape-hatch.action/label  "Browse Hair"
    :suggestions                (for [[idx {:as           looks-suggestion
                                            :product/keys [sku-ids]
                                            :hair/keys    [origin texture]
                                            img-id        :img/id
                                            }]
                                      (map-indexed vector looks-suggestions)
                                      :let [skus                  (mapv looks-suggestions/mini-cellar sku-ids)
                                            {bundles  "bundles"
                                             closures "closures"} (group-by :hair/family skus)]]
                                  {:id            (str "result-option-" idx)
                                   :index-label   (str "Hair + Service Bundle " (inc idx))
                                   :ucare-id      img-id
                                   :primary       (str origin " " texture)
                                   :secondary     (formatted-lengths< bundles closures)
                                   :tertiary      (->> skus (mapv :sku/price) (reduce + 0) mf/as-money)
                                   :tertiary-note "Install Included"
                                   :action/id     (str "result-option-" idx)
                                   :action/label  "Choose this look"
                                   :action/target [e/biz|looks-suggestions|selected
                                                   {:id            shopping-quiz-id
                                                    :selected-look looks-suggestion
                                                    :on/success
                                                    [e/navigate-shopping-quiz-unified-freeinstall-summary]}]})

    :suggestions-v2 (for [looks-suggestion looks-suggestions
                          :let             [{:product/keys [sku-ids]
                                             :hair/keys    [origin texture]
                                             img-id        :img.v2/id} looks-suggestion
                                            skus                  (mapv skus-db sku-ids)
                                            service-sku           (get skus-db (:service/sku-id looks-suggestion))
                                            discounted-price (->> skus
                                                                  (remove #(= "service" (first (:catalog/department %))))
                                                                  (map :sku/price)
                                                                  (apply +))
                                            retail-price (+ discounted-price
                                                            (:sku/price service-sku))
                                            review-sku (first skus)
                                            review-product  (products/find-product-by-sku-id products-db (:catalog/sku-id review-sku))]]
                      (merge
                       (within :image-grid {:height-in-num-px 240
                                            :gap-in-num-px    3})
                       (within :image-grid.hero {:image-url     img-id
                                                 :badge-url     nil
                                                 :gap-in-num-px 3})
                       (within :image-grid.hair-column {:images (map (fn [sku]
                                                                       (let [image (catalog-images/image images-db "cart" sku)]
                                                                         {:image-url (:ucare/id image)
                                                                          :length    (str (first (:hair/length sku)) "\"")}))
                                                                     skus)})
                       (within :title {:primary (str origin " " texture " hair + free install service")})
                       (within :price {:discounted-price (mf/as-money discounted-price)
                                       :retail-price     (mf/as-money retail-price)})
                       (within :line-item-summary {:primary (str (count sku-ids) " products in this look")})
                       (within :action {:id     shopping-quiz-id
                                        :label  "Choose this look"
                                        :target [e/biz|looks-suggestions|selected
                                                 {:id            shopping-quiz-id
                                                  :selected-look looks-suggestion
                                                  :on/success
                                                  [e/navigate-shopping-quiz-unified-freeinstall-summary]}]})
                       #?(:cljs
                          (within :review (let [review-data (reviews/yotpo-data-attributes review-product skus-db)]
                                            {:yotpo-reviews-summary/product-title (some-> review-data :data-name)
                                             :yotpo-reviews-summary/product-id    (some-> review-data :data-product-id)
                                             :yotpo-reviews-summary/data-url      (some-> review-data :data-url)})))))}))

;; Template: 1/Questions

(c/defcomponent questions-template
  [data _ _]
  [:div
   (quiz-header (with :header data))
   (c/build progress-bar/variation-1 (with :progress data))
   [:div.flex.flex-column.mbj3.pbj3
    (c/elements question/variation-1
                data
                :questions)
    [:div.flex.justify-center.items-center
     (actions/large-primary (with :action data))]]])

(defn questions<
  [quiz-progression questions answers progression undo-history]
  (merge
   (let [unanswered (- (count questions)
                       (count progression))]
     (when (<= unanswered 1)
       {:action/id        "quiz-see-results"
        :action/disabled? (not (zero? unanswered))
        :action/target    [e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-recommendations
                                                      {:query-params
                                                       (->> answers
                                                            (maps/map-values spice/kw-name))}]}]
        :action/label     "See Results"}))
   (progress< quiz-progression)
   (header< undo-history (apply max quiz-progression))
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
                       {:questioning/id shopping-quiz-id
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
    (quiz-header (with :header data))]
   [:div.flex.flex-column.items-center.justify-center.flex-auto
    [:div.col-10
     (titles/canela-huge (with :title data))]
    [:div.col-6
     (actions/medium-primary (with :action data))]]])

(defn intro<
  [undo-history step]
  {:title/icon      [:svg/heart {:style {:height "41px" :width "37px"}
                                 :class "fill-p-color"}]
   :title/primary   ["Hair + Service" "One Price"]
   :title/secondary (str "This short quiz will help you find your look "
                         "and stylist to complete your install in your area.")

   :header/back     (not-empty (first undo-history))
   :header/target   [e/navigate-home]
   :header/primary  "Meet Your Stylist" #_(str "Hair Quiz (" step "/3)")

   :action/id     "quiz-continue"
   :action/label  "Continue"
   :action/target [e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-question]}]})

;;;
(c/defcomponent loading-template
  [_ _ _]
  [:div.flex.items-center.justify-between.col-12
   [:div.mx-auto
    (ui/large-spinner {:style {:height "80px"
                               :width  "80px"}})]])

(defn ^:export page
  "
  Shopping Quiz: Unified Products+Service v1

  An adventure for helping customers find hair products for a look
  combined with picking a stylist that can do that look.
  "
  [state]
  (let [quiz-progression (progression/<- state shopping-quiz-id)
        step             (apply max quiz-progression)
        undo-history     (get-in state storefront.keypaths/navigation-undo-stack)]
    (case step

      ;; STEP 3:
      3 (let [matching                    (stylist-matching<- state)
              current-stylist             (api.current/stylist state)
              skus-db                     (get-in state k/v2-skus)
              {:order/keys [items]
               order       :waiter/order} (api.orders/current state)

              ;; Externals
              google-loaded?   (get-in state k/loaded-google-maps)
              convert-loaded?  (get-in state k/loaded-convert)
              quadpay-loaded?  (get-in state k/loaded-quadpay)
              paypal-redirect? (get-in state k/cart-paypal-redirect)

              requesting?
              (or
               (utils/requesting? state request-keys/fetch-matched-stylists)
               (utils/requesting? state request-keys/fetch-stylists-matching-filters)
               (utils/requesting? state request-keys/get-products))

              ;; Experiments
              just-added-control?    (experiments/just-added-control? state)
              just-added-only?       (experiments/just-added-only? state)
              just-added-experience? (experiments/just-added-experience? state)
              stylist-results-test?  (experiments/stylist-results-test? state)
              easy-booking?          (experiments/easy-booking? state)
              booking-done           (booking/<- state ::booking/done)

              address-field-errors (get-in state matching.k/address-field-errors)
              stylist-matched?     (or (:matched/stylist matching)
                                       (and
                                        (not matching)
                                        (api.current/stylist state)))]
          (cond
            ;; If the filter menu is open, render it
            #?(:clj nil :cljs (filter-menu/query state))
            #?(:clj nil :cljs (c/build filter-menu/component (filter-menu/query state)))

            (and easy-booking?
                 stylist-matched?
                 (not booking-done))
            (c/build appointment-booking-template
                     (merge (header< undo-history (apply max quiz-progression))
                            (progress< quiz-progression)
                            (booking.core/ufi-query state)))

            stylist-matched?
            (c/build matched-success-template
                     (matched-success< quiz-progression
                                       items
                                       order
                                       current-stylist
                                       undo-history
                                       quadpay-loaded?
                                       paypal-redirect?))

            ;; Search in progress -- prepared or resulted
            (or (:results/stylists matching)
                (:param/name matching)
                (:param/address matching))
            (c/build stylist-results-template
                     (stylist-results< quiz-progression
                                       matching
                                       skus-db
                                       undo-history
                                       google-loaded?
                                       convert-loaded?
                                       requesting?
                                       just-added-control?
                                       just-added-only?
                                       just-added-experience?
                                       stylist-results-test?
                                       address-field-errors))

            ;; Waiting for Google to load
            (not google-loaded?) ;; TODO(corey) different spinner
            (c/build waiting-template
                     waiting<)

            ;; Find your stylist
            :else
            (c/build find-your-stylist-template
                     (find-your-stylist< quiz-progression matching undo-history))))
      ;; STEP 2: choosing a look
      2 (let [looks-suggestions (looks-suggestions/<- state shopping-quiz-id)
              selected-look     (looks-suggestions/selected<- state shopping-quiz-id)
              products-db       (get-in state k/v2-products)
              skus-db           (get-in state k/v2-skus)
              images-db         (get-in state k/v2-images)]
          (cond
            (utils/requesting? state request-keys/new-order-from-sku-ids)
            (c/build loading-template)

            (utils/requesting? state request-keys/get-products)
            (c/build waiting-template waiting<)

            selected-look
            (c/build summary-template
                     (summary< quiz-progression selected-look undo-history))

            (experiments/shopping-quiz-v2? state)
            (c/build suggestions-template-v2
                     (suggestions< products-db skus-db images-db quiz-progression looks-suggestions undo-history))

            :else
            (c/build suggestions-template
                     (suggestions< products-db skus-db images-db quiz-progression looks-suggestions undo-history))))
      ;; STEP 1: Taking the quiz
      1 (let [{:keys [questions answers progression]} (questioning/<- state shopping-quiz-id)
              wait                                    (wait/<- state shopping-quiz-id)]
          (if wait
            (c/build waiting-template
                     waiting<)
            (c/build questions-template
                     (questions< quiz-progression questions answers progression undo-history))))
      ;; default or 0
      (c/build intro-template (intro< undo-history step)))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-intro
  [_ _ _ state _]
  #?(:cljs (do
             (google-maps/insert)
             (cookie-jar/save-unified-fi-quiz-entered (get-in state k/cookie) {:unified-fi-quiz true})))
  (publish e/biz|progression|reset
           #:progression
            {:id    shopping-quiz-id
             :value #{0}}))

(defmethod trackings/perform-track e/navigate-shopping-quiz-unified-freeinstall-intro
  [_ _ args state]
  #?(:cljs
     (let [has-undo-stack? (seq (get-in state k/navigation-undo-stack))
           has-redo-stack? (seq (get-in state k/navigation-redo-stack))
           double-nav?  (= :module-load (:navigate/caused-by args))
           nav-location (get-in args [:query-params :location])
           location     (cond
                          nav-location                                 nav-location
                          has-undo-stack?                              "back_button"
                          has-redo-stack?                              "back_button"
                          double-nav?                                  :double-nav
                          :else                                        "direct_load")]
       (when-not (= :double-nav location)
         (stringer/track-event "unified_fi-initiated" {:location location})))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-question
  [_ _ _ _ state]
  (let [progress (progression/<- state shopping-quiz-id)]
    (if (nil? progress)
      (publish e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-intro]})
      (do
        (publish e/biz|progression|progressed
                 #:progression
                  {:id    shopping-quiz-id
                   :value 1
                   :regress #{2 3}})
        (publish e/biz|questioning|reset
                 {:questioning/id shopping-quiz-id})
        (publish e/biz|looks-suggestions|reset
                 {:id shopping-quiz-id})))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-recommendations
  [_ _ {params :query-params} _ _]
  (if (nil? params)
    (publish e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-intro]})
    (do
      #?(:cljs (review-hooks/insert-reviews))
      (publish e/biz|looks-suggestions|reset
               {:id shopping-quiz-id})
      (publish e/biz|progression|progressed
               #:progression
                {:id    shopping-quiz-id
                 :value 2
                 :regress #{3}})
      (publish e/biz|questioning|submitted
               {:questioning/id shopping-quiz-id
                :answers        (maps/map-values keyword params)
                :on/success     [e/biz|looks-suggestions|queried]}))))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-summary
  [_ _ _ _ state]
  (if (looks-suggestions/selected<- state shopping-quiz-id)
    (publish e/biz|progression|progressed
             #:progression
              {:id      shopping-quiz-id
               :value   2
               :regress #{3}})
    (publish e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-intro]})))

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
  [_ _ {:keys [navigate/caused-by]} _prev-state state]
  (when (not= caused-by :first-nav)
    #?(:cljs (google-maps/insert)))
  (publish e/biz|progression|progressed
           #:progression
            {:id    shopping-quiz-id
             :value 3})
  (publish e/flow|stylist-matching|initialized)
  (when-let [preferred-services (->> (api.orders/current state)
                                     :order/items
                                     (select ?service)
                                     (reduce services->srv-sku-ids [])
                                     not-empty)]
    (publish e/flow|stylist-matching|param-services-constrained
             {:services preferred-services})))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-stylist-results
  [_ _ {{preferred-services :preferred-services
         moniker            :name
         stylist-ids        :s
         latitude           :lat
         longitude          :long
         address            :address} :query-params
        :as                           args} state state']
  (publish e/biz|follow|defined
           {:follow/after-id e/flow|stylist-matching|matched
            :follow/then     [e/post-stylist-matched-navigation-decided
                              {:decision
                               {:booking e/navigate-shopping-quiz-unified-freeinstall-appointment-booking
                                :success e/navigate-shopping-quiz-unified-freeinstall-match-success}}]})
  #?(:cljs
     (if-not (:unified-fi-quiz (cookie-jar/retrieve-unified-fi-quiz-entered (get-in state k/cookie)))
       (publish e/redirect {:nav-message [e/navigate-adventure-stylist-results args]})
       (do
         (google-maps/insert)
         (publish e/biz|progression|progressed
                  #:progression
                  {:id    shopping-quiz-id
                   :value 3}))))

;; Init the model if there isn't one, e.g. Direct load
  ;; NOTE(corey) perhaps reify params capture as event, for reuse

  (when-not (stylist-matching<- state')
    (publish e/flow|stylist-matching|initialized))
  (publish e/flow|stylist-matching|unmatched)

  ;; Pull stylist-ids (s) from URI; predetermined search results
  (when (seq stylist-ids)
    (publish e/flow|stylist-matching|param-ids-constrained
             {:ids stylist-ids}))

  ;; Pull name search from URI
  (publish e/flow|stylist-matching|set-presearch-field
           {:name moniker})
  (publish e/flow|stylist-matching|param-name-constrained
           {:name moniker})

  ;; Address from URI
  (publish e/flow|stylist-matching|set-address-field
           {:address address})

  (when-let [services (some-> preferred-services
                              not-empty
                              (split (re-pattern service-delimiter))
                              set)]
    (publish e/flow|stylist-matching|param-services-constrained
             {:services services}))
  ;; Pull lat/long from URI; search by proximity
  (when (and (not-empty latitude)
             (not-empty longitude))
    (publish e/flow|stylist-matching|param-location-constrained
             {:latitude  (spice/parse-double latitude)
              :longitude (spice/parse-double longitude)}))
  ;; FIXME(matching)
  (when-not (= (get-in state k/navigation-event)
               (get-in state' k/navigation-event))
    (publish e/initialize-stylist-search-filters))

  (publish e/flow|stylist-matching|searched))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-match-success
  [_ _ _ _ state]
  #?(:cljs (google-maps/insert))
  (if (or (stylist-matching<- state)
          (api.current/stylist state))
    (publish e/biz|progression|progressed
             #:progression
              {:id    shopping-quiz-id
               :value 3})
    (publish e/go-to-navigate {:target [e/navigate-shopping-quiz-unified-freeinstall-intro]})))

(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall
  [_ _ _ state _]
  (when-not (= :shop (accessors.sites/determine-site state))
    (publish e/go-to-navigate {:target [e/navigate-home]})))
