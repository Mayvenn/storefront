(ns appointment-booking.page
  (:require #?@(:cljs [[clojure.string :refer [join]]
                       [storefront.components.formatters :as formatters]])
            api.current
            api.orders
            [mayvenn.visual.tools :refer [with within]]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [mayvenn.visual.lib.radio-section :as radio-section]
            [mayvenn.concept.booking :as booking]
            [spice.maps :as maps]
            [spice.date :as date]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.effects :as fx]
            [storefront.keypaths :as k]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages
             :as messages
             :refer [handle-message] :rename {handle-message publish}]))

(defmethod fx/perform-effects e/navigate-adventure-appointment-booking
  [_ _event _ _ _state]
  (publish e/biz|follow|defined
           {:follow/start    [e/biz|appointment-booking|initialized]
            :follow/after-id e/biz|appointment-booking|done
            :follow/then     [e/biz|appointment-booking|navigation-decided
                              {:choices {:cart    e/navigate-cart
                                         :success e/navigate-adventure-match-success}}]}))


(defmethod fx/perform-effects e/navigate-shopping-quiz-unified-freeinstall-appointment-booking
  [_ _event _ _ _state]
  (publish e/biz|follow|defined
           {:follow/start    [e/biz|appointment-booking|initialized]
            :follow/after-id e/biz|appointment-booking|done
            :follow/then     [e/biz|appointment-booking|navigation-decided
                              {:choices {:success e/navigate-shopping-quiz-unified-freeinstall-match-success}}]}))

(defn split-month [week]
  #?(:cljs
     (->> week
          (map (partial formatters/format-date {:month "short" :year "numeric"}))
          distinct
          (join " / "))))

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


(c/defcomponent header  [{:keys [forced-mobile-layout? target back] :as data} _opts _owner]
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

(def ^:private day-column [::dummy-value 2 3 4 5 6 7 1])

(defn week-day-headers [week]
  (for [date week
        :let [col (date/weekday-index date)]]
    [:div.pt3.title-3.proxima.shout (grid-attrs [2 (get day-column col)]
                                                {:style {:width "2em"}})
     #?(:cljs (formatters/format-date {:weekday "short"} date))]))

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
                    (utils/fake-href e/biz|appointment-booking|date-selected {:date date}))))
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
                      :target      e/biz|appointment-booking|date-selected
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
                      :target      e/biz|appointment-booking|date-selected
                      :target-args {:date (date/add-delta selected-date {:days 7})}
                      :attrs       (grid-attrs [1 7])})

   (week-day-headers week)

   [:div.col-11 (grid-attrs [3 "1 / span 7"])
    [:div.mb4.border-bottom.border-gray]]

   (week-day-selectors selected-date earliest-available-date week)])


(defn time-radio-group [{:keys [selected-time-slot-id]}]
  (let [radio-name "appointment-time-slot-radio"]
    (into [:div]
          (for [{:slot/keys        [id]
                 :slot.picker/keys [copy]} booking/time-slots
                :let                       [radio-id (str radio-name "-" id)]]
            (radio-section/v2
             (merge {:dial.attrs/name          radio-name
                     :dial.attrs/id            radio-id
                     :dial.attrs/data-test     radio-name
                     :dial.attrs/class         "order-last"
                     :label.attrs/data-test-id id
                     :label.attrs/on-click     (utils/send-event-callback e/biz|appointment-booking|time-slot-selected
                                                                          {:time-slot id}
                                                                          {:prevent-default?  true
                                                                           :stop-propagation? true})
                     :label.attrs/class        "col-12 py3 px5"
                     :copy.attrs/class         "order-0 flex-auto proxima"
                     :copy.content/text        copy}
                    (when (= id selected-time-slot-id)
                      {:state/checked "checked"})))))))

(c/defcomponent body [data _opts _owner]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.col-10.py8
    (titles/canela-left (with :top-third.title data))]
   [:div.col-12.px2
    (week-view (with :appointment.picker.date data))]
   [:div.col-12.pt4.px2
    (time-radio-group (with :appointment.picker.time-slot data))]
   [:div.col-6.pt8
    (actions/medium-primary (with :continue.action data))]
   [:div.col-6.pt4
    (actions/medium-tertiary (with :skip.action data))]])

(c/defcomponent template
  [data _ _]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.bg-white.self-stretch
    (c/build header
             (with :appointment.header data))]
   (c/build body data)])

(defn find-previous-sunday [today]
  (let [i     (date/weekday-index today)]
    (date/add-delta today {:days (- i)})))

(defn get-weeks [starting-sunday]
  (let [num-of-shown-weeks 5 ;; NOTE: We want to show 4 weeks *after* the current date,
                             ;;       for a total of 5 weeks.
        length-of-week     7]
    (partition length-of-week
               (map #(date/add-delta starting-sunday {:days %})
                    (range (* num-of-shown-weeks length-of-week))))))


(defn query [app-state]
  (let [selected-time-slot      (booking/<- app-state ::booking/selected-time-slot)
        earliest-available-date (-> (date/now)
                                    (booking/start-of-day)
                                    (date/add-delta {:days 2}))
        selected-date           (or (booking/<- app-state ::booking/selected-date)
                                    earliest-available-date)

        shown-weeks           (-> earliest-available-date
                                  find-previous-sunday
                                  get-weeks)
        displayed-week        (or (get-week shown-weeks selected-date)
                                  (first shown-weeks))
        first-available-week? (= displayed-week (first shown-weeks))
        last-available-week?  (= displayed-week (last shown-weeks))
        time-slots            booking/time-slots]
    (merge
     (within :top-third.title
             {:primary "When do you want to get your hair done?"
              :id      "id"})
     (within :appointment.picker.time-slot
             {:selected-time-slot-id selected-time-slot
              :time-slots            time-slots})

     (within :appointment.picker.date
             {:week                    displayed-week
              :earliest-available-date earliest-available-date
              :selected-date           selected-date
              :first-available-week?   first-available-week?
              :last-available-week?    last-available-week?})
     (within :continue.action
             {:id        "summary-continue"
              :label     "Continue"
              :disabled? (not (and selected-time-slot selected-date))
              :target    [e/biz|appointment-booking|submitted]})
     (within :skip.action
             {:id     "booking-skip"
              :label  "skip this step"
              :target [e/biz|appointment-booking|skipped]}))))



(defn adv-flow-query [app-state]
  (let [nav-undo-stack (get-in app-state k/navigation-undo-stack)
        base           (query app-state)]
    (merge
     base
     (within :appointment.header
             {:forced-mobile-layout? true
              :target                e/navigate-adventure-find-your-stylist
              :back                  (first nav-undo-stack)
              :title/primary         "Some great copy can go here"
              :title/secondary       nil
              :title/id              "appointment.header.title"
              :title/icon            nil}))))

(defn ^:export adv-flow-page
  [app-state _]
  (c/build template (adv-flow-query app-state)))
