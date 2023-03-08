(ns storefront.components.pixels
  (:require [storefront.component :as c]
            [storefront.events :as e]
            [storefront.keypaths :as k]))

(defn query
  [app-state]
  {:artsai
   {:action (condp = (get-in app-state k/navigation-event)
              e/navigate-cart "content"
              e/navigate-checkout-returning-or-guest "lead"
              e/navigate-checkout-payment "signup"
              e/navigate-checkout-confirmation "registration"
              e/navigate-order-complete "purchase"
              nil)}})
     
(c/defcomponent component
  [{:keys [artsai]} _ _]
  (when-let [action (:action artsai)]
    [:img {:src    (str "https://arttrk.com/pixel/?ad_log=referer&action=" action "&pixid=7ee25f99-1df2-4201-aa8b-a6af926b2e12")
           :width  "1"
           :height "1"
           :border "0"}]))

(defn ^:export built-component [app-state opts]
  (c/html
   [:div
    (c/build component {:artsai {:action "misc"}} opts)
    (c/build component (query app-state) opts)]))