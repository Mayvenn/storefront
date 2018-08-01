(ns storefront.components.aladdin-free-install
  (:require [install.faq-accordion :as faq-accordion]
            [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as component]
            [storefront.components.aladdin :as aladdin]
            [storefront.components.accordion :as accordion]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.history :as history]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.components.marquee :as marquee]
            [storefront.transitions :as transitions]))


(defn component [{:keys [footer-data faq-data store gallery-ucare-ids stylist-gallery-open?]} owner _]
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
                         {:data-test    "aladdin-free-install-dismiss"
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
                  [:h1.h3.bold.white.bg-teal.p3
                   "Get a FREE install when you"
                   [:br]
                   "buy 3 bundles or more"]]]
                [:div.my10
                 (aladdin/get-a-free-install {:store                 store
                                              :gallery-ucare-ids     gallery-ucare-ids
                                              :stylist-portrait      (:portrait store)
                                              :stylist-name          (:store-nickname store)
                                              :stylist-gallery-open? stylist-gallery-open?})]
                [:div.mb3.px2

                 [:h6.bold.col-12.center.mb2 "Just check out with promo code: FREEINSTALL"]
                 [:div.col-11.mx-auto
                  (ui/teal-button (merge
                                   {:height-class "py2"}
                                   (utils/fake-href events/control-aladdin-free-install-shop-looks))
                                  [:span "Shop looks"])]]

                aladdin/why-mayvenn-is-right-for-you

                [:div.bg-black.white.p4.flex.h6.medium.items-center
                 [:span.col-7.mr2 "Buy 3 bundles or more and get a FREE install!"]
                 [:div.col-5.flex.justify-end
                  [:div.col-9
                   (ui/teal-button
                    (merge (utils/fake-href events/control-aladdin-free-install-shop-looks)
                           {:data-test "aladdin-free-install-shop"
                            :height-class "py1"})
                    [:span.h6 "Shop"])]]]

                [:div.mt10
                 (aladdin/faq faq-data)]

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer-modal/component footer-data nil)]]]))))

(defn faq-query
  [data]
  {:expanded-index (get-in data keypaths/faq-expanded-section)})

(defn query
  [data]
  (let [store (marquee/query data)]
    {:store                 store
     :gallery-ucare-ids     (->> store
                                 :gallery
                                 :images
                                 (map (comp aladdin/get-ucare-id-from-url :resizable-url)))
     :stylist-gallery-open? (get-in data keypaths/carousel-stylist-gallery-open?)
     :faq-data              (faq-query data)
     :footer-data           (footer-modal/query data)}))

(defn built-component
  [data opts]
  (component/build component data opts))

(defmethod effects/perform-effects events/control-aladdin-free-install-shop-looks [_ event args _ app-state]
  (history/enqueue-navigate events/navigate-shop-by-look {:album-keyword :look}))

(defmethod effects/perform-effects events/control-aladdin-free-install [_ event args _ app-state]
  (scroll/enable-body-scrolling)
  (api/get-promotions (get-in app-state keypaths/api-cache)
                      (or (first (get-in app-state keypaths/order-promotion-codes))
                          (get-in app-state keypaths/pending-promo-code)))
  (cookie-jar/save-pending-promo-code (get-in app-state keypaths/cookie) "freeinstall")
  (when-let [value (get-in app-state keypaths/dismissed-free-install)]
    (cookie-jar/save-dismissed-free-install (get-in app-state keypaths/cookie) value)))

(defmethod transitions/transition-state events/control-aladdin-free-install [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/pending-promo-code "freeinstall")
      (assoc-in keypaths/popup nil)
      (assoc-in keypaths/dismissed-free-install true)))

(defmethod transitions/transition-state events/popup-show-aladdin-free-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :aladdin-free-install))

(defmethod effects/perform-effects events/popup-show-aladdin-free-install [_ event _ _ app-state]
  (scroll/disable-body-scrolling))
