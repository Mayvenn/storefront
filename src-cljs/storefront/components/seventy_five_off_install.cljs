(ns storefront.components.seventy-five-off-install
  (:require [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

(defn ucare-img [img-attrs image-id width]
  (let [retina-url (str "//ucarecdn.com/" image-id "/-/format/auto/-/quality/lightest/")
        default-url (str "//ucarecdn.com/" image-id "/-/format/auto/-/resize/" width "x/")]
    [:picture
     [:source {:src-set (str retina-url " 2x,"
                             default-url " 1x")}]
     [:img (assoc img-attrs :src default-url)]]))

(defn img-with-number-circle [img-attrs image-id number-kw]
  [:div
   (ucare-img img-attrs image-id 280)
   [:div.relative.mb4.pb3
    [:div.absolute.left-0.right-0
     {:style {:top "-50px"}}
     (svg/number-circle-with-white-border number-kw)]]])

(defn component [{:keys [footer-data]} owner _]
  (component/create
   (html
    (ui/modal {:col-class "col-12 col-6-on-tb col-5-on-dt"
               :bg-class  "bg-darken-4"}
              [:div.bg-white
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
                  (ui/clickable-logo {:class "col-12 mx1"
                                      :style {:height "40px"}})]]]
               [:div.flex.flex-column
                [:div.center
                 [:div  ;; Body
                  [:h1.h3.bold.white.bg-teal.mb4.p3
                   "Get $100 Off Your Next Install"
                   [:br]
                   "with promo code: INSTALL"]

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
                      "Closures and frontals count, too! Our hair is 100% human"
                      " and backed by a 30 day guarantee."]]
                    [:div.py4
                     (img-with-number-circle {:alt      "Phone showing appointments"}
                                             "1bfbdd7b-4854-43de-99d3-8d51576bbd17"
                                             :2)
                     [:h2.h2.pt1 "Book your appointment"]
                     [:p.h5.dark-gray.mt1
                      "After completing your purchase, Mayvenn will contact you"
                      " to arrange your discounted install appointment with a"
                      " Mayvenn stylist."]]
                    [:div.py4
                     (img-with-number-circle {:alt ""}
                                             "14ad5231-c062-4eec-b516-23fd6b8e0735"
                                             :3)
                     [:h2.h2.pt1 "Get $100 OFF your install"]
                     [:p.h5.dark-gray.mt1
                      "Mayvenn will contact your stylist to ensure you get $100 off your install."]]]]

                  [:p.h6.my3.bold "Just check out with promo code: ‘INSTALL’"]

                  [:div.my4.mx2  ;;CTA
                   (ui/teal-button
                    (merge (utils/fake-href events/control-seventy-five-off-install-shop-looks)
                           {:data-test "seventy-five-off-install-shop-looks"})
                    "Shop looks")]

                  [:div.col-12.bg-transparent-teal.mt3.py8.px4
                   [:h2.h6.letter-spacing-7.bold.caps.teal.mb1.mt2 "The Hookup"]
                   [:h3.h1 "Treat Yourself"]
                   [:div.my2.mx-auto.bg-teal {:style {:width "30px" :height "2px"}}]

                   [:div.my4
                    (ucare-img {:alt ""}
                               "c81da7fe-f3fb-4728-8428-e1b93bdf34cc"
                               72)
                    [:h6.teal.bold "$100 Off"]
                    [:p.h6 "Get your hair installed for $100 less!"]]

                   [:div.my4
                    (ucare-img {:alt ""}
                               "98a2fb6d-3149-4213-8198-b67a87d8042b"
                               72)
                    [:h6.teal.bold "Risk Free"]
                    [:p.h6 "Wear it, dye it, style it. If your don't love it your"
                     " hair we'll exchange it within 30 days of purchase."]]

                   [:div.my4
                    (ucare-img {:alt ""}
                               "4e19de20-0af3-4330-9598-eb9a70e1f9d8"
                               72)
                    [:h6.teal.bold "Free 3-5 Day Shipping"]
                    [:p.h6 "Need it sooner? Overnight shipping is available."]]]]]

                (let [qa (fn [index question & answer]
                           (into [:div.h5.py3.border-gray
                                  (when-not (zero? index)
                                    {:class "border-top"})
                                  [:a.black.col-12.h5.py2.flex.items-center.justify-center
                                   {:href "#"}
                                   [:div.flex-auto question]
                                   [:div.px2
                                    (svg/dropdown-arrow {:class  "stroke-dark-gray"
                                                         :style  {:stroke-width "3px"}
                                                         :height "12px"
                                                         :width  "12px"})]]]
                                 (map
                                  (fn [paragraph]
                                    [:p.py1.h6 paragraph])
                                  answer)))]
                  [:div.mt10.px4
                   [:h2.center.my5 "Frequently Asked Questions"]
                   (qa 0 "How does the 30 day guarantee work?"
                       ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."]

                       ["EXCHANGES"]
                       ["Wear it, dye it, even flat iron it. If you do not love your"
                        " Mayvenn hair we will exchange it within 30 days of purchase."
                        " Just call us:"
                        [:br]
                        (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")]

                       ["RETURNS"]
                       ["If you are not completely happy with your Mayvenn hair"
                        " before it is installed, we will refund your purchase if the"
                        " bundle is unopened and the hair is in its original condition."
                        " Just call us:"
                        [:br]
                        (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
                   (qa 1 "How does this all work? How do I get $100 off my install?"
                       ["It’s easy! Mayvenn will pay the first $100 of your"
                        " install directly to your Mayvenn stylist. Just purchase 3"
                        " bundles or more (including frontals and closures) and use code"
                        " INSTALL at checkout. Then, schedule an appointment with your"
                        " Mayvenn stylist and just show up. The $100 off will be waiting"
                        " for you. It’s that easy!"])
                   (qa 2 "How does this all work? How do I get $100 off my install?"
                       ["It’s easy! Mayvenn will pay the first $100 of your"
                        " install directly to your Mayvenn stylist. Just purchase 3"
                        " bundles or more (including frontals and closures) and use code"
                        " INSTALL at checkout. Then, schedule an appointment with your"
                        " Mayvenn stylist and just show up. The $100 off will be waiting"
                        " for you. It’s that easy!"])
                   (qa 3 "Who is going to do my hair?"
                       ["The $100 off offer is only valid at your Mayvenn stylist."
                        " If you are unsure if your stylist is participating in the $100"
                        " off offer, you can simply ask them or contact Mayvenn customer"
                        " service:"
                        (ui/link :link/phone :a.dark-gray {} "1-888-562-7952")])
                   (qa 4 "What if I want to get my hair done by a non-Mayvenn?"
                       ["No, you must get your hair done from your Mayvenn"
                        " stylist in order to get $100 off your install."])
                   (qa 5 "Why should I order hair from Mayvenn?"
                       ["Mayvenn hair is 100% human. Our Virgin, Dyed Virgin, and"
                        " 100% Human hair can be found in a variety of textures from"
                        " straight to curly. Virgin hair starts at $54 per bundle and"
                        " 100% Human hair starts at just $30 per bundle. All orders are"
                        " eligible for free shipping and backed by our 30 Day"
                        " Guarantee."])])

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer-modal/component footer-data nil)]]]))))

(defn query
  [data]
  {:footer-data (footer-modal/query data)})

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

