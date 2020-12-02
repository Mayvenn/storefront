(ns stylist-matching.match-success-pick-service-v2020-06
  (:require api.current
            api.orders
            [storefront.accessors.stylists :as stylists]
            [storefront.component :as c]
            [storefront.components.header :as header]
            [storefront.events :as e]
            [storefront.keypaths :as storefront.keypaths]
            [stylist-matching.ui.congrats :as congrats]))

(c/defcomponent template
  [{:keys [header congrats]} _ _]
  [:div
   (header/adventure-header header)
   (c/build congrats/organism congrats)])

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
                                                  "Let's pick your service!"]
   :stylist-matching.ui.congrats.cta/id          "pick-my-service"
   :stylist-matching.ui.congrats.cta/label       "Pick My Service"
   :stylist-matching.ui.congrats.cta/target      [e/navigate-category {:page/slug           "free-mayvenn-services"
                                                                       :catalog/category-id "31"}]})

(defn ^:export page
  [app-state _]
  (let [servicing-stylist (:diva/stylist (api.current/stylist app-state))
        current-order     (api.orders/current app-state)
        browser-history   (get-in app-state storefront.keypaths/navigation-undo-stack)]
    (c/build
     template {:header   (header-query current-order browser-history)
               :congrats (congrats-query servicing-stylist)})))
