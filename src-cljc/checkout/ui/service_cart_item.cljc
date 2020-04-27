(ns checkout.ui.service-cart-item
  (:require
   [storefront.routes :as routes]
   [storefront.component :as c]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.css-transitions :as css-transitions]
   [storefront.events :as e]
   [storefront.platform.component-utils :as utils]
   [storefront.platform.messages :as m]
   [ui.molecules :as M]))

(defn confetti-handler
  [mode]
  (when (= mode "ready")
    (m/handle-message e/set-confetti-mode {:mode "firing"})))

(defn ^:private service-cart-item-image-molecule
  [{:service-cart-item.image/keys [image-url id highlighted? locked? confetti-mode]}]
  (when id
    [:div.flex.justify-center
     (css-transitions/background-fade
      highlighted?
      {:style {:border-radius "50%"
               :width         "56px"
               :height        "56px"}
       ;; QUESTION(jeff): is this an appropriate place for click handler inside css-transition?
       :on-click #(confetti-handler confetti-mode)})

     [:div.relative
      (if locked?
        [:div
         [:div.absolute.z1.col-12.flex.items-center.justify-center
          {:style {:height "100%"}}
          [:div.absolute.z2.col-12.flex.items-center.justify-center
           {:style {:height "100%"}}
           (svg/stylist-lock {:style {:width   "18px"
                                      :height  "25px"}
                              :class "mtn2"})]]
         (ui/ucare-img {:width "56px"
                        :class "mtp3"
                        :style {:filter "contrast(0.1) brightness(1.75)"}} image-url)]
        (ui/ucare-img {:width "56px"
                       :class "mtp3"} image-url))]]))

(defn ^:private service-cart-item-title-molecule
  [{:service-cart-item.title/keys [primary secondary]}]
  (when primary
    [:div
     [:div.proxima.content-2
      primary]
     [:div.content-3 secondary]]))

(defn ^:private service-cart-item-action-molecule
  [{:service-cart-item.action/keys [id target spinning?]}]
  (when target
    (if spinning?
      [:div.h3
       {:style {:width "1.2em"}}
       ui/spinner]
      [:div
       [:a.gray.medium
        (merge {:data-test id}
               (apply utils/fake-href target))
        (svg/line-item-delete {:width  "16px"
                               :height "17px"})]])))

(defn ^:private service-cart-item-price-molecule
  [{:service-cart-item.price/keys [id value]}]
  (when id
    [:div.right.right-align.proxima.content-2
     {:data-test id
      :style     {:height "100%"}}
     value]))

(defn ^:private completed-progress-circle-atom
  [i _]
  [:div.bg-s-color.flex.justify-center.items-center
   {:key (str "complete-" i)
    :style {:height 21
            :width  21
            :border-radius "50%"}}
   (svg/check-mark {:class "fill-white"
                    :style {:width "11px"
                            :height "11px"}})])

(defn ^:private incomplete-progress-circle-atom
  [i content]
  [:div.bg-cool-gray.flex.justify-center.items-center.proxima.button-font-2
   {:key (str "incomplete-" i)
    :style {:height 20
            :width  20
            :border-radius "50%"}}
   (svg/check-mark {:class "fill-gray"
                    :style {:width "13px"
                            :height "13px"}})])

(def ^:private steps-hyphen-seperator-atom
  [:div.border-top.border-gray])

(defn ^:private service-cart-item-steps-molecule
  [{:service-cart-item.steps-to-complete/keys
    [steps current-step label target id]}]
  (when id
    [:div.items-center.mr2.mt2.flex.flex-wrap
     (when (pos? steps)
       (let [[completed uncompleted] (split-at current-step (->> steps range (map inc)))]
         [:div.flex.items-center.mr3.mb2 ; margin bottom for 320px screens
          (interpose (conj steps-hyphen-seperator-atom
                           {:class "mx1"
                            :style {:width "11px"}})
                     (concat
                      (map-indexed completed-progress-circle-atom
                                   completed)
                      (map-indexed incomplete-progress-circle-atom
                                   uncompleted)))]))
     (when (and target label)
       [:div.mb2 ; margin bottom for 320px screens
        (ui/button-small-primary (assoc (apply utils/route-to target)
                                        :data-test id)
                                 label)])]))

(c/defdynamic-component service-cart-item-modal-button
  (did-mount
   [this]
   (let [{:service-cart-item.modal-button/keys [id locked? tracking-target]}
         (c/get-props this)]
     (when (and id (false? locked?) tracking-target)
       (apply m/handle-message tracking-target))))

  (did-update
   [this prev-props prev-state snapshot]
   #?(:cljs
      (let [{was-locked? :service-cart-item.modal-button/locked?}       (.-props prev-props)
            {is-locked?  :cart-item-modify-button/locked?
                         :service-cart-item.modal-button/keys [tracking-target id]} (c/get-props this)]
        (when (and id
                   was-locked?
                   (not is-locked?)
                   tracking-target)
          (apply m/handle-message tracking-target)))))

  (render
   [this]
   (c/html
    (let [{:service-cart-item.modal-button/keys [id target label locked?]}
          (c/get-props this)]
      (when id
        [:div.flex
         (ui/button-small-secondary (merge {:class     "p-color bold mt1"
                                            :disabled? locked?
                                            :data-test id}
                                           (apply utils/route-to target))
                                    label)])))))
(c/defcomponent organism
  [data _ _]
  [:div.p3.flex.bg-white
   (service-cart-item-image-molecule data)
   [:div.flex-grow-1
    [:div.flex
     [:div.flex-grow-1.ml2
      (service-cart-item-title-molecule data)]
     [:div.right.right-align.pt1.flex.flex-column.items-end
      {:style {:min-width "67px"}}
      (service-cart-item-action-molecule data)
      (service-cart-item-price-molecule data)]

     (service-cart-item-steps-molecule data)
     (c/build service-cart-item-modal-button data nil)]]])
