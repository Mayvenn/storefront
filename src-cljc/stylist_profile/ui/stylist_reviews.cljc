(ns stylist-profile.ui.stylist-reviews
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
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

(c/defcomponent organism
  [{:reviews/keys [spinning? cta-target cta-id cta-label id review-count reviews]} _ _]
  (c/html
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
