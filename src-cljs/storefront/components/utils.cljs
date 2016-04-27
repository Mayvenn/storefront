(ns storefront.components.utils
  (:require [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.hooks.fastpass :as fastpass]
            [storefront.messages :refer [handle-message]]
            [sablono.core :refer-macros [html]]))

(defn position [pred coll]
  (first (keep-indexed #(when (pred %2) %1)
                       coll)))

(defn noop-callback [e] (.preventDefault e))

(defn send-event-callback [event & [args]]
  (fn [e]
    (.preventDefault e)
    (handle-message event args)
    nil))

(defn expand-menu-callback [keypath]
  (send-event-callback events/control-menu-expand {:keypath keypath}))

(defn collapse-all-menus-callback []
  (send-event-callback events/control-menu-collapse-all))

(defn route-to [navigation-event & [args]]
  {:href (routes/path-for navigation-event args)
   :on-click
   (fn [e]
     (.preventDefault e)
     (routes/enqueue-navigate navigation-event args))})

(defn current-page? [[current-event current-args] target-event & [args]]
  (and (= (take (count target-event) current-event) target-event)
       (reduce #(and %1 (= (%2 args) (%2 current-args))) true (keys args))))

(defn change-text [app-state owner keypath]
  {:value (get-in app-state keypath)
   :on-change
   (fn [e]
     (handle-message events/control-change-state
                     {:keypath keypath
                      :value (.. e -target -value)}))})

(defn fake-href [event & [args]]
  {:href "#"
   :on-click (send-event-callback event args)})

(defn fake-href-menu-expand [keypath]
  {:href "#"
   :on-click
     (send-event-callback events/control-menu-expand {:keypath keypath})})

(defn change-checkbox [app-state keypath]
  (let [checked-val (when (get-in app-state keypath) "checked")]
    {:checked checked-val
     :value checked-val
     :on-change
     (fn [e]
       (handle-message events/control-change-state
                       {:keypath keypath
                        :value (.. e -target -checked)}))}))

(defn change-radio [app-state keypath value]
  (let [keypath-val (get-in app-state keypath)
        checked-val (when (= keypath-val (name value)) "checked")]
    {:checked checked-val
     :on-change
     (fn [e]
       (handle-message events/control-change-state
                       {:keypath keypath
                        :value value}))}))

(defn change-file [event]
  {:on-change (fn [e]
                (handle-message event
                                {:file (-> (.. e -target -files)
                                           array-seq
                                           first)}))})

(def nbsp [:span {:dangerouslySetInnerHTML {:__html " &nbsp;"}}])
(def rarr [:span {:dangerouslySetInnerHTML {:__html " &rarr;"}}])
(def new-flag
  (html
   [:pyp1.right
    [:.inline-block.border.border-gray.gray.pp2
     [:div {:style {:margin-bottom "-2px" :font-size "7px"}} "NEW"]]]))

(defn spinner
  ([] (spinner {:width "100%" :height "32px"}))
  ([style] [:.img-spinner.bg-no-repeat.bg-center {:style style}]))

(defn drop-down [expanded? menu-keypath [link-tag & link-contents] menu]
  [:div
   (into [link-tag
          (fake-href events/control-menu-expand {:keypath menu-keypath})]
         link-contents)
   (when expanded?
     [:.relative.z1
      {:on-click #(handle-message events/control-menu-collapse-all)}
      [:.fixed.overlay]
      menu])])

(defn circle-picture
  ([src] (circle-picture {} src))
  ([{:keys [width] :as attrs :or {width "4em"}} src]
   [:.circle.bg-silver.overflow-hidden
    (merge {:style {:width width :height width}} attrs)
    [:img {:style {:width width :height width :object-fit "cover"} :src src}]]))

(defn navigate-community
  "Can't be a def because (fastpass/community-url) is impure."
  []
  {:href (or (fastpass/community-url) "#")
   :on-click (send-event-callback events/external-redirect-community)})
