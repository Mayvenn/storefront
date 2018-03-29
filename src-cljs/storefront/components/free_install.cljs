(ns storefront.components.free-install
  (:require [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.history :as history]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.component :as component]
            [storefront.transitions :as transitions]
            [storefront.effects :as effects]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
            [storefront.components.svg :as svg]
            [storefront.components.footer :as footer]
            [storefront.browser.scroll :as scroll]))

(defn component [{:keys [footer-data]} owner _]
  (component/create
   (html
    (ui/modal {:col-class "col-12 col-6-on-tb col-5-on-dt"
               :bg-class  "bg-darken-4"}
              [:div.bg-cover.bg-white
               [:div.col-12.clearfix
                [:div.right.pt2.pr2
                 (svg/simple-x
                  (merge (utils/fake-href events/control-free-install-dismiss)
                         {:data-test    "free-install-dismiss"
                          :height       "1.5rem"
                          :width        "1.5rem"
                          :class        "stroke-black"
                          :stroke-width "5"}))]]
               [:div.mx-auto.col-10-on-dt.col-11-on-tb.flex.flex-column
                [:div.center.p4
                 [:div.flex.justify-center  ;; Images
                  [:div.col-6
                   (ui/clickable-logo {:class "col-12 mx1"
                                       :style {:height "40px"}})]
                  [:div.mb3.self-center
                   [:img {:src    "https://ucarecdn.com/13d14c84-a126-455f-9143-ef1e7f6ed5eb/FV_heart.png"
                          :height "12px"
                          :width  "12px"}]]
                  [:div.col-6.mt2
                   [:img {:src    "https://ucarecdn.com/c5d683b1-e71a-4711-9238-ab2be8b104ab/Fayetteville_Logo.png"
                          :height "54px"}]]]


                 [:div  ;; Body
                  [:h1.h3.bold.teal.mb4 "Get a FREE install when you buy 3 bundles or more"]
                  [:p.h6.mb1
                   "Purchase any 3 bundles or more from Mayvenn and your install by a Mayvenn Certified Stylist is FREE! Just follow 3 easy steps:"]
                  [:div
                   [:div.py3
                    (svg/number-circle :1)
                    [:h3.pt1 "Buy 3 bundles or more"]
                    [:p.h6.dark-gray.mt1
                     "To be eligible for a FREE install, purchase 3 bundles or
                   more (this can also include a closures and frontals) from
                   Mayvenn."]]
                   [:div.py3
                    (svg/number-circle :2)
                    [:h3.pt1 "A Fayetteville, NC exclusive offer "]
                    [:p.h6.dark-gray.mt1
                     "Your Mayvenn order must be shipped to a qualified address in Fayetteville, NC to be eligible."]]
                   [:div.py3
                    (svg/number-circle :3)
                    [:h3.pt1 "Book your FREE install"]
                    [:p.h6.dark-gray.mt1
                     "After you complete your purchase, Mayvenn will contact you to arrange your FREE install appointment with one of our Mayvenn Certified Stylists. You book, we pay!"]]]

                  [:p.h6.my3.bold "Check out with promo code: ‘FREEINSTALL’"]]

                 [:div.my4  ;;CTA
                  (ui/teal-button
                   (merge (utils/fake-href events/control-free-install-shop-looks)
                          {:data-test "free-install-shop-looks"})
                   "Shop looks")]]

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer/minimal-component footer-data nil)]]]))))

(defn query [data]
  {:footer-data (footer/contacts-query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-free-install-shop-looks [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-shop-by-look))

(defmethod effects/perform-effects events/control-free-install [_ event args _ app-state]
  (scroll/enable-body-scrolling)
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (or
                       (first (get-in app-state keypaths/order-promotion-codes))
                       (get-in app-state keypaths/pending-promo-code)))
  (cookie-jar/save-pending-promo-code (get-in app-state keypaths/cookie) "freeinstall")
  (when-let [value (get-in app-state keypaths/dismissed-free-install)]
    (cookie-jar/save-dismissed-free-install (get-in app-state keypaths/cookie) value)))

(defmethod transitions/transition-state events/control-free-install [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/pending-promo-code "freeinstall")
      (assoc-in keypaths/popup nil)
      (assoc-in keypaths/dismissed-free-install true)))


(defmethod transitions/transition-state events/popup-show-free-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :free-install))

(defmethod effects/perform-effects events/popup-show-free-install [_ event _ _ app-state]
  (scroll/disable-body-scrolling))

