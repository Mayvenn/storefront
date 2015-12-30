(ns storefront.components.facebook)

(defn button [data]
  [:button.fb-login-button #_{:on-click "/* TODO: run FB.login() on a <button> click, with public_profile,email scope */"}
   [:div.fb-login-wrapper
    [:img {:src "/images/FacebookWhite.png" :width 29 :height 29}]
    [:div.fb-login-content "Sign in with Facebook"]]])
