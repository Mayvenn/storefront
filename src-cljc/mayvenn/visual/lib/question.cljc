(ns mayvenn.visual.lib.question
  "
  Question prompts
  "
  (:require #?@(:cljs
                [[storefront.browser.scroll :as scroll]
                 storefront.frontend-trackings])
            [mayvenn.visual.tools :refer [with]]
            [storefront.assets :as assets]
            [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

;; TODO(corey) parameterize for color
(def checkmark-circle-atom
  [:div.circle.bg-p-color.flex.items-center.justify-center
   {:style {:height "20px" :width "20px"}}
   (svg/check-mark {:height "12px" :width "16px" :class "fill-white"})])

;; TODO(corey) this can be added to the title ns
(c/defdynamic-component quiz-question-title-molecule
  (did-mount
   [this]
   #?(:cljs
      (some->> (c/get-props this)
               :scroll-to
               (#(str "[data-scroll=" % "]"))
               scroll/scroll-selector-to-top)))
  (render
   [this]
   (let [{:keys [id primary secondary scroll-to]} (c/get-props this)]
     (when id
       (c/html
        [:div
         {:key id}
         (into
          [:div.canela.title-2.mtj3.ptj3
           (when scroll-to
             {:data-scroll scroll-to})]
          (interpose [:br] primary))
         [:div.content-2.dark-gray.my3
          secondary]])))))

(c/defcomponent quiz-question-choice-button-molecule
  [{:action/keys [id primary icon-url target selected?]} _ _]
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

(c/defcomponent variation-1
  [data _ _]
  [:div.mx6.pbj3
   (c/build quiz-question-title-molecule
            (with :title data))
   [:div.my2
    (c/elements quiz-question-choice-button-molecule
                data
                :choices)]])
