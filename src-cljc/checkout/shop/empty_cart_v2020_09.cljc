(ns checkout.shop.empty-cart-v2020-09
  (:require #?@(:cljs [[storefront.components.popup :as popup]])
            [checkout.header :as header]
            [storefront.accessors.experiments :as experiments]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.flash :as flash]
            [storefront.components.footer :as storefront.footer]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [ui.molecules :as ui-molecules]
            [ui.promo-banner :as promo-banner]))

(def ^:private mayvenn-install-category
  {:page/slug           "mayvenn-install"
   :catalog/category-id "23"})

;;; -------------------------------

(defn empty-cta-molecule
  [{:cta/keys [id label target]}]
  (when (and id label target)
    (ui/button-large-primary
     (merge {:data-test id} (apply utils/route-to target))
     [:div.flex.items-center.justify-center.inherit-color label])))

(defn empty-cart-body-molecule
  [{:empty-cart-body/keys [id primary secondary image-id]}]
  (when id
    [:div {:style {:margin-top "70px"}}
     [:h1.canela.title-1.mb4 primary]
     [:div.col-8.mx-auto.mt2.mb6 secondary]]))

(defcomponent template
  [{:keys [header footer popup promo-banner flash cart return-link nav-event]} _ _]
  [:div.flex.flex-column.stretch {:style {:margin-bottom "-1px"}}
   #?(:cljs (popup/built-component popup nil))

   (when promo-banner
     (promo-banner/built-static-organism promo-banner nil))

   (header/built-component header nil)
   [:div.relative.flex.flex-column.flex-auto
    (flash/built-component flash nil)

    [:main.bg-white.flex-auto
     {:data-test (keypaths/->component-str nav-event)}
     [:div
      [:div.hide-on-tb-dt
       [:div.border-bottom.border-gray.border-width-1.m-auto.col-7-on-dt
        [:div.px2.my2 (ui-molecules/return-link return-link)]]]
      [:div.hide-on-mb
       [:div.m-auto.container
        [:div.px2.my2 (ui-molecules/return-link return-link)]]]
      [:div.col-7-on-dt.mx-auto
       (ui/narrow-container
        [:div
         [:div.center {:data-test "empty-cart"}
          (empty-cart-body-molecule cart)
          [:div.col-9.mx-auto
           (empty-cta-molecule cart)]]])]]]

    [:footer
     (storefront.footer/built-component footer nil)]]])

(def return-link<-
  {:return-link/id            "start-shopping"
   :return-link/copy          "Start Shopping"
   :return-link/event-message [events/navigate-category
                               mayvenn-install-category]})

(def empty-cart<-
  {:empty-cart-body/id        "empty-cart-body"
   :empty-cart-body/primary   "Your Bag is Empty"
   :empty-cart-body/secondary (str "Did you know that free Mayvenn Services"
                                   " are included with qualifying purchases?")
   :empty-cart-body/image-id  "6146f2fe-27ed-4278-87b0-7dc46f344c8c"
   :cta/label                 "Start Hair Quiz"
   :cta/id                    "homepage-take-hair-quiz"
   :cta/target                [events/navigate-shopping-quiz-unified-freeinstall-intro {:query-params {:location "empty_cart"}}]})

(def empty-cart-remove-freeinstall<-
  {:empty-cart-body/id        "empty-cart-body"
   :empty-cart-body/primary   "Your Bag is Empty"
   :empty-cart-body/image-id  "6146f2fe-27ed-4278-87b0-7dc46f344c8c"})

(defn ^:export page
  [app-state nav-event]
  (component/build template
                   {:promo-banner app-state
                    :cart         (if (:remove-free-install (get-in app-state keypaths/features))
                                    empty-cart-remove-freeinstall<-
                                    empty-cart<-)
                    :return-link  return-link<-
                    :header       app-state
                    :footer       app-state
                    :popup        app-state
                    :flash        app-state
                    :nav-event    nav-event}))
