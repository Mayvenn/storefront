(ns checkout.free-install
  (:require
   api.orders
   api.current
   api.products
   clojure.set
   [storefront.component :as component :refer [defcomponent]]
   [storefront.components.ui :as ui]
   storefront.utils
   [adventure.components.layered :as layered]))

(defcomponent component
  [data _ _]
  [:div.container.p2
   {:style {:max-width "580px"}}
   [:div.title-2.canela.center.mt6.mb3
    "Your Install Is On Us"]
   [:div.center.col-11.mx-auto
    "You qualify for our complimentary Mayvenn Install service. No catch - it's free."
    (ui/button-small-underline-primary {:class "ml1"} "LEARN MORE")]
   (component/build layered/shop-framed-checklist {:header/value "What's included?"
                                                   :bullets ["Shampoo" "Braid down" "Sew-in and style" "Paid for by Mayvenn"]
                                                   :divider-img nil})
   [:div.flex.flex-column.items-center
    (ui/button-medium-primary {:class "mb3" :style {:width "275px"}} "Add My Free Install")
    (ui/button-medium-underline-primary {:class "mb6"} "Skip & continue")]])

(defn query [state]
  {})

(defn ^:export built-component [data opts]
  (component/build component (query data) opts))
