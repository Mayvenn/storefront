(ns checkout.ui.stylist-cart-item
  (:require
   [storefront.routes :as routes]
   [storefront.component :as c]
   [storefront.components.svg :as svg]
   [storefront.components.ui :as ui]
   [storefront.events :as e]
   [storefront.platform.component-utils :as utils]
   [ui.molecules :as M]))

(defn ^:private stylist-cart-item-image-molecule
  [{:stylist-cart-item.image/keys [image-url]}]
  (->> (ui/square-image {:resizable-url image-url} 50)
       (ui/circle-picture {:width 50})))

(defn ^:private stylist-cart-item-action-molecule
  [{:stylist-cart-item.action/keys [target id]}]
  (when (and id target)
    [:a.block.gray.medium.m1
     {:data-test id
      :href      (routes/path-for e/navigate-adventure-find-your-stylist)
      :on-click  (apply utils/send-event-callback target)}
     (svg/swap-arrows {:width  "16px"
                       :height "20px"})]))

(defn ^:private stylist-cart-item-title-molecule
  [{:stylist-cart-item.title/keys [primary secondary]}]
  [:div
   [:div.content-2.proxima.flex.justify-between
    primary]
   [:div.content-3.proxima
    secondary]])

(defn ^:private stylist-cart-item-link-molecule
  [{:stylist-cart-item.link/keys [id label target]}]
  (when id
    [:div.mt1
     (ui/button-small-underline-primary
      (->> target
           (apply utils/fake-href)
           (merge {:data-test id}))
      label)]))

(defn ^:private stylist-cart-item-rating-molecule
  [{:stylist-cart-item.rating/keys [value]}]
  [:div.mt1
   (M/stars-rating-molecule
    {:rating/value value})])

(c/defcomponent organism
  [data _ _]
  [:div.flex.bg-white.pl3.pr3.py2
   [:div.flex.flex-grow-1.items-center
    (stylist-cart-item-image-molecule data)
    [:div.ml3
     (stylist-cart-item-title-molecule data)
     (stylist-cart-item-link-molecule data)
     (stylist-cart-item-rating-molecule data)]]
   (stylist-cart-item-action-molecule data)])
