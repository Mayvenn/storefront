(ns adventure.stylist-matching.match-success-post-purchase
  (:require [adventure.stylist-matching.stylist-results :as stylist-results]
            [adventure.stylist-matching.stylist-detail-line :as stylist-detail-line]
            [adventure.keypaths :as adv-keypaths]
            [storefront.component :as component]
            #?(:cljs
               [storefront.components.formatters :as formatters])
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [clojure.string :as string]))

(defn stylist-card [{:keys [firstname
                            lastname
                            city
                            state
                            rating
                            portrait
                            name
                            stylist-detail-line-data] :as card-data}]
  [:div.flex.bg-white.px1.my4.mxn2.rounded.py3
   [:div.mr2 (ui/circle-picture {:width "104px"} portrait)]
   [:div.flex-grow-1.left-align.dark-gray.h7.line-height-4
    [:div.h3.black.line-height-1 (str firstname " " lastname)]
    [:div.pyp2 (ui/star-rating rating)]
    [:div.bold (str city ", " state)]
    [:div name]
    (stylist-detail-line/component stylist-detail-line-data)]])

(defn stylist-card-query [data]
  (let [servicing-stylist (get-in data adv-keypaths/adventure-servicing-stylist)
        address           (:address servicing-stylist)
        salon             (:salon servicing-stylist)]
    {:firstname                (:firstname address)
     :lastname                 (:lastname address)
     :city                     (:city salon)
     :state                    (:state salon)
     :rating                   (:rating servicing-stylist)
     :portrait                 (-> servicing-stylist
                                   :portrait
                                   :resizable-url)
     :name                     (:name salon)
     :stylist-detail-line-data (stylist-detail-line/query servicing-stylist)}))

(defn chat-with-your-stylist
  [phone-number servicing-stylist-first-name]
  [:div {:data-test "matched-with-stylist"}
   [:div.py4.h3.bold
    "Chat with your Stylist"]
   [:div.h5.line-height-3.center
    "A group text message will be sent to "
    (if phone-number
      [:span.bold.nowrap #?(:cljs (formatters/phone-number phone-number))]
      "you")
    " and your stylist, "
    [:span.nowrap {:data-test "servicing-stylist-firstname"}
     servicing-stylist-first-name]
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
  [{:keys [servicing-stylist-first-name stylist-card-data phone-number]} _ _]
  (component/create
   [:div.bg-lavender.white {:style {:min-height "95vh"}}
    [:div.border-bottom.border-gray.flex.items-center
     [:div.bg-white.flex-auto.py3 (ui/clickable-logo
                          {:data-test "header-logo"
                           :height    "40px"})]]
    (ui/narrow-container
     [:div.center
      [:div.col-11.mx-auto.py4
       (chat-with-your-stylist phone-number servicing-stylist-first-name)
       (stylist-card stylist-card-data)
       get-inspired-cta]])]))

(defn query
  [data]
  (let [servicing-stylist (get-in data adv-keypaths/adventure-servicing-stylist)]
    {:servicing-stylist-first-name (-> servicing-stylist :address :firstname)
     :stylist-card-data            (stylist-card-query data)
     :stylist-detail-line-data     (stylist-detail-line/query servicing-stylist)
     :phone-number                 (-> data
                                       (get-in keypaths/completed-order)
                                       :shipping-address
                                       :phone)}))

(defn built-component
  [data opts]
  (component/build component (query data) opts))
