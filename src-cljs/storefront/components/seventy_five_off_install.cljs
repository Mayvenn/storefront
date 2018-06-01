(ns storefront.components.seventy-five-off-install
  (:require [sablono.core :refer [html]]
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
   (ui/ucare-img (assoc img-attrs :width "280") image-id)
   [:div.relative.mb4.pb3
    [:div.absolute.left-0.right-0
     {:style {:top "-50px"}}
     (svg/number-circle-with-white-border number-kw)]]])

(def easy-steps
  [:div.border.border-width-2.border-teal.mx3.my2.px2.pt4.pb2
   [:div.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "3 Easy Steps"]
   [:h1 "Get $100 off your install"]
   [:p.h5.my2
    "Purchase any 3 bundles and Mayvenn will give you $100 for your install with a"
    " Mayvenn stylist.  Just follow 3 easy steps:"]

   [:div
    [:div.py4
     (img-with-number-circle {:alt      "30 Day Satisfaction Guarantee"}
                             "b5671f33-4528-4a83-afa7-9fbfe0f825a6"
                             :1)
     [:h2.h2.my1 "Buy 3 bundles or more"]
     [:p.h5.dark-gray.mt1
      "Our hair is 100% human"
      " and backed by a 30 day guarantee."]]
    [:div.py4
     (img-with-number-circle {:alt      "Phone showing appointments"}
                             "68b7a726-effe-49cf-a78b-d89263c3393f"
                             :2)
     [:h2.h2.pt1 "Visit your Mayvenn Stylist"]
     [:p.h5.dark-gray.mt1
      "Your install must be with a Mayvenn stylist"
      " to be eligible for the $100 off promotion."]]
    [:div.py4
     (img-with-number-circle {:alt ""}
                             "14ad5231-c062-4eec-b516-23fd6b8e0735"
                             :3)
     [:h2.h2.pt1 "Get $100 OFF your install"]
     [:p.h5.dark-gray.mt1
      "Mayvenn will contact your stylist to ensure you get $100 off your install."]]]])

(def the-hookup
  [:div.col-12.bg-transparent-teal.mt3.py8.px4
   [:h2.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "The Hookup"]
   [:h3.h1 "Treat Yourself"]
   [:div.my2.mx-auto.bg-teal {:style {:width "30px" :height "2px"}}]

   [:div.my4
    (ui/ucare-img {:alt "" :width "72"}
               "c81da7fe-f3fb-4728-8428-e1b93bdf34cc")
    [:h6.teal.bold "$100 Off"]
    [:p.h6 "Get your hair installed for $100 less!"]]

   [:div.my4
    (ui/ucare-img {:alt "" :width "72"}
               "3bbc41a4-31c2-4817-ad9b-f32936d7a95f")
    [:h6.teal.bold "Risk Free"]
    [:p.h6 "Wear it, dye it, style it. If your don't love it your"
     " hair we'll exchange it within 30 days of purchase."]]

   [:div.my4
    [:div.relative
     {:style {:left "-11px"}}
     (ui/ucare-img {:alt "" :width "72"}
                "4f5d609c-a4f1-4d1b-9ea0-787a1e0a6a07")]
    [:h6.teal.bold "Free 3-5 Day Shipping"]
    [:p.h6 "Need it sooner? Overnight shipping is available."]]])

(defn- faq-section [{:keys [expanded-index]}]
  [:div.mt10.px4
   [:h2.center.my5 "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}
     :sections         [(accordion/section "How does the 30 day guarantee work?"
                                           ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]
                                           ["EXCHANGES"
                                            [:br]
                                            "Wear it, dye it, even flat iron it. If you do not love your"
                                            " Mayvenn hair we will exchange it within 30 days of purchase."
                                            " Just call us:"
                                            [:br]
                                            (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]
                                           ["RETURNS"
                                            [:br]
                                            "If you are not completely happy with your Mayvenn hair"
                                            " before it is installed, we will refund your purchase if the"
                                            " bundle is unopened and the hair is in its original condition."
                                            " Just call us:"
                                            [:br]
                                            (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
                        (accordion/section "How does this all work? How do I get $100 off my install?"
                                           ["It’s easy! Mayvenn will pay the first $100 of your"
                                            " install directly to your Mayvenn stylist. Just purchase 3"
                                            " bundles or more (frontals and closures count as bundles) and use code"
                                            " INSTALL at checkout. Then, schedule an appointment with your"
                                            " Mayvenn stylist and just show up. The $100 off will be waiting"
                                            " for you. It’s that easy!"])
                        (accordion/section "Who is going to do my hair?"
                                           ["The $100 off offer is only valid at your Mayvenn stylist."
                                            " If you are unsure if your stylist is participating in the $100"
                                            " off offer, you can simply ask them or contact Mayvenn customer"
                                            " service:"
                                            (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
                        (accordion/section "What if I want to get my hair done by a non-Mayvenn?"
                                           ["No, you must get your hair done from your Mayvenn"
                                            " stylist in order to get $100 off your install."])
                        (accordion/section "Why should I order hair from Mayvenn?"
                                           ["Mayvenn hair is 100% human. Our Virgin, Dyed Virgin, and"
                                            " 100% Human hair can be found in a variety of textures from"
                                            " straight to curly. Virgin hair starts at $54 per bundle and"
                                            " 100% Human hair starts at just $30 per bundle. All orders are"
                                            " eligible for free shipping and backed by our 30 Day"
                                            " Guarantee."])]}
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
                  (merge (utils/fake-href events/control-seventy-five-off-install-dismiss)
                         {:data-test    "seventy-five-off-install-dismiss"
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
                   "Get $100 Off Your Next Install"
                   [:br]
                   "with promo code: INSTALL"]

                  easy-steps

                  [:p.h6.my3.bold "Just check out with promo code: ‘INSTALL’"]

                  [:div.my4.mx2  ;;CTA
                   (ui/teal-button
                    (merge (utils/fake-href events/control-seventy-five-off-install-shop-looks)
                           {:data-test "seventy-five-off-install-shop-looks"})
                    "Shop looks")]

                  the-hookup]]

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

(defmethod transitions/transition-state events/faq-section-selected [_ _ {:keys [index]} app-state]
  (let [expanded-index (get-in app-state keypaths/faq-expanded-section)]
    (if (= index expanded-index)
      (assoc-in app-state keypaths/faq-expanded-section nil)
      (assoc-in app-state keypaths/faq-expanded-section index))))
