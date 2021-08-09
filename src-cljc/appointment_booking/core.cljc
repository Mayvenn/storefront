(ns appointment-booking.core
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
             :refer [handle-message] :rename {handle-message publish}]
            ui.molecules))

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

(def ^:private day-column [::dummy-value 2 3 4 5 6 7 1])

(defn week-day-headers [week]
  (for [date week
        :let [col (date/weekday-index date)]]
    [:div.pt3.title-3.proxima.shout (grid-attrs [2 (get day-column col)]
                                                {:style {:width "2em"}})
     #?(:cljs (formatters/format-date {:weekday "short"} date))]))

(defn week-day-selectors [selected-date earliest-available-date week week-idx]
  (for [date week
        :let [col (date/weekday-index date)
              day-of-month (.getDate date)
              selected? (= selected-date date)
              selectable? (<= earliest-available-date
                              date)]]
    [:a.black.flex.items-center.justify-center
     (grid-attrs [4 (get day-column col)]
                 (merge
                  (let [id (str "date-selector--week-idx--"
                                week-idx
                                "--col--"
                                col)]
                    {:id        id
                     :date-test id
                     :style     {:height "2em"
                                 :width  "2em"}
                     :class     (day-of-month-class selectable? selected?)})
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
                         week-idx
                         prev-week-idx
                         next-week-idx
                         earliest-available-date
                         selected-date]}]
  [:div {:style {:display               "grid"
                 :grid-template-columns "repeat(7, 1fr)"
                 :grid-auto-rows        "max-content"
                 :align-items           "center"
                 :justify-items         "center"}}
   (disablable-arrow {:disabled?   (= week-idx prev-week-idx)
                      :direction   :left
                      :target      e/control-appointment-booking-caret-clicked
                      :target-args {:week-idx prev-week-idx}
                      :attrs       (grid-attrs [1 1])})

   [:div.center (grid-attrs [1 "2 / span 5"])
    [:div.title-3.canela.col-12 (split-month week)]]

   (disablable-arrow {:disabled?   (= week-idx next-week-idx)
                      :direction   :right
                      :target      e/control-appointment-booking-caret-clicked
                      :target-args {:week-idx next-week-idx}
                      :attrs       (grid-attrs [1 7])})


   (week-day-headers week)

   [:div.col-11 (grid-attrs [3 "1 / span 7"])
    [:div.mb4.border-bottom.border-gray]]

   (week-day-selectors selected-date earliest-available-date week week-idx)])


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


(c/defcomponent modal-body [data _opts _owner]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.col-10.py8
    (titles/canela (with :top-third.title data))]
   [:div.col-12.px2
    (week-view (with :appointment.picker.date data))]
   [:div.col-12.pt4.px2
    (time-radio-group (with :appointment.picker.time-slot data))]
   [:div.col-6.pt8
    [:div.flex.justify-center.items-center.dark-gray.content-4
     (:appointment-time-notice/primary data)]
    (actions/medium-primary (with :finish.action data))]
   [:div.col-6.pt4
    (actions/medium-tertiary (with :skip.action data))]])

(c/defcomponent body [data _opts _owner]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.col-10.py8
    (titles/canela-left (with :top-third.title data))]
   [:div.col-12.px2
    (week-view (with :appointment.picker.date data))]
   [:div.col-12.pt4.px2
    (time-radio-group (with :appointment.picker.time-slot data))]
   [:div.col-6.pt8
    [:div.flex.justify-center.items-center.dark-gray.content-4
     (:appointment-time-notice/primary data)]
    (actions/medium-primary (with :finish.action data))]
   [:div.col-6.pt4
    (actions/medium-tertiary (with :skip.action data))]])

(c/defcomponent template
  [data _ _]
  [:div.flex.flex-auto.flex-column.items-center.stretch
   [:div.bg-white.self-stretch
    (header/adventure-header (with :appointment.header data))]
   (c/build body data)])

(defn ^:private query [app-state]
  (let [{::booking/keys [selected-time-slot
                         selected-date]} (booking/<- app-state)
        {::booking/keys [earliest-available-date
                         weeks]}         (booking/read-view-model app-state)

        week-idx       (or (-> app-state
                                         booking/read-view-model
                                         ::booking/week-idx)
                                     0)
        displayed-week (nth weeks week-idx)
        time-slots     booking/time-slots]
    (merge
     (within :top-third.title
             {:primary "When do you want to get your hair done?"
              :id      "id"})
     (within :appointment.picker.time-slot
             {:selected-time-slot-id selected-time-slot
              :time-slots            time-slots})
     (within :appointment.picker.date
             {:week                    displayed-week
              :week-idx                week-idx
              :prev-week-idx           (max 0 (dec week-idx))
              :next-week-idx           (min (dec (count weeks))
                                            (inc week-idx))
              :earliest-available-date earliest-available-date
              :selected-date           selected-date})
     (within :finish.action
             {:disabled? (not (and selected-time-slot selected-date))
              :target    [e/biz|appointment-booking|submitted]})
     (within :appointment-time-notice
             {:primary (ui.molecules/human-readable-appointment-date selected-date
                                                                     selected-time-slot)}))))

(defn adv-flow-query [app-state]
  (let [nav-undo-stack (get-in app-state k/navigation-undo-stack)
        current-order  (api.orders/current app-state)
        basic-query    (query app-state)]
    (merge
     basic-query
     (within :top-third.title
             {:primary "When do you want to get your hair done?"
              :id      "id"})
     (within :appointment.header
             {:header.title/id               "adventure-title"
              :header.title/primary          "Appointment Booking"
              :header.back-navigation/id     "adventure-back"
              :header.back-navigation/target [e/navigate-adventure-find-your-stylist]
              :header.back-navigation/back   (first nav-undo-stack)
              :header.cart/id                "mobile-cart"
              :header.cart/value             (or (:order.items/quantity current-order) 0)
              :header.cart/color             "white"})
     (within :finish.action
             {:id        "appointment-booking-continue"
              :label     "Continue"})
     (within :skip.action
             {:id     "booking-skip"
              :label  "skip this step"
              :target [e/biz|appointment-booking|skipped]}))))

(defn ufi-query [app-state]
  (merge
   (query app-state)
   (within :top-third.title
           {:primary "When do you want to get your hair done?"
            :id      "id"})
   (within :finish.action
           {:id        "appointment-booking-continue"
            :label     "Continue"})
   (within :skip.action
           {:id     "booking-skip"
            :label  "skip this step"
            :target [e/biz|appointment-booking|skipped]})))

(defn modal-query [app-state]
  (merge
   (query app-state)
   (within :top-third.title
           {:primary "Edit Appointment"
            :id      "id"
            :icon    [:svg/calendar {:class  "fill-p-color"
                                     :width  "27px"
                                     :height "30px"}]})
   (within :finish.action
           {:id        "appointment-booking-update"
            :label     "Update"})))

(defn ^:export adv-flow-page
  [app-state _]
  (c/build template (adv-flow-query app-state)))
