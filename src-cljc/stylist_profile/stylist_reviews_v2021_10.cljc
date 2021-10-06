(ns stylist-profile.stylist-reviews-v2021-10
  (:require adventure.keypaths
            api.stylist
            [stylist-profile.ui-v2021-10.stylist-reviews-cards :as stylist-reviews-cards-v2]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.events :as e]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            stylist-directory.keypaths))

(def install-type->display-name
  {"leave-out"         "Leave Out Install"
   "closure"           "Closure Install"
   "frontal"           "Frontal Install"
   "360-frontal"       "360° Frontal Install"
   "wig-customization" "Wig Customization"})

(c/defcomponent template
  [{:reviews/keys [spinning? cta-target cta-id cta-label id review-count reviews]} _ _]
  (c/html
   (when id
     [:div.mx3.my6
      {:key id
       :id  "reviews"}
      [:div.flex.justify-between
       [:div.flex.items-center
        [:div.h6.title-3.proxima.shout "REVIEWS"]
        [:div.content-3.proxima.ml1
         (str "(" review-count ")")]]]

      (map-indexed stylist-reviews-cards-v2/review-card reviews)
      (when cta-id
        [:div.p5.center
         {:data-test cta-id}
         (if spinning?
           ui/spinner
           (ui/button-medium-underline-primary
            {:on-click (apply utils/send-event-callback cta-target)} cta-label))])])))

(defn ^:private reviews<-
  [fetching-reviews?
   {:stylist.rating/keys [publishable? score] diva-stylist :diva/stylist}
   {stylist-reviews :reviews :as paginated-reviews}]
  (when (and publishable?
             (seq stylist-reviews))
    (merge
     {:reviews/id           "stylist-reviews"
      :reviews/spinning?    fetching-reviews?
      :reviews/rating       score
      :reviews/review-count (:review-count diva-stylist)
      :reviews/reviews      (->> stylist-reviews
                                 (mapv #(update %
                                                :review-date
                                                f/short-date)))}
     (when (not= (:current-page paginated-reviews)
                 (:pages paginated-reviews))
       {:reviews/cta-id     "more-stylist-reviews"
        :reviews/cta-target [e/control-fetch-stylist-reviews]
        :reviews/cta-label  "View More"}))))

(defn query
  [state]
  (let [stylist-id        (get-in state adventure.keypaths/stylist-profile-id)
        detailed-stylist  (api.stylist/by-id state stylist-id)
        paginated-reviews (get-in state stylist-directory.keypaths/paginated-reviews)
        fetching-reviews? (utils/requesting? state request-keys/fetch-stylist-reviews)]
    (reviews<- fetching-reviews?
               detailed-stylist
               paginated-reviews)))

(defn ^:export page
  [app-state]
  (c/build template (query app-state)))
