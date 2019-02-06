(ns adventure.components.program-details-popup
  (:require [sablono.core :refer [html]]
            [storefront.api :as api]
            [storefront.component :as component]
            [adventure.faq :as faq]
            [storefront.components.accordion :as accordion]
            [storefront.components.footer-modal :as footer-modal]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]))

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

(defn component [{:keys [footer-data faq-data]} owner _]
  (component/create
   (html
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
                [:div.mt10.mb6 get-a-free-install]

                why-mayvenn-is-right-for-you

                [:div.mt10
                 (faq/component (assoc faq-data :modal? true))]

                [:div.hide-on-tb-dt.pt3 ;; Footer
                 (component/build footer-modal/component footer-data nil)]]]))))

(defn query
  [data]
  {:faq-data    (faq/query data)
   :footer-data (footer-modal/query data)})

(defn built-component
  [data opts]
  (component/build component data opts))

(defmethod transitions/transition-state events/control-adventure-free-install-dismiss [_ event args app-state]
  (assoc-in app-state keypaths/popup nil))

(defmethod transitions/transition-state events/popup-show-adventure-free-install [_ event args app-state]
  (assoc-in app-state keypaths/popup :adventure-free-install))
