(ns storefront.components.to-adventure-popup
  (:require [sablono.core :refer [html]]
            [storefront.browser.cookie-jar :as cookie-jar]
            [storefront.component :as component]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [clojure.string :as string]))

(defn entry
  [{:keys [icon-uuid icon-width title description]}]
  [:div.col-12.my2.flex.flex-column.items-center.items-end
   [:div.flex.justify-center.items-end.mb1
    {:style {:height "35px"}}
    (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
   [:div.h5.medium.my1 title]

   [:p.h6.col-11.center.dark-gray description]])

(def close-dialog-href
  (utils/fake-href events/control-to-adventure-popup-dismiss))

(defmethod popup/component :to-adventure
  [_ _ _]
  (let [external-redirect (utils/fake-href events/external-redirect-freeinstall
                                           {:query-string
                                            (string/join
                                             "&"
                                             ["utm_medium=referral"
                                              "utm_source=toadventurehomepagemodal"
                                              "utm_term=fi_shoptofreeinstall"])})]
    (component/create
     (html
      (ui/modal {:col-class   "col-12 col-6-on-tb col-6-on-dt my8-on-tb-dt flex justify-center"
                 :close-attrs close-dialog-href
                 :bg-class    "bg-darken-4"}
                [:div.bg-white
                 {:style {:max-width "400px"}}
                 [:div.col-12.clearfix.py2
                  [:div.right.pt2.pr2.pointer
                   (svg/simple-x
                    (merge close-dialog-href
                           {:data-test    "to-adventure-popup-dismiss"
                            :height       "1.5rem"
                            :width        "1.5rem"
                            :class        "stroke-black"
                            :stroke-width "5"}))]
                  [:div.flex.justify-center.pb2
                   [:div.col-6
                    (ui/clickable-logo {:class "col-12 mx4"
                                        :style {:height "40px"}})]]]
                 [:div.flex.flex-column
                  [:div.flex.flex-auto.items-end.mb6
                   {:style {:height              "696px"
                            :background-size     "cover"
                            :background-position "top"
                            :background-image    (str "url('//ucarecdn.com/279dac3f-317a-469a-a487-5b54857529aa/-/format/jpeg/')")
                            :alt                 "Buy 3 bundles or more (closures and frontals included) and we'll pay for you to get your hair installed by a Mayvenn Certified Stylist. We've decided to improve the way you purchase hair and book your sew-ins. Learn more"}}
                   [:div.mx-auto
                    {:style {:width "150px"}}
                    (ui/teal-button (merge external-redirect {:height-class "py2"
                                                              :data-test    "to-adventure-modal-learn-more"})
                                                       "Learn More")]]

                  [:div.col-12.bg-transparent-teal.mt3.pt6.pb8.px4
                   [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

                    [:div.my2.flex.flex-column.items-center.col-12
                     [:h2.titleize "How It Works"]
                     [:div.h6.dark-gray "It's simple"]]

                    (entry {:icon-uuid   "3d2b326c-7773-4672-827e-f13dedfae15a"
                            :icon-width  "22"
                            :title       "1. Choose a Mayvenn Certified Stylist"
                            :description "We’ve partnered with thousands of top stylists around the nation. Choose one in your local area and we’ll pay the stylist to do your install."})
                    (entry {:icon-uuid   "c1e0b9f0-d78e-42bc-b78a-966f0632364f"
                            :icon-width  "24"
                            :title       "2. Buy Any Three Bundles or More"
                            :description "This includes closures, frontals, and 360 frontals. Risk free - your virgin hair and service are covered by our 30 day guarantee."})
                    (entry {:icon-uuid   "b8e49f7b-935e-4350-bf25-103c3f86fed3"
                            :icon-width  "35"
                            :title       "3. Schedule Your Appointment"
                            :description "We’ll connect you to your Mayvenn Certified Stylist and book an install appointment that’s convenient for you."})]
                   [:div.mx-auto
                    {:style {:width "150px"}}
                    (ui/teal-button (merge
                                     {:height-class "py2"}
                                     external-redirect)
                                    [:span "Learn More"])]]]])))))

(defmethod popup/query :to-adventure [data] {})

(defmethod transitions/transition-state events/popup-show-to-adventure
  [_ _ _ app-state]
  (assoc-in app-state keypaths/popup :to-adventure))

(defmethod effects/perform-effects events/control-to-adventure-popup-dismiss
  [_ _ _ _ app-state]
  (cookie-jar/save-dismissed-to-adventure (get-in app-state keypaths/cookie) true)
  (messages/handle-message events/popup-hide))

(defmethod transitions/transition-state events/control-to-adventure-popup-dismiss
  [_ _ _ app-state]
  (-> app-state
      (assoc-in keypaths/dismissed-to-adventure true)
      (assoc-in keypaths/popup nil)))
