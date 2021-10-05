(ns stylist-profile.ui-v2021-10.stylist-reviews
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
  [:div.flex.items-baseline
   [:div.flex.items-center.mr1
    (svg/symbolic->html [:svg/whole-star {:class "fill-p-color mr1"
                                          :style {:height "0.8em"
                                                  :width  "0.8em"}}])
    [:div.proxima.title-1.p-color rating " • "]]
   [:div review-count " Ratings"]])

(defn slide [{:keys [review-id install-type review-content reviewer-name review-date target]}]
  [:div.border.border-cool-gray.rounded.p2.mx1.proxima
   {:key       review-id
    :data-test (str "review-" review-id)}
   [:div.mb3
    ;; Image will go here
    [:div.flex.justify-between.items-baseline
     [:div.title-3.proxima reviewer-name]
     [:div.proxima.content-4.dark-gray review-date]]
    [:div.proxima.content-4 (get install-type->display-name install-type)]]
   [:p.proxima.content-2.ellipsis-5 review-content]
   [:div.mt2.flex.items-center [:a {} #_(apply utils/route-to target) "Show more "] (ui/forward-caret {})]])

(c/defcomponent organism
  [{:reviews/keys [rating cta-target cta-id cta-label id review-count reviews]} _ _]
  (c/html
   (when id
     [:div.m2
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
                          :controls             false
                          :items                1
                          :loop                 false}
               :slides   (map slide reviews)}})
      (when cta-id
        (ui/button-medium-underline-primary
         {:on-click (apply utils/send-event-callback cta-target)} cta-label))])))
