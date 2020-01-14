(ns storefront.components.accordion
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]))

(defn- slide-down [content]
  (css-transitions/transition-group
   {:classNames "slide-down"
    :in         (boolean content)
    :timeout    300}
   (if content
     ^:inline content
     (component/html [:div]))))

(defn section
  "A data constructor for convenience"
  [title & paragraphs]
  {:title      title
   :paragraphs paragraphs})

(defn section->html [{:keys [title paragraphs]}]
  {:title      (component/html title)
   :paragraphs (component/html paragraphs)})

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
    ^:inline (slide-down
              (when (or expanded? #?(:clj true)) ;; always show for server-side rendered html
                (component/html
                 [:div.mr8
                  (for [[i paragraph] (map-indexed vector paragraphs)]
                    [:p.py2.h6 {:key (str i)} paragraph])])))]))

(defcomponent component [{:keys [expanded-indices sections]} owner {:keys [section-click-event
                                                                           static-sections]}]
  [:div
   (for [[idx {:keys [title paragraphs]}] (map-indexed vector (if sections
                                                                (map section->html sections)
                                                                static-sections))]
     ^:inline (section-element (contains? (set expanded-indices) idx) idx title paragraphs section-click-event))])
