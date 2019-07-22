(ns storefront.accessors.contentful
  (:require adventure.albums
            [lambdaisland.uri :as uri]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.routes :as routes]))

(def ^:private color-name->color-slug
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

(defn selected-look [data]
  (get-in data
          (conj keypaths/cms-ugc-collection-all-looks
                (keyword (get-in data keypaths/selected-look-id)))))

(defn shared-cart-id [look]
  (-> look
      :shared-cart-url
      product-link
      second
      :shared-cart-id))

(defn look->social-card
  ([current-nav-event album-keyword look]
   (look->social-card current-nav-event album-keyword {} look))
  ([current-nav-event
    album-keyword
    color-details
    {:keys [photo-url
            color
            :content/id
            texture
            description
            social-media-platform]}]
   (let [color-detail     (get color-details (color-name->color-slug color))
         on-adventure?    (routes/sub-page? [current-nav-event] [events/navigate-adventure])
         adventure-album? (boolean (adventure.albums/by-keyword album-keyword))
         cta-nav-message
         (cond
           (and on-adventure? adventure-album?)
           [events/navigate-adventure-look-detail {:album-keyword album-keyword
                                                   :look-id       id}]

           (and on-adventure? (not adventure-album?))
           nil

           :else
           [events/navigate-shop-by-look-details {:album-keyword (or (#{:deals} album-keyword) :look)
                                                  :look-id       id}])]
     (merge
      {:id             id
       :image-url      photo-url
       :overlay        texture
       :description    description
       :desktop-aware? true
       :social-service social-media-platform
       :icon-url       (:option/rectangle-swatch color-detail)
       :title          (or (:option/name color-detail)
                           "Check this out!")}
      (when cta-nav-message
        {:cta/button-type        :underline-button
         :cta/navigation-message cta-nav-message})))) )

(defn look->look-detail-social-card
  ([nav-event album-keyword look]
   (look->look-detail-social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [social-media-handle
            social-media-post]
     :as   look}]
   (let [base (look->social-card nav-event album-keyword color-details look)]
     (merge
      base
      {:title                  social-media-handle
       :description            social-media-post
       :cta/button-type        :teal-button
       :cta/navigation-message (-> base :cta/navigation-message)}))))

(defn look->pdp-social-card
  ([nav-event album-keyword look]
   (look->pdp-social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [social-media-handle]
     :as   look}]
   (let [base (look->social-card nav-event album-keyword color-details look)]
     (merge
      base
      {:title                  social-media-handle
       :cta/button-type        :teal-button
       :cta/navigation-message (-> base :cta/navigation-message)}))))

(defn look->homepage-social-card
  ([nav-event album-keyword look]
   (look->homepage-social-card nav-event album-keyword {} look))
  ([nav-event
    album-keyword
    color-details
    {:keys [description]
     :as   look}]
   (let [base (look->social-card nav-event album-keyword color-details look)]
     (merge
      base
      {:description            description
       :cta/button-type        :teal-button
       :cta/navigation-message (-> base :cta/navigation-message)}))))

(defn album-kw->homepage-social-cards
  [ugc-collections nav-event album-kw]
  (mapv (partial look->homepage-social-card nav-event album-kw)
        (->> ugc-collections album-kw :looks)))
