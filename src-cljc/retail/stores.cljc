(ns retail.stores
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.components.formatters :as formatters]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(def header
  [:div.center
   [:div.max-960.mx-auto
    (ui/defer-ucare-img {:class "col-12"
                         :smart-crop "1000x400"} "991a7fac-b4ed-4d1e-95d5-f2153fe678e2")]
   [:div.max-580.mx-auto.py6
    (ui/defer-ucare-img {:class "col-12 mb3"} "f0ac8b6a-5815-4e95-ad74-688b598498da")]])

(defn store-locations
  [{:keys [locations]}]
  [:div.max-960.mx-auto
   [:h1.canela.title-1.center.my6 "Store Locations"]
   [:div.flex.flex-wrap.col-8-on-dt.container.justify-center-on-mb.mx-auto
    (for [{:keys [name img-url address1-2 city-state-zip phone mon-sat-hours sun-hours
                  directions instagram facebook tiktok email]} locations]
      [:div.col-6-on-tb-dt.col-12-on-mb.px1.py3
       (ui/basic-defer-img {:width "100%"  :class "col-12" :alt ""} img-url)
       [:div.left
        [:h2.canela.title-2 name]
        [:div.proxima.content-3 "Visit us inside Walmart"]]
       [:div.border-top.border-gray.flex.col-12
        [:div.col-5
         [:div.title-3.proxima.shout.bold "Location"]
         [:div.content-4 address1-2]
         [:div.content-4 city-state-zip]
         [:div.content-4.my2 phone]]
        [:div.col-7
         [:div.title-3.proxima.shout.bold "Hours"]
         [:div
          [:div.content-4 mon-sat-hours]
          [:div.content-4 sun-hours]]]]
       [:div.flex
        [:div.col-5 (ui/button-small-underline-primary {:href directions} "Get Directions")]
        [:div.flex.col-7
         (when instagram [:a.block.mx1.flex.items-center {:href instagram :rel "noopener" :target "_blank"}
                          [:div ^:inline (svg/instagram {:style {:height "20px" :width "20px"}})]])
         (when facebook [:a.block.mx1.flex.items-center {:href facebook :rel "noopener" :target "_blank"}
                         [:div ^:inline (svg/facebook-f {:style {:height "20px" :width "20px"}})]])
         (when tiktok [:a.block.mx1.flex.items-center {:href tiktok :rel "noopener" :target "_blank"}
                       [:div ^:inline (svg/tiktok {:style {:height "20px" :width "20px"}})]])
         (when email [:a.block.mx1.flex.items-center {:href (ui/email-url email) :rel "noopener" :target "_blank"}
                      [:div ^:inline (svg/icon-email {:height "20px" :width "28px"})]])]]])]])

(def why-mayvenn-icons-text
  [{:icon svg/heart
    :text "Top-Notch Customer Service"}
   {:icon svg/calendar
    :text "30 Day Guarantee"}
   {:icon svg/worry-free
    :text "100% Virgin Hair"}
   {:icon svg/mirror
    :text "Certified Stylists"} ])

(def why-mayvenn
  [:div.bg-cool-gray.mx-auto.center.py6
   [:h2.canela.title-1.my6 "Why Mayvenn"]
   [:div.flex.max-580.mx-auto.center
    (for [{:keys [icon text]} why-mayvenn-icons-text]
      [:div.pb1.pt6
       [:div
        {:width "32px"}
        (icon {:class  "fill-p-color"
                    :width  "32px"
                    :height "29px"})]
       [:h3.title-3.proxima.py1.shout
        text]])]])

(component/defcomponent template
  [data _ _]
  [:div.p3
   header
   (store-locations data)
   why-mayvenn])

(defn query [app-state]
  (let [locations (get-in app-state keypaths/cms-retail-locations)]
    {:locations (mapv (fn [{:keys [email facebook hero state hours name phone-number instagram tiktok
                                   location address-1 address-2 address-zipcode address-city]}]
                        (when (and name hero)
                          {:name           (str name ", " state)
                           :img-url        (-> hero :file :url) ;; TODO: get the image working
                           :address1-2     (when address-1 (str address-1 (when address-2 (str ", " address-2))))
                           :city-state-zip (when address-city (str address-city ", " state " " address-zipcode))
                           :phone          phone-number
                           :mon-sat-hours  (first hours)
                           :sun-hours      (last hours)
                           :directions     (when (:lat location ) (str "https://www.google.com/maps/dir/?api=1&destination=" (:lat location)"," (:lon location)))
                           :instagram      (when instagram (str "https://www.instagram.com/" instagram))
                           :facebook       (when facebook (str "https://business.facebook.com/" facebook))
                           :tiktok         (when tiktok (str "https://www.tiktok.com/@" tiktok))
                           :email          email}) ) locations)}))

(defn built-component
  [data opts]
  (component/build template (query data) opts))
