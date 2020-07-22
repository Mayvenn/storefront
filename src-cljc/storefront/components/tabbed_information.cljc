(ns storefront.components.tabbed-information
  (:require [storefront.component :as c]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [clojure.string :as string]))

(defn ^:private tab-element
  [{:keys [id primary sections active?]}]
  (when (or active? #?(:clj true))  ;; always show for server-side rendered html
    [:div.my3
     {:key (str id "-tab")}
     [:div primary]
     [:div.flex.flex-wrap.justify-between
      (for [{:keys [content heading]} sections]
        [:div.my2
         {:style {:min-width "50%"}}
         [:div.proxima.title-3.shout heading]
         [:div content]])]]))

(c/defcomponent component [{:tabbed-information/keys [tabs id content]} owner _]
  (when id
    [:div.mx4
     [:div.flex.mx-auto.justify-between.pointer
      (for [{:keys [title id icon active?]} tabs]
        [:div.canela.title-3.col-4.border-bottom.flex.flex-column.justify-end
         ^:attrs (merge (utils/fake-href events/pdtab-selected {:tab id})
                        {:class     (if active?
                                      "black border-width-4 border-black"
                                      "dark-gray border-width-2 border-cool-gray")
                         :style {:padding-bottom (when-not active? "2px")} ; counter the thick border
                         :key (str "tab-" (name id))
                         :data-test (str "tab-" (name id))})

         [:div.flex.justify-center
          (c/html
           [:svg (assoc (:opts icon) :class (if active? "fill-black" "fill-gray"))
            ^:inline (svg/svg-xlink (:id icon))])]
         [:div.center title]])]
     (map tab-element tabs)]))

(defmethod transitions/transition-state events/pdtab-selected [_ _ {:keys [tab]} app-state]
  (let [selected-tab (get-in app-state keypaths/product-details-tab)]
    (when-not (= tab selected-tab)
      (assoc-in app-state keypaths/product-details-tab tab))))
