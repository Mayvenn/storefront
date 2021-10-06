(ns stylist-profile.ui-v2021-10.stylist-reviews-cards
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

(defn avg-rating-and-review-count [rating review-count]
  [:div.flex.items-center.mx3.my2
   [:div.flex.items-center.mr1
    (svg/symbolic->html [:svg/whole-star {:class "fill-p-color mr1"
                                          :style {:height "0.8em"
                                                  :width  "0.9em"}}])
    [:div.proxima.title-1.p-color rating " • "]]
   [:div review-count " Ratings"]])

(defn review-card [ix {:keys [review-id install-type stars review-content reviewer-name review-date target]}]
  [:div.border.border-cool-gray.rounded.p2.mx1.proxima
   {:key       review-id
    :data-test (str "review-" ix)}
   [:div.mb3
    ;; User portrait will go here
    [:div.grid.x2x2.gap-0
     [:div.title-3.proxima reviewer-name]
     (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars stars "13px" {})]
       [:div.flex.justify-end whole-stars partial-star empty-stars])
     [:div.proxima.content-4.dark-gray review-date]
     [:div.proxima.content-4.right-align (get install-type->display-name install-type)]]]
   [:div.proxima.content-3.col-11-on-dt.mx-auto
    [:div {:id    (str "review-" ix "-content")
           :class (if target
                    "ellipsis-5"
                    "ellipsis-15")} review-content]]
   [:div.mt2.content-3
    {:id (str "review-" ix "-content-more")}
    (when target
      [:a.flex.items-center.underline (apply utils/route-to target)
       "Show more"
       (ui/forward-caret {:class "ml1"})])]])

(c/defcomponent organism
  [{:reviews/keys [rating cta-target cta-id cta-label id review-count reviews]} _ _]
  (c/html
   (when id
     (prn cta-target)
     [:div.mx2.mt2.mb8
      {:key      id
       :id       "reviews"
       :data-ref "reviews"}
      (avg-rating-and-review-count rating review-count)
      (c/build
       carousel/component
       {}
       {:opts {:settings {:edgePadding          10
                          :preventScrollOnTouch "auto" ; https://github.com/ganlanyuan/tiny-slider/issues/370
                          :nav                  false
                          :controls             true
                          :items                1
                          :loop                 false
                          :controls-classes     "hide-on-mb-tb"}
               :slides   (map-indexed review-card reviews)}})
      (when cta-id
        [:div.mx3.my4
         (ui/button-small-secondary
          (merge (apply utils/route-to cta-target)
                 {:data-test cta-id})
          cta-label)])])))
