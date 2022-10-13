(ns storefront.components.accordion-v2022-10
  (:require [mayvenn.visual.tools :refer [with within]]
            [storefront.component :as c]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.messages :refer [handle-message] :rename {handle-message publish}]
            [storefront.effects :as effects]))

;; TODO c'mon do this right
(def accordion--reset [:accordion :reset])
(def accordion--opened [:accordion :opened])
(def accordion--closed [:accordion :closed])

(def default-state
  #:accordion{:open-drawers        #{}
              :allow-multi-opened? false
              :allow-all-closed?   false})

(c/defcomponent drawer-component
  [{:keys [face contents opened? closeable? drawer-id accordion-id] :as drawer} _  opts]
  (let [{:accordion.drawer/keys [contents-component]
         opened-face-component :accordion.drawer.open/face-component
         closed-face-component :accordion.drawer.closed/face-component} opts]
    [:div
     (cond
       (not opened?)
       [:a.block.inherit-color.flex.justify-between
        ;; Button states: up, down, hidden
        (utils/fake-href accordion--opened
                         {:accordion/id accordion-id
                          :drawer-id    drawer-id})
        (c/build closed-face-component face)
        [:div.flex.items-center
         ^:inline (svg/dropdown-arrow {:class  "fill-black"
                                       :height "16px"
                                       :width  "16px"})]]

       (and opened? closeable?)
       [:a.block.inherit-color.flex.justify-between
        ;; Button states: up, down, hidden
        (utils/fake-href accordion--closed
                         {:accordion/id accordion-id
                          :drawer-id    drawer-id})
        (c/build opened-face-component face)
        [:div.flex.items-center
         {:style {:transform "scaleY(-1)"}}
         ^:inline (svg/dropdown-arrow {:class  "fill-black"
                                       :height "16px"
                                       :width  "16px"})]]

       :else
       [:div (c/build opened-face-component face)])
     [:div
      #?(:cljs
         (when (not opened?)
           {:class "hide"}))
      (c/build contents-component contents)]]))

(c/defdynamic-component component
  (did-mount
   [this]
   (publish accordion--reset
            (-> this
                c/get-opts
                (select-keys [:accordion/allow-all-closed? :accordion/allow-multi-opened?])
                (assoc :accordion/default-drawer-id (-> this c/get-props :drawers first :drawer-id)
                       :accordion/id                (-> this c/get-props :id)))))
  (render
   [this]
   (let [{:keys [id drawers
                 open-drawers]} (c/get-props this)
         {:accordion/keys [allow-all-closed?]
          :as             opts} (c/get-opts this)
         drawers'               (map (fn [drawer]
                                       (let [opened? (contains? (set open-drawers) (:drawer-id drawer))]
                                         (-> drawer
                                             (assoc :accordion-id id)
                                             (assoc :opened? opened?)
                                             (assoc :closeable? (and opened?
                                                                     (or allow-all-closed?
                                                                         (< 1 (count open-drawers))))))))
                                     drawers)]
     (when (and id (seq drawers))
       (c/html
        [:div.mx4
         {:key id}
         (c/elements drawer-component {:drawers drawers'} :drawers :default opts)])))))

(defn <-
  [state id]
  (merge
   (get-in state (conj keypaths/accordion id))
   {:accordion/id id}))

(defmethod transitions/transition-state accordion--reset
  [_ _ {:accordion/keys [id allow-multi-opened? allow-all-closed? default-drawer-id] :as args} state]
  (let [{:accordion/keys [open-drawers] :as accordion} (<- state id)
        corrected-open-drawers                         (if (and (not allow-all-closed?) (empty? open-drawers))
                                                         #{default-drawer-id}
                                                         open-drawers)]
    (assoc-in state (conj keypaths/accordion id)
              (merge default-state
                     (select-keys args [:accordion/allow-all-closed?
                                        :accordion/allow-multi-opened?])
                     {:accordion/open-drawers corrected-open-drawers}))))

(defmethod transitions/transition-state accordion--opened
  [_ _ {:accordion/keys [id] :keys [drawer-id]} state]
  (let [accordion (<- state id)]
    (cond-> state
      (:accordion/allow-multi-opened? accordion)
      (update-in (conj keypaths/accordion id :accordion/open-drawers)
                 (fnil conj #{})
                 drawer-id)

      (not (:accordion/allow-multi-opened? accordion))
      (assoc-in (conj keypaths/accordion id :accordion/open-drawers)
                #{drawer-id}))))

;; (defmethod effects/perform-effects accordion--opened)
;; Close the diff (opened' - opened)

(defmethod transitions/transition-state accordion--closed
  [_ _ {:accordion/keys [id] :keys [drawer-id]} state]
  (let [{:accordion/keys [open-drawers allow-all-closed?]} (<- state id)]
    (cond-> state

      (or allow-all-closed?
          (< 1 (count open-drawers)))
      (update-in (conj keypaths/accordion id :accordion/open-drawers)
                 disj
                 drawer-id))))
