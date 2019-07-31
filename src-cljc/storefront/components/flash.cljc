(ns storefront.components.flash
  (:require [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]))

(defn success-img []
  (svg/circled-check {:class "stroke-teal"
                      :style {:width "1em" :height "1em"}}))

(defn error-img []
  (svg/error {:class "red"
              :style {:width "1em" :height "1em"}}))

(defn success-box [box-opts body]
  [:div.teal.bg-teal.border.border-teal.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 ^:inline (success-img)]
    [:div.overflow-hidden body]]])

(defn error-box [box-opts body]
  [:div.red.bg-red.border.border-red.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 ^:inline (error-img)]
    [:div.overflow-hidden body]]])

(defn component [{:keys [success failure errors]} _ _]
  (component/create
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
          [:div.px2 success]))]))))

(defn query [data]
  {:success (get-in data keypaths/flash-now-success-message)
   :failure (get-in data keypaths/flash-now-failure-message)
   :errors  (get-in data keypaths/errors)})

(defn built-component [data opts]
  (component/build component (query data) opts))
