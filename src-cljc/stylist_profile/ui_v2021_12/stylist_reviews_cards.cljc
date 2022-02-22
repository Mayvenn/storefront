(ns stylist-profile.ui-v2021-12.stylist-reviews-cards
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.carousel :as carousel]
            [storefront.platform.component-utils :as utils]))

;; TODO(corey) molecularize
;; TODO(corey) reviews seem to be stuck in the past wrt to services, ask product
;; if there are new 'types' here

(def install-type->display-name
  {"leave-out"         "Leave Out Install"
   "closure"           "Closure Install"
   "frontal"           "Frontal Install"
   "360-frontal"       "360° Frontal Install"
   "wig-customization" "Wig Customization"})

(defn avg-rating-and-rating-count [rating rating-count]
  [:div.flex.items-center.mx3.my2
   [:div.flex.items-center.mr1
    (svg/symbolic->html [:svg/whole-star {:class "fill-p-color mr1"
                                          :style {:height "0.8em"
                                                  :width  "0.9em"}}])
    [:div.proxima.title-1.p-color rating " • "]]
   [:div rating-count " Ratings"]])

(c/defdynamic-component review-card
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
          {:keys [overflow?]}                                                                    (c/get-state this)
          {:keys [idx]}                                                                          (c/get-opts this)]
      [:div.border.border-cool-gray.rounded.p3.mx1.proxima
       {:key       review-id
        :data-test (str "review-" idx)}
       [:div.mb2.col-11-on-dt.mx-auto.proxima.content-4
        ;; User portrait will go here
        [:div.flex.justify-between.items-baseline
         [:div.content-3.bold.proxima reviewer-name]
         [:div.dark-dark-gray.right-align review-date]]
        [:div.flex.justify-between
         (install-type->display-name install-type)
         (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars stars "11px" {:class "fill-p-color"})]
           [:div.flex.justify-end whole-stars partial-star empty-stars])]]
       [:div.proxima.content-3.col-11-on-dt.mx-auto.ellipsis-5
        {:ref (c/use-ref this (str "slide-" review-id))}
        [:span.line-height-4
         {:style {:overflow-wrap "break-word"}}
         review-content]]
       [:div.mt2.content-3.col-11-on-dt.mx-auto
        (when overflow?
          [:a.flex.items-center.underline.black.bold
           (apply utils/route-to target)
           "Show more"
           (ui/forward-caret {:class "ml1"})])]]))))

(c/defcomponent organism
  [{:reviews/keys [rating cta-target cta-id cta-label id rating-count] :as data} _ _]
  (when id
    (c/html
     [:div
      [:div.border-top.border-cool-gray.my10.col-11.mx-auto.hide-on-mb
       {:key      (str "desktop-" id)
        :id       "desktop-reviews"
        :data-ref "desktop-reviews"}
       [:div.mt5.col-11.mx-auto
        (avg-rating-and-rating-count rating rating-count)
        [:div.grid.grid-cols-2.gap-y-6.gap-x-5
         (c/elements review-card data :reviews/desktop)]
        (when cta-id
          [:div.mx3.my4.mx-auto.col-2.flex.justify-center
           (ui/button-small-underline-primary
            (merge (apply utils/route-to cta-target)
                   {:data-test cta-id})
            cta-label)])]]
      [:div.border-top.border-cool-gray.pt6.mt2.mb8.hide-on-tb-dt
       [:div
        {:key      id
         :id       id
         :data-ref "reviews"}
        (avg-rating-and-rating-count rating rating-count)
        (c/build
         carousel/component
         {}
         {:opts {:settings {:edgePadding          10
                            :preventScrollOnTouch "auto" ; https://github.com/ganlanyuan/tiny-slider/issues/370
                            :nav                  false
                            :controls             true
                            :items                1.1
                            :loop                 false
                            :controls-classes     "hide-on-mb-tb"}
                 :slides   (c/elements review-card data :reviews/reviews)}})
        (when cta-id
          [:div.mx3.my4
           (ui/button-small-secondary
            (merge (apply utils/route-to cta-target)
                   {:data-test cta-id})
            cta-label)])]]])))
