(ns storefront.components.accordion
  (:require [storefront.component :as component]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]))

(defn- slide-down [& content]
  (css-transitions/transition-group
   {:transitionName          "slide-down"
    :transitionEnter         true
    :transitionEnterTimeout  300
    :transitionLeave         false
    :transitionLeaveTimeout  300}
   content))

(defn section
  "A data constructor for convenience"
  [title & paragraphs]
  {:title title
   :paragraphs paragraphs})

(defn- section-element
  [expanded? index title paragraphs section-click-event]
  [:div.h5.py3.border-gray
   (merge
    {:key (str "accordion-" index)}
    (when-not (zero? index)
      {:class "border-top"}))
   [:div.pointer.col-12.h5.py2.flex.items-center.justify-center
    (merge (utils/fake-href section-click-event {:index index})
           {:data-test (str "accordion-" index)})
    [:div.flex-auto title]
    [:div.px2
     (when expanded?
       {:class "rotate-180"})
     (svg/dropdown-arrow {:class  "stroke-dark-gray"
                          :style  {:stroke-width "3px"}
                          :height "12px"
                          :width  "12px"})]]
   (slide-down
    (when (or expanded? #?(:clj true)) ;; always show for server-side rendered html
      (into [:div.mr8]
            (map (fn [paragraph]
                   [:p.py2.h6 paragraph])
                 paragraphs))))])

(defn component [{:keys [expanded-indices sections]} owner {:keys [section-click-event]}]
  (component/create
   [:div
    (for [[idx {:keys [title paragraphs]}] (map-indexed vector sections)]
      (section-element (contains? (set expanded-indices) idx) idx title paragraphs section-click-event))]))
