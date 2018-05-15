(ns storefront.components.seventy-five-off-install
  (:require [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component]
            [storefront.components.footer-minimal :as footer-minimal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

(defn component [{:keys [footer-data]} owner _]
  (component/create
   (html
    (ui/modal {:col-class "col-12 col-6-on-tb col-5-on-dt"
               :bg-class  "bg-darken-4"}
              [:div.bg-cover.bg-white
               [:div.col-12.clearfix
                [:div.right.pt2.pr2
                 (svg/simple-x
                  (merge (utils/fake-href events/control-seventy-five-off-install-dismiss)
                         {:data-test    "seventy-five-off-install-dismiss"
                          :height       "1.5rem"
                          :width        "1.5rem"
                          :class        "stroke-black"
                          :stroke-width "5"}))]]
               [:div.mx-auto.col-10-on-dt.col-11-on-tb.flex.flex-column
                [:div.center.p4
                 [:div.flex.justify-center.pb2
                  [:div.col-6
                   (ui/clickable-logo {:class "col-12 mx1"
                                       :style {:height "40px"}})]]
                 [:div  ;; Body
                  [:h1.h3.bold.teal.mb4 "Get $100 Off Your Install with promo code: INSTALL"]
                  [:p.h6.mb1
                   "Purchase any 3 bundles or more from Mayvenn and Mayvenn will
                   give you $100 to use for your install at your Mayvenn Stylist.
                   Just follow 3 easy steps:"]
                  [:div
                   [:div.py4
                    (svg/number-circle :1)
                    [:h3.pt1 "Buy 3 bundles or more"]
                    [:p.h6.dark-gray.mt1
                     "To receive $100 off your install, purchase 3 bundles or
                     more (this can also include closures and frontals) from
                     Mayvenn." ]]
                   [:div.py4
                    (svg/number-circle :2)
                    [:h3.pt1 "An exclusive offer"]
                    [:p.h6.dark-gray.mt1
                     "Your Mayvenn order must be with your Mayvenn stylist to be
                     eligible for the $100 off promotion."]]
                   [:div.py4
                    (svg/number-circle :3)
                    [:h3.pt1 "Book your $100 off install"]
                    [:p.h6.dark-gray.mt1
                     "After you complete your purchase, Mayvenn will contact you
                     to arrange your discounted install appointment with your
                     Mayvenn stylist."]]]

                  [:p.h6.my3.bold "Check out with promo code: ‘INSTALL’"]]

                 [:div.my4  ;;CTA
                  (ui/teal-button
                   (merge (utils/fake-href events/control-seventy-five-off-install-shop-looks)
                          {:data-test "seventy-five-off-install-shop-looks"})
                   "Shop looks")]]

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer-minimal/component footer-data nil)]]]))))

(defn query
  [data]
  {:footer-data (footer-minimal/query data)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-seventy-five-off-install-shop-looks [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-shop-by-look {:album-keyword :look}))

(defmethod effects/perform-effects events/control-seventy-five-off-install [_ event args _ app-state]
  (scroll/enable-body-scrolling)
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (or
                       (first (get-in app-state keypaths/order-promotion-codes))
                       (get-in app-state keypaths/pending-promo-code)))
  (cookie-jar/save-pending-promo-code (get-in app-state keypaths/cookie) "install")
  (when-let [value (get-in app-state keypaths/dismissed-seventy-five-off-install)]
    (cookie-jar/save-dismissed-seventy-five-off-install (get-in app-state keypaths/cookie) value)))

(defmethod transitions/transition-state events/control-seventy-five-off-install [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/pending-promo-code "install")
      (assoc-in keypaths/popup nil)
      (assoc-in keypaths/dismissed-seventy-five-off-install true)))

(defmethod transitions/transition-state events/popup-show-seventy-five-off-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :seventy-five-off-install))

(defmethod effects/perform-effects events/popup-show-seventy-five-off-install [_ event _ _ app-state]
  (scroll/disable-body-scrolling))

