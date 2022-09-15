(ns retail.stores
  (:require #?@(:cljs [[goog.string]])
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]))

(def header
  [:div.center
   [:div.max-960.mx-auto
    (ui/defer-ucare-img {:class "col-12"
                         :smart-crop "1000x400"
                         :alt ""} "991a7fac-b4ed-4d1e-95d5-f2153fe678e2")]
   [:div.max-580.mx-auto.py6
    (ui/defer-ucare-img {:class "col-12 mb3"
                         :alt "Mayvenn Beauty Lounge"} "f0ac8b6a-5815-4e95-ad74-688b598498da")]])

(defn store-locations
  [{:keys [locations]}]
  [:div.max-960.mx-auto
   [:h1.canela.title-1.center.mb6 "Store Locations"]
   [:div.flex.flex-wrap.container.justify-center-on-mb.mx-auto
    (for [{:keys [name img-url address1-2 city-state-zip phone mon-sat-hours sun-hours
                  directions instagram facebook tiktok email show-page-target]} locations]
      [:div.col-6-on-tb-dt.col-12.px2.py3
       [:a (merge (utils/route-to show-page-target)
                  {:aria-label (str name " Mayvenn Beauty Lounge")})
        (ui/aspect-ratio 3 2 (ui/img {:width "100%" :class "col-12" :alt "" :src img-url}))]
       [:div.flex.justify-between.pt2
        [:div
         [:h2.canela.title-2 name]
         [:div.proxima.content-3 "Visit us inside Walmart"]]
        [:div
         (ui/button-medium-primary (merge (utils/route-to show-page-target)
                                          {:aria-label (str "Learn more about " name " Beauty Lounge")})
                                   "Learn More")]]
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
         (when instagram [:a.block.mx1.flex.items-center {:href instagram :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Instagram")}
                          [:div ^:inline (svg/instagram {:style {:height "20px" :width "20px"}})]])
         (when facebook [:a.block.mx1.flex.items-center {:href facebook :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Facebook")}
                         [:div ^:inline (svg/facebook-f {:style {:height "20px" :width "20px"}})]])
         (when tiktok [:a.block.mx1.flex.items-center {:href tiktok :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Tiktok")}
                       [:div ^:inline (svg/tiktok {:style {:height "20px" :width "20px"}})]])
         (when email [:a.block.mx1.flex.items-center {:href (ui/email-url email) :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn email")}
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

(def navigate-show-page
  {"katy"          events/navigate-retail-walmart-katy
   "houston"       events/navigate-retail-walmart-houston
   "grand-prairie" events/navigate-retail-walmart-grand-prairie
   "dallas"        events/navigate-retail-walmart-dallas
   "mansfield"     events/navigate-retail-walmart-mansfield})

(defn query [app-state]
  (let [locations (get-in app-state keypaths/cms-retail-location)]
    {:locations (mapv (fn [[_ {:keys [email facebook hero state hours name phone-number instagram tiktok
                                      location address-1 address-2 address-zipcode address-city slug]}]]
                        (when (and name slug)
                          {:name             (str name ", " state)
                           :img-url          (-> hero :file :url)
                           :address1-2       (when address-1 (str address-1 (when address-2 (str ", " address-2))))
                           :city-state-zip   (when address-city (str address-city ", " state " " address-zipcode))
                           :phone            phone-number
                           :mon-sat-hours    (first hours)
                           :sun-hours        (last hours)
                           :show-page-target (get navigate-show-page slug)
                           :directions       #?(:cljs (when (:lat location) (str "https://maps.apple.com/?q=" (goog.string/urlEncode (str "Mayvenn Beauty Lounge " address-1 (when address-2 address-2))) "&ll=" (:lat location)"," (:lon location)))
                                                :clj  "")
                           :instagram        (when instagram (str "https://www.instagram.com/" instagram))
                           :facebook         (when facebook (str "https://business.facebook.com/" facebook))
                           :tiktok           (when tiktok (str "https://www.tiktok.com/@" tiktok))
                           :email            email})) locations)}))

(defn built-component
  [data opts]
  (component/build template (query data) opts))

(defmethod effects/perform-effects events/navigate-retail-walmart
  [_ _ _ _ app-state]
  (effects/fetch-cms2 app-state [:retailLocation]))
