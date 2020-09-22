(ns storefront.components.accordion
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]))

(defn section
  "A data constructor for convenience"
  [title & paragraphs]
  {:title      title
   :paragraphs paragraphs})

(defn- section-element
  [expanded? index title paragraphs section-click-event]
  (component/html
   [:div.h5.py1 {:key (str "accordion-" index)}
    [:div.pointer.col-12.h5.py2.flex.items-center.justify-center
     ^:attrs (merge (utils/fake-href section-click-event {:index index})
                    {:data-test (str "accordion-" index)})
     [:div.flex-auto title]
     [:div.px2
      (when expanded?
        {:class "rotate-180"})
      ^:inline (svg/dropdown-arrow {:class  "fill-black"
                                    :height "16px"
                                    :width  "16px"})]]
    ^:inline (css-transitions/slide-down
              (when (or expanded? #?(:clj true)) ;; always show for server-side rendered html
                (component/html
                 [:div.mr8
                  [:p.py2.h6 paragraphs]])))]))

(defcomponent component [{:keys [expanded-indices sections]} owner {:keys [section-click-event]}]
  [:div
   (for [[idx {:keys [title paragraphs]}] (map-indexed vector sections)]
     ^:inline (section-element (contains? (set expanded-indices) idx) idx title paragraphs section-click-event))])
