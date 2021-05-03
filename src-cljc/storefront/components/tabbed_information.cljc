(ns storefront.components.tabbed-information
  (:require [storefront.component :as c]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.ui :as ui]))

(defn ^:private tab-content
  [{:keys [id primary sections active?]}]
  (when (or active? #?(:clj true))  ;; always show for server-side rendered html
    [:div.my3
     {:key (str id "-tab")}
     [:div primary]
     [:div.flex.flex-wrap.justify-between
      (for [{:keys [content heading] :as section} sections]
        [:div.my2.pr2
         {:style {:min-width "50%"}}
         [:div.proxima.title-3.shout heading]
         [:div content]
         (when-let [link-content (:link/content section)]
           (ui/button-small-underline-primary
            {}
            link-content))])]]))

(c/defcomponent component [{:tabbed-information/keys [tabs id keypath]} owner _]
  (when id
    [:div.mx4
     [:div.flex.mx-auto.justify-between
      (for [{:keys [title id icon active?]} tabs]
        [:a.block.canela.title-3.col-4.border-bottom.flex.flex-column.justify-end.pt3
         ^:attrs (merge
                  (utils/fake-href events/tabbed-information-tab-selected {:tab     id
                                                                           :keypath keypath})
                  {:class     (if active?
                                "black border-width-4 border-black"
                                "dark-gray border-width-2 border-cool-gray")
                   :style     {:padding-bottom (when-not active? "2px")} ; counter the thick border
                   :key       (str "tab-" (name id))
                   :data-test (str "tab-" (name id))})

         [:div.flex.justify-center
          [:svg (assoc (:opts icon) :class (if active? "fill-black" "fill-gray"))
           ^:inline (svg/svg-xlink (:id icon))]]
         [:div.center title]])]
     (map tab-content tabs)]))

(defmethod transitions/transition-state events/tabbed-information-tab-selected [_ _ {:keys [tab keypath]} app-state]
  (let [selected-tab (get-in app-state keypath)]
    (when-not (= tab selected-tab)
      (assoc-in app-state keypath tab))))
