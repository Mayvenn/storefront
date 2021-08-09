(ns mayvenn.visual.lib.radio-section
  "Call out boxes"
  (:require [storefront.component :as c]
            [mayvenn.visual.tools :refer [with]]))

(defn v2
   [{:as   data
     :state/keys [checked disabled]}]
   (c/html
    [:label.flex.items-center
     (with :label.attrs data)
     [:div.circle.bg-white.border.flex.items-center.justify-center
      ^:attrs (cond-> (with :dial.attrs data)
                true     (assoc :style {:height "22px" :width "22px"})
                disabled (update :class str " bg-cool-gray border-gray"))
      (when checked
        [:div.circle.bg-p-color
         {:style {:height "10px" :width "10px"}}])]
     [:input.hide.mx2.h2
      ^:attrs (merge (with :input.attrs data)
                     {:type "radio"})]
     (into
      [:div ^:attrs (with :copy.attrs data)]
      (:copy.content/text data))]))
