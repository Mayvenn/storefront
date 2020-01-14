(ns storefront.components.v2
  (:require [storefront.components.modal-gallery :as modal-gallery]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.platform.component-utils :as utils]
            [storefront.components.accordion :as accordion]))

(defcomponent get-a-free-install
  [{:keys [store
           gallery-ucare-ids
           stylist-portrait
           stylist-name
           modal?
           stylist-gallery-open?]
    :as data}
   _ _]
  (let [step (fn [{:keys [icon-uuid icon-width title description]}]
               [:div.col-12.mt2.center
                (when (not modal?)
                  {:class "col-4-on-dt"})
                [:div.flex.justify-center.items-end.mb2
                 {:style {:height "39px"}}
                 (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                [:div.h5.p-color.medium title]
                [:p.h6.col-10.col-9-on-dt.mx-auto description]])]

    [:div.col-12
     [:div.mt2.flex.flex-column.items-center
      [:h2 "Get a FREE Install"]
      [:div.h6 "In three easy steps"]]

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
             :description (if (contains? #{"store" "shop"}
                                         (:store-slug store))
                            (str "Redeem the voucher at your appointment with a Mayvenn stylist. "
                                 "Check out some of our certified stylists below:")
                            "Redeem the voucher when you go in for your appointment with: ")})]

     [:div.mt2.flex.flex-column.items-center
      [:div.h6.my1 "Your Stylist"]
      [:div.circle.hide-on-mb-tb
       (if (:resizable-url stylist-portrait)
         (ui/circle-picture {:width "100"} (ui/square-image stylist-portrait 100))
         (ui/circle-ucare-img {:width "100"} "23440740-c1ed-48a9-9816-7fc01f92ad2c"))]
      [:div.circle.hide-on-dt
       (if (:resizable-url stylist-portrait)
         (ui/circle-picture (ui/square-image stylist-portrait 70))
         (ui/circle-ucare-img {:width "70"} "23440740-c1ed-48a9-9816-7fc01f92ad2c"))]
      [:div.h5.bold stylist-name]
      [:div.h6
       (when (:licensed store)
         [:div.flex.items-center {:style {:height "1.5em"}}
          ^:inline (svg/check {:class "stroke-p-color" :height "2em" :width "2em"}) "Licensed"])
       [:div.flex.items-center {:style {:height "1.5em"}}
        (ui/ucare-img {:width "7" :class "pr2"} "bd307d38-277d-465b-8360-ac8717aedb03")
        (let [salon-city    (-> store :salon :city not-empty)
              salon-state   (-> store :salon :state not-empty)
              stylist-city  (-> store :location :city)
              stylist-state (-> store :location :state-abbr)]
          (if (and salon-city salon-state)
            (str salon-city ", " salon-state)
            (str stylist-city ", " stylist-state)))]]
      (when (seq gallery-ucare-ids)
        [:div.h6.pt1.flex.items-center
         ;; TODO: Non teal asset
         (ui/ucare-img {:width "25"} "18ced560-296f-4b6c-9c82-79a4e8c15d95")
         [:a.ml1.p-color.medium
          (utils/fake-href events/control-stylist-gallery-open)
          "Hair Gallery"]
         (modal-gallery/simple
          {:slides      (map modal-gallery/ucare-img-slide gallery-ucare-ids)
           :open?       stylist-gallery-open?
           :close-event events/control-stylist-gallery-close})])]]))

(defmethod transitions/transition-state events/control-stylist-gallery-open [_ _ _ app-state]
  (assoc-in app-state keypaths/carousel-stylist-gallery-open? true))

(defmethod transitions/transition-state events/control-stylist-gallery-close [_ _ _ app-state]
  (assoc-in app-state keypaths/carousel-stylist-gallery-open? false))

(defn why-mayvenn-is-right-for-you
  ([] (why-mayvenn-is-right-for-you false))
  ([modal?]
   (let [entry (fn [{:keys [icon-uuid icon-width title description]}]
                 [:div.col-12.my2.flex.flex-column.items-center.items-end
                  (when (not modal?)
                    {:class "col-3-on-dt"})
                  [:div.flex.justify-center.items-end.mb1
                   {:style {:height "35px"}}
                   (ui/ucare-img {:alt title :width icon-width} icon-uuid)]
                  [:div.h6.p-color.medium.mbnp4 title]

                  [:p.h6.col-11.center description]])]
     [:div.col-12.bg-pale-purple.mt3.py8.px4
      [:div.col-11-on-dt.justify-center.flex.flex-wrap.mx-auto.pb2

       [:div.my2.flex.flex-column.items-center.col-12
        [:h2.titleize "Why Mayvenn?"]
        [:div.h6.black.titleize "It's not just about hair"]]

       (entry {:icon-uuid   "ab1d2ed4-ff93-40e6-978a-721133ca88a7"
               :icon-width  "29"
               :title       "Top Notch Customer Service"
               :description "Our team is made up of hair experts who are ready to help you by phone, text, and email."})
       (entry {:icon-uuid   "8787e30c-2879-4a43-8d01-9d6790575084"
               :icon-width  "52"
               :title       "30 Day Guarantee"
               :description "Wear it, dye it, even cut it! If you're not in love with your hair, we'll exchange it within 30 days."})
       (entry {:icon-uuid   "e02561dd-c294-43b7-bb33-c40bfabea518"
               :icon-width  "35"
               :title       "100% Virgin Hair"
               :description "Our hair is gently steam-processed and lasts a year or more with proper care. Available in 8 textures, we also offer a range of color options."})
       (entry {:icon-uuid   "3f622e92-6d95-49e2-a0c1-51a535b22975"
               :icon-width  "35"
               :title       "Free Install"
               :description "Get your hair installed absolutely FREE!"})]])))

(def ^:private faq-section-copy
  (let [phone-link (ui/link :link/phone :a.inherit-color {} "1-888-562-7952")]
    [(accordion/section (component/html [:h6 "How does this all work? How do I get a free install?"])
                        (component/html ["It’s easy! Mayvenn will pay your stylist directly for your install."
                                         " Just purchase 3 bundles or more (frontals and closures count as bundles)"
                                         " and use code FREEINSTALL at checkout. You’ll receive a voucher as soon"
                                         " as your order ships. Schedule an appointment with your Mayvenn stylist,"
                                         " and present the voucher to them at the appointment."
                                         " Your stylist will receive the full payment for your install"
                                         " immediately after the voucher has been scanned!"]))
     (accordion/section (component/html [:h6 "What's included in the install?"])
                        (component/html ["Typically a full install includes a wash, braid down, and simple styling."
                                         " Service details may vary so it would be best to check with your stylist"
                                         " to confirm what is included."]))
     (accordion/section (component/html [:h6 "How does the 30 day guarantee work?"])
                        (component/html ["Buy Mayvenn hair RISK FREE with easy returns and exchanges."])
                        (component/html ["EXCHANGES" [:br] "Wear it, dye it, even cut it! If you're not satified with your"
                                         " hair, we'll exchange it within 30 days of purchase. Our customer service"
                                         " team is ready to answer any questions you may have. Give us a call:"
                                         ^:inline phone-link])
                        (component/html ["RETURNS" [:br] "If you are not completely happy with your Mayvenn hair"
                                         " before it is installed, we will refund your purchase if the"
                                         " bundle is unopened and the hair is in its original condition."
                                         " Give us a call to start your return:"
                                         ^:inline phone-link]))
     (accordion/section (component/html [:h6 "Who is going to do my hair?"])
                        (component/html ["The free-install offer is only valid at your Mayvenn"
                                         " stylist. If you are unsure if your stylist is"
                                         " participating in the free-install offer, you can simply"
                                         " ask them or contact Mayvenn customer service: "
                                         ^:inline phone-link
                                         [:br]
                                         [:br]
                                         "Our stylists specialize in sew-in installs with leave-out, closures,"
                                         " frontals, and 360 frontals so you can rest assured that we have a stylist"
                                         " to help you achieve the look you want."]))
     (accordion/section (component/html [:h6 "What if I want to get my hair done by another stylist?"
                                         " Can I still get the free install?"])
                        (component/html ["You must get your hair done from a Mayvenn stylist in"
                                         " order to get your hair installed for free."]))
     (accordion/section (component/html [:h6 "Why should I order hair from Mayvenn?"])
                        (component/html ["Mayvenn is a Black owned company that offers 100% virgin hair."
                                         " Our Virgin and Dyed Virgin hair can be found in a variety of textures from"
                                         " straight to curly. Virgin hair starts at $55 per bundle."
                                         " All orders are eligible for free shipping and backed by our 30 Day"
                                         " Guarantee."]))]))

(defcomponent faq [{:keys [expanded-index modal?] :as data} _ _]
  [:div.px6.mx-auto.mb6
   (when (not modal?)
     {:class "col-5-on-dt"})
   [:h2.center "Frequently Asked Questions"]
   (component/build
    accordion/component
    {:expanded-indices #{expanded-index}}
    {:opts {:section-click-event events/faq-section-selected
            :static-sections     faq-section-copy}})])

(defn get-ucare-id-from-url
  [ucare-url]
  (last (re-find #"ucarecdn.com/([a-z0-9-]+)/" (str ucare-url))))
