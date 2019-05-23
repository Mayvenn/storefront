(ns storefront.components.ugc
  (:require  [storefront.platform.component-utils :as util]
             [storefront.components.ui :as ui]
             [storefront.components.svg :as svg]
             [storefront.component :as component]
             [storefront.routes :as routes]
             [storefront.events :as events]
             [storefront.keypaths :as keypaths]
             adventure.albums
             [lambdaisland.uri :as uri]
             #?@(:cljs [[goog.string]])))

(defn adventure-social-image-card-component
  [{:keys                [id image-url overlay description social-service icon-url title]
    [nav-event nav-args] :cta/navigation-message
    button-type          :cta/button-type}
   owner
   {:keys [copy]}]
  (let [cta-button-fn (case button-type
                        :teal-button      ui/teal-button
                        :underline-button ui/underline-button)]
    (component/create
     [:div.p2.col-12.col-6-on-tb.left-align
      (merge
       {:key       (str "small-" id)
        :data-test "adventure-look"}
       (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                          :short-name (:short-name copy)}))
      [:div.border.border-light-gray
       [:div.relative
        (ui/aspect-ratio
         1 1
         {}
         [:img.col-12.block {:src image-url}])
        (when overlay
          [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
           [:div {:style {:width       "0"
                          :height      "0"
                          :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                          :border-left "21px solid transparent"}}]
           [:div.flex.items-center.px3.medium.h6.bg-transparent-light-teal
            overlay]])]
       [:div.bg-light-gray.p1.px2.pb2
        [:div.h5.medium.mt1.mb2.black
         [:div.flex.items-center.justify-between.mb2
          [:div.flex.items-center
           (when icon-url
             [:img.mr2.lit.rounded-0
              {:height "30px"
               :width  "50px"
               :src    icon-url}])
           title]
          [:div.m1.self-end {:style {:width "30px" :height "30px"}}
           (svg/social-icon social-service)]]
         description
         [:div.mt2 (cta-button-fn nil "Shop Look")]]]]])))


(defn social-image-card-component
  [{:keys                [desktop-aware?
                          id image-url overlay description social-service icon-url title]
    [nav-event nav-args] :cta/navigation-message
    button-type          :cta/button-type}
   owner
   {:keys [copy]}]
  (let [cta-button-fn (case button-type
                        :teal-button      ui/teal-button
                        :underline-button ui/underline-button)]
    (component/create
     [:div.p2.col-12
      (merge {:key (str "small-" id)}
             (when desktop-aware?
               {:class "col-6-on-tb col-4-on-dt"}))
      [:div.relative
       (ui/aspect-ratio
        1 1
        {}
        [:img.col-12.block {:src image-url}])
       (when overlay
         [:div.absolute.flex.justify-end.bottom-0.right-0.mb8
          [:div {:style {:width       "0"
                         :height      "0"
                         :border-top  "28px solid rgba(159, 229, 213, 0.8)"
                         :border-left "21px solid transparent"}}]
          [:div.flex.items-center.px3.medium.h6.bg-transparent-light-teal
           overlay]])]
      [:div.bg-light-gray.p1.px2.pb2
       [:div.h5.medium.mt1.mb2
        [:div.flex.items-center.justify-between.mb2
         [:div.flex.items-center
          (when icon-url
            [:img.mr2.lit.rounded-0
             {:height "30px"
              :width  "50px"
              :src    icon-url}])
          title]
         [:div.m1.self-end {:style {:width "20px" :height "20px"}}
          (svg/social-icon social-service)]]
        description]
       (when nav-event
         (cta-button-fn
          (merge
           (util/route-to nav-event nav-args {:back-copy  (:back-copy copy)
                                              :short-name (:short-name copy)})
           {:data-test (str "look-" id)})
          [:span.bold (:button-copy copy)]))]])))

(def color-name->color-slug
  {"Natural Black"                           "black"
   "Vibrant Burgundy"                        "vibrant-burgundy"
   "Blonde (#613)"                           "blonde"
   "#1 Jet Black"                            "#1-jet-black"
   "#2 Chocolate Brown"                      "#2-chocolate-brown"
   "#4 Caramel Brown"                        "#4-caramel-brown"
   "Dark Blonde (#27)"                       "dark-blonde"
   "Dark Blonde (#27) with Dark Roots (#1B)" "dark-blonde-dark-roots"
   "Blonde (#613) with Dark Roots (#1B)"     "blonde-dark-roots"})

(defn- product-link [string-uri]
  (-> string-uri
      uri/uri
      :path
      routes/navigation-message-for))

(defn contentful-shared-cart-id [look]
  (-> look
      :shared-cart-url
      product-link
      second
      :shared-cart-id))

(defn contentful-look->social-card
  ([nav-event album-keyword look]
   (contentful-look->social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [photo-url
            color
            :content/id
            texture
            description
            social-media-platform
            shared-cart-url]}]
   (let [color-detail                  (get color-details (color-name->color-slug color))
         [nav-event _ :as nav-message] (product-link shared-cart-url)
         links
         (merge
          {:view-other nav-message}
          (cond
            (and (= nav-event events/navigate-shared-cart)
                 (adventure.albums/by-keyword album-keyword))
            {:view-look [events/navigate-adventure-look-detail
                         {:album-keyword album-keyword
                          :look-id       id}]}

            (= nav-event events/navigate-shared-cart)
            {:view-look [events/navigate-shop-by-look-details
                         {:album-keyword (or (#{:deals} album-keyword) :look)
                          :look-id       id}]}

            :else nil))]
     {:id                     id
      :links                  links
      :image-url              photo-url
      :overlay                texture
      :description            description
      :desktop-aware?         true
      :social-service         social-media-platform
      :cta/button-type        :underline-button
      :cta/navigation-message (or (:view-look links)
                                  (:view-other links))
      :icon-url               (:option/rectangle-swatch color-detail)
      :title                  (or (:option/name color-detail)
                                  "Check this out!")})) )

(defn selected-look [data]
  (get-in data
          (conj keypaths/cms-ugc-collection-all-looks
                (keyword (get-in data keypaths/selected-look-id)))))

(defn contentful-look->look-detail-social-card
  ([nav-event album-keyword look]
   (contentful-look->look-detail-social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [title
            social-media-handle]
     :as   contentful-look}]
   (let [base (contentful-look->social-card nav-event album-keyword color-details contentful-look)]
     (merge
      base
      {:title                  social-media-handle
       :description            title
       :cta/button-type        :teal-button
       :cta/navigation-message (-> base :links :view-look)}))))

(defn contentful-look->pdp-social-card
  ([nav-event album-keyword look]
   (contentful-look->pdp-social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [social-media-handle]
     :as   contentful-look}]
   (let [base (contentful-look->social-card nav-event album-keyword color-details contentful-look)]
     (merge
      base
      {:title                  social-media-handle
       :cta/button-type        :teal-button
       :cta/navigation-message (-> base :links :view-look)}))))

(defn pixlee-look->social-card
  ([look]
   (pixlee-look->social-card {} look))
  ([color-details
    {:keys [look-attributes
            social-service
            id
            imgs
            links]}]
   (let [color-detail (get color-details (:color look-attributes))]
     {:id                     id
      :image-url              (or (-> imgs :medium :src)
                                  (:medium imgs))
      :overlay                (:texture look-attributes)
      :description            (:lengths look-attributes)
      :desktop-aware?         true
      :social-service         social-service
      :cta/button-type        :underline-button
      :cta/navigation-message (or (:view-look links)
                                  (:view-other links))
      :icon-url               (:option/rectangle-swatch color-detail)
      :title                  (or (:option/name color-detail)
                                  "Check this out!")})))

(defn decode-title [title]
  #?(:cljs
     (try
       ;; Sometimes Pixlee gives us URL encoded titles
       (js/decodeURIComponent title)
       (catch :default e
         title))
     :clj title))

(defn pixlee-look->look-detail-social-card
  ([look]
   (pixlee-look->look-detail-social-card {} look))
  ([color-details
    {:keys [links
            title
            user-handle]
     :as   pixlee-look}]
   (merge
    (pixlee-look->social-card color-details pixlee-look)
    {:title                  (str "@" user-handle)
     :description            (decode-title title)
     :cta/button-type        :teal-button
     :cta/navigation-message (:view-look links)})))

(defn pixlee-look->pdp-social-card
  ([look]
   (pixlee-look->pdp-social-card {} look))
  ([color-details
    {:keys [links
            user-handle]
     :as   pixlee-look}]
   (merge
    (pixlee-look->social-card color-details pixlee-look)
    {:title                  (str "@" user-handle)
     :cta/button-type        :teal-button
     :cta/navigation-message (:view-look links)})))
