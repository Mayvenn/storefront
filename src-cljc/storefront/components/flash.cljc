(ns storefront.components.flash
  (:require [clojure.string :as string]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.keypaths :as keypaths]))

(def flash-line-height "1.25em")

(def success-img
  (svg/circled-check {:class "stroke-green align-middle"
                      :style {:width flash-line-height :height flash-line-height}}))

(def error-img
  (svg/error {:class "fill-orange"
              :style {:width flash-line-height :height flash-line-height}}))

(defn success-box [box-opts body]
  [:div.green.bg-green.border.border-green.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 success-img]
    [:div.overflow-hidden body]]])

(defn error-box [box-opts body]
  [:div.orange.bg-orange.border.border-orange.rounded.light.letter-spacing-1
   [:div.clearfix.px2.py1.bg-lighten-5.rounded box-opts
    [:div.right.ml1 error-img]
    [:div.overflow-hidden body]]])

(defn component [{:keys [success failure errors]} _ _]
  (component/create
   (when (or success failure (seq errors))
     (ui/narrow-container
      (cond
        (or failure (seq errors))
        (error-box
         {:data-test "flash-error"}
         [:div.px2 {:style {:line-height flash-line-height}}
          (or failure (get errors :error-message))])

        success
        (success-box
         {:data-test "flash-success"}
         [:div.px2 {:style {:line-height flash-line-height}}
          success]))))))

(defn query [data]
  {:success            (get-in data keypaths/flash-success-message)
   :failure            (get-in data keypaths/flash-failure-message)
   :errors             (get-in data keypaths/errors)})

(defn built-component [data opts]
  (component/build component (query data) opts))
