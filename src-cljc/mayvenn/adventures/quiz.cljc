(ns mayvenn.adventures.quiz
  "Visual Layer: Quiz in an adventure setting"
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.browser.scroll :as scroll]
                 [storefront.hooks.stringer :as stringer]
                 storefront.frontend-trackings
                 [storefront.trackings :as trackings]])
            api.orders
            [clojure.string :as string]
            [storefront.assets :as assets]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :as mf]
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
            [storefront.request-keys :as request-keys]
            [storefront.transitions :as t]
            [api.orders :as api.orders]))

;; state

(def questions
  [{:quiz/question-id     :texture
    :quiz/question-prompt ["Let's talk about texture."
                           "What do you want your final look to be?"]
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
    :quiz/question-prompt ["What about length?"]
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

(def BNS-short {:img/id          "16029dd0-285c-4bc6-803c-0c201c3d402c"
                :hair/origin     "Brazilian"
                :hair/texture    "Straight"
                :service/sku-id  "SRV-LBI-000"
                :product/sku-ids ["BNS10","BNS12","BNS14"]})
(def BNS-short-closure
  (-> BNS-short
      (update :product/sku-ids conj "BNSLC10")
      (assoc :service/sku-id  "SRV-CBI-000")))

(def BNS-medium {:img/id          "16029dd0-285c-4bc6-803c-0c201c3d402c"
                 :hair/origin     "Brazilian"
                 :hair/texture    "Straight"
                 :service/sku-id  "SRV-LBI-000"
                 :product/sku-ids ["BNS14","BNS16","BNS18"]})
(def BNS-medium-closure
  (-> BNS-medium
      (update :product/sku-ids conj "BNSLC14")
      (assoc :service/sku-id  "SRV-CBI-000")))

(def BNS-long {:img/id          "16029dd0-285c-4bc6-803c-0c201c3d402c"
               :hair/origin     "Brazilian"
               :hair/texture    "Straight"
               :service/sku-id  "SRV-LBI-000"
               :product/sku-ids ["BNS18","BNS20","BNS22"]})
(def BNS-long-closure
  (-> BNS-long
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BNSLC18")))

(def BNS-extra-long {:img/id          "16029dd0-285c-4bc6-803c-0c201c3d402c"
                     :hair/origin     "Brazilian"
                     :hair/texture    "Straight"
                     :service/sku-id  "SRV-LBI-000"
                     :product/sku-ids ["BNS22","BNS24","BNS26"]})
(def BNS-extra-long-closure
  (-> BNS-extra-long
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BNSLC18")))

(def BLW-short {:img/id          "f7568a58-d240-4856-9d7d-21096bafda1c"
                :hair/origin     "Brazilian"
                :hair/texture    "Loose Wave"
                :service/sku-id  "SRV-LBI-000"
                :product/sku-ids ["BLW10","BLW12","BLW14"]})
(def BLW-short-closure
  (-> BLW-short
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BLWLC10")))

(def BLW-medium {:img/id          "f7568a58-d240-4856-9d7d-21096bafda1c"
                 :hair/origin     "Brazilian"
                 :hair/texture    "Loose Wave"
                 :service/sku-id  "SRV-LBI-000"
                 :product/sku-ids ["BLW14","BLW16","BLW18"]})
(def BLW-medium-closure
  (-> BLW-medium
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BLWLC14")))

(def BLW-long {:img/id          "f7568a58-d240-4856-9d7d-21096bafda1c"
               :hair/origin     "Brazilian"
               :hair/texture    "Loose Wave"
               :service/sku-id  "SRV-LBI-000"
               :product/sku-ids ["BLW18","BLW20","BLW22"]})
(def BLW-long-closure
  (-> BLW-long
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BLWLC18")))

(def BLW-extra-long
  {:img/id          "f7568a58-d240-4856-9d7d-21096bafda1c"
   :hair/origin     "Brazilian"
   :hair/texture    "Loose Wave"
   :service/sku-id  "SRV-LBI-000"
   :product/sku-ids ["BLW22","BLW24","BLW26"]})
(def BLW-extra-long-closure
  (-> BLW-extra-long
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "BLWLC18")))

(def MBW-short {:img/id          "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                :hair/origin     "Malaysian"
                :hair/texture    "Body Wave"
                :service/sku-id  "SRV-LBI-000"
                :product/sku-ids ["MBW10","MBW12","MBW14"]})
(def MBW-short-closure
  (-> MBW-short
      (assoc :service/sku-id  "SRV-CBI-000")
      (update :product/sku-ids conj "MBWLC10")))

(def MBW-medium {:img/id          "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                 :hair/origin     "Malaysian"
                 :hair/texture    "Body Wave"
                 :service/sku-id  "SRV-LBI-000"
                 :product/sku-ids ["MBW14","MBW16","MBW18"]})
(def MBW-medium-closure
  (-> MBW-medium
      (assoc :service/sku-id "SRV-CBI-000")
      (update :product/sku-ids conj "MBWLC14")))

(def MBW-long {:img/id          "888b9c79-265d-4547-b8ce-0c7ce56c8741"
               :hair/origin     "Malaysian"
               :hair/texture    "Body Wave"
               :service/sku-id  "SRV-LBI-000"
               :product/sku-ids ["MBW18","MBW20","MBW22"]})
(def MBW-long-closure
  (-> MBW-long
      (assoc :service/sku-id "SRV-CBI-000")
      (update :product/sku-ids conj "MBWLC18")))

(def MBW-extra-long {:img/id          "888b9c79-265d-4547-b8ce-0c7ce56c8741"
                     :hair/origin     "Malaysian"
                     :hair/texture    "Body Wave"
                     :service/sku-id  "SRV-LBI-000"
                     :product/sku-ids ["MBW22","MBW24","MBW26"]})
(def MBW-extra-long-closure
  (-> MBW-extra-long
      (assoc :service/sku-id "SRV-CBI-000")
      (update :product/sku-ids conj "MBWLC18")))

(def mini-cellar
  {"BLW10"
   {:hair/family "bundles",
    :sku/price 59.99,
    :hair/length "10",
    :catalog/sku-id "BLW10",
    :legacy/variant-id 599},
   "BLW12"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "12",
    :catalog/sku-id "BLW12",
    :legacy/variant-id 2},
   "BLW14"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "14",
    :catalog/sku-id "BLW14",
    :legacy/variant-id 3},
   "BLW16"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "16",
    :catalog/sku-id "BLW16",
    :legacy/variant-id 4},
   "BLW18"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "18",
    :catalog/sku-id "BLW18",
    :legacy/variant-id 5},
   "BLW20"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "20",
    :catalog/sku-id "BLW20",
    :legacy/variant-id 6},
   "BLW22"
   {:hair/family "bundles",
    :sku/price 89.99,
    :hair/length "22",
    :catalog/sku-id "BLW22",
    :legacy/variant-id 7},
   "BLW24"
   {:hair/family "bundles",
    :sku/price 114.99,
    :hair/length "24",
    :catalog/sku-id "BLW24",
    :legacy/variant-id 8},
   "BLW26"
   {:hair/family "bundles",
    :sku/price 134.99,
    :hair/length "26",
    :catalog/sku-id "BLW26",
    :legacy/variant-id 9},
   "BLWLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "BLWLC10",
    :legacy/variant-id 600},
   "BLWLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "BLWLC14",
    :legacy/variant-id 12},
   "BLWLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "BLWLC18",
    :legacy/variant-id 13},
   "BNS10"
   {:hair/family "bundles",
    :sku/price 55.99,
    :hair/length "10",
    :img/url "//ucarecdn.com//",
    :catalog/sku-id "BNS10",
    :legacy/variant-id 479},
   "BNS12"
   {:hair/family "bundles",
    :sku/price 59.99,
    :hair/length "12",
    :catalog/sku-id "BNS12",
    :legacy/variant-id 80},
   "BNS14"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "14",
    :catalog/sku-id "BNS14",
    :legacy/variant-id 81},
   "BNS16"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "16",
    :catalog/sku-id "BNS16",
    :legacy/variant-id 82},
   "BNS18"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "18",
    :catalog/sku-id "BNS18",
    :legacy/variant-id 83},
   "BNS20"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "20",
    :catalog/sku-id "BNS20",
    :legacy/variant-id 84},
   "BNS22"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "22",
    :catalog/sku-id "BNS22",
    :legacy/variant-id 85},
   "BNS24"
   {:hair/family "bundles",
    :sku/price 109.99,
    :hair/length "24",
    :catalog/sku-id "BNS24",
    :legacy/variant-id 86},
   "BNS26"
   {:hair/family "bundles",
    :sku/price 129.99,
    :hair/length "26",
    :catalog/sku-id "BNS26",
    :legacy/variant-id 87},
   "BNSLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "BNSLC10",
    :legacy/variant-id 601},
   "BNSLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "BNSLC14",
    :legacy/variant-id 90},
   "BNSLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "BNSLC18",
    :legacy/variant-id 91},
   "MBW10"
   {:hair/family "bundles",
    :sku/price 64.99,
    :hair/length "10",
    :img/url "//ucarecdn.com//",
    :catalog/sku-id "MBW10",
    :legacy/variant-id 602},
   "MBW12"
   {:hair/family "bundles",
    :sku/price 69.99,
    :hair/length "12",
    :catalog/sku-id "MBW12",
    :legacy/variant-id 15},
   "MBW14"
   {:hair/family "bundles",
    :sku/price 74.99,
    :hair/length "14",
    :catalog/sku-id "MBW14",
    :legacy/variant-id 16},
   "MBW16"
   {:hair/family "bundles",
    :sku/price 79.99,
    :hair/length "16",
    :catalog/sku-id "MBW16",
    :legacy/variant-id 17},
   "MBW18"
   {:hair/family "bundles",
    :sku/price 84.99,
    :hair/length "18",
    :catalog/sku-id "MBW18",
    :legacy/variant-id 18},
   "MBW20"
   {:hair/family "bundles",
    :sku/price 89.99,
    :hair/length "20",
    :catalog/sku-id "MBW20",
    :legacy/variant-id 19},
   "MBW22"
   {:hair/family "bundles",
    :sku/price 94.99,
    :hair/length "22",
    :catalog/sku-id "MBW22",
    :legacy/variant-id 20},
   "MBW24"
   {:hair/family "bundles",
    :sku/price 119.99,
    :hair/length "24",
    :catalog/sku-id "MBW24",
    :legacy/variant-id 21},
   "MBW26"
   {:hair/family "bundles",
    :sku/price 139.99,
    :hair/length "26",
    :catalog/sku-id "MBW26",
    :legacy/variant-id 22},
   "MBWLC10"
   {:hair/family "closures",
    :sku/price 89.99,
    :hair/length "10",
    :catalog/sku-id "MBWLC10",
    :legacy/variant-id 603},
   "MBWLC14"
   {:hair/family "closures",
    :sku/price 94.99,
    :hair/length "14",
    :catalog/sku-id "MBWLC14",
    :legacy/variant-id 25},
   "MBWLC18"
   {:hair/family "closures",
    :sku/price 104.99,
    :hair/length "18",
    :catalog/sku-id "MBWLC18",
    :legacy/variant-id 26}

   "SRV-LBI-000"
   {:hair/family "bundles",
    :sku/price 0, ;; Free in this context
    :catalog/sku-id "SRV-LBI-000",
    :legacy/variant-id 1036}

   "SRV-CBI-000"
   {:hair/family "closures",
    :sku/price 0, ;; Free in this context
    :catalog/sku-id "SRV-CBI-000",
    :legacy/variant-id 1037}})

(def answers->results
  {:straight {:short      {:yes    [BNS-short              BNS-medium]
                           :no     [BNS-short-closure      BNS-medium-closure]
                           :unsure [BNS-short              BNS-short-closure]}
              :medium     {:yes    [BNS-medium             BNS-long]
                           :no     [BNS-medium-closure     BNS-long-closure]
                           :unsure [BNS-medium             BNS-medium-closure]}
              :long       {:yes    [BNS-long               BNS-extra-long]
                           :no     [BNS-long-closure       BNS-extra-long-closure]
                           :unsure [BNS-long               BNS-long-closure]}
              :extra-long {:yes    [BNS-extra-long         BNS-long]
                           :no     [BNS-extra-long-closure BNS-long-closure]
                           :unsure [BNS-extra-long         BNS-extra-long-closure]}
              :unsure     {:yes    [BNS-medium             BNS-long]
                           :no     [BNS-medium-closure     BNS-long-closure]
                           :unsure [BNS-medium             BNS-long-closure]}}
   :wavy     {:short      {:yes    [BLW-short              MBW-short]
                           :no     [BLW-short-closure      MBW-short-closure]
                           :unsure [BLW-short              MBW-short-closure]}
              :medium     {:yes    [BLW-medium             MBW-medium]
                           :no     [BLW-medium-closure     MBW-medium-closure]
                           :unsure [BLW-medium             MBW-medium-closure]}
              :long       {:yes    [BLW-long               MBW-long]
                           :no     [BLW-long-closure       MBW-long-closure]
                           :unsure [BLW-long               MBW-long-closure]}
              :extra-long {:yes    [BLW-extra-long         MBW-extra-long]
                           :no     [BLW-extra-long-closure MBW-extra-long-closure]
                           :unsure [BLW-extra-long         MBW-extra-long-closure]}
              :unsure     {:yes    [BLW-medium             MBW-long]
                           :no     [BLW-medium-closure     MBW-long-closure]
                           :unsure [BLW-medium             MBW-long-closure]}}
   :unsure   {:short      {:yes    [BNS-short              BLW-short]
                           :no     [BNS-short-closure      BLW-short-closure]
                           :unsure [BNS-short              BLW-short-closure]}
              :medium     {:yes    [BNS-medium             BLW-medium]
                           :no     [BNS-medium-closure     BLW-medium-closure]
                           :unsure [BNS-medium             BLW-medium-closure]}
              :long       {:yes    [BNS-long               BLW-long]
                           :no     [BNS-long-closure       BLW-long-closure]
                           :unsure [BNS-long               BLW-long-closure]}
              :extra-long {:yes    [BNS-extra-long         BLW-extra-long]
                           :no     [BNS-extra-long-closure BLW-extra-long-closure]
                           :unsure [BNS-extra-long         BLW-extra-long-closure]}
              :unsure     {:yes    [BNS-medium             BLW-long]
                           :no     [BNS-medium-closure     BLW-long-closure]
                           :unsure [BNS-medium             BLW-long-closure]}}})



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
  [{:quiz.question.choice.button/keys [id primary icon-url target selected?]} _ _]
  [:div
   {:key id}
   ;; TODO(corey) rationale for if
   ((if selected?
      ui/button-choice-selected
      ui/button-choice-unselected)
    (merge
     {:class "my2"}
     (when target
       {:data-test id
        :on-click  (apply utils/send-event-callback target)}))
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

(def wait-template
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
    {:quiz.see-results.button/id        "quiz-see-results"
     :quiz.see-results.button/disabled? (not= (count questions)
                                              (count progression))
     :quiz.see-results.button/target    [e/flow|quiz|submitted {:quiz/id :quiz/shopping}]
     :quiz.see-results.button/label     "See Results"}))

(defn quiz-questions<
  [quiz-answers progression]
  {:quiz/questions
   (for [[question-idx {:quiz/keys [question-id question-prompt question-info choices]}]
         (map-indexed vector questions)
         :when (< question-idx (inc (count progression)))]
     {:quiz.question.title/primary   question-prompt
      :quiz.question.title/id        (str "q-" question-idx)
      :quiz.question.title/secondary question-info
      :quiz.question.title/scroll-to (when (> question-idx 0)
                                       (str "q-" question-idx))
      :quiz.question/choices
      (for [[choice-idx {:quiz/keys [choice-id choice-answer img-url]}]
            (map-indexed vector choices)
            :let [answered? (= choice-id
                               (get quiz-answers question-id))]]
        #:quiz.question.choice.button
        {:icon-url  img-url
         :primary   choice-answer
         :id        (str (name question-id) "-" (name choice-id))
         :target    [e/flow|quiz|answered
                     {:quiz/id           :quiz/shopping
                      :quiz/question-idx question-idx
                      :quiz/question-id  question-id
                      :quiz/choice-idx   choice-idx
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

   [:div.center.ptj2
    {:style {:padding-bottom "160px"}} ;; Footer height...
    [:div.flex.flex-column.px2
     [:div.shout.proxima.title-2 (:quiz.results/primary quiz-results)]
     [:div.m3.canela.title-1 (:quiz.results/secondary quiz-results)]]
    (c/elements quiz-results-organism quiz-results :quiz.results/options)]
   [:div.absolute.bottom-0.left-0.right-0
    green-divider-atom
    (let [{:quiz.alternative/keys [primary cta-label cta-target id]} quiz-results]
      [:div.bg-white.py5.flex.flex-column.center.items-center
       primary
       (ui/button-small-secondary (merge {:data-test id :class "mt2 mx-auto"}
                                         (apply utils/route-to cta-target)) cta-label)])]])



(defn quiz-result-option< [idx quiz-result]
  (let [product-skus          (mapv  #(get mini-cellar %) (:product/sku-ids quiz-result))
        {bundles  "bundles"
         closures "closures"} (group-by :hair/family product-skus)
        includes-closure?     (seq closures)]
    {:quiz.result/id            (str "result-option-" idx)
     :quiz.result/index-label   (str "Option " (inc idx))
     :quiz.result/ucare-id      (:img/id quiz-result)
     :quiz.result/primary       (str (:hair/origin quiz-result) " " (:hair/texture quiz-result))
     :quiz.result/secondary     (str
                                 (string/join
                                  ", "
                                  (->> bundles
                                       (mapv :hair/length)
                                       (mapv #(str % "”"))))
                                 (when includes-closure?
                                   (str " + "
                                        (-> closures first :hair/length)
                                        "” Closure")))
     :quiz.result/tertiary      (->> product-skus (mapv :sku/price) (reduce + 0) mf/as-money)
     :quiz.result/cta-label     "Add To Bag"
     :quiz.result/cta-target    [e/flow|quiz|results|option-selected (select-keys quiz-result [:product/sku-ids :service/sku-id])]
     :quiz.result/tertiary-note "Install Included"}))

(defn quiz-results< [quiz-answers quiz-results]
  (merge
   (if (every? #{:unsure} (vals quiz-answers))
     {:quiz.results/primary   "Still Undecided?"
      :quiz.results/secondary "These are our most popular styles."}
     {:quiz.results/primary   "Hair & Services"
      :quiz.results/secondary "We think these styles will look great on you."})
   {:quiz.results/options        (map-indexed quiz-result-option< quiz-results)
    :quiz.alternative/primary    "Wanna explore more options?"
    :quiz.alternative/id         "quiz-result-alternative"
    :quiz.alternative/cta-target [e/navigate-category {:page/slug           "mayvenn-install"
                                                       :catalog/category-id "23"}]
    :quiz.alternative/cta-label  "Browse Hair"}))

(defn ^:export page
  [state]
  (let [{:order.items/keys [quantity]} (api.orders/current state)
        quiz-id                        :quiz/shopping
        progression                    (get-in state (conj k/models-progressions quiz-id))
        quiz-answers                   (get-in state (conj k/models-quizzes quiz-id))
        quiz-results                   (get-in state (conj k/models-quizzes-results quiz-id))
        header-data                    {:forced-mobile-layout? true
                                        :quantity              (or quantity 0)}]
    (cond
      (utils/requesting-from-endpoint? state request-keys/new-order-from-sku-ids)

      (c/html
       [:div.flex.items-center.justify-between.col-12
        [:div.mx-auto
         (ui/large-spinner {:style {:height "80px"
                                    :width  "80px"}})]])

      (or (get-in state (conj k/models-wait quiz-id))
          (utils/requesting-from-endpoint? state request-keys/get-products)) wait-template

      (seq quiz-results) (->> {:quiz-results (quiz-results< quiz-answers quiz-results)
                               :header       header-data}
                              (c/build results-template))
      :else              (->> {:header           header-data
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
  [_ _ {quiz-id :quiz/id} _ state]
  (publish e/flow|wait|begun {:wait/id quiz-id})
  (let [{:keys [texture length leave-out]} (get-in state (conj k/models-quizzes quiz-id))]
    (publish e/flow|quiz|results|resulted
             {:quiz/id      quiz-id
              :quiz/results (get-in answers->results [texture length leave-out])})))

(defmethod t/transition-state e/flow|quiz|results|resulted
  [_ _ {:quiz/keys [id results]} state]
  (-> state
      (assoc-in (conj k/models-quizzes-results id)
                results)))
(defmethod fx/perform-effects e/flow|quiz|results|resulted
  [_ _ {:quiz/keys [id results]} _ state]
  (publish e/ensure-sku-ids
           {:sku-ids (set
                      (concat
                       (mapcat :product/sku-ids results)
                       (map :service/sku-id results)))}))

#?(:cljs
   (defmethod trackings/perform-track e/flow|quiz|results|resulted
     [_ _ {:quiz/keys [id results]} state]
     (let [choices (get-in state (conj k/models-quizzes id))]
       (stringer/track-event
        "quiz_submitted"
        {:all_question_copy (mapv (comp (partial string/join " " ) :quiz/question-prompt) questions)
         :all_answers_copy  (mapv (fn [q]
                                    (let [choice-id (get choices (:quiz/question-id q))]
                                      (->> q
                                           :quiz/choices
                                           (filter (comp #{choice-id} :quiz/choice-id))
                                           first
                                           :quiz/choice-answer)))
                                  questions)
         :quiz_results      (->> results
                                 (mapv
                                  (fn [{product-sku-ids :product/sku-ids
                                        service-sku-id  :service/sku-id}]
                                    (->> (conj product-sku-ids service-sku-id)
                                         (mapv (fn [sku-id]
                                                 (-> (get mini-cellar sku-id)
                                                     (select-keys [:legacy/variant-id :catalog/sku-id]))))))))}))))

#?(:cljs
   (defmethod fx/perform-effects e/flow|quiz|results|option-selected
     [_ _ {product-sku-ids :product/sku-ids
           service-sku-id  :service/sku-id} _ state]
     (let [{servicing-stylist-id :services/stylist-id} (api.orders/services state (get-in state k/order))]
       (api/new-order-from-sku-ids (get-in state k/session-id)
                                   {:store-stylist-id     (get-in state k/store-stylist-id)
                                    :servicing-stylist-id servicing-stylist-id
                                    :sku-id->quantity     (->> (conj product-sku-ids service-sku-id)
                                                               (map (fn [s] [s 1]))
                                                               (into {}))}
                                   ;; TODO: more specific handler
                                   (fn [{:keys [order]}]
                                     (messages/handle-message
                                      e/api-success-update-order
                                      {:order    order
                                       :navigate e/navigate-cart})
                                     (storefront.frontend-trackings/track-cart-initialization
                                      "shopping-quiz"
                                      nil
                                      {:skus-db          (get-in state k/v2-skus)
                                       :image-catalog    (get-in state k/v2-images)
                                       :store-experience (get-in state k/store-experience)
                                       :order            order}))))))

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
            :progression/value question-idx}))

#?(:cljs
   (defmethod trackings/perform-track e/flow|quiz|answered
     [_ _ {:quiz/keys [question-idx choice-idx]} _]
     (let [{:quiz/keys [choices question-prompt]} (get questions question-idx)
           {:quiz/keys [choice-answer]}           (get choices choice-idx)]
       (stringer/track-event
        "quiz_question_answered"
        {:question_copy          (string/join " " question-prompt)
         :answer_option_selected choice-answer
         :question_position      question-idx
         :answer_position        choice-idx}))))

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
