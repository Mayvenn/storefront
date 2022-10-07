(ns storefront.components.accordion
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.css-transitions :as css-transitions]))

(defn section
  "A data constructor for convenience"
  [title & content]
  {:title      title
   :content content})

(defn content-block [content]
  (map-indexed (fn [i {blocks :paragraph}]
                 [:div.p2.bg-cool-gray {:key (str "paragraph-" i)}
                  (map-indexed (fn [j {:keys [text url]}]
                                 (if url
                                   [:a.p-color {:href url :key (str "text-" j)} text]
                                   [:span {:key (str "text-" j)} text]))
                               blocks)])
               content))

(defn- section-element
  [expanded? index title content section-click-event]
  (component/html
   [:div.h5.border-bottom.border-cool-gray {:key (str "accordion-" index)}
    [:a.pointer.col-12.h5.p2.flex.items-center.justify-center.inherit-color
     ^:attrs (merge (utils/fake-href section-click-event {:index index})
                    {:data-test (str "accordion-" index)})
     [:h3.proxima.shout.title-3.flex-auto
      (when (not expanded?)
        {:style {:font-weight 400}})
      title]
     [:div.px2
      (when expanded?
        {:class "rotate-180"})
      ^:inline (svg/dropdown-arrow {:class  "fill-black"
                                    :height "16px"
                                    :width  "16px"})]]
    ^:inline (css-transitions/slide-down
              (when (or expanded? #?(:clj true)) ;; always show for server-side rendered html
                (component/html
                 [:div
                  (content-block content)])))]))

(defcomponent component [{:keys [expanded-indices sections]} owner {:keys [section-click-event]}]
  [:div
   (for [[idx {:keys [title content]}] (map-indexed vector sections)]
     ^:inline (section-element (contains? (set expanded-indices) idx) idx title content section-click-event))])
