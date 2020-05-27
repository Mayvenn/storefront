(ns checkout.ui.secure-checkout
  (:require [storefront.component :as c]
            [storefront.platform.component-utils :as utils]
            [storefront.components.facebook :as facebook]
            [storefront.components.ui :as ui]))

(defn ^:private secure-checkout-title-molecule
  [{:secure-checkout.title/keys [primary secondary]}]
  [:div.center
   [:h1.canela.title-1 primary]
   [:div.canela.content-2.col-10.mx-auto secondary]])

(defn ^:private secure-checkout-cta-molecule
  [{:secure-checkout.cta/keys [id value target]}]
  [:div.col-10.mx-auto.py1
   (ui/button-medium-primary (assoc (utils/route-to target)
                                    :data-test id)
                             value)])

(defn ^:private secure-checkout-facebook-cta-atom
  [{:secure-checkout.facebook-cta/keys [loaded?]}]
  [:div.col-10.mx-auto.py1
   (facebook/narrow-sign-in-button loaded?)])

(c/defcomponent organism
  [data _ _]
  [:div.px2.pt2
   (secure-checkout-title-molecule data)
   [:div.my2.mx-auto.col-12.col-8-on-tb-dt
    (secure-checkout-cta-molecule data)
    (secure-checkout-facebook-cta-atom data)]])
