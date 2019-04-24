(ns adventure.stylist-matching.match-success-post-purchase
  (:require [adventure.stylist-matching.stylist-detail-line :as stylist-detail-line]
            [adventure.keypaths :as adv-keypaths]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
            [adventure.components.checkout-header-info-card :as checkout-header-info-card]
            [adventure.components.profile-card :as profile-card]
            [storefront.keypaths :as keypaths]
            [storefront.components.ui :as ui]
            [spice.date :as date]
            [spice.maps :as maps]))

(defn query
  [data]
  (let [servicing-stylist   (get-in data adv-keypaths/adventure-servicing-stylist)
        phone               (-> data
                                (get-in keypaths/completed-order)
                                :shipping-address
                                :phone)]
    {:title        "Chat with your Stylist"
     :subtitle     [:div.h5.line-height-3.center
                    "A group text message will be sent to "
                    (if phone
                      [:span.bold.nowrap (formatters/phone-number phone)]
                      "you")
                    " and your stylist, "
                    [:span.nowrap {:data-test "servicing-stylist-name"}
                     (-> servicing-stylist :address :firstname)]
                    "."]
     :card-data    (profile-card/stylist-profile-card-data servicing-stylist)
     :cta-title    "In the meantime…"
     :cta-subtitle "Get inspired for your appointment"
     :button       {:href "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                    :text "View #MayvennFreeInstall"}
     :data-test    "matched-with-stylist"}))

(defn built-component
  [data opts]
  (component/build checkout-header-info-card/component (query data) nil))
