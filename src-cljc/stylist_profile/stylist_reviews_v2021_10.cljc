(ns stylist-profile.stylist-reviews-v2021-10
  (:require #?(:cljs
               [storefront.browser.scroll :as scroll])
            adventure.keypaths
            api.stylist
            [mayvenn.visual.tools :refer [with within]]
            [spice.core :as spice]
            [storefront.component :as c]
            [storefront.components.formatters :as f]
            [storefront.components.header :as header]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.effects :as fx]
            [storefront.events :as e]
            storefront.keypaths
            [storefront.platform.component-utils :as utils]
            stylist-directory.keypaths))

(def install-type->display-name
  {"leave-out"         "Leave Out Install"
   "closure"           "Closure Install"
   "frontal"           "Frontal Install"
   "360-frontal"       "360° Frontal Install"
   "wig-customization" "Wig Customization"})

(defn header
  [{:keys [title close-id close-back close-route]}]
  [:div
   [:div.fixed.z4.top-0.left-0.right-0
    (header/mobile-nav-header
     {:class "bg-white black"
      :style {:height "70px"}}
     (when close-id
       (c/html
        [:a.block.flex.items-center.black
         (merge {:data-test close-id}
                (apply utils/route-back-or-to close-back close-route))
         (svg/left-arrow {:width  "20"
                          :height "20"})]))
     (c/html [:div.center.content-2.proxima title])
     (c/html nil))]
   [:div {:style {:margin-top "70px"}}]])

(c/defdynamic-component review
  (constructor
   [this props]
   (c/create-ref! this (str "slide-" (-> this c/get-props :review-id)))
   {:overflow? true})
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
    (let [{:keys [review-id install-type stars review-content reviewer-name review-date target expanded?]} (c/get-props this)
          {:keys [idx]}                                                                          (c/get-opts this)
          {:keys [overflow?]}                                                                    (c/get-state this)]
      [:div.border-cool-gray.border-bottom.m4.pb2
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
       [:div.relative
        [:div.proxima.content-3
         {:ref   (c/use-ref this (str "slide-" review-id))
          :class (when (and overflow? (not expanded?)) "ellipsis-15")}
         [:span.line-height-4
          {:style {:overflow-wrap "break-word"}}
          review-content]]
        [:div.mt2.content-3.absolute.bottom-0.right-0.bg-white.pl1
         (when (and overflow? (not expanded?))
           [:a.p-color.bold.pointer
            {:on-click #?(:cljs #(js/setTimeout (c/set-state! this :overflow? false) 0)
                          :clj nil)}
            "...More"])]]]))))

(defn avg-rating-and-rating-count [rating rating-count]
  [:div.flex.items-center.mx4.pt4
   [:div.flex.items-center.mr1
    (svg/symbolic->html [:svg/whole-star {:class "fill-p-color mr1"
                                          :style {:height "0.8em"
                                                  :width  "0.9em"}}])
    [:div.proxima.title-1.p-color rating " • "]]
   [:div rating-count " Ratings"]])

(defn back-cta [{:keys [id target label]}]
  [:div.p4
   (ui/button-small-secondary
    (merge (apply utils/route-to target)
           {:data-test id})
    label)])

(c/defcomponent template
  [{:reviews/keys [id rating-count rating reviews
                   cta-id cta-label cta-target] :as data} _ _]
  (c/html
   (when id
     [:div
      (header (with :reviews.header data))
      [:div.border-top.border-cool-gray {:key id
                                         :id  "reviews"}
       (avg-rating-and-rating-count rating rating-count)
       (c/elements review data :reviews/reviews)
       (when cta-id
         [:div.p5.center
          {:data-test cta-id}
          (ui/button-medium-underline-primary
           {:on-click (apply utils/send-event-callback cta-target)} cta-label)])
       (back-cta (with :reviews.back-cta data))]])))

(defn query
  [state]
  (let [stylist-id                         (get-in state adventure.keypaths/stylist-profile-id)
        {:stylist.rating/keys [publishable? score cardinality]
         :stylist/keys        [slug name]} (api.stylist/by-id state stylist-id)
        paginated-reviews                  (get-in state stylist-directory.keypaths/paginated-reviews)
        {:keys [offset]}                   (get-in state storefront.keypaths/navigation-query-params)
        stylist-reviews                    (mapv #(update % :review-date f/short-date) (:reviews paginated-reviews))]
    (when (and publishable?
               (seq stylist-reviews))
      (merge
       (within :reviews {:id           "stylist-reviews"
                         :rating       score
                         :review-count (:count paginated-reviews)
                         :rating-count cardinality
                         :reviews      (if-let [offset (spice/parse-int offset)]
                                         (-> stylist-reviews
                                             (update-in [offset] #(assoc % :expanded? true)))
                                         stylist-reviews)})
       (when (not= (:current-page paginated-reviews)
                   (:pages paginated-reviews))
         {:reviews/cta-id     "more-stylist-reviews"
          :reviews/cta-target [e/control-fetch-stylist-reviews]
          :reviews/cta-label  "View More"})
       (within :reviews.header {:title       "Ratings"
                                :close-id    "header-back-to-profile"
                                :close-back  (first (get-in state storefront.keypaths/navigation-undo-stack))
                                :close-route [e/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                                                    :store-slug slug}]})
       (within :reviews.back-cta
               {:id     "back-to-profile"
                :target [e/navigate-adventure-stylist-profile {:stylist-id stylist-id
                                                               :store-slug slug}]
                :label  (str "Back to " name "'s profile")})))))

(defn ^:export page
  [app-state _]
  (c/build template (query app-state)))

(defmethod fx/perform-effects e/navigate-adventure-stylist-profile-reviews
  [_ _ {:keys [query-params]} _ state]
  #?(:cljs
     (when-let [offset (:offset query-params)]
       (scroll/scroll-selector-to-top (str "[data-ref=review-" offset "]") -60))))
