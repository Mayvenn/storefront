(ns storefront.components.free-install
  (:require [install.faq-accordion :as faq-accordion]
            [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component]
            [storefront.components.accordion :as accordion]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

(defn img-with-number-circle [img-attrs image-id number-kw]
  [:div
   (ui/ucare-img (assoc img-attrs :class "col-12") image-id)
   [:div.relative.my3.pb3
    [:div.absolute.left-0.right-0
     {:style {:top "-50px"}}
     (svg/number-circle-with-white-border number-kw)]]])

(def easy-steps
  [:div.border.border-width-2.border-teal.mx3.my2.px2.pt4.pb2
   [:div.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "3 Easy Steps"]
   [:h1 "Get a FREE install"]
   [:p.h5.my2
    "Purchase 3 bundles and your install by a"
    " Mayvenn Certified Stylist is FREE!"]

   [:div
    [:div.py4
     (img-with-number-circle {:alt      "30 Day Satisfaction Guarantee"}
                             "b5671f33-4528-4a83-afa7-9fbfe0f825a6"
                             :1)
     [:h2.h2.my1 "Buy 3 bundles or more"]
     [:p.h5.dark-gray.mt1
      "Closures and frontals count, too!"
      " Our hair is 100% human"
      " and backed by a 30 day guarantee"
      " and starts at $30 per bundle."]]
    [:div.py4
     (img-with-number-circle {:alt      ""}
                             "6263c536-f548-45dc-ba89-ca68ad7c44c8"
                             :2)
     [:h2.h2.pt1 "A Fayetteville, NC exclusive offer"]
     [:p.h5.dark-gray.mt1
      "Your FREE Mayvenn install can only be redeemed in Fayetteville, NC."]]
    [:div.py4
     (img-with-number-circle {:alt ""}
                             "52dcdffb-cc44-4f80-88c8-325de7c3fa62"
                             :3)
     [:h2.h2.pt1 "Book your FREE install"]
     [:p.h5.dark-gray.mt1
      "After completing your purchase, Mayvenn will contact you to arrange"
      " your FREE install appointment with a Mayvenn Certified Stylist."]]]])

(def the-hookup
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:h2.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "The Hookup"]
   [:h3.h1 "Treat Yourself"]
   [:div.my2.mx-auto.bg-teal {:style {:width "30px" :height "2px"}}]

   [:div.my4
    [:div.flex.justify-center
     (ui/ucare-img {:alt "" :width "72"}
                   "c81da7fe-f3fb-4728-8428-e1b93bdf34cc")]
    [:h6.teal.bold "Free Install"]
    [:p.h6 "Get your hair installed absolutely FREE."]]

   [:div.my4
    [:div.flex.justify-center
     (ui/ucare-img {:alt "" :width "72"}
                   "3bbc41a4-31c2-4817-ad9b-f32936d7a95f")]
    [:h6.teal.bold "Risk Free"]
    [:p.h6 "Wear it, dye it, style it. If your don't love it your"
     " hair we'll exchange it within 30 days of purchase."]]

   [:div.my4
    [:div.flex.justify-center
     (ui/ucare-img {:alt "" :width "51"}
                   "1690834e-84c8-45c7-9047-57be544e89b0")]
    [:h6.teal.bold "Mayvenn Certified Stylists"]
    [:p.h6 "All Mayvenn Certified Stylists are licensed and work in salons."]]])

(defn- faq-section [{:keys [expanded-index]}]
  [:div.mt10.px4
   [:h2.center.my5 "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         faq-accordion/free-install-sections}
    {:opts {:section-click-event events/faq-section-selected}})])

(defn component [{:keys [footer-data faq-data]} owner _]
  (component/create
   (html
    (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
               :bg-class  "bg-darken-4"}
              [:div.bg-white
               {:style {:max-width "400px"}}
               [:div.col-12.clearfix.pt1.pb2
                [:div.right.pt2.pr2.pointer
                 (svg/simple-x
                  (merge (utils/fake-href events/control-free-install-dismiss)
                         {:data-test    "free-install-dismiss"
                          :height       "1.5rem"
                          :width        "1.5rem"
                          :class        "stroke-black"
                          :stroke-width "5"}))]
                [:div.flex.justify-center.pb2
                 [:div.col-6
                  (ui/clickable-logo {:class "col-12 mx4"
                                      :style {:height "40px"}})]]]
               [:div.flex.flex-column
                [:div.center
                 [:div  ;; Body
                  [:h1.h3.bold.white.bg-teal.mb4.p3
                   "Get a FREE install when you"
                   [:br]
                   "buy 3 bundles or more"]

                  easy-steps

                  [:p.h6.my3.bold "Just check out with promo code: ‘FREEINSTALL’"]

                  [:div.my4.mx2  ;;CTA
                   (ui/teal-button
                    (merge (utils/fake-href events/control-free-install-shop-looks)
                           {:data-test "free-install-shop-looks"})
                    "Shop looks")]

                  the-hookup]]

                [:div.bg-black.white.p4.flex.h6.medium.items-center
                 [:span.flex-auto.mr2 "Buy 3 bundles or more and get a FREE install!"]
                 [:div.col-5
                  (ui/teal-button
                   (merge (utils/fake-href events/control-free-install-shop-looks)
                          {:data-test "free-install-shop"})
                   "Shop")]]

                (faq-section faq-data)

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer-modal/component footer-data nil)]]]))))

(defn faq-query
  [data]
  {:expanded-index (get-in data keypaths/faq-expanded-section)})

(defn query
  [data]
  {:faq-data    (faq-query data)
   :footer-data (footer-modal/query data)})

(defn built-component
  [data opts]
  (component/build component data opts))

(defmethod effects/perform-effects events/control-free-install-shop-looks [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-shop-by-look {:album-keyword :look}))

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
