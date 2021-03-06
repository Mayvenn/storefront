(ns stylist-profile.ui.card
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.components.svg :as svg]))

(defn card-circle-portrait-molecule
  [{:card.circle-portrait/keys [portrait]}]
  [:div.mx2
   (ui/circle-picture {:width "72px"}
                      (ui/square-image portrait 72))])

(defn card-transposed-title-molecule
  [{:card.transposed-title/keys [id primary secondary]}]
  [:div {:data-test id}
   [:div.content-2.proxima secondary]
   [:div.title-2.proxima.shout primary]])

(defn card-star-rating-molecule
  [{:card.star-rating/keys [id value rating-content scroll-anchor]}]
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

(defn card-just-added-molecule
  [{:card.just-added/keys [content id]}]
  (when id
    [:div.pb1.flex
     [:div.content-3.proxima.bold.items-center.flex.border.border-dark-gray.px2
      {:data-test id}
      [:img {:src "https://ucarecdn.com/b0f70f0a-51bf-4369-b6b8-80480b54b6f1/-/format/auto/" :alt "" :width 9 :height 14}]
      [:div.pl1.shout.dark-gray.letter-spacing-1 content]]]))

(defn card-phone-link-molecule
  [{:card.phone-link/keys [target phone-number]}]
  (when (and target phone-number)
    (ui/link :link/phone
             :a.inherit-color.proxima.content-2
             {:data-test "stylist-phone"
              :class     "block mt1 flex items-center"
              :on-click  (apply utils/send-event-callback target)}
             (svg/phone {:style {:width  "15px"
                                 :height "15px"}
                         :class "mr1"})
             phone-number)))

(defn share-icon-molecule
  [share-icon-data]
  [:div.flex.items-top.justify-center.mr2.col-1
   (ui/navigator-share share-icon-data)])

(c/defcomponent organism
  [data _ _]
  [:div.flex.bg-white.rounded.p2
   (card-circle-portrait-molecule data)
   [:div.left-align.ml2
    (card-transposed-title-molecule data)
    (card-star-rating-molecule data)
    (card-just-added-molecule data)
    (card-phone-link-molecule data)]
   (share-icon-molecule data)])
