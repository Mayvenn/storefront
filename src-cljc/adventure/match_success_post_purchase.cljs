(ns adventure.match-success-post-purchase
  (:require [adventure.stylist-results :as stylist-results]
            [adventure.keypaths :as adv-keypaths]
            [storefront.component :as component]
            [storefront.components.formatters :as formatters]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]))

(defn stylist-card [servicing-stylist]
  (let [firstname (-> servicing-stylist
                      :address
                      :firstname)
        lastname  (-> servicing-stylist
                      :address
                      :lastname)
        city      (-> servicing-stylist
                      :salon
                      :city)
        state     (-> servicing-stylist
                      :salon
                      :state)
        rating    (:rating servicing-stylist)
        portrait  (-> servicing-stylist
                      :portrait
                      :resizable-url)
        name      (-> servicing-stylist
                      :salon
                      :name)]
    [:div.flex.bg-white.px1.my4.mxn2.rounded.py3
     [:div.mr2 (ui/circle-picture {:width "104px"} portrait)]
     [:div.flex-grow-1.left-align.dark-gray.h7.line-height-4
      [:div.h3.black.line-height-1 (clojure.string/join  " " [firstname lastname])]
      [:div.pyp2 (ui/star-rating rating)]
      [:div.bold (str city ", " state)]
      [:div name]
      (stylist-results/stylist-detail-line servicing-stylist)]]))

(defn chat-with-your-stylist
  [phone-number servicing-stylist]
  [:div {:data-test "matched-with-stylist"}
   [:div.py4.h3.bold
    "Chat with your Stylist"]
   [:div.h5.line-height-3.center
    "A group text message will be sent to "
    [:span.bold.nowrap (formatters/phone-number phone-number)]
    " and your stylist, "
    [:span.nowrap {:data-test "servicing-stylist-firstname"}
     (-> servicing-stylist :address :firstname)]
    "."]])

(def get-inspired-cta
  [:div.py2
   [:h3.bold "In the meantime…"]
   [:h4.py2 "Get inspired for your appointment"]
   [:div.py2
    (ui/teal-button {:href  "https://www.instagram.com/explore/tags/mayvennfreeinstall/"
                     :class "bold"}
                    "View #MayvennFreeInstall")]])

(defn component
  [{:keys [servicing-stylist phone-number]} _ _]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    [:div.border-bottom.border-gray.flex.items-center
     [:div.bg-white.flex-auto.py3 (ui/clickable-logo
                          {:data-test "header-logo"
                           :height    "40px"})]]
    (ui/narrow-container
     [:div.center
      [:div.col-11.mx-auto.py4
       (chat-with-your-stylist phone-number servicing-stylist)
       (stylist-card servicing-stylist)
       get-inspired-cta]])]))

(defn query
  [data]
  {:servicing-stylist (get-in data adv-keypaths/adventure-servicing-stylist)
   :phone-number      (-> data
                          (get-in keypaths/completed-order)
                          :shipping-address
                          :phone)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))
