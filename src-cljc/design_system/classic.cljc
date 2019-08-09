(ns design-system.classic
  (:require [catalog.product-details :as product-details]
            [design-system.organisms :as organisms]
            [design-system.molecules :as molecules]
            [catalog.ui.molecules :as ui-molecules]
            [catalog.ui.add-to-cart :as add-to-cart]
            [catalog.ui.freeinstall-banner :as freeinstall-banner]
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
     :cta/id                                    "add-to-cart"
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
     :freeinstall-add-to-cart-block/icon        "d7fbb4a1-6ad7-4122-b737-ade7dec8dfd3"
     :freeinstall-add-to-cart-block/show?       true}}

   {:organism/label     :freeinstall-banner
    :organism/component freeinstall-banner/organism
    :organism/query
    {:freeinstall-banner/title          "Buy 3 items and we'll pay for your hair install"
     :freeinstall-banner/subtitle       [:span "Choose any " [:span.bold "Mayvenn stylist"] " in your area"]
     :freeinstall-banner/button-copy    "browse stylists"
     :freeinstall-banner/nav-event      [utils/noop-callback]
     :freeinstall-banner/image-ucare-id "f4c760b8-c240-4b31-b98d-b953d152eaa5"
     :freeinstall-banner/show?          true}}])

(def molecules
  [{:molecule/label     :product-description
    :molecule/component ui-molecules/product-description
    :molecule/query
    #:product-description {:summary                   []
                           :hair-family               "bundles"
                           :description               ["Virgin human hair,  machine-wefted and backed by our 30 Day Quality Guarantee,  our sleek straight bundles have no curl,  no wave and are smooth from root to tip."],
                           :materials                 nil
                           :colors                    "Natural Black"
                           :weights                   "3.5oz"
                           :stylist-exclusives-family nil
                           :learn-more-nav-event      nil}}])



(defn component
  [data owner opts]
  (component/create
   [:div.py3
    [:div.h1 "Classic Template"]
    [:section
     [:div.h2 "Organisms"]
     [:section.p4
      (organisms/demo organisms (:organisms data))]]
    [:section
     [:div.h2 "Molecules"]
     [:section.p4
      (molecules/demo molecules)]]]))

(defn built-component
  [{:keys [design-system]} opts]
  (component/build component design-system nil))

(defmethod effects/perform-effects events/navigate-design-system-classic
  [_ _ _ _ _]
  #?(:cljs (do
             (reviews/insert-reviews)
             ;; hack to unhack the fact that reviews expect two instances of reviews
             (js/setTimeout #(reviews/start) 2000))))

