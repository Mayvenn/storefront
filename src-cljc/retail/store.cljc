(ns retail.store
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

(defn video
  [{:video/keys [title file]}]
  [:div.max-960.center.mx-auto
   [:embed.hide-on-tb-dt {:src            file
                          :wMode          "transparent"
                          :alloFullscreen true
                          :type           "video/mp4"
                          :width          "100%"
                          :height         "100%"
                          :title          title}]
   [:embed.hide-on-mb {:src            file
                       :wMode          "transparent"
                       :alloFullscreen true
                       :type           "video/mp4"
                       :width          "800px"
                       :height         "450px"
                       :title          title}]])

(defn store-info
  [{:location-card/keys [name img-url address1-2 city-state-zip phone mon-sat-hours sun-hours
                         directions instagram facebook tiktok email]}]
  [:div.max-960.py3.flex-wrap.flex.mx-auto
   [:div.col-6-on-tb-dt.col-12.px2
    [:div.mb2
     [:div.left
      [:h1.canela.title-2 name]
      [:div.proxima.content-3 "Visit us inside Walmart"]]
     [:div.flex.right
      (when instagram [:a.block.mx1.flex.items-center {:href instagram :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Instagram")}
                       [:div ^:inline (svg/instagram {:style {:height "20px" :width "20px"}})]])
      (when facebook [:a.block.mx1.flex.items-center {:href facebook :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Facebook")}
                      [:div ^:inline (svg/facebook-f {:style {:height "20px" :width "20px"}})]])
      (when tiktok [:a.block.mx1.flex.items-center {:href tiktok :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn Tiktok")}
                    [:div ^:inline (svg/tiktok {:style {:height "20px" :width "20px"}})]])
      (when email [:a.block.mx1.flex.items-center {:href (ui/email-url email) :rel "noopener" :target "_blank" :aria-label (str name " Mayvenn email")}
                   [:div ^:inline (svg/icon-email {:height "20px" :width "28px"})]])]]
    [:div.border-top.border-gray.flex.col-12.py2
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
     [:div.col-5.pb3 (ui/button-medium-primary {:href directions} "Get Directions")]]]
   [:div.col-6-on-tb-dt.col-12.px2 (ui/basic-defer-img {:width "100%" :alt ""} img-url)]])

(defn follow-us
  [{:follow-us/keys [instagram photos]}]
  [:div.bg-pale-purple.center.py5
   [:h2.proxima.title-2.shout.pt5 "Follow Us"]
   [:div.title-1.canela.pb5
    {:style {:overflow-wrap "break-word"}}
    (str "@" instagram)]
   [:div.flex.flex-wrap
    (for [{:keys [file description]} photos]
      [:a.col-6.col-3-on-tb-dt
       {:key file
        :href (str "https://www.instagram.com/" instagram)}
       (ui/basic-defer-img {:class "bg-pale-purple" :width "100%" :height "100%" :alt description}
                           (:url file))])]])

(def why-mayvenn-icons-text
  [{:icon svg/heart
    :text "Top-Notch Customer Service"}
   {:icon svg/calendar
    :text "30 Day Guarantee"}
   {:icon svg/worry-free
    :text "100% Virgin Hair"}
   {:icon svg/mirror
    :text "Certified Stylists"}])

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
  [:div
   (video data)
   (store-info data)
   (follow-us data)
   why-mayvenn])

(defn query-all [{:keys [email facebook hero state hours name phone-number instagram tiktok
                         location address-1 address-2 address-zipcode address-city video-tour instagram-photos]}]
  {:video/file                   (-> video-tour :file :url)
   :video/title                  (:title video-tour)
   :location-card/name           (str name ", " state)
   :location-card/img-url        (-> hero :file :url)
   :location-card/address1-2     (when address-1 (str address-1 (when address-2 (str ", " address-2))))
   :location-card/city-state-zip (when address-city (str address-city ", " state " " address-zipcode))
   :location-card/phone          phone-number
   :location-card/mon-sat-hours  (first hours)
   :location-card/sun-hours      (last hours)
   :location-card/directions     (when (:lat location ) (str "https://www.google.com/maps/dir/?api=1&destination=" (:lat location)"," (:lon location)))
   :location-card/instagram      (when instagram (str "https://www.instagram.com/" instagram))
   :location-card/facebook       (when facebook (str "https://business.facebook.com/" facebook))
   :location-card/tiktok         (when tiktok (str "https://www.tiktok.com/@" tiktok))
   :location-card/email          email
   :follow-us/instagram          instagram
   :follow-us/photos             instagram-photos})

(defn query-gp [app-state]
  (let [store (->> keypaths/cms-retail-locations
                   (get-in app-state)
                   (filter (fn [location] (= "Grand Prairie" (:name location))))
                   first)]
    (query-all store)))

(defn query-katy [app-state]
  (let [store (->> keypaths/cms-retail-locations
                   (get-in app-state)
                   (filter (fn [location] (= "Katy" (:name location))))
                   first)]
    (query-all store)))

(defn query-dallas [app-state]
  (let [store (->> keypaths/cms-retail-locations
                   (get-in app-state)
                   (filter (fn [location] (= "Dallas" (:name location))))
                   first)]
    (query-all store)))

(defn query-mf [app-state]
  (let [store (->> keypaths/cms-retail-locations
                   (get-in app-state)
                   (filter (fn [location] (= "Mansfield" (:name location))))
                   first)]
    (query-all store)))

(defn query-houston [app-state]
  (let [store (->> keypaths/cms-retail-locations
                   (get-in app-state)
                   (filter (fn [location] (= "Houston" (:name location))))
                   first)]
    (query-all store)))

(defn built-component-grand-prairie
  [data opts]
  (component/build template (query-gp data) opts))

(defn built-component-katy
  [data opts]
  (component/build template (query-katy data) opts))

(defn built-component-dallas
  [data opts]
  (component/build template (query-dallas data) opts))

(defn built-component-mansfield
  [data opts]
  (component/build template (query-mf data) opts))

(defn built-component-houston
  [data opts]
  (component/build template (query-houston data) opts))
