(ns stylist-matching.match-success
  (:require #?@(:cljs [[storefront.history :as history]])
            api.current
            api.orders
            api.stylist
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.header :as header]
            [storefront.effects :as effects]
            [storefront.events :as e]
            storefront.keypaths
            [stylist-matching.ui.atoms :as stylist-matching.A]
            [stylist-matching.ui.congrats :as congrats]))

(defmethod effects/perform-effects e/navigate-adventure-match-success
  [_ _ _ _ state]
  (when-not (api.current/stylist state)
    #?(:cljs
       (history/enqueue-redirect e/navigate-adventure-find-your-stylist))))

(defn header-query
  [{:order.items/keys [quantity]}
   browser-history]
  (cond-> {:header.title/id               "adventure-title"
           :header.title/primary          "Meet Your Stylist"
           :header.back-navigation/id     "adventure-back"
           :header.back-navigation/target [e/navigate-adventure-find-your-stylist]
           :header.cart/id                "mobile-cart"
           :header.cart/value             (or quantity 0)
           :header.cart/color             "white"}

    (seq browser-history)
    (merge {:header.back-navigation/back (first browser-history)})))

(defn congrats-query
  [stylist]
  {:stylist-matching.ui.congrats.title/primary   (str
                                                  "Congratulations on matching with "
                                                  (stylists/->display-name stylist) "!")
   :stylist-matching.ui.congrats.title/secondary ["Now for the fun part!"
                                                  "Let's pick your hair!"]
   :stylist-matching.ui.congrats.cta/id          "pick-my-hair"
   :stylist-matching.ui.congrats.cta/label       "Pick My Hair"
   :stylist-matching.ui.congrats.cta/target      [e/navigate-category {:page/slug           "mayvenn-install"
                                                                       :catalog/category-id "23"}]})

(defcomponent template
  [{:keys [header congrats]} _ _]
  [:div.center.flex.flex-auto.flex-column
   stylist-matching.A/bottom-right-party-background
   (header/adventure-header header)
   (component/build congrats/organism congrats nil)])

(defn ^:export page
  [app-state _]
  (let [servicing-stylist (:diva/stylist (api.current/stylist app-state))
        order             (api.orders/current app-state)
        browser-history   (get-in app-state storefront.keypaths/navigation-undo-stack)]
    (component/build template
                     {:header   (header-query order browser-history)
                      :congrats (congrats-query servicing-stylist)})))
