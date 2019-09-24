(ns adventure.stylist-matching.stylist-profile
  "This organism is a stylist profile that includes a map and gallery."
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.platform.maps :as maps]])
            [adventure.keypaths :as keypaths]
            api.orders
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]
            storefront.keypaths
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]
            [spice.core :as spice]
            [stylist-matching.ui.header :as header-org]))

(defn transposed-title-molecule
  [{:transposed-title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:div.medium secondary]
   [:div.h3.black.medium primary]])

(defn stars-rating-molecule
  [{rating :rating/value}]
  (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars rating)]
    [:div.flex.items-center
     [:span.orange.bold.mr1 rating]
     whole-stars
     partial-star
     empty-stars]))

(defn stylist-phone-molecule
  [{:phone-link/keys [target phone-number]}]
  (when (and target phone-number)
    (ui/link :link/phone
             :a.dark-gray
             {:data-test "stylist-phone"
              :class     "block mt1 flex items-center"
              :on-click  (apply utils/send-event-callback target)}
             (svg/phone {:style {:width  "7px"
                                 :height "13px"}
                         :class "mr1"}) phone-number)))

(defn circle-portrait-molecule  [{:circle-portrait/keys [portrait-url]}]
  [:div.mr2 (ui/circle-picture {:width "72px"} portrait-url)])

(defn stylist-profile-card-component
  [query _ _]
  (component/create
   [:div.flex.bg-white.px1.mxn2.rounded.py3
    ;; TODO: image-url should be format/auto?
    (circle-portrait-molecule query)
    [:div.flex-grow-1.left-align.dark-gray.h6.line-height-2
     (transposed-title-molecule query)
     (stars-rating-molecule query)
     (stylist-phone-molecule query)]]))

(defn checks-or-x
  [specialty specialize?]
  [:div.h6.flex.items-center
   (if specialize?
     [:span.mr1 (ui/ucare-img {:width "12"} "2560cee9-9ac7-4706-ade4-2f92d127b565")]
     (svg/simple-x {:class "dark-silver mr1"
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
        footer-cta-target            (if direct-load?
                                       [(if post-purchase?
                                          events/navigate-adventure-stylist-results-post-purchase
                                          events/navigate-adventure-stylist-results-pre-purchase)
                                        {:query-params {:lat latitude :long longitude}}]
                                       [events/browser-back])]
    (when stylist
      {:header-data (cond-> {:header.title/id               "adventure-title"
                             :header.title/primary          (str "More about " stylist-name)
                             :header.back-navigation/id     "adventure-back"
                             :header.back-navigation/back   undo-history
                             :header.back-navigation/target [events/navigate-adventure-find-your-stylist]}
                      (not post-purchase?)
                      (merge {:header.cart/id    "mobile-cart"
                              :header.cart/value (:order.items/quantity current-order)
                              :header.cart/color "white"}))

       :footer-data {:footer/copy "Meet more stylists in your area"
                     :footer/id   "meet-more-stylists"
                     :cta/id      "browse-stylists"
                     :cta/label   "Browse Stylists"
                     :cta/target  footer-cta-target}

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
                    :section-details/content (:service-menu stylist)})]})))

(defn carousel-molecule
  [{:carousel/keys [items]}]
  (component/build carousel/component
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
                    :settings {:swipe        true
                               :initialSlide 0
                               :arrows       true
                               :dots         false
                               :slidesToShow 3
                               :infinite     true}}
                   {}))

(defn cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (ui/teal-button
     (merge {:data-test id} (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label]) ))

(defn section-details-molecule
  [{:section-details/keys [title content]}]
  [:div.medium.h5.py3
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
        (checks-or-x "Frontal" (:specialty-sew-in-frontal content))]])]] )

(defn footer-body-molecule
  [{:footer/keys [copy id]}]
  (when id
    [:div.dark-gray.h5.mb2
     {:data-test id}
     copy]) )

(defn footer-cta-molecule
  [{:cta/keys [id label]
    [event :as target]
    :cta/target}]
  (when id
    [:div.col-7.mx-auto
     (ui/ghost-button
      (merge {:height-class "py2"
              :data-test    id}
             (if (= :navigate (first event))
               (apply utils/route-to target)
               (apply utils/fake-href target)))
      [:span.medium.dark-gray.border-bottom.border-gray label])]) )

(defn footer [data _ _]
  (component/create
   (when (seq data)
     [:div.mt6.border-top.border-fate-white.border-width-2
      [:div.py5.center
       (footer-body-molecule data)
       (footer-cta-molecule data)]])))

(defn component
  [{:keys [header-data footer-data google-map-data] :as query} owner opts]
  (component/create
   [:div.col-12.bg-white.mb6 {:style {:min-height    "100vh"
                                      :margin-bottom "-1px"}}
    [:div.white (component/build header-org/organism header-data nil)]
    [:main
     [:div.px3 (component/build stylist-profile-card-component query nil)]

     #?(:cljs (component/build maps/component google-map-data))


     [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
      [:div.mb3 (cta-molecule query)]

      (carousel-molecule query)

      (for [section-details (:details query)]
        (section-details-molecule section-details))]
     [:div.clearfix]]

    [:footer (component/build footer footer-data nil)]]))

(defn built-component
  [data opts]
  (component/build component (query data) {}))

(defmethod effects/perform-effects events/navigate-adventure-stylist-profile
  [dispatch event {:keys [stylist-id]} prev-app-state app-state]
  #?@(:cljs
      [(google-maps/insert)
       (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache) stylist-id)]))

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
