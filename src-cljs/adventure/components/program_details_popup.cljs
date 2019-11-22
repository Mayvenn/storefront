(ns adventure.components.program-details-popup
  (:require [storefront.component :as component :refer [defcomponent]]
            [adventure.faq :as faq]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.browser.scroll :as scroll]
            [storefront.accessors.experiments :as experiments]
            [adventure.organisms.call-out-center :as call-out-center]))

(def get-a-free-install
  (let [step (fn [{:keys [icon-uuid icon-width title description]}]
               [:div.col-12.mt2.center
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                [:div.h5.teal.medium title]
                [:p.h6.col-10.col-9-on-dt.mx-auto description]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h2 "Get a FREE Install"]
      [:div.h6.dark-gray "In three easy steps"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (step {:icon-uuid   "e90526f9-546f-4a6d-a05a-3bea94aedc21"
             :icon-width  "28"
             :title       "Buy Any 3 Bundles or More"
             :description "Including closures and frontals! Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."})
      (step {:icon-uuid   "cddd38e0-f598-4aca-90fc-4350dd4469fb"
             :icon-width  "35"
             :title       "Get Your Voucher"
             :description "We’ll send you a free install voucher via SMS and email after your order ships."})
      (step {:icon-uuid   "7712537c-3805-4d92-90b5-a899748a21c5"
             :icon-width  "35"
             :title       "Show Your Stylist The Voucher"
             :description "Redeem the voucher when you go in for your appointment with your Certified Mayvenn Stylist."})]]))

(def why-mayvenn-is-right-for-you
  (let [entry (fn [{:keys [icon-uuid icon-width title description]}]
                [:div.col-12.my2.flex.flex-column.items-center.items-end
                 [:div.flex.justify-center.items-end.mb1
                  {:style {:height "35px"}}
                  (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                 [:div.h6.teal.medium.mbnp4 title]

                 [:p.h6.col-11.center description]])]
    [:div.col-12.bg-transparent-teal.mt3.py8.px4
     [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

      [:div.my2.flex.flex-column.items-center.col-12
       [:h2.titleize "Why mayvenn is right for you"]
       [:div.h6.dark-gray.titleize "It's not just about hair"]]

      (entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
              :icon-width  "29"
              :title       "Top Notch Customer Service"
              :description "Our team is made up of hair experts ready to help you by phone, text, and email."})
      (entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
              :icon-width  "52"
              :title       "30 Day Guarantee"
              :description "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."})
      (entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
              :icon-width  "35"
              :title       "100% Virgin Hair"
              :description "Our hair is gently steam processed and can last up to a year. Available in 8 textures and 8 shades."})
      (entry {:icon-uuid   "3f622e92-6d95-49e2-a0c1-51a535b22975"
              :icon-width  "35"
              :title       "Free Install"
              :description "Get your hair installed absolutely FREE!"})]]))

(defmethod popup/component :adventure-free-install
  [{:keys [footer-data faq-data]} owner _]
  (ui/modal {:col-class "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs (utils/fake-href events/control-adventure-free-install-dismiss)
             :bg-class  "bg-darken-4"}
            [:div.bg-white
             {:style {:max-width "400px"}}
             [:div.col-12.clearfix.pt1.pk2
              [:div.right.pt2.pr2.pointer
               (svg/simple-x
                (merge (utils/fake-href events/control-adventure-free-install-dismiss)
                       {:data-test    "adventure-popup-dismiss"
                        :height       "27px"
                        :width        "27px"
                        :class        "black"}))]
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
              [:div.mt10.mb6 get-a-free-install]

              why-mayvenn-is-right-for-you

              [:div.mt10
               (faq/component (assoc faq-data :modal? true))]

              [:div.hide-on-tb-dt.pt3 ;; Footer
               (component/build footer-modal/component footer-data nil)]]]))

(defmethod popup/query :adventure-free-install
  [data]
  {:faq-data    (faq/free-install-query data)
   :footer-data (footer-modal/query data)})

(defmethod transitions/transition-state events/control-adventure-free-install-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-adventure-free-install-dismiss [_ event args previous-app-state app-state]
  (scroll/enable-body-scrolling))

(defmethod transitions/transition-state events/popup-show-adventure-free-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :adventure-free-install))

;;; Consolidated cart

(def get-a-mayvenn-install-black-friday
  (let [step (fn [{:keys        [icon/uuid icon/width]
                   header-value :header/value
                   body-value   :body/value}]
               [:div.col-12.mt2.center
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt header-value :width width} uuid)]
                [:div.h5.medium header-value]
                [:p.h6.col-8.col-9-on-dt.mx-auto.dark-gray
                 body-value]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h1 "How to get a FREE Install"]
      [:div.h5.dark-gray "In three easy steps"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (map step
           [{:icon/uuid    "3d2b326c-7773-4672-827e-f13dedfae15a"
             :icon/width   "22"
             :header/value "1. Choose a Mayvenn Certified Stylist"
             :body/value   "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."}
            {:icon/uuid    "08e9d3d8-6f3d-4b3c-bc46-3590175a9a4d"
             :icon/width   "24"
             :header/value "2. Buy Any 3 Items or More"
             :body/value   "Purchase 3 or more bundles, closures or frontals. Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."}
            {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
             :icon/width   "27"
             :header/value "3. Schedule Your Appointment"
             :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}])]]))

(def get-a-mayvenn-install
  (let [step (fn [{:keys        [icon/uuid icon/width]
                   header-value :header/value
                   body-value   :body/value}]
               [:div.col-12.mt2.center
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt header-value :width width} uuid)]
                [:div.h5.medium header-value]
                [:p.h6.col-8.col-9-on-dt.mx-auto.dark-gray
                 body-value]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h1 "Get a FREE Install"]
      [:div.h5.dark-gray "In three easy steps"]]

     [:div.col-8-on-dt.mx-auto.flex.flex-wrap
      (map step
           [{:icon/uuid    "3d2b326c-7773-4672-827e-f13dedfae15a"
             :icon/width   "22"
             :header/value "1. Choose a Mayvenn Certified Stylist"
             :body/value   "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."}
            {:icon/uuid    "08e9d3d8-6f3d-4b3c-bc46-3590175a9a4d"
             :icon/width   "24"
             :header/value "2. Buy Any 3 Items or More"
             :body/value   "Purchase 3 or more bundles, closures or frontals. Rest easy - your 100% virgin hair purchase is backed by our 30 day guarantee."}
            {:icon/uuid    "3fb9c2bf-c30e-4bee-957c-f273b1b5a233"
             :icon/width   "27"
             :header/value "3. Schedule Your Appointment"
             :body/value   "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."}])]]))

(def mayvenn-is-more-than-a-hair-company
  (let [entry (fn [{:keys [icon-uuid icon-width title description]}]
                [:div.col-12.my2.flex.flex-column.items-center.items-end
                 [:div.flex.justify-center.items-end.mb1
                  {:style {:height "35px"}}
                  (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                 [:div.h5.medium.my2 title]
                 [:p.h6.col-8.center.dark-gray description]])]

    [:div.col-12.bg-transparent-teal.mt3.py8.px4
     [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

      [:div.my2.flex.flex-column.items-center.col-12
       [:h1.titleize.center "Mayvenn is more than a hair company"]
       [:div.h5.mt2.dark-gray "It's a movement"]]

      (entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
              :icon-width  "21"
              :title       "Top Notch Customer Service"
              :description "Our team is made up of hair experts ready to help you by phone, text, and email."})
      (entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
              :icon-width  "41"
              :title       "30 Day Guarantee"
              :description "Wear it, dye it, even cut it! If you're not satisfied we'll exchange it within 30 days."})
      (entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
              :icon-width  "30"
              :title       "100% Virgin Hair"
              :description "Our hair is gently steam processed and can last up to a year. Available in 8 textures and 8 shades."})
      (entry {:icon-uuid   "6f63157c-dc3a-4bbb-abcf-e03b08d6e102"
              :icon-width  "31"
              :title       "Certified Stylists"
              :description "Our stylists are chosen because of their industry-leading standards. Both our hair and service are quality guaranteed."})]]))

(defn ^:private cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (-> (merge {:data-test id} (apply utils/route-to target))
        (ui/teal-button [:div.flex.items-center.justify-center.inherit-color label]))))

(defmethod popup/component :consolidated-cart-free-install
  [{:keys [footer-data faq-data black-friday-time?] :as queried-data} owner _]
  (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
             :close-attrs (utils/fake-href events/control-consolidated-cart-free-install-dismiss)
             :bg-class    "bg-darken-4"}
            [:div.bg-white
             {:style {:max-width "400px"}}
             [:div.col-12.clearfix.pt1.pk2.bg-lavender.white
              [:div.right.pt2.pr2.pointer
               (svg/simple-x
                (merge (utils/fake-href events/control-consolidated-cart-free-install-dismiss)
                       {:data-test "consolidated-cart-free-install-popup-dismiss"
                        :height    "20px"
                        :width     "20px"
                        :class     "white"}))]

              (if black-friday-time?
                [:div.py2.center.col-8.bold.mx-auto.pointer.h6.medium
                 "25% off EVERYTHING"
                 [:div "Use promo code: " [:span.bold "SALE"]]]

                [:div.py2.center.col-8.bold.mx-auto
                 "Buy 3 items and receive your free Mayvenn Install"])]

             [:div.flex.flex-column
              [:div.mt10.mb6
               (if black-friday-time?
                 get-a-mayvenn-install-black-friday
                 get-a-mayvenn-install)

               [:div.col-8.mx-auto.mt5
                (cta-molecule queried-data)]]

              mayvenn-is-more-than-a-hair-company
              [:div.my8
               (faq/component (assoc faq-data :modal? true))]

              (component/build call-out-center/organism queried-data nil)

              [:div.hide-on-tb-dt ;; Footer
               (component/build footer-modal/component footer-data nil)]]]))

(defmethod transitions/transition-state events/popup-show-consolidated-cart-free-install
  [_ event args app-state]
  (assoc-in app-state keypaths/popup :consolidated-cart-free-install))

(defmethod transitions/transition-state events/control-consolidated-cart-free-install-dismiss
  [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod effects/perform-effects events/control-consolidated-cart-free-install-dismiss
  [_ event args previous-app-state app-state]
  (scroll/enable-body-scrolling))

(defmethod popup/query :consolidated-cart-free-install
  [data]
  {:faq-data                    (faq/free-install-query data)
   :footer-data                 (footer-modal/query data)
   :black-friday-time?          (experiments/black-friday-time? data)
   :call-out-center/bg-class    "bg-lavender"
   :call-out-center/bg-ucare-id "6a221a42-9a1f-4443-8ecc-595af233ab42"
   :call-out-center/title       "We can't wait to pay for your install!"
   :call-out-center/subtitle    "" ;; For spacing
   :cta/id                      "browse-stylists"
   :cta/target                  [events/navigate-adventure-find-your-stylist]
   :cta/label                   "Browse Stylists"
   :react/key                   "browse-stylists"})
