(ns stylist-profile.stylist-reviews-v2021-10
  (:require #?@(:cljs
                [[storefront.browser.scroll :as scroll]])
            adventure.keypaths
            api.stylist
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            [storefront.platform.component-utils :as utils]
            [storefront.request-keys :as request-keys]
            stylist-directory.keypaths))

;; TODO
;; * style correctly (inc stars)
;; * header

(def install-type->display-name
  {"leave-out"         "Leave Out Install"
   "closure"           "Closure Install"
   "frontal"           "Frontal Install"
   "360-frontal"       "360° Frontal Install"
   "wig-customization" "Wig Customization"})

(c/defdynamic-component review
  (constructor
   [this props]
   (c/create-ref! this (str "slide-" (-> this c/get-props :review-id)))
   {})
  (did-mount
   [this]
   #?(:cljs
      (let [element (some->> (c/get-props this)
                             :review-id
                             (str "slide-")
                             (c/get-ref this))]
        (->> (< (.-offsetHeight element) (.-scrollHeight element))
             (c/set-state! this :overflow?)))))
  (render
   [this]
   (c/html
    (let [{:keys [review-id install-type stars review-content reviewer-name review-date target]} (c/get-props this)
          {:keys [idx]}                                                                          (c/get-opts this)
          {:keys [overflow?]}                                                                    (c/get-state this)]
      [:div.border.border-cool-gray.rounded.p3.mx1.proxima
       {:key       review-id
        :data-test (str "review-" idx)
        :data-ref  (str "review-" idx)}
       [:div.mb2.proxima.content-4
        ;; User portrait will go here
        [:div.flex.justify-between.items-baseline.content-3
         [:div.bold reviewer-name]
         (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars stars "11px" {:class "fill-p-color"})]
           [:div.flex.justify-end whole-stars partial-star empty-stars])]
        [:div.flex.justify-between.items-baseline
         [:div (install-type->display-name install-type)]
         [:div.dark-gray.right-align review-date]]]
       [:div.proxima.content-3
        {:id    (str "review-" idx "-content")
         :ref   (c/use-ref this (str "slide-" review-id))
         :class (when overflow? "ellipsis-15")}
        [:span.line-height-4 review-content]]
       [:div.mt2.content-3
        {:id (str "review-" idx "-content-more")}
        (when overflow?
          [:a.flex.items-center.underline.black.bold.pointer
           {:on-click #?(:cljs #(js/setTimeout (c/set-state! this :overflow? false) 0)
                         :clj nil)}
           "Show more"
           (ui/forward-caret {:class "ml1"})])]]))))

(c/defcomponent template
  [{:reviews/keys [spinning? cta-target cta-id cta-label id review-count reviews] :as data} _ _]
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

      (c/elements review data :reviews/reviews)
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
                                 (mapv #(update % :review-date f/short-date)))}
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
  [app-state _]
  (c/build template (query app-state)))

(defmethod fx/perform-effects e/navigate-adventure-stylist-profile-reviews
  [_ _ {:keys [query-params]} _ state]
  #?(:cljs
     (when-let [offset (:offset query-params)]
       (scroll/scroll-to-selector (str "[data-ref=review-" offset "]")))))
