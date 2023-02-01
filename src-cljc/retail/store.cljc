(ns retail.store
  (:require #?@(:cljs [[storefront.hooks.google-maps :as google-maps]])
            [storefront.component :as component]
            [mayvenn.visual.tools :as vt]
            [mayvenn.visual.ui.dividers :as dividers]
            [adventure.components.layered :as layered]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [homepage.ui.promises :as promises]
            [storefront.keypaths :as keypaths]
            [storefront.accessors.experiments :as experiments]
            [storefront.events :as events]
            [clojure.string :as string]
            [ui.wig-services-menu :as wig-services-menu]))

(defn video
  [{:video/keys [youtube-id]}]
  (when youtube-id
    [:div.max-960.center.mx-auto
     (ui/youtube-responsive (str "https://www.youtube.com/embed/" youtube-id "?rel=0&color=white&showinfo=0&controls=1&loop=1&modestbranding"))]))

(defn store-info
  [{:keys [name img-url address1-2 city-state-zip phone mon-sat-hours sun-hours
           directions instagram facebook tiktok email metro]}]
  [:div.max-960.py3.flex.mx-auto.flex-column-on-mb.flex-row.gap-4.mx4-on-mb
   [:div.col-6-on-tb-dt.col-12.flex.flex-column.gap-4
    [:div.flex.justify-between
     {:style {:align-items "start"}}
     [:div.left
      [:h1.canela.title-2 name]
      [:div.proxima.content-3 "Visit us inside Walmart"]]
     [:div.flex
      (when instagram [:a.block.mx1.flex.items-center {:href instagram :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Instagram")}
                       [:div ^:inline (svg/instagram {:style {:height "20px" :width "20px"}})]])
      (when facebook [:a.block.mx1.flex.items-center {:href facebook :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Facebook")}
                      [:div ^:inline (svg/facebook-f {:style {:height "20px" :width "20px"}})]])
      (when tiktok [:a.block.mx1.flex.items-center {:href tiktok :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Tiktok")}
                    [:div ^:inline (svg/tiktok {:style {:height "20px" :width "20px"}})]])
      (when email [:a.block.mx1.flex.items-center {:href (ui/email-url email) :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn email")}
                   [:div ^:inline (svg/icon-email {:height "20px" :width "28px"})]])]]
    [:p.content-3
     (str "Welcome to Mayvenn Beauty Lounge in " metro " where we carry a large selection of 100% virgin human "
          "hair wigs, bundles and seamless hair extensions to protect your tresses and create your perfect look.")]
    [:div.border-top.border-gray.flex.col-12.justify-between.gap-4
     [:div
      [:div.title-3.proxima.shout.bold "Location"]
      [:div.content-4 address1-2]
      [:div.content-4 city-state-zip]
      [:div.content-4.mt2 phone]]
     [:div
      [:div.title-3.proxima.shout.bold "Hours"]
      [:div
       [:div.content-4 mon-sat-hours]
       [:div.content-4 sun-hours]]]]
    (ui/button-small-underline-primary {:href directions} "Get Directions")
    [:div.flex.flex-column.gap-2
     (->> ["Lace Front Wigs, Closure Wigs, & More"
           "Bundles, Closures, Clip-ins, & More"
           "Coloring, Wig Services, & More"]
          (map (fn [text] [:div.shout.content-3.bg-pale-purple.p2.bold text])))]]
   [:div.col-6-on-tb-dt.col-12 (ui/basic-defer-img {:width "100%" :alt ""} img-url)]])

(defn follow-us
  [{:follow-us/keys [instagram photos]}]
  [:div.bg-pale-purple.center.py5
   [:h2.proxima.title-2.shout.pt5 "Follow Us"]
   [:a.title-1.canela.pb5.inherit-color
    {:style {:overflow-wrap "break-word"}
     :href  (str "https://www.instagram.com/" instagram)}
    (str "@" instagram)]
   [:div.flex.flex-wrap.pt3
    (for [{:keys [file description]} photos]
      [:a.col-6.col-3-on-tb-dt
       {:key        file
        :aria-label "Instagram"
        :href       (str "https://www.instagram.com/" instagram)}
       (ui/aspect-ratio 1 1 (ui/img {:src   (:url file)
                                     :style {:object-fit "cover"
                                             :min-height "100%"}
                                     :class "flex-auto col-12"
                                     :alt   description}))])]])

(component/defcomponent template
  [{:keys [retail-stores-more-info?] :as data} _ _]
  [:div
   (video data)
   [:div.mx-auto.max-960
    (component/build promises/organism {:list/icons
                                        [{:promises.icon/symbol :svg/check-cloud,
                                          :promises.icon/title  "100% Virgin Human Hair"}
                                         {:promises.icon/symbol :svg/custom-wig-services,
                                          :promises.icon/title  "Custom Wig Services"}
                                         {:promises.icon/symbol :svg/hand-heart,
                                          :promises.icon/title  "Top Notch Service"}
                                         {:promises.icon/symbol :svg/shield,
                                          :promises.icon/title  "30-Day Guarantee"}]})]
   (store-info (vt/with :location-card data))
   (when retail-stores-more-info?
     [:div
      dividers/green
      (component/build layered/lp-split (vt/with :customize-your-wig data))
      dividers/purple
      (component/build wig-services-menu/component (vt/with :wig-services-menu data))
      dividers/green])
   (follow-us data)])

(defn build-google-maps-url [location address-1 address-2]
  #?(:cljs (when (:lat location)
             (str "https://www.google.com/maps/search/?api=1&query="
                  (goog.string/urlEncode
                   (str "Mayvenn Beauty Lounge " address-1 address-2) "," (:lat location)"," (:lon location))))
     :clj ""))

(defn query-all [{:keys [email facebook hero state hours name phone-number instagram tiktok location address-1 address-2
                         address-zipcode address-city store-tour-you-tube-video-id instagram-photos metro]}
                 retail-stores-more-info?]
  (merge {:retail-stores-more-info?     retail-stores-more-info?
          :video/youtube-id             store-tour-you-tube-video-id

          :follow-us/instagram          instagram
          :follow-us/photos             instagram-photos}
         (vt/within :location-card
                    {:name           (str name ", " state)
                     :img-url        (-> hero :file :url)
                     :address1-2     (string/join ", " (keep identity [address-1 address-2]))
                     :city-state-zip (when address-city (str address-city ", " state " " address-zipcode))
                     :phone          phone-number
                     :mon-sat-hours  (first hours)
                     :sun-hours      (last hours)
                     :directions     (build-google-maps-url location address-1 address-2)
                     :instagram      (when instagram (str "https://www.instagram.com/" instagram))
                     :facebook       (when facebook (str "https://business.facebook.com/" facebook))
                     :tiktok         (when tiktok (str "https://www.tiktok.com/@" tiktok))
                     :email          email
                     :metro          metro})
         (vt/within :customize-your-wig
                    {:desktop-ordering "Top|Bottom"
                     :top              {:layer/type         :lp-split-image
                                        :alt                "Customize Your Wig"
                                        :image              {:title              "Get a Custom Wig"
                                                             :description        ""
                                                             :file               {:url          "//images.ctfassets.net/76m8os65degn/1XUbEPeRiokZAE9Z2Thgmk/597c421c3a0f108a29320329b5dc3420/omni_growth_get_a_custom_wig.jpg"
                                                                                  :details      {:size  1084931
                                                                                                 :image {:width  1400
                                                                                                         :height 900}}
                                                                                  :file-name    "omni_growth_get_a_custom_wig.jpg"
                                                                                  :content-type "image/jpeg"}
                                                             :content/updated-at 1675102861991
                                                             :content/type       "Asset"
                                                             :content/id         "1XUbEPeRiokZAE9Z2Thgmk"}
                                        :navigation-message nil}
                     :bottom           {:layer/type       :lp-title-text-cta-background-color
                                        :header/value     "Customize Your Wig"
                                        :body/value       (str "Mayvenn Beauty Lounges offer a variety of services to provide unlimited looks.\n\n"
                                                               "- We'll personalize your lace to ensure a natural hairline.\n"
                                                               "- Layers, blunt cuts, bangs—you name it, we'll cut it.\n"
                                                               "- Pick a color, any color, and we'll achieve it for you. Highlights, balayage and all.\n"
                                                               "- Need more volume, we'll add more hair to reach your FULL expectations.\n"
                                                               "- We're here every step of the way, drop your wig off and we'll restyle it like new!")
                                        :cta/value        "View Services"
                                        :cta/id           "landing-page--cta"
                                        :cta/target       "https://shop.diva-acceptance.com/info/walmart"
                                        :background/color "cool-gray"
                                        :content/color    "black"}})
         (vt/within :wig-services-menu
                    wig-services-menu/service-menu-data)))

(def nav-event->cms-key
  {events/navigate-retail-walmart-grand-prairie :grand-prairie
   events/navigate-retail-walmart-katy          :katy
   events/navigate-retail-walmart-houston       :houston
   events/navigate-retail-walmart-mansfield     :mansfield
   events/navigate-retail-walmart-dallas        :dallas})

(defn query [app-state]
  (let [retail-store-cms-key     (nav-event->cms-key (get-in app-state keypaths/navigation-event))
        retail-stores-more-info? (experiments/retail-stores-more-info? app-state)
        store                    (-> app-state
                                     (get-in keypaths/cms-retail-location)
                                     (get retail-store-cms-key))]
    (query-all store retail-stores-more-info?)))

(defn built-component
  [data opts]
  (component/build template (query data) opts))
