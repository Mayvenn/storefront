(ns checkout.ui.molecules
  (:require [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.component :as component]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn cart-summary-line-molecule
  [{:cart-summary-line/keys
    [id primary secondary tertiary icon value action-id action-target action-icon class]}]
  (component/html
   (when id
     [:tr.proxima.content-2
      {:data-test (str "cart-summary-line-" id)
       :key       (str "cart-summary-line-" id)}
      [:td.pyp1.flex.items-center.align-middle
       (svg/symbolic->html icon)
       ^String (str primary)
       (when secondary
         [:div.h7.ml1 ^String (str secondary)])
       (when tertiary
         [:div.h7.ml1.red ^String (str tertiary)])
       (when action-id
         [:a.ml1.h6.gray.flex.items-center
          ^:attrs (merge {:data-test action-id}
                         (apply utils/fake-href action-target))
          (svg/symbolic->html action-icon)])]
      [:td.pyp1.right-align.medium
       {:class class}
       value]])))

(defn cart-summary-total-line
  [{:cart-summary-total-line/keys [id label value]}]
  (component/html
   (when id
     [:div.flex {:data-test id}
      [:div.flex-auto.content-1.proxima label]
      [:div.right-align.title-2.proxima value]])))

(defn cart-summary-total-incentive
  [{:cart-summary-total-incentive/keys [id label savings]}]
  (component/html
   (when id
     [:div.flex.justify-end {:data-test id}
      [:div.right-align.content-3
       [:div.bg-warm-gray.px2.py1.nowrap.mb1
        label]
       (when savings
         [:div.light.pxp1.nowrap.italic
          "You've saved "
          [:span.bold.p-color {:data-test "total-savings"}
           savings]])]])))

(defn free-install-added-atom
  [{:free-install-added/keys [primary]}]
  (when primary
    [:div.bg-warm-gray.proxima.content-2.shout.bold.center.py1
     primary]))
