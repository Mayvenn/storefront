(ns storefront.components.accordion
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]))

(defn- slide-down [content]
  content
  #_
  (css-transitions/transition-group
   {:classNames "slide-down"
    :in         true
    :timeout    300}
   (component/html
    content)))

(defn section
  "A data constructor for convenience"
  [title & paragraphs]
  {:title title
   :paragraphs paragraphs})

(defn- section-element
  [expanded? index title paragraphs section-click-event]
  (component/html
   [:div.h5.py1.border-gray
    {:key   (str "accordion-" index)
     :class "border-bottom"}
    [:div.pointer.col-12.h5.py2.flex.items-center.justify-center
     (merge (utils/fake-href section-click-event {:index index})
            {:data-test (str "accordion-" index)})
     [:div.flex-auto title]
     [:div.px2
      (when expanded?
        {:class "rotate-180"})
      ^:inline (svg/dropdown-arrow {:class  "stroke-dark-gray"
                                    :style  {:stroke-width "3px"}
                                    :height "12px"
                                    :width  "12px"})]]
    (slide-down
     (when (or expanded? #?(:clj true)) ;; always show for server-side rendered html
       [:div.mr8
        (map-indexed (fn [i paragraph]
                       (component/html
                        [:p.py2.h6 {:key (str i)} paragraph]))
                     paragraphs)]))]))

(defcomponent component [{:keys [expanded-indices sections]} owner {:keys [section-click-event]}]
  [:div
   (for [[idx {:keys [title paragraphs]}] (map-indexed vector sections)]
     (section-element (contains? (set expanded-indices) idx) idx title paragraphs section-click-event))])
