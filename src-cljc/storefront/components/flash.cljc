(ns storefront.components.flash
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(defn success-img []
  (svg/circled-check {:class "stroke-p-color"
                      :style {:width "1em" :height "1em"}}))

(defn error-img []
  (svg/error {:class "error"
              :style {:width "1em" :height "1em"}}))

(defn success-box [box-opts body]
  [:div.p-color.bg-p-color.border.border-p-color.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 ^:inline (success-img)]
    [:div.overflow-hidden body]]])

(defn error-box [box-opts body]
  [:div.warning-red.bg-error.border.border-error.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 ^:inline (error-img)]
    [:div.overflow-hidden body]]])

(defcomponent component [{:keys [success failure errors]} _ _]
  (when (or success failure (and (seq (:error-message errors)) (seq errors)))
    (ui/narrow-container
     [:div.p2
      (cond
        (or failure (seq errors))
        (error-box
         {:data-test "flash-error"}
         [:div.px2 (or failure (get errors :error-message))])

        success
        (success-box
         {:data-test "flash-success"}
         [:div.px2 success]))])))

(defn query [data]
  {:success (get-in data keypaths/flash-now-success-message)
   :failure (get-in data keypaths/flash-now-failure-message)
   :errors  (get-in data keypaths/errors)})

(defn built-component [data opts]
  (component/build component (query data) opts))
