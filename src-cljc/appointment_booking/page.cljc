(ns appointment-booking.page
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]
                       [storefront.history :as history]])
            [api.catalog :refer [select ?service]]
            api.current
            api.orders
            [clojure.string :refer [starts-with? split join]]
            [mayvenn.concept.follow :as follow]
            [mayvenn.concept.looks-suggestions :as looks-suggestions]
            [mayvenn.concept.progression :as progression]
            [mayvenn.concept.questioning :as questioning]
            [mayvenn.concept.wait :as wait]
            [mayvenn.live-help.core :as live-help]
            [mayvenn.visual.lib.card :as card]
            [mayvenn.visual.lib.escape-hatch :as escape-hatch]
            [mayvenn.visual.lib.progress-bar :as progress-bar]
            [mayvenn.visual.lib.question :as question]
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [spice.core :as spice]
            [spice.maps :as maps]
            [spice.date :as date]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.platform.messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]))

(c/defcomponent header  [{:keys [forced-mobile-layout? primary target back] :as data} _opts _owner]
  (header/mobile-nav-header
   {:class (str "border-bottom border-gray "
                (when-not forced-mobile-layout?
                  "hide-on-dt"))
    :style {:height "70px"}}
   (c/html
    [:a.block.black.p2.flex.justify-center.items-center
     #_(apply utils/route-back-or-to back target)
     (svg/left-arrow {:width  "20"
                      :height "20"})])
   (c/html (titles/proxima-content (with :title data)))
   (c/html [:div])))

;; TODO(ellie, 2021-07-15): [START] Move to spice.date
(def ^:private months ["Jan."
             "Feb."
             "Mar."
             "Apr."
             "May"
             "Jun."
             "Jul."
             "Aug."
             "Sep."
             "Oct."
             "Nov."
             "Dec."])

(def ^:private days ["Mon"
                     "Tue"
                     "Wed"
                     "Thu"
                     "Fri"
                     "Sat"
                     "Sun"])

(defn ^:private start-of-day [date]
  #?(:cljs
     (doto date
       (.setHours 0)
       (.setMinutes 0)
       (.setSeconds 0)
       (.setMilliseconds 0))))

(defn ^:private name-of-day [date]
  (let [idx (dec (date/weekday-index date))]
    (get days idx)))

(defn ^:private name-of-month [date]
  (get months (.getMonth date)))

;; TODO(ellie, 2021-07-15): [END] Move to spice.date

(defn split-month [week]
  (->> week
       (map (fn [date]
              (str (name-of-month date)
                   " "
                   (.getFullYear date))))
       distinct
       (join " / ")))

(defn day-of-month-class [selectable? selected?]
  (cond
    (not selectable?)
    "gray"

    selected?
    "circle bg-pale-purple"))


(defn week-view [{:keys [shown-weeks
                         earliest-available-date
                         selected-date]}]
  (let [week (first shown-weeks)]
    [:div.p5

     (let []
       (into [:div {:style {:display               "grid"
                            :grid-template-columns "repeat(7, 1fr)"
                            :grid-auto-rows        "max-content"
                            :grid-column-gap       "20px"}}

              [:div.left {:style {:grid-row    1
                                  :grid-column 1}}
               (ui/back-caret {})]

              [:div.title-3.canela.col-12.center
               {:style
                {:grid-row    1
                 :grid-column "2 / span 5"}}
               (split-month week)]

              [:div {:style {:grid-row    1
                             :grid-column 7}}
               (ui/forward-caret {:class "right"})]


              [:div.col-12
               {:style {:margin      "auto"
                        :grid-row    "3"
                        :grid-column "1 / span 7"}}
               [:div.mb4.border-bottom.border-cool-gray]]]

             (for [date   week
                   header [true false]
                   :let   [col (date/weekday-index date)
                           day-of-month (.getDate date)
                           selected? (= (.getDate selected-date) day-of-month)
                           selectable? (<= (.getDate earliest-available-date)
                                           day-of-month)]]
               (if header
                 [:div.col-12.pb2.center
                  {:style {:grid-column col
                           :grid-row    2}}
                  (name-of-day date)]
                 [:div.col-12
                  {:style {:height      "2em"
                           :width       "2em"
                           :grid-row    4
                           :grid-column col
                           :margin      "auto"}}
                  [:div.flex.items-center.justify-center
                   {:style {:height "2em"
                            :width  "2em"}
                    :class (day-of-month-class selectable? selected?)}

                   day-of-month]]))))]))

(def ^:private
  time-slots [{:slot/id "08-to-11"
               :slot/copy "8:00am - 11:00am"}
              {:slot/id "11-to-14"
               :slot/copy "11:00am - 2:00pm"}
              {:slot/id "14-to-17"
               :slot/copy "2:00pm - 5:00pm"}])


(defn ^:private radio-section-v2
  [{:as   data
    :state/keys [checked disabled]}]
  (c/html
   [:label.flex.items-center
    (with :label.attrs data)
    [:div.circle.bg-white.border.flex.items-center.justify-center
     ^:attrs (cond-> (with :dial.attrs data)
               true     (assoc :style {:height "22px" :width "22px"})
               disabled (update :class str " bg-cool-gray border-gray"))
     (when checked
       [:div.circle.bg-p-color
        {:style {:height "10px" :width "10px"}}])]
    [:input.hide.mx2.h2
     ^:attrs (merge (with :input data)
                    {:type "radio"})]
    (into
     [:div ^:attrs (with :copy.attrs data)]
          (:copy.content/text data))]))


(defn time-radio-group [{:keys [selected-time-slot-id]}]
  (let [radio-name "appointment-time-slot-radio"]
    (into [:div]
          (for [{:slot/keys [id copy]} time-slots
                :let                   [radio-id (str radio-name "-" id)]]
            (radio-section-v2
             (merge {:dial.attrs/name          radio-name
                     :dial.attrs/id            radio-id
                     :dial.attrs/data-test     radio-name
                     :dial.attrs/class         "order-last"
                     :label.attrs/data-test-id id
                     :label.attrs/on-click     nil
                     :label.attrs/class        "col-12 py3 px5"
                     :copy.attrs/class         "order-0 flex-auto proxima"
                     :copy.content/text       copy}
                    (when (= id selected-time-slot-id)
                      {:state/checked "checked"})))))))

(c/defcomponent template
  [data _ _]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.bg-white.self-stretch
    #_[:div "suyp"]
    (c/build header
             (with :appointment.header data))
    (c/build progress-bar/variation-1
             (with :appointment.progression data))]
   [:div.col-10
    (titles/canela-left (with :title data))]
   [:div.col-12
    (week-view (with :appointment.picker.date data))]
   [:div.col-12
    (time-radio-group (with :appointment.picker.time-slot data))]
   [:div.col-6
    (actions/medium-primary (with :action data))]])



(defn find-previous-sunday [today]
  (let [i     (date/weekday-index today)]
    (date/add-delta today {:days (- i)})))

(defn get-weeks [starting-sunday]
  (let [num-of-shown-weeks 4
        length-of-week     7]
    (partition length-of-week
               (map #(date/add-delta starting-sunday {:days %})
                    (range (* num-of-shown-weeks length-of-week))))))

(defn query [app-state]
  (merge
   {:title/primary "When do you want to get your hair done?"
    ;; :title/secondary "secondary"
    :title/id      "id"
    :title/icon    nil
    :appointment.progression/portions
    [{:bar/units   6
      :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
     {:bar/units 6}]

    #_#_#_#_#_#_:appointment.header.back-navigation/id "adventure-back"
    :appointment.header.back-navigation/target         target
    :appointment.header.back-navigation/back           back

    :appointment.picker.time-slot/selected nil
    ;;:appointment.

    :action/id     "summary-continue"
    :action/label  "Continue"
    :action/target [e/redirect
                    {:nav-message
                     [e/navigate-shopping-quiz-unified-freeinstall-find-your-stylist]}]}
   (let  [earliest-available-date (-> (date/now)
                                      start-of-day
                                      (date/add-delta {:days 2}))]
     (within :appointment.picker.date
             {:earliest-available-date earliest-available-date
              :shown-weeks             (-> earliest-available-date
                                           find-previous-sunday
                                           get-weeks)
              :selected-date           (or
                                        nil ;; fill in from app-state
                                        earliest-available-date)}))
   (within :appointment.header
           {:forced-mobile-layout? true
            :target                e/navigate-home
            :back                  nil
            :title/primary         "Some great copy can go here"
            :title/secondary       nil
            :title/id              "appointment.header.title"
            :title/icon            nil})))

(defn ^:export page
  [app-state _]
  (c/build template (query app-state)))
