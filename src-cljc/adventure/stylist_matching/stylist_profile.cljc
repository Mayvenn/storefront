(ns adventure.stylist-matching.stylist-profile
  "This organism is a stylist profile that includes a map and gallery."
  (:require #?@(:cljs
                [[storefront.api :as api]
                 [storefront.hooks.facebook-analytics :as facebook-analytics]
                 [storefront.hooks.google-maps :as google-maps]
                 [storefront.platform.messages :refer [handle-message]]])
            [adventure.keypaths :as keypaths]
            [adventure.stylist-matching.maps :as maps]
            api.orders
            [clojure.string :as string]
            [spice.date :as date]
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
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]
            [stylist-directory.stylists :as stylists]
            stylist-directory.keypaths
            [spice.core :as spice]
            [storefront.request-keys :as request-keys]
            [storefront.accessors.experiments :as experiments]))

(defn transposed-title-molecule
  [{:transposed-title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:div.content-2.proxima secondary]
   [:div.title-2.proxima.shout primary]])

;; fork of molecules/stars-rating-molecule
(defn stars-rating-molecule
  [{:star-rating/keys [id value rating-content scroll-anchor]}]
  (when id
    (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars value "13px")]
     [:div.flex.items-center.mtn1
      {:data-test id}
      whole-stars
      partial-star
      empty-stars
      (when rating-content
        (if scroll-anchor
          (ui/button-small-underline-secondary
           (merge {:class "mx1 shout"}
                  (utils/scroll-href scroll-anchor))
           rating-content)
          [:div.s-color.proxima.title-3.ml1 rating-content]))])))

(defn stylist-phone-molecule
  [{:phone-link/keys [target phone-number]}]
  (when (and target phone-number)
    (ui/link :link/phone
             :a.inherit-color.proxima.content-2
             {:data-test "stylist-phone"
              :class     "block mt1 flex items-center"
              :on-click  (apply utils/send-event-callback target)}
             (svg/phone {:style {:width  "15px"
                                 :height "15px"}
                         :class "mr1"}) phone-number)))

(defn circle-portrait-molecule  [{:circle-portrait/keys [portrait]}]
  [:div.mx2 (ui/circle-picture {:width "72px"} (ui/square-image portrait 72))])

(defn share-icon-molecule
  [share-icon-data]
  [:div.flex.items-top.justify-center.mr2.col-1
   (ui/navigator-share share-icon-data)])

(defn stylist-just-added-molecule
  [{:stylist.just-added/keys [content id]}]
  (when id
    [:div.pb1.flex
     [:div.content-3.proxima.bold.items-center.flex.border.border-dark-gray.px2
      {:data-test id}
      [:img {:src "https://ucarecdn.com/b0f70f0a-51bf-4369-b6b8-80480b54b6f1/-/format/auto/" :alt "" :width 9 :height 14}]
      [:div.pl1.shout.dark-gray.letter-spacing-1 content]]]))

(defcomponent stylist-profile-card-component
  [query _ _]
  [:div.flex.bg-white.rounded.p2
   (circle-portrait-molecule query)
   [:div.left-align.ml2
    (transposed-title-molecule query)
    (stars-rating-molecule query)
    (stylist-just-added-molecule query)
    (stylist-phone-molecule query)]
   (share-icon-molecule query)])

(defn checks-or-x
  [specialty specialize?]
  [:div.h6.flex.items-center.content-2
   (if specialize?
     (svg/check-mark {:class "black mr2"
                      :style {:width  12
                              :height 12}})
     (svg/x-sharp {:class "black mr2"
                   :style {:width  12
                           :height 12}}))
   specialty])

(defn header<-
  [{:order.items/keys [quantity]} undo-history]
  {:header.title/id               "adventure-title"
   :header.title/primary          "Meet Your Stylist"
   :header.back-navigation/id     "adventure-back"
   :header.back-navigation/back   (not-empty (first undo-history))
   :header.back-navigation/target [events/navigate-adventure-find-your-stylist]
   :header.cart/id                "mobile-cart"
   :header.cart/value             quantity
   :header.cart/color             "white"})

(def footer<-
  {:footer/copy "Meet more stylists in your area"
   :footer/id   "meet-more-stylists"
   :cta/id      "browse-stylists"
   :cta/label   "Browse Stylists"
   :cta/target  [events/navigate-adventure-find-your-stylist]})

(defn cta<-
  [stylist]
  {:cta/id     "select-stylist"
   :cta/target [events/control-adventure-select-stylist-pre-purchase
                {:servicing-stylist stylist
                 :card-index        0}]
   :cta/label  (str "Select " (stylists/->display-name stylist))})

(defn carousel-molecule
  [{:carousel/keys [items]}]
  (when (seq items)
    (component/build
     carousel/component
     {:data     items
      :settings {:controls true
                 :nav      false
                 :items    3
                 ;; setting this to true causes some of our event listeners to
                 ;; get dropped by tiny-slider.
                 :loop     false}}
     {:opts {:mode   :multi
             :slides (map (fn [{:keys [target-message
                                       key
                                       ucare-img-url]}]
                            (component/html
                             [:a.px1.block
                              (merge (apply utils/route-to target-message)
                                     {:key key})
                              (ui/aspect-ratio
                               1 1
                               [:img {:src   (str ucare-img-url "-/scale_crop/204x204/-/format/auto/")
                                      :class "col-12"}])]))
                          items)}})))

(defn cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge {:data-test id} (apply utils/fake-href target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn section-details-molecule
  [{:section-details/keys [title content]}]
  [:div.my3 {:key title}
   [:div.title-3.proxima.shout title]
   [:div.content-2 content]])

(def install-type->display-name
  {"leave-out"         "Leave Out Install"
   "closure"           "Closure Install"
   "frontal"           "Frontal Install"
   "360-frontal"       "360° Frontal Install"
   "wig-customization" "Wig Customization"})

(defn stars-rating-large-molecule
  [{:rating/keys [value rating-count]}]
  (component/html
   (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars value "20px")]
     (when rating-count
       [:div.flex.justify-center
        whole-stars
        partial-star
        empty-stars
        [:div.pl1
         (str "(" rating-count ")")]]))))

(defn rating-bar-molecule [star-rating star-count max-ratings-count]
  (component/html             ;; NOTE: cast to double to handle server side render
   (let [green-bar-percentage (double (/ star-count max-ratings-count))]
     [:div.flex.px8.items-center
      [:div.flex.items-baseline
       [:div.bold.proxima.title-3 (name star-rating)]
       [:div.px2 (svg/whole-star {:class  "fill-gray"
                                  :height "13px"
                                  :width  "13px"})]]
      [:div.flex-grow-1.flex
       [:div.bg-s-color {:style {:width (str (* 100 green-bar-percentage) "%") :height "12px"}} ui/nbsp]
       [:div.bg-white {:style {:width (str (* 100 (- 1 green-bar-percentage)) "%") :height "12px"}} ui/nbsp]]
      [:div.proxima.content-3.pl2
       {:style {:width "20px"}}(str "(" star-count ")")]])))

(defn ratings-bar-chart-molecule
  [{:ratings-bar-chart/keys [id rating-star-counts] :as data}]
  (component/html
   (when id
     (let [max-ratings-count   (apply max (vals rating-star-counts))
           sorted-rating-count (->> rating-star-counts
                                    (sort-by first)
                                    reverse)]
       [:div.bg-cool-gray.flex-column.center.py5.mt3
        {:id id}
        [:div.shout.bold.proxima.title-3 "Ratings"]
        (for [[star-rating star-count] sorted-rating-count]
          (rating-bar-molecule star-rating star-count max-ratings-count))]))))

(defn reviews-molecule
  [{:reviews/keys [spinning? cta-target cta-id cta-label id review-count reviews]}]
  (component/html
   (when id
     [:div.mx3.my6
      {:key id
       :id "reviews"}
      [:div.flex.justify-between
       [:div.flex.items-center
        [:div.h6.title-3.proxima.shout "REVIEWS"]
        [:div.content-3.proxima.ml1
         (str "(" review-count ")")]]]

      (for [{:keys [review-id stars install-type review-content reviewer-name review-date]} reviews]
        [:div.py2.border-bottom.border-cool-gray
         {:key review-id}
         [:div
          (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars stars "13px")]
            [:div.flex
             whole-stars
             partial-star
             empty-stars
             [:div.ml2.content-3.proxima (get install-type->display-name install-type)]])]
         [:div.py1 review-content]
         [:div.flex
          [:div "— " reviewer-name]
          [:div.ml1.dark-gray review-date]]])
      (when cta-id
        [:div.p5.center
         {:data-test cta-id}
         (if spinning?
           ui/spinner
           (ui/button-medium-underline-primary
            {:on-click (apply utils/send-event-callback cta-target)} cta-label))])])))

(defmethod effects/perform-effects events/control-fetch-stylist-reviews
  [dispatch event args prev-app-state app-state]
  #?(:cljs
     (api/fetch-stylist-reviews (get-in app-state storefront.keypaths/api-cache)
                                {:stylist-id (get-in app-state keypaths/stylist-profile-id)
                                 :page       (-> (get-in app-state stylist-directory.keypaths/paginated-reviews)
                                                 :current-page
                                                 (or 0)
                                                 inc)})))

(defn footer-body-molecule
  [{:footer/keys [copy id]}]
  (component/html
   (when id
     [:div.h5.mb2
      {:data-test id}
      copy])))

(defn footer-cta-molecule
  [{:cta/keys [id label]
    [event :as target]
    :cta/target}]
  (component/html
   (when id
     [:div.col-5.mx-auto
      (ui/button-small-secondary
       (merge {:data-test id}
              (if (= :navigate (first event))
                (apply utils/route-to target)
                (apply utils/fake-href target)))
       label)])))

(defcomponent footer-organism [data _ _]
  (when (seq data)
    [:div.mt6.border-top.border-cool-gray.border-width-2
     [:div.py5.center
      (footer-body-molecule data)
      (footer-cta-molecule data)]]))

(defcomponent component
  [{:keys [header footer cta carousel reviews stylist-profile-card google-maps
           ratings-bar-chart section-details] :as query} owner opts]
  [:div.bg-white.col-12.mb6.stretch {:style {:margin-bottom "-1px"}}
   [:main
    (components.header/adventure-header header)
    [:div (component/build stylist-profile-card-component stylist-profile-card nil)]

    (component/build maps/component google-maps)

    [:div.my2.m1-on-tb-dt.mb2-on-tb-dt.px3
     [:div.mb3 (cta-molecule cta)]

     (carousel-molecule carousel)

     (for [section-detail section-details]
       (section-details-molecule section-detail))]
    [:div.clearfix]
    (ratings-bar-chart-molecule ratings-bar-chart)

    (reviews-molecule reviews)]

   [:footer (component/build footer-organism footer nil)]])

(defn carousel<-
  [{:keys [store-slug stylist-id gallery-images]}]
  {:carousel/items
   (map-indexed
    (fn [j {:keys [resizable-url]}]
      {:key            (str "gallery-img-" stylist-id "-" j)
       :ucare-img-url  resizable-url
       :target-message [events/navigate-adventure-stylist-gallery
                        {:stylist-id   stylist-id
                         :store-slug   store-slug
                         :query-params {:offset j}}]})
    gallery-images)})

(defn reviews<-
  [fetching-reviews?
   {mayvenn-rating-publishable? :mayvenn-rating-publishable
    stylist-rating              :rating
    review-count                :review-count}
   {stylist-reviews :reviews
    :as             paginated-reviews}]
  (when (and mayvenn-rating-publishable?
             (seq stylist-reviews))
    (merge
     {:reviews/id           "stylist-reviews"
      :reviews/spinning?    fetching-reviews?
      :reviews/rating       stylist-rating
      :reviews/review-count review-count
      :reviews/reviews      (mapv #(assoc % :review-date
                                          #?(:cljs (-> % :review-date formatters/short-date)
                                             :clj  "")) ;; TODO no
                                  stylist-reviews)}
     (when (not= (:current-page paginated-reviews)
                 (:pages paginated-reviews))
       {:reviews/cta-id       "more-stylist-reviews"
        :reviews/cta-target   [events/control-fetch-stylist-reviews]
        :reviews/cta-label    "View More"}))))

(defn stylist-profile-card<-
  [host-name
   hide-star-distribution?
   newly-added-stylist-ui-experiment?
   {mayvenn-rating-publishable? :mayvenn-rating-publishable
    stylist-rating              :rating
    :keys                       [stylist-id store-slug rating-star-counts salon]
    :as                         stylist}]
  (let [rating-count                 (reduce + 0 (vals rating-star-counts))
        show-star-bar-chart?         (and (> rating-count 0)
                                          mayvenn-rating-publishable?
                                          (not hide-star-distribution?))
        stylist-name                 (stylists/->display-name stylist)
        show-newly-added-stylist-ui? (and (< rating-count 3)
                                          newly-added-stylist-ui-experiment?)]
    {:star-rating/value          (spice/parse-double stylist-rating)
     :star-rating/scroll-anchor  (when show-star-bar-chart?
                                   "star-distribution-table")
     :star-rating/rating-content (str "(" stylist-rating ")")

     :star-rating/id             (when-not show-newly-added-stylist-ui?
                                   (str "rating-count-" store-slug))
     :stylist.just-added/id      (when show-newly-added-stylist-ui?
                                   (str "just-added-" store-slug))
     :stylist.just-added/content "Just Added"

     :transposed-title/id        "stylist-name"
     :transposed-title/primary   stylist-name
     :transposed-title/secondary (:name salon)
     :phone-link/target          [events/control-adventure-stylist-phone-clicked
                                  {:stylist-id   stylist-id
                                   :phone-number (some-> stylist :address :phone formatters/phone-number)}]
     :phone-link/phone-number    (some-> stylist :address :phone formatters/phone-number-parens)
     :circle-portrait/portrait   (-> stylist :portrait)
     :share-icon/target          [events/share-stylist
                                  {:stylist-id stylist-id
                                   :title      (str stylist-name " - " (:city salon))
                                   :text       (str stylist-name " is a Mayvenn Certified Stylist with top-rated reviews, great professionalism, and amazing work. Check out this stylist here:")
                                   :url        (str "https://" host-name ".com/stylist/" stylist-id "-" store-slug
                                                    "?utm_campaign=" stylist-id "&utm_term=fi_stylist_share&utm_medium=referral")}]
     :share-icon/icon            (svg/share-icon {:height "19px"
                                                  :width  "18px"})}))

(defn ratings-bar-chart<-
  [hide-star-distribution?
   {mayvenn-rating-publishable? :mayvenn-rating-publishable
    :keys                       [rating-star-counts]}]
  (let [rating-count           (reduce + 0 (vals rating-star-counts))
        show-rating-bar-chart? (and (pos? rating-count)
                                    mayvenn-rating-publishable?
                                    (not hide-star-distribution?))]
    {:ratings-bar-chart/id                 (when show-rating-bar-chart?
                                             "star-distribution-table")
     :ratings-bar-chart/rating-star-counts rating-star-counts}))

(defn details<-
  [hide-bookings?
   {:keys [service-menu rating-star-counts] :as stylist}]
  (let [rating-count (reduce + 0 (vals rating-star-counts))]
    [{:section-details/title   "Experience"
      :section-details/content ^:ignore-interpret-warning
      [:div
       [:div (string/join ", "
                          (remove nil?
                                  [(when-let [stylist-since (:stylist-since stylist)]
                                     (ui/pluralize-with-amount
                                      (- (date/year (date/now)) stylist-since)
                                      "year"))
                                   (case (-> stylist :salon :salon-type)
                                     "salon"   "in-salon"
                                     "in-home" "in-home"
                                     nil)
                                   (when (:licensed stylist)
                                     "licensed")]))]
       (when (and (not hide-bookings?)
                  (> rating-count 0))
         [:div (str "Booked " (ui/pluralize-with-amount rating-count "time") " with Mayvenn")])]}
     {:section-details/title "Specialties"
      :section-details/content
      ^:ignore-interpret-warning
      [:div.mt1.col-12.col
       (for [s (filter (comp true? second)
                       [["Leave Out Install"         (:specialty-sew-in-leave-out service-menu)]
                        ["Closure Install"           (:specialty-sew-in-closure service-menu)]
                        ["360 Frontal Install"       (:specialty-sew-in-360-frontal service-menu)]
                        ["Frontal Install"           (:specialty-sew-in-frontal service-menu)]
                        ["Wig Customization"         (:specialty-wig-customization service-menu)]
                        ["Natural Hair Trim"         (:specialty-addon-natural-hair-trim service-menu)]
                        ["Weave Take Down"           (:specialty-addon-weave-take-down service-menu)]
                        ["Hair Deep Conditioning"    (:specialty-addon-hair-deep-conditioning service-menu)]
                        ["Closure Customization"     (:specialty-addon-closure-customization service-menu)]
                        ["Frontal Customization"     (:specialty-addon-frontal-customization service-menu)]
                        ["360 Frontal Customization" (:specialty-addon-360-frontal-customization service-menu)]
                        ["Custom U-Part Wig"         (:specialty-custom-unit-leave-out service-menu)]
                        ["Custom Lace Closure Wig"   (:specialty-custom-unit-closure service-menu)]
                        ["Custom Lace Front Wig"     (:specialty-custom-unit-frontal service-menu)]
                        ["Custom 360 Lace Wig"       (:specialty-custom-unit-360-frontal service-menu)]
                        ["Wig Install"               (:specialty-wig-install service-menu)]
                        ["Silk Press"                (:specialty-silk-press service-menu)]
                        ["Weave Maintenance"         (:specialty-weave-maintenance service-menu)]
                        ["Wig Maintenance"           (:specialty-wig-maintenance service-menu)]
                        ["Braid Down"                (:specialty-braid-down service-menu)]
                        ["Leave Out Reinstall"       (:specialty-reinstall-leave-out service-menu)]
                        ["Closure Reinstall"         (:specialty-reinstall-closure service-menu)]
                        ["Frontal Reinstall"         (:specialty-reinstall-frontal service-menu)]
                        ["360 Frontal Reinstall"     (:specialty-reinstall-360-frontal service-menu)]])]
         [:div.col-6.col (apply checks-or-x s)])]}]))

(defn built-component
  [data _]
  (let [;; data layer - session
        {host-name :host}                  (get-in data storefront.keypaths/navigation-uri)
        undo-history                       (get-in data storefront.keypaths/navigation-undo-stack)
        stylist-id                         (get-in data keypaths/stylist-profile-id)
        fetching-reviews?                  (utils/requesting? data request-keys/fetch-stylist-reviews)
        hide-bookings?                     (experiments/hide-bookings? data)
        hide-star-distribution?            (experiments/hide-star-distribution? data)
        newly-added-stylist-ui-experiment? (and (experiments/stylist-results-test? data)
                                                (or (experiments/just-added-only? data)
                                                    (experiments/just-added-experience? data)))

        ;; business layers
        current-order     (api.orders/current data)
        stylist           (stylists/by-id data stylist-id)
        paginated-reviews (get-in data stylist-directory.keypaths/paginated-reviews)]
    (component/build component (merge {:header (header<- current-order undo-history)
                                       :footer footer<-}
                                      (when stylist
                                        {:cta                  (cta<- stylist)
                                         :carousel             (carousel<- stylist)
                                         :reviews              (reviews<- fetching-reviews? stylist paginated-reviews)
                                         :stylist-profile-card (stylist-profile-card<-
                                                                host-name
                                                                hide-star-distribution?
                                                                newly-added-stylist-ui-experiment?
                                                                stylist)
                                         :ratings-bar-chart    (ratings-bar-chart<- hide-star-distribution?
                                                                                    stylist)
                                         :section-details      (details<- hide-bookings? stylist)
                                         :google-maps          (maps/map-query data)})))))

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
  (-> app-state
      (assoc-in keypaths/stylist-profile-id (spice/parse-int stylist-id))
      (assoc-in stylist-directory.keypaths/paginated-reviews nil)))

(defmethod effects/perform-effects events/navigate-adventure-stylist-profile
  [dispatch event {:keys [stylist-id]} prev-app-state app-state]
  #?(:cljs
     (let [stylist-id (spice/parse-int stylist-id)]
       (google-maps/insert)
       (api/fetch-stylist-details (get-in app-state storefront.keypaths/api-cache) stylist-id)
       (api/fetch-stylist-reviews (get-in app-state storefront.keypaths/api-cache) {:stylist-id stylist-id
                                                                                    :page       1}))))
(defmethod trackings/perform-track events/navigate-adventure-stylist-profile
  [_ event {:keys [stylist-id]} app-state]
  #?(:cljs
     (facebook-analytics/track-event "ViewContent" {:content_type "stylist"
                                                    :content_ids [(spice/parse-int stylist-id)]})))
