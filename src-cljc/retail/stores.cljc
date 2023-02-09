(ns retail.stores
  (:require #?@(:cljs [[goog.string]])
            [mayvenn.visual.tools :as vt]
            [mayvenn.visual.ui.dividers :as dividers]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.components.svg :as svg]
            [storefront.components.money-formatters :as mf]
            [storefront.components.ui :as ui]
            [homepage.ui.promises :as promises]
            [storefront.keypaths :as keypaths]
            [storefront.events :as events]
            [storefront.platform.component-utils :as utils]
            [storefront.accessors.experiments :as experiments]
            [spice.maps :as maps]
            [mayvenn.visual.ui.titles :as titles]
            [clojure.string :as string]
            [ui.wig-services-menu :as wig-services-menu]
            [ui.wig-customization-spotlights :as wig-customization-spotlights]
            [adventure.components.layered :as layered]))


(def subheader
  [:div.max-580.mx-auto.py6
   (ui/defer-ucare-img {:class "col-12 mb3"
                        :alt   "Mayvenn Beauty Lounge"}
     "f0ac8b6a-5815-4e95-ad74-688b598498da")
   (titles/canela
    {:id      "retail-canela-subheader"
     :primary (str "Shop high-quality 100% virgin human hair wigs, extensions, bundles, and more "
                   "or get a consultation with a beauty expert! "
                   "Personalize your next wig for as low as $25 at a Mayvenn Beauty Lounge near you!")})])

(def header
  [:div.center.p3
   ;; What we might call a hero elsewhere
   [:div.max-960.mx-auto
    [:div.hide-on-mb
     (ui/defer-ucare-img {:class "col-12 flex"
                          :smart-crop "1000x400"
                          :alt "A mayvenn retail store"}
       "478e43d5-d03e-45d2-adb0-7a0bc49d546f")]
    [:div.hide-on-tb-dt
     (ui/defer-ucare-img {:class "col-12 flex"
                          :alt "A mayvenn retail store"}
       "5ed37232-15f0-4e24-ab74-ff5a227419f9")]
    (component/build layered/promises-omni)]
   ;; Sub header image
   subheader])

(defn store-locations
  [{:keys [metro-locations]}]
  [:div.flex.flex-column.gap-8
   (for [[metro locations] metro-locations
         :let              [key (str "metro-" (string/lower-case metro))]]
     [:div.col-12.max-960.mx-auto
      {:key key
       :id  key}
      [:h1.canela.title-1.center.mb6 (str metro " Locations")]
      [:div.flex.flex-wrap.container.justify-center-on-mb.mx-auto
       (for [{:keys [name slug img-url address1-2 city-state-zip phone mon-sat-hours sun-hours
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
            [:a.block.black.content-4.my2
             {:href       (str "tel:" phone)
              :id         (str "phone-retail-" slug)
              :aria-label (str "Call " name " Beauty Lounge")}
             phone]]
           [:div
            [:div.title-3.proxima.shout.bold "Hours"]
            [:div
             [:div.content-4 mon-sat-hours]
             [:div.content-4 sun-hours]]]]
          [:div.flex.justify-between.gap-4
           [:div (ui/button-small-underline-primary {:href directions
                                                     :id   (str "directions-retail-" slug)}
                                                    "Get Directions")]
           [:div.flex
            (when instagram [:a.block.mx1.flex.items-center {:href instagram :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Instagram")}
                             [:div ^:inline (svg/instagram {:style {:height "20px" :width "20px"}})]])
            (when facebook [:a.block.mx1.flex.items-center {:href facebook :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Facebook")}
                            [:div ^:inline (svg/facebook-f {:style {:height "20px" :width "20px"}})]])
            (when tiktok [:a.block.mx1.flex.items-center {:href tiktok :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Tiktok")}
                          [:div ^:inline (svg/tiktok {:style {:height "20px" :width "20px"}})]])
            (when email [:a.block.mx1.flex.items-center {:href (ui/email-url email) :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn email")}
                         [:div ^:inline (svg/icon-email {:height "20px" :width "28px"})]])]]])]])])

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

(def customize-your-wig-data
  ;; NOTE(le): This is copied from the omni experience landing page
  {:desktop-ordering "Top|Bottom"
   :top {:layer/type :image
         :alt "Customize Your Wig"
         :image {:title "Get a Custom Wig"
                 :description ""
                 :file {:url "//images.ctfassets.net/76m8os65degn/1XUbEPeRiokZAE9Z2Thgmk/597c421c3a0f108a29320329b5dc3420/omni_growth_get_a_custom_wig.jpg"
                        :details {:size 1084931
                                  :image {:width 1400
                                          :height 900}}
                        :file-name "omni_growth_get_a_custom_wig.jpg"
                        :content-type "image/jpeg"}
                 :content/updated-at 1675102861991
                 :content/type "Asset"
                 :content/id "1XUbEPeRiokZAE9Z2Thgmk"}
         :navigation-message nil}
   :bottom {:layer/type :lp-title-text-cta-background-color
            :header/value "Customize Your Wig"
            :body/value (str "Mayvenn Beauty Lounges offer a variety of services to provide unlimited looks.\n\n"
                             "- We'll personalize your lace to ensure a natural hairline.\n"
                             "- Layers, blunt cuts, bangs—you name it, we'll cut it.\n"
                             "- Pick a color, any color, and we'll achieve it for you. Highlights, balayage and all.\n"
                             "- Need more volume, we'll add more hair to reach your FULL expectations.\n"
                             "- We're here every step of the way, drop your wig off and we'll restyle it like new!")
            :cta/value "View Services"
            :cta/id "landing-page--cta"
            :cta/target "https://shop.diva-acceptance.com/info/walmart"
            :background/color "cool-gray"
            :content/color "black"}})

(component/defcomponent template
  [{:keys [retail-stores-more-info?] :as data} _ opts]
  [:div
   [:div.p3
    header
    (store-locations data)]
   (when retail-stores-more-info?
     [:div
      dividers/green
      (component/build layered/lp-split customize-your-wig-data opts)
      dividers/purple
      (component/build wig-services-menu/component (vt/with :wig-services-menu data))
      dividers/green])])

(def navigate-show-page
  {"katy"          events/navigate-retail-walmart-katy
   "houston"       events/navigate-retail-walmart-houston
   "grand-prairie" events/navigate-retail-walmart-grand-prairie
   "dallas"        events/navigate-retail-walmart-dallas
   "mansfield"     events/navigate-retail-walmart-mansfield})

(defn location-query
  [{:keys [email facebook hero state hours name phone-number instagram tiktok
           location address-1 address-2 address-zipcode address-city slug] :as data}]
  (when (and name slug)
    {:name             (str name ", " state)
     :slug             slug
     :img-url          (-> hero :file :url)
     :address1-2       (when address-1 (str address-1 (when address-2 (str ", " address-2))))
     :city-state-zip   (when address-city (str address-city ", " state " " address-zipcode))
     :phone            phone-number
     :mon-sat-hours    (first hours)
     :sun-hours        (last hours)
     :show-page-target (get navigate-show-page slug)
     :directions       #?(:cljs (when (:lat location)
                                  (str "https://www.google.com/maps/search/?api=1&query="
                                       (goog.string/urlEncode (str "Mayvenn Beauty Lounge "
                                                                   address-1
                                                                   address-2)
                                                              ","
                                                              (:lat location)
                                                              ","
                                                              (:lon location))))
                          :clj "")
     :instagram        (when instagram (str "https://www.instagram.com/" instagram))
     :facebook         (when facebook (str "https://business.facebook.com/" facebook))
     :tiktok           (when tiktok (str "https://www.tiktok.com/@" tiktok))
     :email            email}))

(defn query [app-state]
  (merge
   {:retail-stores-more-info? (experiments/retail-stores-more-info? app-state)
    :metro-locations          (->> (get-in app-state keypaths/cms-retail-location)
                                   vals
                                   (group-by :metro)
                                   (maps/map-values (partial map location-query)))}
   (vt/within :customize-your-wig customize-your-wig-data)
   (vt/within :wig-customization-guide wig-customization-spotlights/standard-data)
   (vt/within :wig-services-menu wig-services-menu/service-menu-data)))


(defn built-component
  [data opts]
  (component/build template (query data) opts))

(defmethod effects/perform-effects events/navigate-retail-walmart
  [_ _ _ _ app-state]
  (effects/fetch-cms2 app-state [:retailLocation]))
