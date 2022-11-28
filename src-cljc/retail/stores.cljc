(ns retail.stores
  (:require #?@(:cljs [[goog.string]])
            [mayvenn.visual.tools :as vt]
            [mayvenn.visual.ui.dividers :as dividers]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.components.svg :as svg]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]))

(def header
  [:div.center.p3
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
       [:div.border-top.border-gray.flex.col-12.justify-between.gap-4
        [:div
         [:div.title-3.proxima.shout.bold "Location"]
         [:div.content-4 address1-2]
         [:div.content-4 city-state-zip]
         [:div.content-4.my2 phone]]
        [:div
         [:div.title-3.proxima.shout.bold "Hours"]
         [:div
          [:div.content-4 mon-sat-hours]
          [:div.content-4 sun-hours]]]]
       [:div.flex.justify-between.gap-4
        [:div (ui/button-small-underline-primary {:href directions} "Get Directions")]
        [:div.flex
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

(defn wig-customization-spotlight-section
  [ix {:keys [title copy url]}]
  [:div.flex.flex-column.items-center.pb4
   {:style {:max-width "250px"}}
   (ui/circle-ucare-img {:width "160" :alt ""} url)
   [:div.col-12.pt2.canela (->> ix inc (str "0"))]
   [:div.col-12.proxima.content-2.bold.shout title]
   [:div copy]])

(defn wig-customization-spotlights
  [{:header/keys [title subtitle] :as data}]
  [:div.wig-customization.flex.flex-column.items-center.p8.gap-4.bg-cool-gray
   [:div.canela.title-1.shout title]
   [:div.proxima.title-1.bold.shout subtitle]
   (into [:div.grid.gap-4] (map-indexed wig-customization-spotlight-section (:sections data)))])

(defn wig-services-menu-item
  [ix {:keys [title price]}]
  [:div.flex.justify-between {:key ix}
   [:div title]
   [:div (mf/as-money-without-cents price)]])

(defn wig-services-menu-section
  [ix {:keys [header/title items]}]
  [:div.pt5 {:key ix}
   [:div.proxima.content-2.bold.shout title]
   (map-indexed wig-services-menu-item items)])

(defn wig-services-menu
  [{:keys [header/title sections]}]
  [:div.bg-pale-purple.p6
   [:div.flex.flex-column.mx-auto.col-8-on-tb-dt
    {:style {:max-width "375px"}}
    [:div.center.canela.title-1 title]
    (map-indexed wig-services-menu-section sections)]])

(component/defcomponent template
  [{:keys [retail-stores-more-info?] :as data} _ _]
  [:div
   [:div.p3
    header
    (store-locations data)]
   (when retail-stores-more-info?
     (wig-customization-spotlights (vt/with :wig-customization-guide data)))
   (when retail-stores-more-info?
     dividers/purple)
   (when retail-stores-more-info?
     (wig-services-menu (vt/with :wig-services-menu data)))
   (when retail-stores-more-info?
     dividers/green)
   why-mayvenn])

(def navigate-show-page
  {"katy"          events/navigate-retail-walmart-katy
   "houston"       events/navigate-retail-walmart-houston
   "grand-prairie" events/navigate-retail-walmart-grand-prairie
   "dallas"        events/navigate-retail-walmart-dallas
   "mansfield"     events/navigate-retail-walmart-mansfield})

(defn query [app-state]
  (let [locations (get-in app-state keypaths/cms-retail-location)]
    (merge
     {:retail-stores-more-info? (experiments/retail-stores-more-info? app-state)
      :locations                (mapv (fn [[_ {:keys [email facebook hero state hours name phone-number instagram tiktok
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
                                           :directions       #?(:cljs (when (:lat location ) (str "https://www.google.com/maps/search/?api=1&query=" (goog.string/urlEncode (str "Mayvenn Beauty Lounge " address-1 (when address-2 address-2)) "," (:lat location)"," (:lon location))))
                                                                :clj "")
                                           :instagram        (when instagram (str "https://www.instagram.com/" instagram))
                                           :facebook         (when facebook (str "https://business.facebook.com/" facebook))
                                           :tiktok           (when tiktok (str "https://www.tiktok.com/@" tiktok))
                                           :email            email})) locations)}
     (vt/within :wig-customization-guide
                {:header/title    "Wig Customization"
                 :header/subtitle "Here's how it works:"
                 :sections        [{:title "Select your wig"
                                    :copy  "Choose a pre-customized, factory-made, or tailor-made unit."
                                    :url   "https://ucarecdn.com/1596ef7a-8ea8-4e2d-b98f-0e2083998cce/select_your_wig.png"}
                                   {:title "We customize it"
                                    :copy  "Choose from ten different customization services— we'll make your dream look come to life."
                                    :url   "https://ucarecdn.com/b8902af1-9262-4369-ab88-35e82fd2f3b7/we_customize_it.png"}
                                   {:title "Take it home"
                                    :copy  "Rock your new unit the same day or pick it up within 2-5 days."
                                    :url   "https://ucarecdn.com/8d4b8e12-48a7-4e90-8a41-3f1ef1267a93/take_it_home.png"}]})
     (vt/within :wig-services-menu
                {:header/title "Wig Services"
                 :sections     [{:header/title "Customization"
                                 :items        [{:title "Basic Lace Customization"
                                                 :price 25}
                                                {:title "Wig Customization"
                                                 :price 60}
                                                {:title "Basic Wig Coloring"
                                                 :price 95}
                                                {:title "Advanced Wig Coloring"
                                                 :price 150}]}
                                {:header/title "Cut"
                                 :items        [{:title "Basic Wig Cut"
                                                 :price 35}
                                                {:title "Advanced Wig Cut"
                                                 :price 50}]}
                                {:header/title "Stylist"
                                 :items        [{:title "Basic Wig Styling"
                                                 :price 35}
                                                {:title "Advanced Wig Styling"
                                                 :price 50}]}]}))))

(defn built-component
  [data opts]
  (component/build template (query data) opts))

(defmethod effects/perform-effects events/navigate-retail-walmart
  [_ _ _ _ app-state]
  (effects/fetch-cms2 app-state [:retailLocation]))
