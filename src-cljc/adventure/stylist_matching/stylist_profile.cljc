(ns adventure.stylist-matching.stylist-profile
  "This organism is a stylist profile that includes a map and gallery."
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.platform.maps :as maps]
                 [storefront.platform.messages :refer [handle-message]]])
            [adventure.keypaths :as keypaths]
            api.orders
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.formatters :as formatters]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.header :as components.header]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            storefront.keypaths
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]
            [spice.core :as spice]
            [stylist-matching.ui.header :as header-org]
            [storefront.platform.strings :as strings]))

(defn transposed-title-molecule
  [{:transposed-title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:div.medium secondary]
   [:div.h3.black.medium primary]])

(defn stars-rating-molecule
  [{rating :rating/value}]
  (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars rating)]
    [:div.flex.items-center
     [:span.s-color.bold.mr1 rating]
     whole-stars
     partial-star
     empty-stars]))

(defn stylist-phone-molecule
  [{:phone-link/keys [target phone-number]}]
  (when (and target phone-number)
    (ui/link :link/phone
             :a.inherit-color
             {:data-test "stylist-phone"
              :class     "block mt1 flex items-center"
              :on-click  (apply utils/send-event-callback target)}
             (svg/phone {:style {:width  "7px"
                                 :height "13px"}
                         :class "mr1"}) phone-number)))

(defn circle-portrait-molecule  [{:circle-portrait/keys [portrait-url]}]
  [:div.mx2 (ui/circle-picture {:width "72px"} portrait-url)])

(defn share-icon-molecule
  [share-icon-data]
  [:div.flex.items-top.justify-center.mr2.col-1
   (ui/navigator-share share-icon-data)])

(defcomponent stylist-profile-card-component
  [query _ _]
  [:div.flex.bg-white.rounded.py3
    ;; TODO: image-url should be format/auto?
   (circle-portrait-molecule query)
   [:div.flex-grow-1.left-align.h6.line-height-2
    (transposed-title-molecule query)
    (stars-rating-molecule query)
    (stylist-phone-molecule query)]
   (share-icon-molecule query)])

(defn checks-or-x
  [specialty specialize?]
  [:div.h6.flex.items-center
   (if specialize?
     [:span.mr1 (ui/ucare-img {:width "12"} "2560cee9-9ac7-4706-ade4-2f92d127b565")]
     (svg/simple-x {:class "mr1"
                    :style {:width "12px" :height "12px"}}))
   specialty])

(def post-purchase? #{events/navigate-adventure-stylist-profile-post-purchase})

(defn query
  [data]
  (let [stylist-id     (get-in data keypaths/stylist-profile-id)
        stylist        (stylists/by-id data stylist-id)
        stylist-name   (stylists/->display-name stylist)
        current-order  (api.orders/current data)
        post-purchase? (post-purchase? (get-in data storefront.keypaths/navigation-event))
        undo-history   (get-in data storefront.keypaths/navigation-undo-stack)
        direct-load?   (zero? (count undo-history))

        main-cta-target              [(if post-purchase?
                                        events/control-adventure-select-stylist-post-purchase
                                        events/control-adventure-select-stylist-pre-purchase)
                                      {:servicing-stylist stylist
                                       :card-index        0}]
        {:keys [latitude longitude]} (:salon stylist)
        environment                  (case (get-in data storefront.keypaths/environment)
                                       "production" "mayvenn"
                                       "diva-acceptance")]
    (when stylist
      (cond-> {:header-data (cond-> {:header.title/id               "adventure-title"
                                     :header.title/primary          "Meet Your Stylist"
                                     :header.back-navigation/id     "adventure-back"
                                     :header.back-navigation/back   (first undo-history)
                                     :header.back-navigation/target [events/navigate-adventure-find-your-stylist]}
                              (not post-purchase?)
                              (merge {:header.cart/id    "mobile-cart"
                                      :header.cart/value (:order.items/quantity current-order)
                                      :header.cart/color "white"}))

               :footer-data {:footer/copy "Meet more stylists in your area"
                             :footer/id   "meet-more-stylists"
                             :cta/id      "browse-stylists"
                             :cta/label   "Browse Stylists"
                             :cta/target  [events/navigate-adventure-find-your-stylist]}

               :google-map-data #?(:cljs (maps/map-query data)
                                   :clj  nil)
               :cta/id          "select-stylist"
               :cta/target      main-cta-target
               :cta/label       (str "Select " stylist-name)

               :transposed-title/id          "stylist-name"
               :transposed-title/primary     stylist-name
               :transposed-title/secondary   (-> stylist :salon :name)
               :rating/value                 (:rating stylist)
               :phone-link/target            [events/control-adventure-stylist-phone-clicked
                                              {:stylist-id   (:stylist-id stylist)
                                               :phone-number (some-> stylist :address :phone formatters/phone-number)}]
               :phone-link/phone-number      (some-> stylist :address :phone formatters/phone-number-parens)
               :circle-portrait/portrait-url (-> stylist :portrait :resizable-url)
               :carousel/items               (let [ucare-img-urls (map :resizable-url (:gallery-images stylist))]
                                               (map-indexed (fn [j ucare-img-url]
                                                              {:key            (str "gallery-img-" stylist-id "-" j)
                                                               :ucare-img-url  ucare-img-url
                                                               :target-message [events/navigate-adventure-stylist-gallery
                                                                                {:stylist-id   stylist-id
                                                                                 :store-slug   (:store-slug stylist)
                                                                                 :query-params {:offset j}}]})
                                                            ucare-img-urls))

               :share-icon/target [events/share-stylist {:stylist-id (:stylist-id stylist)
                                                         :title      (str stylist-name " - " (get-in data (conj storefront.keypaths/store :location :city)))
                                                         :text       (str stylist-name " is a Mayvenn Certified Stylist with top-rated reviews, great professionalism, and amazing work. Check out this stylist here:")
                                                         :url        (strings/format "https://shop.%s.com/stylist/%d-%s?utm_campaign=%d&utm_term=fi_stylist_share&utm_medium=referral"
                                                                                     environment
                                                                                     stylist-id
                                                                                     (:store-slug stylist)
                                                                                     stylist-id)}]
               :share-icon/icon   (svg/share-icon {:height "19px"
                                                   :width  "18px"})

               :details [{:section-details/title   "Experience"
                          :section-details/content (string/join ", " (remove nil?
                                                                             [(when-let [stylist-since (:stylist-since stylist)]
                                                                                (ui/pluralize-with-amount
                                                                                 (- (date/year (date/now)) stylist-since)
                                                                                 "year"))
                                                                              (case (-> stylist :salon :salon-type)
                                                                                "salon"   "in-salon"
                                                                                "in-home" "in-home"
                                                                                nil)
                                                                              (when (:licensed stylist)
                                                                                "licensed")]))}
                         (when (-> stylist :service-menu :specialty-sew-in-leave-out)
                           {:section-details/title   "Specialties"
                            :section-details/content (:service-menu stylist)})]}
        (and (experiments/mayvenn-rating? data)
             (:mayvenn-rating stylist))
        (merge
         {:rating/value (:mayvenn-rating stylist)})))))

(defn carousel-molecule
  [{:carousel/keys [items]}]
  (when (seq items)
    (component/build
     carousel/component
     {:slides   (map (fn [{:keys [target-message
                                  key
                                  ucare-img-url]}]
                       [:a.px1.block
                        (merge (apply utils/route-to target-message)
                               {:key key})
                        (ui/aspect-ratio
                         1 1
                         [:img {:src   (str ucare-img-url "-/scale_crop/204x204/-/format/auto/")
                                :class "rounded col-12"}])])
                     items)
      :settings {:controls true
                 :nav      false
                 :items    3
                 ;; setting this to true causes some of our event listeners to
                 ;; get dropped by tiny-slider.
                 :loop     false}}
     {})))

(defn cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge {:data-test id} (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn section-details-molecule
  [{:section-details/keys [title content]}]
  [:div.medium.h5.py3
   {:key title}
   title
   [:div.mt1.h6.regular
    (if (string? content)
      content
      [:div.mt1.col-12.col.regular
       [:div.col-4.col
        (checks-or-x "Leave Out" (:specialty-sew-in-leave-out content))
        (checks-or-x "360" (:specialty-sew-in-360-frontal content))]
       [:div.col-4.col
        (checks-or-x "Closure" (:specialty-sew-in-closure content))
        (checks-or-x "Frontal" (:specialty-sew-in-frontal content))]])]])

(defn footer-body-molecule
  [{:footer/keys [copy id]}]
  (when id
    [:div.h5.mb2
     {:data-test id}
     copy]))

(defn footer-cta-molecule
  [{:cta/keys [id label]
    [event :as target]
    :cta/target}]
  (when id
    [:div.col-7.mx-auto
     (ui/button-medium-secondary
      (merge {:data-test id}
             (if (= :navigate (first event))
               (apply utils/route-to target)
               (apply utils/fake-href target)))
      [:span.medium.border-bottom.border-p-color label])]))

(defcomponent footer [data _ _]
  (when (seq data)
    [:div.mt6.border-top.border-cool-gray.border-width-2
     [:div.py5.center
      (footer-body-molecule data)
      (footer-cta-molecule data)]]))

(defcomponent component
  [{:keys [header-data footer-data google-map-data] :as query} owner opts]
  [:div.bg-white.mb6 {:style {:min-height    "100vh"
                              :margin-bottom "-1px"}}
   [:main
    (components.header/adventure-header (:header.back-navigation/target header-data)
                                        (:header.title/primary header-data)
                                        {:quantity (:header.cart/value header-data)})
    [:div (component/build stylist-profile-card-component query nil)]

    #?(:cljs (component/build maps/component google-map-data))

    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     [:div.mb3 (cta-molecule query)]

     (carousel-molecule query)

     (for [section-details (:details query)]
       (section-details-molecule section-details))]
    [:div.clearfix]]

   [:footer (component/build footer footer-data nil)]])

(defn built-component
  [data opts]
  (component/build component (query data) {}))

(defmethod effects/perform-effects events/navigate-adventure-stylist-profile
  [dispatch event {:keys [stylist-id]} prev-app-state app-state]
  #?@(:cljs
      [(google-maps/insert)
       (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache) stylist-id)]))

(defmethod effects/perform-effects events/share-stylist
  [_ _ {:keys [url text title stylist-id]} _]
  #?(:cljs
     (.. (js/navigator.share (clj->js {:title title
                                       :text  text
                                       :url   url}))
         (then  (fn []
                  (handle-message events/api-success-shared-stylist {:stylist-id stylist-id})))
         (catch (fn [err]
                  (handle-message events/api-failure-shared-stylist {:stylist-id stylist-id
                                                                     :error (.toString err)}))))))

(defmethod transitions/transition-state events/navigate-adventure-stylist-profile
  [_ _ {:keys [stylist-id]} app-state]
  (assoc-in app-state keypaths/stylist-profile-id (spice/parse-int stylist-id)))

(defmethod effects/perform-effects events/navigate-adventure-stylist-profile-post-purchase
  [dispatch event {:keys [stylist-id]} prev-app-state app-state]
  #?@(:cljs
      [(google-maps/insert)
       (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache) stylist-id)]))

(defmethod transitions/transition-state events/navigate-adventure-stylist-profile-post-purchase
  [_ _ {:keys [stylist-id]} app-state]
  (assoc-in app-state keypaths/stylist-profile-id (spice/parse-int stylist-id)))
