(ns storefront.components.facebook)

(defn button [data]
  [:button.fb-login-button {:on-click "/* TODO: run FB.login() on a <button> click, with public_profile,email scope */"}
   [:img {:src "/images/FacebookWhite.png"}]
   "Sign in with Facebook"])
