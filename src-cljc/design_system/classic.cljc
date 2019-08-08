(ns design-system.classic
  (:require [catalog.product-details :as product-details]
            [design-system.organisms :as organisms]
            [catalog.ui.add-to-cart :as add-to-cart]
            [popup.organisms :as popup]
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            #?(:cljs [storefront.hooks.reviews :as reviews])
            [storefront.platform.component-utils :as utils]))

(def nowhere events/navigate-design-system-adventure)

(def organisms
  [{:organism/label     :popup
    :organism/component popup/organism
    :organism/popup?    true
    :organism/query
    {:modal-close/event             organisms/dismiss
     :pre-title/content             [:h7 "Pre-title"]
     :monstrous-title/copy          ["Monstrous" "Title"]
     :subtitle/copy                 "Subtitle"
     :description/copy              ["Description"]
     :single-field-form/callback    (utils/fake-href organisms/dismiss)
     :single-field-form/field-data  {:errors    nil
                                     :keypath   nil
                                     :focused   false
                                     :label     "Placeholder"
                                     :name      "textfield"
                                     :type      "textfield"
                                     :value     ""
                                     :data-test "textfield-input"}
     :single-field-form/button-data {:title        "Submit"
                                     :color-kw     :color/teal
                                     :height-class :large
                                     :data-test    "email-input-submit"}}}

   {:organism/label     :product-details
    :organism/component product-details/organism
    :organism/query
    {:title/primary                       "A product title"
     :yotpo-reviews-summary/product-title "A product title"
     :yotpo-reviews-summary/product-id    80
     :yotpo-reviews-summary/data-url      "/products/9-brazilian-straight-bundles"
     :price-block/primary                 108
     :price-block/secondary               "per item"}}

   {:organism/label     :add-to-cart
    :organism/component add-to-cart/organism
    :organism/query
    {:title/primary                             "Add to cart"
     :cta/id                                    "add-to-bag"
     :cta/label                                 "Add to Cart"
     :cta/target                                [events/control-add-sku-to-bag {:sku      "MBW10"
                                                                                :quantity 1}]
     :cta/disabled?                             false
     :cta/spinning?                             false
     :quadpay/loaded?                           true
     :quadpay/price                             108
     :freeinstall-add-to-cart-block/message     "Save 10% & get a free Mayvenn Install when you purchase 3 bundles, closure, or frontals.* "
     :freeinstall-add-to-cart-block/footnote    "*Mayvenn Install cannot be combined with other promo codes."
     :freeinstall-add-to-cart-block/link-target [events/popup-show-adventure-free-install]
     :freeinstall-add-to-cart-block/link-label  "Learn more"
     :freeinstall-add-to-cart-block/icon        "d7fbb4a1-6ad7-4122-b737-ade7dec8dfd3"}}])

(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Classic Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section.p4
      (organisms/demo organisms (:organisms data))]]]))

(defn built-component
  [{:keys [design-system]} opts]
  (component/build component design-system nil))

(defmethod effects/perform-effects events/navigate-design-system-classic
  [_ _ _ _ _]
  #?(:cljs (do
             (reviews/insert-reviews)
             ;; hack to unhack the fact that reviews expect two instances of reviews
             (js/setTimeout #(reviews/start) 2000))))

