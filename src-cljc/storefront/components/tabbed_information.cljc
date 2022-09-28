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
    [:div.bg-cool-gray.p2
     {:key (str id "-tab")}
     [:div primary]
     [:div.flex.flex-wrap.justify-between
      (for [{:keys [content heading] :as section} sections]
        [:div.my2.pr2
         {:style {:min-width "50%"}}
         [:h3.proxima.title-3.shout heading]
         [:div
          (if-not (string? content)
            (for [block content]
              [:div block])
            content)]
         (when-let [link-content (:link/content section)]
           (ui/button-small-underline-primary
            (assoc
             (apply utils/fake-href (:link/target section))
             :data-test (:link/id section))
            link-content))])]]))

(c/defcomponent component [{:tabbed-information/keys [tabs id keypath]} owner _]
  (when id
    [:div.mx4
     (for [{:keys [title id icon active?] :as tab} tabs]
       [:div
        [:a.flex.justify-between.shout.proxima.title-3.inherit-color.p2.border-bottom.border-cool-gray
         ^:attrs (merge
                  (utils/fake-href events/tabbed-information-tab-selected {:tab     id
                                                                           :keypath keypath})
                  {:key       (str "tab-" (name id))
                   :data-test (str "tab-" (name id))})
         [:div title]
         [:div
          ^:inline
          (when (not active?)
            (svg/dropdown-arrow {:class  (str "ml1 "
                                              #_(when active? "rotate-180"))
                                 :height "1em"
                                 :width  "1em"
                                 :data-test "toggle-cash-balance"}))]]
        (tab-content tab)])]))

(defmethod transitions/transition-state events/tabbed-information-tab-selected [_ _ {:keys [tab keypath]} app-state]
  (let [selected-tab (get-in app-state keypath)]
    (when-not (= tab selected-tab)
      (assoc-in app-state keypath tab))))
