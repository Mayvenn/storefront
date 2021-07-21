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
            [appointment-booking.keypaths :as k]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.keypaths :as storefront.k]
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
     (utils/route-back-or-to
            back
            target)
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

(defn grid-attrs
  ([[row col] opts]
   (maps/deep-merge opts
                    {:style {:grid-row row
                             :grid-column col}}))
  ([grid-spec]
   (grid-attrs grid-spec {})))

(def ^:private day-column [::dummy-value 2 3 4 5 6 7 1])

(defn week-day-headers [week]
  (for [date week
        :let [col (date/weekday-index date)]]
    [:div.pt3.title-3.proxima.shout (grid-attrs [2 (get day-column col)]
                                                {:style {:width "2em"}})
     (name-of-day date)]))

(defn week-day-selectors [selected-date earliest-available-date week]
  (for [date week
        :let [col (date/weekday-index date)
              day-of-month (.getDate date)
              selected? (= selected-date date)
              selectable? (<= earliest-available-date
                              date)]]
    [:a.black.flex.items-center.justify-center
     (grid-attrs [4 (get day-column col)]
                 (merge
                  {:style {:height "2em"
                           :width  "2em"}
                   :class (day-of-month-class selectable? selected?)}
                  (when selectable?
                    (utils/fake-href e/flow|appointment-booking|date-selected {:date date}))))
     day-of-month]))

(def arrow-directions
  {:left {:style {:transform "rotate(90deg)"}}
   :right {:style {:transform "rotate(-90deg)"}}})

(defn week-arrow [direction disabled?]
  (svg/dropdown-arrow
   (maps/deep-merge
    {:height "15px"
     :width  "15px"
     :class  (if disabled?
               "stroke-gray"
               "stroke-p-color")
     :style  {:stroke-width "1px"}}
    (arrow-directions direction))))

(defn week-contains-date? [selected-date week]
  (first (filter #(= selected-date %) week)))

(defn get-week [shown-weeks selected-date]
  (first (filter (partial week-contains-date? selected-date)
                 shown-weeks)))

(defn disablable-arrow [{:keys [disabled? direction target target-args attrs]}]
  (conj (if disabled?
          [:div.center attrs]
          [:a.center
           (maps/deep-merge attrs
                            (when disabled?
                              {:aria-disabled "true"
                               :disabled      "true"})
                            (utils/fake-href target target-args))])
        (week-arrow direction disabled?)))

(defn week-view [{:keys [week
                         first-available-week?
                         last-available-week?
                         earliest-available-date
                         selected-date]}]
  [:div {:style {:display               "grid"
                 :grid-template-columns "repeat(7, 1fr)"
                 :grid-auto-rows        "max-content"
                 :align-items           "center"
                 :justify-items         "center"}}
   (disablable-arrow {:disabled?   first-available-week?
                      :direction   :left
                      :target      e/control-appointment-booking-week-chevron-clicked
                      :target-args {:date
                                    (let [one-week-before-selected-date
                                          (date/add-delta selected-date {:days -7})]
                                      (if (date/after? one-week-before-selected-date earliest-available-date)
                                        one-week-before-selected-date
                                        earliest-available-date))}
                      :attrs       (grid-attrs [1 1])})

   [:div.center (grid-attrs [1 "2 / span 5"])
    [:div.title-3.canela.col-12 (split-month week)]]

   (disablable-arrow {:disabled?   last-available-week?
                      :direction   :right
                      :target      e/control-appointment-booking-week-chevron-clicked
                      :target-args {:date (date/add-delta selected-date {:days 7})}
                      :attrs       (grid-attrs [1 7])})

   (week-day-headers week)

   [:div.col-11 (grid-attrs [3 "1 / span 7"])
    [:div.mb4.border-bottom.border-gray]]

   (week-day-selectors selected-date earliest-available-date week)])

(def ^:private
  time-slots [{:slot/id "08-to-11"
               :slot/copy "8:00am - 11:00am"}
              {:slot/id "11-to-14"
               :slot/copy "11:00am - 2:00pm"}
              {:slot/id "14-to-17"
               :slot/copy "2:00pm - 5:00pm"}])

 ;; TODO(ellie, 2021-07-20): Move to a visual lib namespace
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
                     :label.attrs/on-click     (utils/send-event-callback e/control-appointment-booking-time-clicked
                                                                          {:slot-id id}
                                                                          {:prevent-default?  true
                                                                           :stop-propagation? true})
                     :label.attrs/class        "col-12 py3 px5"
                     :copy.attrs/class         "order-0 flex-auto proxima"
                     :copy.content/text        copy}
                    (when (= id selected-time-slot-id)
                      {:state/checked "checked"})))))))

(c/defcomponent template
  [data _ _]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.bg-white.self-stretch
    (c/build header
             (with :appointment.header data))
    (c/build progress-bar/variation-1
             (with :appointment.progression data))]
   [:div.col-10.py8
    (titles/canela-left (with :title data))]
   [:div.col-12.px2
    (week-view (with :appointment.picker.date data))]
   [:div.col-12.pt4.px2
    (time-radio-group (with :appointment.picker.time-slot data))]
   [:div.col-6.pt8
    (actions/medium-primary (with :continue.action data))]
   [:div.col-6.pt4
    (actions/medium-tertiary (with :skip.action data))]])

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
  (let [selected-time-slot        (get-in app-state k/booking-selected-time-slot)
        earliest-available-date   (-> (date/now)
                                      (start-of-day)
                                      (date/add-delta {:days 2}))
        selected-date             (or (get-in app-state k/booking-selected-date)
                                      earliest-available-date)
        nav-undo-stack            (get-in app-state storefront.k/navigation-undo-stack)
        shopping-quiz-unified-fi? (experiments/shopping-quiz-unified-fi? app-state)]
    (merge
     {:title/primary "When do you want to get your hair done?"
      ;; :title/secondary "secondary"
      :title/id      "id"
      :title/icon    nil
      :appointment.progression/portions
      [{:bar/units   6
        :bar/img-url "//ucarecdn.com/92611996-290e-47ae-bffa-e6daba5dd60b/"}
       {:bar/units 6}]

      :appointment.picker.time-slot/selected-time-slot-id selected-time-slot}
     (within :continue.action {:id        "summary-continue"
                               :label     "Continue"
                               :disabled? (not (and selected-time-slot selected-date))
                               :target    (if shopping-quiz-unified-fi?
                                            ;; TODO save to order
                                            [e/navigate-shopping-quiz-unified-freeinstall-match-success]
                                            [e/navigate-adventure-match-success])})
     (within :skip.action {:id     "booking-skip"
                           :label  "skip this step"
                           :target (if shopping-quiz-unified-fi?
                                     [e/navigate-shopping-quiz-unified-freeinstall-match-success]
                                     [e/navigate-adventure-match-success])})
     (let  [shown-weeks           (-> earliest-available-date
                                      find-previous-sunday
                                      get-weeks)
            week                  (or (get-week shown-weeks selected-date)
                                      (first shown-weeks))
            first-available-week? (= week (first shown-weeks))
            last-available-week?  (= week (last shown-weeks))]

       (within :appointment.picker.date
               (maps/auto-map week
                              earliest-available-date
                              selected-date
                              first-available-week?
                              last-available-week?)))

     (within :appointment.header
             {:forced-mobile-layout? true
              :target                e/navigate-adventure-find-your-stylist
              :back                  (first nav-undo-stack)
              :title/primary         "Some great copy can go here"
              :title/secondary       nil
              :title/id              "appointment.header.title"
              :title/icon            nil}))))

(defn ^:export page
  [app-state _]
  (c/build template (query app-state)))
